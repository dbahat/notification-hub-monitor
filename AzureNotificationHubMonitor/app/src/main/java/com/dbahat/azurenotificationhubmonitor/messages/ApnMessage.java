package com.dbahat.azurenotificationhubmonitor.messages;

/**
 * An Apn message, used for serialization
 */
public class ApnMessage {
	private Aps aps;
	private String category;

	public ApnMessage setApns(Aps apns) {
		this.aps = apns;
		return this;
	}

	public ApnMessage setCategory(String var1) {
		this.category = var1;
		return this;
	}

	public static class Aps {
		private String alert;
		private String sound;

		public Aps() {
		}

		public Aps setAlert(String alert) {
			this.alert = alert;
			return this;
		}

		public Aps setSound(String sound) {
			this.sound = sound;
			return this;
		}
	}
}
