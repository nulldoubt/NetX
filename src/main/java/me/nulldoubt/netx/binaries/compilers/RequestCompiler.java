package me.nulldoubt.netx.binaries.compilers;

import me.nulldoubt.netx.binaries.Request;
import me.nulldoubt.netx.binaries.configurations.Configuration;
import me.nulldoubt.netx.exceptions.CompilationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class RequestCompiler extends Configuration<Request> {
	
	@Override
	public Request compile(final byte[] buffer) throws CompilationException {
		try {
			final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
			final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
			final Object object = objectInputStream.readObject();
			if (!(object instanceof Request request))
				throw new CompilationException("Compiled object isn't an instance of Request: " + object.toString());
			objectInputStream.close();
			byteArrayInputStream.close();
			return request;
		} catch (Exception e) {
			throw new CompilationException(e);
		}
	}
	
	@Override
	public byte[] decompile(final Request request) throws CompilationException {
		try {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(request);
			final byte[] buffer = byteArrayOutputStream.toByteArray();
			objectOutputStream.close();
			byteArrayOutputStream.close();
			return buffer;
		} catch (Exception e) {
			throw new CompilationException(e);
		}
	}
	
}
