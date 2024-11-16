package me.nulldoubt.netx;

import me.nulldoubt.netx.binaries.RequestHandler;
import me.nulldoubt.netx.binaries.configurations.BufferConfiguration;
import me.nulldoubt.netx.binaries.configurations.SerialConfiguration;
import me.nulldoubt.netx.binaries.configurations.StringConfiguration;
import me.nulldoubt.netx.binaries.model.PacketReceivedListener;
import me.nulldoubt.netx.binaries.model.PacketSentListener;
import me.nulldoubt.netx.binaries.model.SerialObject;
import me.nulldoubt.netx.exceptions.BuilderException;

import java.io.PrintStream;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public final class ClientBuilder<T> {
	
	private final Client<T> client;
	
	private boolean algorithm;
	private boolean host;
	private boolean port;
	
	private ClientBuilder(final Client<T> client) {
		this.client = client;
		algorithm = false;
		host = false;
		port = false;
	}
	
	public boolean isReady() {
		return (algorithm && host && port);
	}
	
	public PrintStream err() {
		return client.err();
	}
	
	public ClientBuilder<T> err(final PrintStream errorStream) {
		if (errorStream == null)
			throw new NullPointerException("ErrorStream cannot be null");
		client.setErr(errorStream);
		return this;
	}
	
	public NetX.CipherAlgorithm algorithm() {
		if (!(algorithm))
			throw new NullPointerException("Algorithm hasn't been set yet");
		return client.getAlgorithm();
	}
	
	public ClientBuilder<T> algorithm(final NetX.CipherAlgorithm algorithm) {
		if (algorithm == null)
			throw new NullPointerException("Algorithm cannot be null");
		client.setAlgorithm(algorithm);
		this.algorithm = true;
		return this;
	}
	
	public InetAddress host() {
		if (!(host))
			throw new NullPointerException("Host hasn't been set yet");
		return client.getHost();
	}
	
	public ClientBuilder<T> host(final InetAddress host) {
		if (host == null)
			throw new NullPointerException("Host cannot be null");
		client.setHost(host);
		this.host = true;
		return this;
	}
	
	public int port() {
		if (!(port))
			throw new NullPointerException("Port hasn't been set yet");
		return client.getPort();
	}
	
	public ClientBuilder<T> port(final int port) {
		if ((port < NetX.NETWORK_MIN_PORT) || (port > NetX.NETWORK_MAX_PORT))
			throw new NullPointerException("Port has to be in range between %d and %d".formatted(NetX.NETWORK_MIN_PORT, NetX.NETWORK_MAX_PORT));
		client.setPort(port);
		this.port = true;
		return this;
	}
	
	public long timeout() {
		return client.getDefaultTimeout();
	}
	
	public TimeUnit timeUnit() {
		return client.getDefaultTimeUnit();
	}
	
	public ClientBuilder<T> timeout(final long timeout, final TimeUnit timeUnit) {
		if (timeout == 0)
			throw new NullPointerException("Timeout cannot be 0");
		if (timeUnit == null)
			throw new NullPointerException("TimeUnit cannot be null");
		client.setDefaultTimeout(timeout, timeUnit);
		return this;
	}
	
	public ClientBuilder<T> requestHandler(final RequestHandler handler) {
		if (handler == null)
			throw new NullPointerException("Handler cannot be null");
		client.registerRequestHandler(handler);
		return this;
	}
	
	public ClientBuilder<T> requestHandler(final RequestHandler... handlers) {
		for (final RequestHandler handler : handlers)
			requestHandler(handler);
		return this;
	}
	
	public ClientBuilder<T> onPacketReceived(final PacketReceivedListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		client.onPacketReceived(listener);
		return this;
	}
	
	public ClientBuilder<T> onPacketReceived(final long[] key, final PacketReceivedListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		if (key == null)
			throw new NullPointerException("Key cannot be null");
		if (key.length != 1)
			throw new BuilderException("Key has to have a length of 1");
		key[0] = client.onPacketReceived(listener);
		return this;
	}
	
	public ClientBuilder<T> onPacketReceived(final PacketReceivedListener<T>... listeners) {
		for (final PacketReceivedListener<T> listener : listeners)
			onPacketReceived(listener);
		return this;
	}
	
	public ClientBuilder<T> onPacketReceived(final long[] keys, final PacketReceivedListener<T>... listeners) {
		int i = 0;
		for (final PacketReceivedListener<T> listener : listeners) {
			final long[] key = new long[1];
			onPacketReceived(key, listener);
			keys[i] = key[i++];
		}
		return this;
	}
	
	public ClientBuilder<T> onPacketSent(final PacketSentListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		client.onPacketSent(listener);
		return this;
	}
	
	public ClientBuilder<T> onPacketSent(final long[] key, final PacketSentListener<T> listener) {
		if (listener == null)
			throw new NullPointerException("Listener cannot be null");
		if (key == null)
			throw new NullPointerException("Key cannot be null");
		if (key.length != 1)
			throw new BuilderException("Key has to have a length of 1");
		key[0] = client.onPacketSent(listener);
		return this;
	}
	
	@SafeVarargs
	public final ClientBuilder<T> onPacketSent(final PacketSentListener<T>... listeners) {
		for (final PacketSentListener<T> listener : listeners)
			onPacketSent(listener);
		return this;
	}
	
	@SafeVarargs
	public final ClientBuilder<T> onPacketSent(final long[] keys, final PacketSentListener<T>... listeners) {
		int i = -1;
		for (final PacketSentListener<T> listener : listeners) {
			final long[] key = new long[1];
			onPacketSent(key, listener);
			keys[i++] = key[i];
		}
		return this;
	}
	
	public Client<T> build() {
		if (!(isReady()))
			throw new BuilderException("Builder isn't ready: " + this.toString());
		return client;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("ClientBuilder { ")
				.append("Algorithm: ").append(algorithm().getAlgorithm())
				.append("; Host: ").append(host().getHostAddress())
				.append("; Port: ").append(port())
				.append("; Timeout: ").append(timeout())
				.append("; TimeUnit: ").append(timeUnit())
				.append("; RRHs: <");
		for (final int requestId : client.getRegisteredRequestHandlers())
			builder.append(requestId).append("|");
		if (builder.charAt(builder.length() - 1) == '|')
			builder.setCharAt(builder.length() - 1, '>');
		else
			builder.append('>');
		return builder
				.append("; PRLs: ").append(client.getPacketReceivedListeners())
				.append("; PSLs: ").append(client.getPacketSentListeners())
				.append(" }")
			.toString();
	}
	
	public static ClientBuilder<byte[]> buffer() {
		return new ClientBuilder<>(new Client<>(new BufferConfiguration()));
	}
	
	public static ClientBuilder<String> string() {
		return new ClientBuilder<>(new Client<>(new StringConfiguration()));
	}
	
	public static ClientBuilder<SerialObject> serial() {
		return new ClientBuilder<>(new Client<>(new SerialConfiguration()));
	}
	
}
