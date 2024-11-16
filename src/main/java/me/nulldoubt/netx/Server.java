package me.nulldoubt.netx;

import me.nulldoubt.netx.NetX.CipherAlgorithm;
import me.nulldoubt.netx.NetX.SignalHolder;
import me.nulldoubt.netx.NetX.SignalModifier;
import me.nulldoubt.netx.binaries.*;
import me.nulldoubt.netx.binaries.compilers.RequestCompiler;
import me.nulldoubt.netx.binaries.compilers.ResponseCompiler;
import me.nulldoubt.netx.binaries.configurations.Configuration;
import me.nulldoubt.netx.binaries.connection.ConnectionHandler;
import me.nulldoubt.netx.binaries.connection.ConnectionThreadFactory;
import me.nulldoubt.netx.binaries.model.PacketReceivedListener;
import me.nulldoubt.netx.binaries.model.PacketSentListener;
import me.nulldoubt.netx.binaries.model.ServerClientConnectListener;
import me.nulldoubt.netx.binaries.model.ServerClientDisconnectListener;
import me.nulldoubt.netx.exceptions.CompilationException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
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

public class Server<T> extends ErrorLogEntry {
	
	private final Map<Long, PacketReceivedListener<T>> packetReceivedListeners;
	private final Map<Long, PacketSentListener<T>> packetSentListeners;
	
	private final Map<Long, ServerClientConnectListener> clientConnectListeners;
	private final Map<Long, ServerClientDisconnectListener> clientDisconnectListeners;
	
	private final Map<Integer, RequestHandler> requestHandlers;
	
	private final Map<UUID, ConnectionHandler> clients;
	private final Configuration<T> configuration;
	
	private final ConnectionThreadFactory threadFactory;
	private CipherAlgorithm algorithm;
	
	private int port;
	private boolean open;
	
	private ServerSocket serverSocket;
	private Thread networkThread;
	private final Random random;
	private UUID lastUUID;
	
	private long timeout;
	private TimeUnit timeUnit;
	
	private final RequestCompiler requestCompiler;
	private final ResponseCompiler responseCompiler;
	
	protected Server(final Configuration<T> configuration) {
		super(System.err);
		this.configuration = configuration;
		
		timeout = Short.MAX_VALUE;
		timeUnit = TimeUnit.MILLISECONDS;
		
		packetReceivedListeners = new HashMap<>();
		packetSentListeners = new HashMap<>();
		
		requestHandlers = new HashMap<>();
		
		clientConnectListeners = new HashMap<>();
		clientDisconnectListeners = new HashMap<>();
		
		requestCompiler = new RequestCompiler();
		responseCompiler = new ResponseCompiler();
		
		threadFactory = new ConnectionThreadFactory();
		clients = new HashMap<>();
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
		lastUUID = generateUUID();
		serverSocket = new ServerSocket(port);
		networkThread = threadFactory.create("Server", () -> {
			try {
				while (open) {
					final Socket socket = serverSocket.accept();
					final ConnectionHandler handler = new ConnectionHandler(algorithm, socket);
					registerHandler(handler);
				}
			} catch (SocketException _) {
			} catch (IOException e) {
				err().println("An error occurred while accepting socket (Internal): " + e.getLocalizedMessage());
			} catch (InvalidKeyException e) {
				err().println("An error occurred while accepting socket (Invalid Key): " + e.getLocalizedMessage());
			} catch (NoSuchAlgorithmException e) {
				err().println("An error occurred while accepting socket (Invalid Algorithm): " + e.getLocalizedMessage());
			} catch (NoSuchPaddingException e) {
				err().println("An error occurred while accepting socket (Invalid Padding): " + e.getLocalizedMessage());
			} catch (IllegalBlockSizeException e) {
				err().println("An error occurred while writing to accepted socket (Illegal Block Size): " + e.getLocalizedMessage());
			} catch (BadPaddingException e) {
				err().println("An error occurred while writing to accepted socket (Bad Padding): " + e.getLocalizedMessage());
			}
		}, Thread.MAX_PRIORITY);
		networkThread.start();
	}
	
