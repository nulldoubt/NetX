package me.nulldoubt.netx.binaries.configurations;

import me.nulldoubt.netx.binaries.model.SerialObject;
import me.nulldoubt.netx.exceptions.CompilationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class SerialConfiguration extends Configuration<SerialObject> {
	
	@Override
	public SerialObject compile(final byte[] buffer) throws CompilationException {
		try {
			final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
			final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
			final Object object = objectInputStream.readObject();
			if (!(object instanceof SerialObject serialObject))
				throw new CompilationException("Compiled object isn't an instance of SerialObject: " + object.toString());
			objectInputStream.close();
			byteArrayInputStream.close();
			return serialObject;
		} catch (Exception e) {
			throw new CompilationException(e);
		}
	}
	
	@Override
	public byte[] decompile(final SerialObject serialObject) throws CompilationException {
		try {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(serialObject);
			final byte[] buffer = byteArrayOutputStream.toByteArray();
			objectOutputStream.close();
			byteArrayOutputStream.close();
			return buffer;
		} catch (Exception e) {
			throw new CompilationException(e);
		}
	}
	
}
