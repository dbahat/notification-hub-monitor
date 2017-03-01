package com.dbahat.azurenotificationhubmonitor;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dbahat.azurenotificationhubmonitor.messages.ApnMessage;
import com.dbahat.azurenotificationhubmonitor.messages.GcmMessage;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.dbahat.azurenotificationhubmonitor.NotificationHubInfo.ApiUrl;

/**
 * Small example app for communicating with the Azure Notification Hub.
 * Allows fetching the number of registered hub devices per tag, as well as sending push notifications
 * to specific tags.
 */
public class MainActivity extends AppCompatActivity {

	private RequestQueue requestQueue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		requestQueue = Volley.newRequestQueue(this);

		final TextView remainingCharsCountView = (TextView) findViewById(R.id.remainingCharsCount);
		final EditText editText = (EditText) findViewById(R.id.editText);

		// Count the remaining characters as the user types the message
		if (editText != null && remainingCharsCountView != null) {
			editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

				}

				@Override
				public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

				}

				@Override
				public void afterTextChanged(Editable editable) {
					remainingCharsCountView.setText(String.format(Locale.US, "%d/1900", editText.getText().length()));
				}
			});
		}
	}

	public void getRegistrationsPerCategoryOnClick(View caller) {
		int checkedRadioButtonId = ((RadioGroup) this.findViewById(R.id.radio_group)).getCheckedRadioButtonId();
		if (checkedRadioButtonId == -1) {
			Toast.makeText(this, R.string.select_tag, Toast.LENGTH_SHORT).show();
			return;
		}

		String tag = getTagById(checkedRadioButtonId);
		Toast.makeText(this, R.string.check_in_progress, Toast.LENGTH_SHORT).show();
		new RegistrationsPerTagRetriever(requestQueue).retrieve(tag, new RegistrationsPerTagRetriever.Callback() {
			public void onComplete(Registrations registrations) {
				Toast.makeText(MainActivity.this, registrations.toString(), Toast.LENGTH_SHORT).show();
			}

			public void onError() {
				Toast.makeText(MainActivity.this, R.string.check_failed, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void sendAndroidNotificationButtonOnClick(View view) {
		showAreYouSureDialogAndSend(DialogType.Android, this::sendGcmRequest);
	}

	public void sendiOSNotificationButtonOnClick(View view) {
		showAreYouSureDialogAndSend(DialogType.iOS, this::sendApnRequest);
	}

	public void sendBothNotificationButtonOnClick(View view) {
		showAreYouSureDialogAndSend(DialogType.Both, tag -> {
			sendApnRequest(tag);
			sendGcmRequest(tag);
		});
	}

	private void showAreYouSureDialogAndSend(final DialogType dialogType, final SendNotificationTask sendNotificationTask) {
		int checkedRadioButtonId = ((RadioGroup) findViewById(R.id.radio_group)).getCheckedRadioButtonId();
		if (checkedRadioButtonId == -1) {
			Toast.makeText(this, R.string.select_tag, Toast.LENGTH_SHORT).show();
		} else {
			String messageText = ((EditText) this.findViewById(R.id.editText)).getText().toString();
			if (TextUtils.isEmpty(messageText)) {
				Toast.makeText(this, R.string.error_missing_message, Toast.LENGTH_SHORT).show();
			} else if (messageText.length() > 1900) {
				Toast.makeText(this, R.string.error_message_too_long, Toast.LENGTH_SHORT).show();
			} else {
				final String tag = getTagById(checkedRadioButtonId);
				Toast.makeText(this, R.string.send_in_progress, Toast.LENGTH_SHORT).show();
				new RegistrationsPerTagRetriever(requestQueue).retrieve(tag, new RegistrationsPerTagRetriever.Callback() {
					public void onComplete(Registrations registrations) {
						int numberOfDevices = dialogType == DialogType.Both ? registrations.getTotal()
								: dialogType == DialogType.iOS ? registrations.getiOS()
								: registrations.getAndroid();
						new AlertDialog.Builder(MainActivity.this)
								.setTitle(getString(R.string.message_in_tag, tag))
								.setMessage(getString(R.string.message_send_to_count, numberOfDevices))
								.setPositiveButton(R.string.send, (dialog, which) -> sendNotificationTask.run(tag))
								.setNegativeButton(R.string.cancel, null)
								.create()
								.show();
					}

					public void onError() {
						Toast.makeText(MainActivity.this, R.string.send_failed, Toast.LENGTH_LONG).show();
					}
				});
			}
		}
	}

	private String getTagById(int id) {
		return findViewById(id).getTag().toString();
	}

	private void sendApnRequest(final String tag) {
		EditText editText = (EditText) findViewById(R.id.editText);
		String message = editText.getText().toString();
		final String serializedMessage = new Gson().toJson(
				new ApnMessage()
						.setApns(new ApnMessage.Aps()
								.setAlert(message)
								.setSound("default"))
						.setCategory(tag)
		);

		StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiUrl,
				response -> Toast.makeText(MainActivity.this, R.string.send_success, Toast.LENGTH_SHORT).show(),
				error -> Toast.makeText(MainActivity.this, R.string.send_failed_ios_passed_android, Toast.LENGTH_SHORT).show()) {

			@Override
			public byte[] getBody() throws AuthFailureError {
				try {
					return serializedMessage.getBytes("UTF-8");
				} catch (UnsupportedEncodingException ex) {
					throw new RuntimeException(ex);
				}
			}

			@Override
			public String getBodyContentType() {
				return "application/json;charset=utf-8";
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				HashMap<String, String> headers = new HashMap<>();
				headers.put("Authorization", AuthTokenGenerator.generate(ApiUrl));
				headers.put("ServiceBusNotification-Format", "apple");
				headers.put("ServiceBusNotification-Tags", tag);
				headers.put("ServiceBusNotification-Apns-Expiry", "2016-10-21T20:00+02:00");
				return headers;
			}
		};
		requestQueue.add(stringRequest);
	}

	private void sendGcmRequest(final String tag) {
		String message = ((EditText) this.findViewById(R.id.editText)).getText().toString();
		final String serializedMessage = new Gson().toJson(
				new GcmMessage()
						.setPriority("high")
						.setData(new GcmMessage.Data()
								.setMessage(message)
								.setCategory(tag)));
		Toast.makeText(this, R.string.send_in_progress, Toast.LENGTH_SHORT).show();

		StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiUrl,
				response -> Toast.makeText(MainActivity.this, R.string.send_success, Toast.LENGTH_SHORT).show(),
				error -> Toast.makeText(MainActivity.this, R.string.send_failed, Toast.LENGTH_SHORT).show()) {

			@Override
			public byte[] getBody() throws AuthFailureError {
				try {
					return serializedMessage.getBytes("UTF-8");
				} catch (UnsupportedEncodingException var2x) {
					throw new RuntimeException(var2x);
				}
			}

			@Override
			public String getBodyContentType() {
				return "application/json;charset=utf-8";
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				HashMap<String, String> headers = new HashMap<>();
				headers.put("Authorization", AuthTokenGenerator.generate(ApiUrl));
				headers.put("ServiceBusNotification-Format", "gcm");
				headers.put("ServiceBusNotification-Tags", tag);
				return headers;
			}
		};

		requestQueue.add(stringRequest);
	}

	private enum DialogType {
		iOS,
		Android,
		Both
	}

	private interface SendNotificationTask {
		void run(String tag);
	}
}
