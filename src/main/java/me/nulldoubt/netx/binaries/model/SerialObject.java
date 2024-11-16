package me.nulldoubt.netx.binaries.model;

import java.io.Serializable;

public abstract class SerialObject implements Serializable {
	
	@java.io.Serial
	private static final long serialVersionUID = 5288580335532899308L;
	
	private final String header;
	
	public SerialObject(final String header) {
		this.header = header;
	}
	
	public String getHeader() {
		return header;
	}
	
	@Override
	public String toString() {
		return new StringBuilder("SerialObject(").append(header).append(")").toString();
	}
	
}
