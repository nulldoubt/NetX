package me.nulldoubt.netx.exceptions;

public final class BuilderException extends NetworkException {
	
	private static final long serialVersionUID = -6519025101111012292L;
	
	public BuilderException() {
		super();
	}
	
	public BuilderException(final String message) {
		super(message);
	}
	
	public BuilderException(final Exception e) {
		super(e);
	}
	
}
