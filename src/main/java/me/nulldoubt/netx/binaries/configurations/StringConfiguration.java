package me.nulldoubt.netx.binaries.configurations;

import me.nulldoubt.netx.exceptions.CompilationException;

public final class StringConfiguration extends Configuration<String> {
	
	@Override
	public String compile(final byte[] buffer) throws CompilationException {
		return new String(buffer);
	}
	
	@Override
	public byte[] decompile(final String string) throws CompilationException {
		return string.getBytes();
	}
	
}
