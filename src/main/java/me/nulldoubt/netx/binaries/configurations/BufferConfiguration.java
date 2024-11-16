package me.nulldoubt.netx.binaries.configurations;

import me.nulldoubt.netx.exceptions.CompilationException;

public final class BufferConfiguration extends Configuration<byte[]> {
	
	@Override
	public byte[] compile(final byte[] buffer) throws CompilationException {
		return buffer;
	}
	
	@Override
	public byte[] decompile(final byte[] buffer) throws CompilationException {
		return buffer;
	}
	
}
