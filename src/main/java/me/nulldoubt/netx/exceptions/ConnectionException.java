package me.nulldoubt.netx.exceptions;

public final class ConnectionException extends NetworkException {
	
	private static final long serialVersionUID = -7737342902643389101L;
	
	public ConnectionException() {
		super();
	}
	
	public ConnectionException(final String message) {
		super(message);
	}
	
	public ConnectionException(final Exception e) {
		super(e);
	}
	
}
