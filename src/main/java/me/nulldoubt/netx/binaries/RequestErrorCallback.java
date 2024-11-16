package me.nulldoubt.netx.binaries;

@FunctionalInterface
public interface RequestErrorCallback {
	
	public Response onError(final ErrorResponse response);
	
}
