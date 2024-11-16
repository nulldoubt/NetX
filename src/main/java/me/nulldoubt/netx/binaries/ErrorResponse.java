package me.nulldoubt.netx.binaries;

public final class ErrorResponse extends Response {
	
	@java.io.Serial
	private static final long serialVersionUID = 3599995737481779704L;
	
	private final String error;
	
	public ErrorResponse(final String error, final Request request) {
		super(request);
		this.error = error;
	}
	
	public String getError() {
		return error;
	}
	
}
