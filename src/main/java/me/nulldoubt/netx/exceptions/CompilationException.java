package me.nulldoubt.netx.exceptions;

public final class CompilationException extends NetworkException {
	
	private static final long serialVersionUID = -3087109841152725431L;
	
	public CompilationException() {
		super();
	}
	
	public CompilationException(final String message) {
		super(message);
	}
	
	public CompilationException(final Exception e) {
		super(e);
	}
	
}
