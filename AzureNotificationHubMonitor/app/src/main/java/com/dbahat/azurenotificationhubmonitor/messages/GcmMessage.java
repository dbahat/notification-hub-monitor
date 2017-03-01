package com.dbahat.azurenotificationhubmonitor.messages;

/**
 * An Gcm message, used for serialization
 */
public class GcmMessage {
	private Data data;
	private String priority;

	public GcmMessage setData(Data data) {
		this.data = data;
		return this;
	}

	public GcmMessage setPriority(String priority) {
		this.priority = priority;
		return this;
	}

	public static class Data {
		private String category;
		private String message;

		public Data setCategory(String category) {
			this.category = category;
			return this;
		}

		public Data setMessage(String message) {
			this.message = message;
			return this;
		}
	}
}
