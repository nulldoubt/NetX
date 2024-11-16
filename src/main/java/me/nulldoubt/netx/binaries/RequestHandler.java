package me.nulldoubt.netx.binaries;

public abstract class RequestHandler {
	
	private final int requestId;
	
	public RequestHandler(final int requestId) {
		this.requestId = requestId;
	}
	
	public abstract Response handle(final Request request);
	
	public final int getRequestId() {
		return requestId;
	}
	
}
