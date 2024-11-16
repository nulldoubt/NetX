package me.nulldoubt.netx.binaries.connection;

import me.nulldoubt.netx.NetX;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public final class ConnectionCipher {
	
	private final NetX.CipherAlgorithm algorithm;
	
	private Cipher encoder;
	private Cipher decoder;
	
	private boolean open;
	
	public ConnectionCipher(final NetX.CipherAlgorithm algorithm) {
		this.algorithm = algorithm;
		open = false;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public void open(final Key key) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (open)
			return;
		
		encoder = Cipher.getInstance(algorithm.getAlgorithm());
		encoder.init(Cipher.ENCRYPT_MODE, key);
		
		decoder = Cipher.getInstance(algorithm.getAlgorithm());
		decoder.init(Cipher.DECRYPT_MODE, key);
		
		open = true;
	}
	
	public byte[] encode(final byte[] buffer) throws IllegalBlockSizeException, BadPaddingException {
		return encoder.doFinal(buffer);
	}
	
	public byte[] decode(final byte[] buffer) throws IllegalBlockSizeException, BadPaddingException {
		return decoder.doFinal(buffer);
	}
	
	public NetX.CipherAlgorithm getAlgorithm() {
		return algorithm;
	}
	
}
