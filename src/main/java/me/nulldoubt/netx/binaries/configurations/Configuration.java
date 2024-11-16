package me.nulldoubt.netx.binaries.configurations;

import me.nulldoubt.netx.exceptions.CompilationException;

public abstract class Configuration<T> {
	
	public abstract T compile(final byte[] buffer) throws CompilationException;
	
	public abstract byte[] decompile(final T t) throws CompilationException;
	
}
