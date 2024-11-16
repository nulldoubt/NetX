package me.nulldoubt.netx;

public final class NetX {
	
	public static final String PREFIX;
	public static final int VERSION;
	
	public static final int NETWORK_MIN_PORT;
	public static final int NETWORK_MAX_PORT;
	
	static {
		PREFIX = "[NetX] ";
		VERSION = 1;
		
		NETWORK_MIN_PORT = 1024;
		NETWORK_MAX_PORT = 65535;
	}
	
	public static enum CipherAlgorithm {
		
		CIPHER_AES("AES"),
		CIPHER_Blowfish("Blowfish"),
		CIPHER_ARCFOUR("ARCFOUR"),
		CIPHER_RC2("RC2"),
		CIPHER_RC4("RC4");
		
		private final String algorithm;
		
		CipherAlgorithm(final String algorithm) {
			this.algorithm = algorithm;
		}
		
		public String getAlgorithm() {
			return algorithm;
		}
		
	}
	
	public static enum SignalModifier {
		
		SIGNAL_TYPE((byte) 1),
		SIGNAL_REQUEST((byte) 2),
		SIGNAL_RESPONSE((byte) 4),
		SIGNAL_CLOSE((byte) 8);
		
		private final byte modifier;
		
		SignalModifier(final byte modifier) {
			this.modifier = modifier;
		}
		
		public byte getModifier() {
			return modifier;
		}
		
		public static boolean isType(final byte modifier) {
			return (modifier == SIGNAL_TYPE.getModifier());
		}
		
		public static boolean isRequest(final byte modifier) {
			return (modifier == SIGNAL_REQUEST.getModifier());
		}
		
		public static boolean isResponse(final byte modifier) {
			return (modifier == SIGNAL_RESPONSE.getModifier());
		}
		
		public static boolean isClose(final byte modifier) {
			return (modifier == SIGNAL_CLOSE.getModifier());
		}
		
	}
	
	public static final class SignalHolder {
		
		private final byte signalModifier;
		private final byte[] buffer;
		
		public SignalHolder(final byte signalModifier, final byte[] buffer) {
			this.signalModifier = signalModifier;
			this.buffer = buffer;
		}
		
		public byte getSignalModifier() {
			return signalModifier;
		}
		
		public byte[] getBuffer() {
			return buffer;
		}
		
	}
	
}
