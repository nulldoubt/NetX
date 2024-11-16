package me.nulldoubt.netx.binaries.model;

import java.util.UUID;

@FunctionalInterface
public interface ServerClientConnectListener {
	
	public void onClientConnect(final UUID handler);
	
}
