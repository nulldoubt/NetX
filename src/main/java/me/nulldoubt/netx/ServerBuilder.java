package me.nulldoubt.netx;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import me.nulldoubt.netx.NetX.CipherAlgorithm;
import me.nulldoubt.netx.binaries.RequestHandler;
import me.nulldoubt.netx.binaries.configurations.BufferConfiguration;
import me.nulldoubt.netx.binaries.configurations.SerialConfiguration;
import me.nulldoubt.netx.binaries.configurations.StringConfiguration;
import me.nulldoubt.netx.binaries.model.PacketReceivedListener;
import me.nulldoubt.netx.binaries.model.PacketSentListener;
import me.nulldoubt.netx.binaries.model.SerialObject;
import me.nulldoubt.netx.binaries.model.ServerClientConnectListener;
import me.nulldoubt.netx.binaries.model.ServerClientDisconnectListener;
import me.nulldoubt.netx.exceptions.BuilderException;

public final class ServerBuilder<T> {

	private final Server<T> server;
	
	private boolean algorithm;
	private boolean port;
	
	private ServerBuilder(final Server<T> server) {
		this.server = server;
		algorithm = false;
		port = false;
	}
	
	public boolean isReady() {
		return (algorithm && port);
	}
	
	public PrintStream err() {
		return server.err();
	}
	
	public ServerBuilder<T> err(final PrintStream errorStream) {
		if (errorStream == null)
			throw new NullPointerException("ErrorStream cannot be null");
		server.setErr(errorStream);
		return this;
	}
	
	public CipherAlgorithm algorithm() {
		if (!(algorithm))
			throw new NullPointerException("Algorithm hasn't been set yet");
		return server.getAlgorithm();
	}
	
	public ServerBuilder<T> algorithm(final CipherAlgorithm algorithm) {
		if (algorithm == null)
			throw new NullPointerException("Algorithm cannot be null");
		server.setAlgorithm(algorithm);
		this.algorithm = true;
		return this;
	}
	
	public int port() {
		if (!(port))
			throw new NullPointerException("Port hasn't been set yet");
		return server.getPort();
	}
	
	public ServerBuilder<T> port(final int port) {
		if ((port < NetX.NETWORK_MIN_PORT) || (port > NetX.NETWORK_MAX_PORT))
			throw new NullPointerException("Port has to be in range between %d and %d".formatted(NetX.NETWORK_MIN_PORT, NetX.NETWORK_MAX_PORT));
		server.setPort(port);
		this.port = true;
		return this;
	}
	
	public long timeout() {
		return server.getDefaultTimeout();
	}
	
	public TimeUnit timeUnit() {
		return server.getDefaultTimeUnit();
	}
	
	public ServerBuilder<T> timeout(final long timeout, final TimeUnit timeUnit) {
		if (timeout == 0)
			throw new NullPointerException("Timeout cannot be 0");
		if (timeUnit == null)
			throw new NullPointerException("TimeUnit cannot be null");
		server.setDefaultTimeout(timeout, timeUnit);
		return this;
	}
	
	public ServerBuilder<T> requestHandler(final RequestHandler handler) {
		if (handler == null)
			throw new NullPointerException("Handler cannot be null");
		server.registerRequestHandler(handler);
		return this;
	}
	
	public ServerBuilder<T> requestHandler(final RequestHandler... handlers) {
		for (final RequestHandler handler : handlers)
			requestHandler(handler);
		return this;
	}
	
	public ServerBuilder<T> onPacketReceived(final PacketReceivedListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		server.onPacketReceived(listener);
		return this;
	}
	
	public ServerBuilder<T> onPacketReceived(final long[] key, final PacketReceivedListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		if (key == null)
			throw new NullPointerException("Key cannot be null");
		if (key.length != 1)
			throw new BuilderException("Key has to have a length of 1");
		key[0] = server.onPacketReceived(listener);
		return this;
	}
	
	public ServerBuilder<T> onPacketReceived(final PacketReceivedListener<T>... listeners) {
		for (final PacketReceivedListener<T> listener : listeners)
			onPacketReceived(listener);
		return this;
	}
	
	public ServerBuilder<T> onPacketReceived(final long[] keys, final PacketReceivedListener<T>... listeners) {
		int i = -1;
		for (final PacketReceivedListener<T> listener : listeners) {
			final long[] key = new long[1];
			onPacketReceived(key, listener);
			keys[i++] = key[i];
		}
		return this;
	}
	
