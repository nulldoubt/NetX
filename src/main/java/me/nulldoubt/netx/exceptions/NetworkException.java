package me.nulldoubt.netx.exceptions;

public class NetworkException extends RuntimeException {
	
	private static final long serialVersionUID = -4475113283836137892L;
	
	public NetworkException() {
		super();
	}
	
	public NetworkException(final String message) {
		super(message);
	}
	
	public NetworkException(final Exception e) {
		super(e);
	}
	
}
