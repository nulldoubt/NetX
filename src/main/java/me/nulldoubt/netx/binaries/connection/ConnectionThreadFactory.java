package me.nulldoubt.netx.binaries.connection;

public final class ConnectionThreadFactory {
	
	private static final ThreadGroup GROUP;
	
	private static final String PREFIX;
	private static final String SUFFIX;
	
	private static long THREADS;
	
	static {
		GROUP = new ThreadGroup("NetX");
		PREFIX = "NetX-";
		SUFFIX = "-Thread#";
		THREADS = 1;
	}
	
	public Thread create(final Runnable runnable) {
		return create("Temp", runnable);
	}
	
	public Thread create(final String name, final Runnable runnable) {
		final Thread thread = new Thread(GROUP, runnable, (PREFIX + name + SUFFIX + Long.toHexString(THREADS)));
		THREADS++;
		return thread;
	}
	
	public Thread create(final String name, final Runnable runnable, final int priority) {
		final Thread thread = create(name, runnable);
		thread.setPriority(priority);
		return thread;
	}
	
}
