package me.nulldoubt.netx;

import me.nulldoubt.netx.binaries.*;
import me.nulldoubt.netx.binaries.compilers.RequestCompiler;
import me.nulldoubt.netx.binaries.compilers.ResponseCompiler;
import me.nulldoubt.netx.binaries.configurations.Configuration;
import me.nulldoubt.netx.binaries.connection.ConnectionHandler;
import me.nulldoubt.netx.binaries.connection.ConnectionThreadFactory;
import me.nulldoubt.netx.binaries.model.PacketReceivedListener;
import me.nulldoubt.netx.binaries.model.PacketSentListener;
import me.nulldoubt.netx.exceptions.CompilationException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Client<T> extends ErrorLogEntry {
	
	private final Map<Long, PacketReceivedListener<T>> packetReceivedListeners;
	private final Map<Long, PacketSentListener<T>> packetSentListeners;
	
	private final Map<Integer, RequestHandler> requestHandlers;
	
	private final Configuration<T> configuration;
	
	private final ConnectionThreadFactory threadFactory;
	private ConnectionHandler handler;
	private Thread networkThread;
	private final Random random;
	private UUID uuid;
	
	private NetX.CipherAlgorithm algorithm;
	private InetAddress host;
	private int port;
	
	private long timeout;
	private TimeUnit timeUnit;
	
	private final RequestCompiler requestCompiler;
	private final ResponseCompiler responseCompiler;
	
	private boolean open;
	
	protected Client(final Configuration<T> configuration) {
		super(System.err);
		this.configuration = configuration;
		
		timeout = Short.MAX_VALUE;
		timeUnit = TimeUnit.MILLISECONDS;
		
		packetReceivedListeners = new HashMap<>();
		packetSentListeners = new HashMap<>();
		
		requestHandlers = new HashMap<>();
		
		requestCompiler = new RequestCompiler();
		responseCompiler = new ResponseCompiler();
		
		threadFactory = new ConnectionThreadFactory();
		random = new Random();
		open = false;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public void open() throws IOException {
		if (open)
			return;
		open = true;
		handler = new ConnectionHandler(algorithm, new Socket(host, port));
		handler.openRaw();
		uuid = UUID.fromString(new String(handler.protocol_read().getBuffer()));
		try {
			handler.open(new SecretKeySpec(uuid.toString().substring(4).getBytes(), algorithm.getAlgorithm()));
			handler.write(NetX.SignalModifier.SIGNAL_TYPE, ByteBuffer.allocate(4).putInt(NetX.VERSION).array());
		} catch (InvalidKeyException e) {
			err().println("An error occurred while opening handler (Invalid Key): " + e.getLocalizedMessage());
			return;
		} catch (NoSuchAlgorithmException e) {
			err().println("An error occurred while opening handler (Invalid Algorithm): " + e.getLocalizedMessage());
			return;
		} catch (NoSuchPaddingException e) {
			err().println("An error occurred while opening handler (Invalid Padding): " + e.getLocalizedMessage());
			return;
		} catch (IllegalBlockSizeException e) {
			err().println("An error occurred while writing to opened handler (Illegal Block Size): " + e.getLocalizedMessage());
			return;
		} catch (BadPaddingException e) {
			err().println("An error occurred while writing to opened handler (Bad Padding): " + e.getLocalizedMessage());
			return;
		}
		networkThread = threadFactory.create("Client", () -> {
			while (open) {
				try {
					final NetX.SignalHolder holder = handler.read();
					final byte signalModifier = holder.getSignalModifier();
					final byte[] buffer = holder.getBuffer();
					if (NetX.SignalModifier.isType(signalModifier))
						handleType(configuration.compile(buffer));
					else if (NetX.SignalModifier.isRequest(signalModifier))
						handleRequest(requestCompiler.compile(buffer));
					else if (NetX.SignalModifier.isResponse(signalModifier))
						handleResponse(responseCompiler.compile(buffer));
					else if (NetX.SignalModifier.isClose(signalModifier))
						break;
					else
						err().println("Received Invalid Signal (Illegal State)");
				} catch (SocketException _) {
				} catch (IllegalBlockSizeException e) {
					err().println("Unable to read (Illegal Block Size): " + e.getLocalizedMessage());
				} catch (BadPaddingException e) {
					err().println("Unable to read (Bad Padding): " + e.getLocalizedMessage());
				} catch (IOException e) {
					err().println("Unable to read (Internal): " + e.getLocalizedMessage());
				} catch (CompilationException e) {
					err().println("Unable to compile type: " + e.getLocalizedMessage());
				}
			}
			handleClose();
		}, Thread.MAX_PRIORITY);
		handler.setNetworkThread(networkThread);
		networkThread.start();
	}
	
	private void handleType(final T t) {
		for (final PacketReceivedListener<T> listener : packetReceivedListeners.values())
			listener.onPacketReceived(t, handler.getUUID());
	}
	
	private void handleRequest(final Request request) throws IllegalBlockSizeException, BadPaddingException, IOException {
		final int requestId = request.getRequestId();
		if (!(requestHandlers.containsKey(requestId))) {
			err().println("No request handler registered for request id " + requestId);
			return;
		}
		final RequestHandler requestHandler = requestHandlers.get(request.getRequestId());
		final Response response = requestHandler.handle(request);
		final byte[] buffer = responseCompiler.decompile(response);
		handler.write(NetX.SignalModifier.SIGNAL_RESPONSE, buffer);
	}
	
	private void handleResponse(final Response response) {
		if (!(handler.hasAwaitConsumers())) {
			err().println("Unable to handle response without a registered and awaiting consumer");
			return;
		}
		((Consumer<Response>) handler.popAwaitConsumer()).accept(response);
	}
	
	private void handleClose() {
		try {
			close();
		} catch (IOException e) {
			err().println("An error occurred on the low-level layer while closing: " + e.getLocalizedMessage());
			return;
		}
	}
	
	public void close() throws IOException {
		if (!(open))
			return;
		open = false;
		handler.close();
		try {
			networkThread.join(5);
		} catch (Exception e) {
			networkThread.interrupt();
		}
	}
	
	public long onPacketSent(final PacketSentListener<T> listener) {
		final long key = random.nextLong();
		packetSentListeners.put(key, listener);
		return key;
	}
	
	public long onPacketReceived(final PacketReceivedListener<T> listener) {
		final long key = random.nextLong();
		packetReceivedListeners.put(key, listener);
		return key;
	}
	
	public int getPacketSentListeners() {
		return packetSentListeners.size();
	}
	
	public int getPacketReceivedListeners() {
		return packetReceivedListeners.size();
	}
	
	public boolean removePacketSentListener(final long key) {
		if (!(packetSentListeners.containsKey(key)))
			return false;
		packetSentListeners.remove(key);
		return true;
	}
	
	public boolean removePacketReceivedListener(final long key) {
		if (!(packetReceivedListeners.containsKey(key)))
			return false;
		packetReceivedListeners.remove(key);
		return true;
	}
	
	public void registerRequestHandler(final RequestHandler handler) {
		requestHandlers.put(handler.getRequestId(), handler);
	}
	
	public void unregisterRequestHandler(final RequestHandler handler) {
		requestHandlers.remove(handler.getRequestId());
	}
	
	public int[] getRegisteredRequestHandlers() {
		return requestHandlers.keySet().stream().mapToInt(Integer::intValue).toArray();
	}
	
	protected T await() throws InterruptedException {
		return await(timeout, timeUnit);
	}
	
	public T awaitAfter(final Runnable runnable) throws InterruptedException {
		return awaitAfter(runnable, timeout, timeUnit);
	}
	
	protected T await(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final Object[] type = new Object[1];
		final Consumer<T> consumer = ((t) -> {
			type[0] = t;
			latch.countDown();
		});
		handler.pushAwaitConsumer(consumer);
		latch.await(timeout, timeUnit);
		return ((T) type[0]);
	}
	
	protected T awaitAfter(final Runnable runnable, final long timeout, final TimeUnit timeUnit) throws InterruptedException 	{
		final CountDownLatch latch = new CountDownLatch(1);
		final Object[] type = new Object[1];
		final Consumer<T> consumer = ((t) -> {
			type[0] = t;
			latch.countDown();
		});
		runnable.run();
		handler.pushAwaitConsumer(consumer);
		latch.await(timeout, timeUnit);
		return ((T) type[0]);
	}
	
	public void send(final T t) {
		byte[] buffer;
		buffer = configuration.decompile(t);
		try {
			handler.write(NetX.SignalModifier.SIGNAL_TYPE, buffer);
		} catch (IllegalBlockSizeException e) {
			err().println("Unable to write to handler (Illegal Block Size): " + e.getLocalizedMessage());
		} catch (BadPaddingException e) {
			err().println("Unable to write to handler (Bad Padding): " + e.getLocalizedMessage());
		} catch (IOException e) {
			err().println("Unable to write to handler (Internal): " + e.getLocalizedMessage());
		}
	}
	
	public void sendAfter(final Runnable runnable, final T t) {
		byte[] buffer;
		try {
			buffer = configuration.decompile(t);
		} catch (Exception e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return;
		}
		runnable.run();
		try {
			handler.write(NetX.SignalModifier.SIGNAL_TYPE, buffer);
		} catch (IllegalBlockSizeException e) {
			err().println("Unable to write to handler (Illegal Block Size): " + e.getLocalizedMessage());
		} catch (BadPaddingException e) {
			err().println("Unable to write to handler (Bad Padding): " + e.getLocalizedMessage());
		} catch (IOException e) {
			err().println("Unable to write to handler (Internal): " + e.getLocalizedMessage());
		}
	}
	
	public T sendAndAwait(final T t) throws InterruptedException {
		return sendAndAwait(t, timeout, timeUnit);
	}
	
	public T sendAndAwaitAfter(final Runnable runnable, final T t) throws InterruptedException {
		return sendAndAwaitAfter(runnable, t, timeout, timeUnit);
	}
	
	public T sendAndAwait(final T t, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		send(t);
		return await(timeout, timeUnit);
	}
	
	public T sendAndAwaitAfter(final Runnable runnable, final T t, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		sendAfter(runnable, t);
		return await(timeout, timeUnit);
	}
	
	public Response request(final Request request) throws InterruptedException {
		return request(request, timeout, timeUnit);
	}
	
	public Response requestAfter(final Runnable runnable, final Request request) throws InterruptedException {
		return requestAfter(runnable, request, timeout, timeUnit);
	}
	
	public Response request(final Request request, final RequestErrorCallback errorCallback) {
		return request(request, timeout, timeUnit, errorCallback);
	}
	
	public Response requestAfter(final Runnable runnable, final Request request, final RequestErrorCallback errorCallback) {
		return requestAfter(runnable, request, timeout, timeUnit, errorCallback);
	}
	
	public Response request(final Request request, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		byte[] buffer;
		try {
			buffer = requestCompiler.decompile(request);
		} catch (CompilationException e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to decompile type: " + e.getLocalizedMessage(), request);
		}
		try {
			handler.write(NetX.SignalModifier.SIGNAL_REQUEST, buffer);
		} catch (IllegalBlockSizeException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Illegal Block Size): " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to write to handler with UUID '" + handler.getUUID() + "', (Illegal Block Size): " + e.getLocalizedMessage(), request);
		} catch (BadPaddingException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Bad Padding): " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to write to handler with UUID '" + handler.getUUID() + "', (Bad Padding): " + e.getLocalizedMessage(), request);
		} catch (IOException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Internal): " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to write to handler with UUID '" + handler.getUUID() + "', (Internal): " + e.getLocalizedMessage(), request);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final Response[] response = new Response[1];
		final Consumer<Response> consumer = ((r) -> {
			response[0] = r;
			latch.countDown();
		});
		handler.pushAwaitConsumer(consumer);
		latch.await(timeout, timeUnit);
		return response[0];
	}
	
	public Response requestAfter(final Runnable runnable, final Request request, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		byte[] buffer;
		try {
			buffer = requestCompiler.decompile(request);
		} catch (CompilationException e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to decompile type: " + e.getLocalizedMessage(), request);
		}
		runnable.run();
		try {
			handler.write(NetX.SignalModifier.SIGNAL_REQUEST, buffer);
		} catch (IllegalBlockSizeException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Illegal Block Size): " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to write to handler with UUID '" + handler.getUUID() + "', (Illegal Block Size): " + e.getLocalizedMessage(), request);
		} catch (BadPaddingException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Bad Padding): " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to write to handler with UUID '" + handler.getUUID() + "', (Bad Padding): " + e.getLocalizedMessage(), request);
		} catch (IOException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Internal): " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to write to handler with UUID '" + handler.getUUID() + "', (Internal): " + e.getLocalizedMessage(), request);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final Response[] response = new Response[1];
		final Consumer<Response> consumer = ((r) -> {
			response[0] = r;
			latch.countDown();
		});
		handler.pushAwaitConsumer(consumer);
		latch.await(timeout, timeUnit);
		return response[0];
	}
	
	public Response request(final Request request, final long timeout, final TimeUnit timeUnit, final RequestErrorCallback errorCallback) {
		try {
			final byte[] buffer = requestCompiler.decompile(request);
			handler.write(NetX.SignalModifier.SIGNAL_REQUEST, buffer);
			final CountDownLatch latch = new CountDownLatch(1);
			final Response[] response = new Response[1];
			final Consumer<Response> consumer = ((r) -> {
				response[0] = r;
				latch.countDown();
			});
			handler.pushAwaitConsumer(consumer);
			latch.await(timeout, timeUnit);
			return response[0];
		} catch (Exception e) {
			return errorCallback.onError(new ErrorResponse(e.getLocalizedMessage(), request));
		}
	}
	
	public Response requestAfter(final Runnable runnable, final Request request, final long timeout, final TimeUnit timeUnit, final RequestErrorCallback errorCallback) {
		try {
			final byte[] buffer = requestCompiler.decompile(request);
			runnable.run();
			handler.write(NetX.SignalModifier.SIGNAL_REQUEST, buffer);
			final CountDownLatch latch = new CountDownLatch(1);
			final Response[] response = new Response[1];
			final Consumer<Response> consumer = ((r) -> {
				response[0] = r;
				latch.countDown();
			});
			handler.pushAwaitConsumer(consumer);
			latch.await(timeout, timeUnit);
			return response[0];
		} catch (Exception e) {
			return errorCallback.onError(new ErrorResponse(e.getMessage(), request));
		}
	}
	
	public NetX.CipherAlgorithm getAlgorithm() {
		return algorithm;
	}
	
	public void setAlgorithm(final NetX.CipherAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
	
	public long getDefaultTimeout() {
		return timeout;
	}
	
	public TimeUnit getDefaultTimeUnit() {
		return timeUnit;
	}
	
	public void setDefaultTimeout(final long timeout, final TimeUnit timeUnit) {
		this.timeout = timeout;
		this.timeUnit = timeUnit;
	}
	
	public InetAddress getHost() {
		return host;
	}
	
	public void setHost(final InetAddress host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(final int port) {
		this.port = port;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
}
