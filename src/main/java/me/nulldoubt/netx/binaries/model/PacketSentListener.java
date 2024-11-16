package me.nulldoubt.netx.binaries.model;

@FunctionalInterface
public interface PacketSentListener<T> {
	
	public void onPacketSent(final T packet);
	
}
