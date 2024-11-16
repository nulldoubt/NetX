package me.nulldoubt.netx.binaries;

import java.io.Serializable;
import java.util.UUID;

public abstract class Request implements Serializable {
	
	@java.io.Serial
	private static final long serialVersionUID = -9061714738659069128L;
	
	private UUID sender;
	private final UUID uuid;
	private final int requestId;
	
	public Request(final int requestId) {
		this.uuid = UUID.randomUUID();
		this.requestId = requestId;
	}
	
	public final void setSender(final UUID sender) {
		if (this.sender != null)
			return;
		this.sender = sender;
	}
	
	public final UUID getSender() {
		return sender;
	}
	
	public final UUID getUUID() {
		return uuid;
	}
	
	public final int getRequestId() {
		return requestId;
	}
	
}