	public void close() throws IOException {
		if (!(open))
			return;
		open = false;
		for (final ConnectionHandler handler : clients.values())
			handler.close();
		serverSocket.close();
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
	
	public long onClientConnect(final ServerClientConnectListener listener) {
		final long key = random.nextLong();
		clientConnectListeners.put(key, listener);
		return key;
	}
	
	public long onClientDisconnect(final ServerClientDisconnectListener listener) {
		final long key = random.nextLong();
		clientDisconnectListeners.put(key, listener);
		return key;
	}
	
	public int getPacketSentListeners() {
		return packetSentListeners.size();
	}
	
	public int getPacketReceivedListeners() {
		return packetReceivedListeners.size();
	}
	
	public int getClientConnectListeners() {
		return clientConnectListeners.size();
	}
	
	public int getClientDisconnectListeners() {
		return clientDisconnectListeners.size();
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
	
	public boolean removeClientConnectListener(final long key) {
		if (!(clientConnectListeners.containsKey(key)))
			return false;
		clientConnectListeners.remove(key);
		return true;
	}
	
	public boolean removeClientDisconnectListener(final long key) {
		if (!(clientDisconnectListeners.containsKey(key)))
			return false;
		clientDisconnectListeners.remove(key);
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
	
	private void registerHandler(final ConnectionHandler handler) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, IllegalBlockSizeException, BadPaddingException {
		handler.setConnectionUUID(lastUUID);
		lastUUID = generateUUID();
		handler.openRaw();
		clients.put(handler.getUUID(), handler);
		handler.protocol_write(SignalModifier.SIGNAL_TYPE, handler.getUUID().toString().getBytes());
		handler.open(new SecretKeySpec(String.valueOf(handler.getUUID().toString().substring(4)).getBytes(), algorithm.getAlgorithm()));
		final int version = ByteBuffer.wrap(handler.read().getBuffer()).getInt();
		if (version != NetX.VERSION) {
			kick(handler);
			return;
		}
		handler.setNetworkThread(openHandlerThread(handler));
		for (final ServerClientConnectListener listener : clientConnectListeners.values())
			listener.onClientConnect(handler.getUUID());
	}
	
	private Thread openHandlerThread(final ConnectionHandler handler) {
		final Thread thread = threadFactory.create("Handler", () -> {
			while (handler.isOpen()) {
				try {
					final SignalHolder holder = handler.read();
					final byte signalModifier = holder.getSignalModifier();
					final byte[] buffer = holder.getBuffer();
					if (SignalModifier.isType(signalModifier))
						handleType(handler, configuration.compile(buffer));
					else if (SignalModifier.isRequest(signalModifier))
						handleRequest(handler, requestCompiler.compile(buffer));
					else if (SignalModifier.isResponse(signalModifier))
						handleResponse(handler, responseCompiler.compile(buffer));
					else
						err().println("Received Invalid Signal (Illegal State)");
				} catch (IllegalBlockSizeException e) {
					err().println("An error occurred while deciphering buffer (Illegal Block Size): " + e.getLocalizedMessage());
				} catch (BadPaddingException e) {
					err().println("An error occurred while deciphering buffer (Bad Padding): " + e.getLocalizedMessage());
				} catch (IOException e) {
					try {
						kick(handler);
					} catch (IOException _) {}
				} catch (Exception e) {
					err().println("An error occurred while compiling type: " + e.getLocalizedMessage());
				}
			}
		});
		thread.start();
		return thread;
	}
	
	private void handleType(final ConnectionHandler handler, final T t) {
		if (handler.hasAwaitConsumers())
			((Consumer<T>) handler.popAwaitConsumer()).accept(t);
		for (final PacketReceivedListener<T> listener : packetReceivedListeners.values())
			listener.onPacketReceived(t, handler.getUUID());
	}
	
	private void handleRequest(final ConnectionHandler handler, final Request request) throws IllegalBlockSizeException, BadPaddingException, IOException {
		final int requestId = request.getRequestId();
		if (!(requestHandlers.containsKey(requestId))) {
			err().println("No request handler registered for request id " + requestId);
			return;
		}
		final RequestHandler requestHandler = requestHandlers.get(request.getRequestId());
		final Response response = requestHandler.handle(request);
		final byte[] buffer = responseCompiler.decompile(response);
		handler.write(SignalModifier.SIGNAL_RESPONSE, buffer);
	}
	
	private void handleResponse(final ConnectionHandler handler, final Response response) {
		if (!(handler.hasAwaitConsumers()))
			err().println("Unable to handle response without a registered and awaiting consumer");
		((Consumer<Response>) handler.popAwaitConsumer()).accept(response);
	}
	
	private UUID generateUUID() {
		final UUID uuid = UUID.randomUUID();
		if (clients.containsKey(uuid))
			return generateUUID();
		return uuid;
	}
	
	public void kick(final UUID handler) throws IOException {
		kick(clients.get(handler));
	}
	
	protected void kick(final ConnectionHandler handler) throws IOException {
		handler.close();
		clients.remove(handler.getUUID());
		for (final ServerClientDisconnectListener listener : clientDisconnectListeners.values())
			listener.onClientDisconnect(handler.getUUID());
	}
	
	public InetAddress getHostOf(final UUID handler) {
		return getHostOf(clients.get(handler));
	}
	
	protected InetAddress getHostOf(final ConnectionHandler handler) {
		return handler.getHost();
	}
	
	public void broadcast(final T t) {
		byte[] buffer;
		try {
			buffer = configuration.decompile(t);
		} catch (CompilationException e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return;
		}
		for (final ConnectionHandler handler : clients.values()) {
			try {
				handler.write(SignalModifier.SIGNAL_TYPE, buffer);
			} catch (IllegalBlockSizeException e) {
				err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Illegal Block Size): " + e.getLocalizedMessage());
			} catch (BadPaddingException e) {
				err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Bad Padding): " + e.getLocalizedMessage());
			} catch (IOException e) {
				err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Internal): " + e.getLocalizedMessage());
			}
		}
	}
	
