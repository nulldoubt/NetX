package me.nulldoubt.netx.binaries.model;

import java.util.UUID;

@FunctionalInterface
public interface PacketReceivedListener<T> {
	
	public void onPacketReceived(final T packet, final UUID sender);
	
}