	public ServerBuilder<T> onPacketSent(final PacketSentListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		server.onPacketSent(listener);
		return this;
	}
	
	public ServerBuilder<T> onPacketSent(final long[] key, final PacketSentListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		if (key == null)
			throw new NullPointerException("Key cannot be null");
		if (key.length != 1)
			throw new BuilderException("Key has to have a length of 1");
		key[0] = server.onPacketSent(listener);
		return this;
	}
	
	public ServerBuilder<T> onPacketSent(final PacketSentListener<T>... listeners) {
		for (final PacketSentListener<T> listener : listeners)
			onPacketSent(listener);
		return this;
	}
	
	public ServerBuilder<T> onPacketSent(final long[] keys, final PacketSentListener<T>... listeners) {
		int i = 0;
		for (final PacketSentListener<T> listener : listeners) {
			final long[] key = new long[1];
			onPacketSent(key, listener);
			keys[i] = key[i++];
		}
		return this;
	}
	
	public ServerBuilder<T> onClientConnect(final ServerClientConnectListener listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		server.onClientConnect(listener);
		return this;
	}
	
	public ServerBuilder<T> onClientConnect(final long[] key, final ServerClientConnectListener listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		if (key == null)
			throw new NullPointerException("Key cannot be null");
		if (key.length != 1)
			throw new BuilderException("Key has to have a length of 1");
		key[0] = server.onClientConnect(listener);
		return this;
	}
	
	public ServerBuilder<T> onClientConnect(final ServerClientConnectListener... listeners) {
		for (final ServerClientConnectListener listener : listeners)
			onClientConnect(listener);
		return this;
	}
	
	public ServerBuilder<T> onClientConnect(final long[] keys, final ServerClientConnectListener... listeners) {
		int i = -1;
		for (final ServerClientConnectListener listener : listeners) {
			final long[] key = new long[1];
			onClientConnect(key, listener);
			keys[i++] = key[i];
		}
		return this;
	}
	
	public ServerBuilder<T> onClientDisconnect(final ServerClientDisconnectListener listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		server.onClientDisconnect(listener);
		return this;
	}
	
	public ServerBuilder<T> onClientDisconnect(final long[] key, final ServerClientDisconnectListener listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		if (key == null)
			throw new NullPointerException("Key cannot be null");
		if (key.length != 1)
			throw new BuilderException("Key has to have a length of 1");
		key[0] = server.onClientDisconnect(listener);
		return this;
	}
	
	public ServerBuilder<T> onClientDisconnect(final ServerClientDisconnectListener... listeners) {
		for (final ServerClientDisconnectListener listener : listeners)
			onClientDisconnect(listener);
		return this;
	}
	
	public ServerBuilder<T> onClientDisconnect(final long[] keys, final ServerClientDisconnectListener... listeners) {
		int i = 0;
		for (final ServerClientDisconnectListener listener : listeners) {
			final long[] key = new long[1];
			onClientDisconnect(key, listener);
			keys[i] = key[i++];
		}
		return this;
	}
	
	public Server<T> build() {
		if (!(isReady()))
			throw new BuilderException("Builder isn't ready: " + this.toString());
		return server;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("ServerBuilder { ")
				.append("Algorithm: ").append(algorithm().getAlgorithm())
				.append("; Port: ").append(port())
				.append("; Timeout: ").append(timeout())
				.append("; TimeUnit: ").append(timeUnit())
				.append("; RRHs: <");
		for (final int requestId : server.getRegisteredRequestHandlers())
			builder.append(requestId).append("|");
		if (builder.charAt(builder.length() - 1) == '|')
			builder.setCharAt(builder.length() - 1, '>');
		else
			builder.append('>');
		return builder
				.append("; PRLs: ").append(server.getPacketReceivedListeners())
				.append("; PSLs: ").append(server.getPacketSentListeners())
				.append("; CCLs: ").append(server.getClientConnectListeners())
				.append("; CDLs: ").append(server.getClientDisconnectListeners())
				.append(" }")
			.toString();
	}
	
	public static ServerBuilder<byte[]> buffer() {
		return new ServerBuilder<>(new Server<>(new BufferConfiguration()));
	}
	
	public static ServerBuilder<String> string() {
		return new ServerBuilder<>(new Server<>(new StringConfiguration()));
	}
	
	public static ServerBuilder<SerialObject> serial() {
		return new ServerBuilder<>(new Server<>(new SerialConfiguration()));
	}
	
}
