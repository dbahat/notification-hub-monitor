package com.dbahat.azurenotificationhubmonitor;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Retrieves the number of registrations per platform
 */
class RegistrationsPerTagRetriever {
	private Registrations totalRegistrations = new Registrations();
	private RequestQueue requestQueue;

	// Since the API to get the registrations only returns chunks of 100, keep track of the current batch
	// so we can continue sending requests until getting no more registration results.
	private int currentBatchTotalRegistrations = -1;
	private int currentBatchIndex = 0;

	RegistrationsPerTagRetriever(RequestQueue requestQueue) {
		this.requestQueue = requestQueue;
	}

	void retrieve(final String tag, final Callback callback) {
		if (currentBatchTotalRegistrations != 0) {
			this.getNumberOfRegistrationsForTag(tag, currentBatchIndex * 100, new Callback() {
				public void onComplete(Registrations result) {
					RegistrationsPerTagRetriever.this.currentBatchTotalRegistrations = result.getTotal();
					totalRegistrations.addToTotal(result.getTotal());
					totalRegistrations.addToiOS(result.getiOS());
					totalRegistrations.addToAndroid(result.getAndroid());

					// Continue in recursion until we reach a batch with no more registrations
					currentBatchIndex++;
					RegistrationsPerTagRetriever.this.retrieve(tag, callback);
				}

				public void onError() {
					callback.onError();
				}
			});
		} else {
			callback.onComplete(totalRegistrations);
		}
	}

	private void getNumberOfRegistrationsForTag(String tag, int batch, final Callback callback) {
		final String url = NotificationHubInfo.Endpoint + NotificationHubInfo.Name + "/tags/" + tag + "/registrations?api-version=2015-01&$skip=" + batch;
		StringRequest stringRequest = new StringRequest(url,
				new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						callback.onComplete(Registrations.parse(response));
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e(RegistrationsPerTagRetriever.class.getSimpleName(), "failed to get regisrations", error);
						callback.onError();
					}
				}) {
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				HashMap<String, String> headers = new HashMap<>();
				headers.put("Authorization", AuthTokenGenerator.generate(url));
				return headers;
			}

			@Override
			public RetryPolicy getRetryPolicy() {
				return new DefaultRetryPolicy(90 * 1000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
			}
		};
		requestQueue.add(stringRequest);
	}

	interface Callback {
		void onComplete(Registrations registrations);

		void onError();
	}
}
