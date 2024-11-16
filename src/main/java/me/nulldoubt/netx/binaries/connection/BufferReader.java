package me.nulldoubt.netx.binaries.connection;

import me.nulldoubt.netx.NetX;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BufferReader {
	
	private final DataInputStream inputStream;
	
	public BufferReader(final InputStream inputStream) {
		this.inputStream = new DataInputStream(inputStream);
	}
	
	public NetX.SignalHolder read() throws IOException {
		final byte[] buffer = new byte[inputStream.readInt()];
		final byte signalModifier = inputStream.readByte();
		inputStream.readFully(buffer);
		return new NetX.SignalHolder(signalModifier, buffer);
	}
	
	public void close() throws IOException {
		inputStream.close();
	}
	
	public DataInputStream getInputStream() {
		return inputStream;
	}
	
}
