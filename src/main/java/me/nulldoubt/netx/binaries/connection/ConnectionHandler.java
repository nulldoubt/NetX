package me.nulldoubt.netx.binaries.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Consumer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import me.nulldoubt.netx.NetX;
import me.nulldoubt.netx.exceptions.ConnectionException;

public final class ConnectionHandler {
	
	private BufferReader reader;
	private BufferWriter writer;
	private Socket socket;
	private UUID uuid;
	
	private final ConnectionCipher cipher;
	private final InetAddress host;
	private final int port;
	
	private boolean open;
	private boolean raw;
	
	private Thread networkThread;
	
	private Stack<Consumer<?>> awaitConsumers;
	
	public ConnectionHandler(final NetX.CipherAlgorithm algorithm, final Socket socket) {
		cipher = new ConnectionCipher(algorithm);
		this.host = socket.getInetAddress();
		this.port = socket.getPort();
		this.socket = socket;
		awaitConsumers = new Stack<>();
		open = false;
		raw = false;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public void openRaw() throws IOException {
		writer = new BufferWriter(socket.getOutputStream());
		writer.getOutputStream().flush();
		reader = new BufferReader(socket.getInputStream());
		open = true;
		raw = true;
	}
	
	public void open(final Key key) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (open && !raw)
			return;
		if (raw) {
			cipher.open(key);
			raw = false;
			return;
		}
		writer = new BufferWriter(socket.getOutputStream());
		writer.getOutputStream().flush();
		reader = new BufferReader(socket.getInputStream());
		cipher.open(key);
		open = true;
	}
	
	public void close() throws IOException {
		if (!(open))
			return;
		reader.close();
		writer.close();
		socket.close();
		try {
			if (networkThread != null)
				networkThread.join(5);
		} catch (InterruptedException e) {
			throw new ConnectionException("Handler termination interrupted: " + e.getLocalizedMessage());
		}
		open = false;
	}
	
	public void setNetworkThread(final Thread networkThread) {
		this.networkThread = networkThread;
	}
	
	public void write(final NetX.SignalModifier modifier, final byte[] buffer) throws IOException, IllegalBlockSizeException, BadPaddingException {
		final byte[] encrypted = cipher.encode(buffer);
		protocol_write(modifier, encrypted);
	}
	
	public void protocol_write(final NetX.SignalModifier modifier, final byte[] buffer) throws IOException {
		if (!(open))
			throw new ConnectionException("Unable to write, handler closed");
		writer.write(modifier, buffer);
	}
	
	public NetX.SignalHolder read() throws IOException, IllegalBlockSizeException, BadPaddingException {
		final NetX.SignalHolder holder = protocol_read();
		//: if (!(SignalModifier.isType(holder.getSignalModifier())))
		//: 	throw new ConnectionException("Unable to read type from buffer (Invalid Signal Modifier)");
		final byte[] buffer = holder.getBuffer();
		return new NetX.SignalHolder(holder.getSignalModifier(), cipher.decode(buffer));
	}
	
	public NetX.SignalHolder protocol_read() throws IOException {
		if (!(open))
			throw new ConnectionException("Unable to read, handler closed");
		return reader.read();
	}
	
	public boolean hasAwaitConsumers() {
		return (!(awaitConsumers.isEmpty()));
	}
	
	public Consumer<?> popAwaitConsumer() {
		return awaitConsumers.pop();
	}
	
	public void pushAwaitConsumer(final Consumer<?> awaitConsumer) {
		awaitConsumers.push(awaitConsumer);
	}
	
	public InetAddress getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setConnectionUUID(final UUID uuid) {
		this.uuid = uuid;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
}
