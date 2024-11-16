package me.nulldoubt.netx.binaries.connection;

import me.nulldoubt.netx.NetX;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class BufferWriter {
	
	private final DataOutputStream outputStream;
	
	public BufferWriter(final OutputStream outputStream) {
		this.outputStream = new DataOutputStream(outputStream);
	}
	
	public void write(final NetX.SignalModifier modifier, final byte[] buffer) throws IOException {
		outputStream.writeInt(buffer.length);
		outputStream.writeByte(modifier.getModifier());
		outputStream.write(buffer, 0, buffer.length);
		outputStream.flush();
	}
	
	public void close() throws IOException {
		outputStream.close();
	}
	
	public DataOutputStream getOutputStream() {
		return outputStream;
	}
	
}
