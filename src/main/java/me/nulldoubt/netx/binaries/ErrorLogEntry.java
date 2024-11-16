package me.nulldoubt.netx.binaries;

import java.io.PrintStream;

public abstract class ErrorLogEntry {
	
	private PrintStream errorStream;
	
	public ErrorLogEntry(final PrintStream errorStream) {
		this.errorStream = errorStream;
	}
	
	public PrintStream err() {
		return errorStream;
	}
	
	public void setErr(final PrintStream errorStream) {
		this.errorStream = errorStream;
	}
	
}
