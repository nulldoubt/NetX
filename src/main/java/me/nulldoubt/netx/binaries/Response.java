package me.nulldoubt.netx.binaries;

import java.io.Serializable;
import java.util.UUID;

public abstract class Response implements Serializable {
	
	@java.io.Serial
	private static final long serialVersionUID = 5146726380725690696L;
	
	private final UUID uuid;
	private final UUID sender;
	private final int responseId;
	
	public Response(final Request request) {
		uuid = request.getUUID();
		sender = request.getSender();
		responseId = request.getRequestId();
	}
	
	public final UUID getUUID() {
		return uuid;
	}
	
	public final UUID getSender() {
		return sender;
	}
	
	public final int getResponseId() {
		return responseId;
	}
	
}
