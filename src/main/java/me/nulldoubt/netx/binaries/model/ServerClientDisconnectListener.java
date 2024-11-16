package me.nulldoubt.netx.binaries.model;

import java.util.UUID;

@FunctionalInterface
public interface ServerClientDisconnectListener {
	
	public void onClientDisconnect(final UUID handler);
	
}