	public T await(final UUID handler) throws InterruptedException {
		return await(clients.get(handler));
	}
	
	protected T await(final ConnectionHandler handler) throws InterruptedException {
		return await(handler, timeout, timeUnit);
	}
	
	public T awaitAfter(final Runnable runnable, final UUID handler) throws InterruptedException {
		return awaitAfter(runnable, clients.get(handler));
	}
	
	protected T awaitAfter(final Runnable runnable, final ConnectionHandler handler) throws InterruptedException {
		return awaitAfter(runnable, handler, timeout, timeUnit);
	}
	
	public T await(final UUID handler, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		return await(clients.get(handler), timeout, timeUnit);
	}
	
	protected T await(final ConnectionHandler handler, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
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
	
	public T awaitAfter(final Runnable runnable, final UUID handler, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		return awaitAfter(runnable, clients.get(handler), timeout, timeUnit);
	}
	
	protected T awaitAfter(final Runnable runnable, final ConnectionHandler handler, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
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
	
	public void send(final UUID handler, final T t) {
		send(clients.get(handler), t);
	}
	
	protected void send(final ConnectionHandler handler, final T t) {
		byte[] buffer;
		try {
			buffer = configuration.decompile(t);
		} catch (CompilationException e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return;
		}
		try {
			handler.write(SignalModifier.SIGNAL_TYPE, buffer);
		} catch (IllegalBlockSizeException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Illegal Block Size): " + e.getLocalizedMessage());
		} catch (BadPaddingException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Bad Padding): " + e.getLocalizedMessage());
		} catch (IOException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Internal): " + e.getLocalizedMessage());
		}
	}
	
	public void sendAfter(final Runnable runnable, final UUID handler, final T t) {
		sendAfter(runnable, clients.get(handler), t);
	}
	
	protected void sendAfter(final Runnable runnable, final ConnectionHandler handler, final T t) {
		byte[] buffer;
		try {
			buffer = configuration.decompile(t);
		} catch (CompilationException e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return;
		}
		runnable.run();
		try {
			handler.write(SignalModifier.SIGNAL_TYPE, buffer);
		} catch (IllegalBlockSizeException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Illegal Block Size): " + e.getLocalizedMessage());
		} catch (BadPaddingException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Bad Padding): " + e.getLocalizedMessage());
		} catch (IOException e) {
			err().println("Unable to write to handler with UUID '" + handler.getUUID() + "', (Internal): " + e.getLocalizedMessage());
		}
	}
	
	public T sendAndAwait(final UUID handler, final T t) throws InterruptedException {
		return sendAndAwait(clients.get(handler), t);
	}
	
	protected T sendAndAwait(final ConnectionHandler handler, final T t) throws InterruptedException {
		return sendAndAwait(handler, t, timeout, timeUnit);
	}
	
	public T sendAndAwaitAfter(final Runnable runnable, final UUID handler, final T t) throws InterruptedException {
		return sendAndAwaitAfter(runnable, clients.get(handler), t);
	}
	
	protected T sendAndAwaitAfter(final Runnable runnable, final ConnectionHandler handler, final T t) throws InterruptedException {
		return sendAndAwaitAfter(runnable, handler, t, timeout, timeUnit);
	}
	
	public T sendAndAwait(final UUID handler, final T t, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		return sendAndAwait(clients.get(handler), t, timeout, timeUnit);
	}
	
	protected T sendAndAwait(final ConnectionHandler handler, final T t, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		send(handler, t);
		return await(handler, timeout, timeUnit);
	}
	
	public T sendAndAwaitAfter(final Runnable runnable, final UUID handler, final T t, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		return sendAndAwaitAfter(runnable, clients.get(handler), t, timeout, timeUnit);
	}
	
	protected T sendAndAwaitAfter(final Runnable runnable, final ConnectionHandler handler, final T t, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		sendAfter(runnable, handler, t);
		return await(handler, timeout, timeUnit);
	}
	
	public Response request(final UUID handler, final Request request) throws InterruptedException {
		return request(clients.get(handler), request);
	}
	
	protected Response request(final ConnectionHandler handler, final Request request) throws InterruptedException {
		return request(handler, request, timeout, timeUnit);
	}
	
	public Response requestAfter(final Runnable runnable, final UUID handler, final Request request) throws InterruptedException {
		return requestAfter(runnable, clients.get(handler), request);
	}
	
	protected Response requestAfter(final Runnable runnable, final ConnectionHandler handler, final Request request) throws InterruptedException {
		return requestAfter(runnable, handler, request, timeout, timeUnit);
	}
	
	public Response request(final UUID handler, final Request request, final RequestErrorCallback errorCallback) {
		return request(clients.get(handler), request, errorCallback);
	}
	
	protected Response request(final ConnectionHandler handler, final Request request, final RequestErrorCallback errorCallback) {
		return request(handler, request, timeout, timeUnit, errorCallback);
	}
	
	public Response requestAfter(final Runnable runnable, final UUID handler, final Request request, final RequestErrorCallback errorCallback) {
		return requestAfter(runnable, clients.get(handler), request, errorCallback);
	}
	
	protected Response requestAfter(final Runnable runnable, final ConnectionHandler handler, final Request request, final RequestErrorCallback errorCallback) {
		return requestAfter(runnable, handler, request, timeout, timeUnit, errorCallback);
	}
	
	public Response request(final UUID handler, final Request request, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		return request(clients.get(handler), request, timeout, timeUnit);
	}
	
	protected Response request(final ConnectionHandler handler, final Request request, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		byte[] buffer;
		try {
			buffer = requestCompiler.decompile(request);
		} catch (CompilationException e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to decompile type: " + e.getLocalizedMessage(), request);
		}
		try {
			handler.write(SignalModifier.SIGNAL_REQUEST, buffer);
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
	
	public Response requestAfter(final Runnable runnable, final UUID handler, final Request request, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		return requestAfter(runnable, clients.get(handler), request, timeout, timeUnit);
	}
	
	protected Response requestAfter(final Runnable runnable, final ConnectionHandler handler, final Request request, final long timeout, final TimeUnit timeUnit) throws InterruptedException {
		byte[] buffer;
		try {
			buffer = requestCompiler.decompile(request);
		} catch (CompilationException e) {
			err().println("Unable to decompile type: " + e.getLocalizedMessage());
			return new ErrorResponse("Unable to decompile type: " + e.getLocalizedMessage(), request);
		}
		runnable.run();
		try {
			handler.write(SignalModifier.SIGNAL_REQUEST, buffer);
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
	
	public Response request(final UUID handler, final Request request, final long timeout, final TimeUnit timeUnit, final RequestErrorCallback errorCallback) {
		return request(clients.get(handler), request, timeout, timeUnit, errorCallback);
	}
	
	protected Response request(final ConnectionHandler handler, final Request request, final long timeout, final TimeUnit timeUnit, final RequestErrorCallback errorCallback) {
		try {
			final byte[] buffer = requestCompiler.decompile(request);
			handler.write(SignalModifier.SIGNAL_REQUEST, buffer);
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
	
	public Response requestAfter(final Runnable runnable, final UUID handler, final Request request, final long timeout, final TimeUnit timeUnit, final RequestErrorCallback errorCallback) {
		return requestAfter(runnable, clients.get(handler), request, timeout, timeUnit, errorCallback);
	}
	
	protected Response requestAfter(final Runnable runnable, final ConnectionHandler handler, final Request request, final long timeout, final TimeUnit timeUnit, final RequestErrorCallback errorCallback) {
		try {
			final byte[] buffer = requestCompiler.decompile(request);
			runnable.run();
			handler.write(SignalModifier.SIGNAL_REQUEST, buffer);
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
	
	public CipherAlgorithm getAlgorithm() {
		return algorithm;
	}
	
	public void setAlgorithm(final CipherAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
	
	public int getConnectedClients() {
		return clients.size();
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
		return serverSocket.getInetAddress();
	}
	
	public int getLocalPort() {
		return serverSocket.getLocalPort();
	}
	
	public void setPort(final int port) {
		if (open)
			throw new RuntimeException("Unable to change port while opened");
		this.port = port;
	}
	
	public int getPort() {
		return port;
	}
	
}
