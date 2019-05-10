package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;

import be.nikiroo.utils.streams.Base64InputStream;
import be.nikiroo.utils.streams.Base64OutputStream;

/**
 * Small utility class to do AES encryption/decryption.
 * <p>
 * For the moment, it is multi-thread compatible, but beware:
 * <ul>
 * <li>The encrypt/decrypt calls are serialized</li>
 * <li>The streams are independent and thus parallel</li>
 * </ul>
 * <p>
 * Do not assume it is actually secure, it is actually not.
 * <p>
 * It just here to offer a more-or-less protected exchange of data because
 * anonymous and self-signed certificates backed SSL is against Google wishes
 * (so, don't even try, they own Internet).
 * 
 * @author niki
 */
public class CryptUtils {
	// // any size
	// static private final String AES_NAME = "AES/CFB128/NoPadding";

	// // 16bit blocks
	// static private final String AES_NAME = "AES/CBC/NoPadding";
// static private final String RSA_NAME = "RSA/ECB/PKCS1Padding";
	
	static private final String AES_NAME = "AES/CFB128/NoPadding";

	static private final String RSA_NAME = "RSA/CFB128/PKCS1Padding";

	// AES/CBC/NoPadding
	// AES/ECB/NoPadding
	// RSA/ECB/PKCS1Padding (1024/2048)

	private Cipher ecipher;
	private Cipher dcipher;
	private boolean symmetric;

	// Symmetric only
	private Key key;

	// Asymmetric only
	private Key encKey;
	private Key decKey;

	/**
	 * Use the static generate* methods instead.
	 * 
	 * @param symmetric
	 *            TRUE for symmetric encryption, FALSE for asymmetric encryption
	 */
	protected CryptUtils(boolean symmetric) {
		this.symmetric = symmetric;
	}

	/**
	 * Wrap the given {@link InputStream} so it is transparently encrypted by
	 * the current {@link CryptUtils}.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 * @return the auto-encode {@link InputStream}
	 */
	public InputStream encrypt(InputStream in) {
		Cipher ecipher = newCipher(Cipher.ENCRYPT_MODE);
		return new CipherInputStream(in, ecipher);
	}

	/**
	 * Wrap the given {@link InputStream} so it is transparently encrypted by
	 * the current {@link CryptUtils} and encoded in base64.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 * 
	 * @return the auto-encode {@link InputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream encrypt64(InputStream in) throws IOException {
		return new Base64InputStream(encrypt(in), true);
	}

	/**
	 * Wrap the given {@link OutputStream} so it is transparently encrypted by
	 * the current {@link CryptUtils}.
	 * 
	 * @param out
	 *            the {@link OutputStream} to wrap
	 * 
	 * @return the auto-encode {@link OutputStream}
	 */
	public OutputStream encrypt(OutputStream out) {
		Cipher ecipher = newCipher(Cipher.ENCRYPT_MODE);
		return new CipherOutputStream(out, ecipher);
	}

	/**
	 * Wrap the given {@link OutputStream} so it is transparently encrypted by
	 * the current {@link CryptUtils} and encoded in base64.
	 * 
	 * @param out
	 *            the {@link OutputStream} to wrap
	 * 
	 * @return the auto-encode {@link OutputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public OutputStream encrypt64(OutputStream out) throws IOException {
		return encrypt(new Base64OutputStream(out, true));
	}

	/**
	 * Wrap the given {@link OutputStream} so it is transparently decoded by the
	 * current {@link CryptUtils}.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 * 
	 * @return the auto-decode {@link InputStream}
	 */
	public InputStream decrypt(InputStream in) {
		Cipher dcipher = newCipher(Cipher.DECRYPT_MODE);
		return new CipherInputStream(in, dcipher);
	}

	/**
	 * Wrap the given {@link OutputStream} so it is transparently decoded by the
	 * current {@link CryptUtils} and decoded from base64.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 * 
	 * @return the auto-decode {@link InputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream decrypt64(InputStream in) throws IOException {
		return decrypt(new Base64InputStream(in, false));
	}

	/**
	 * Wrap the given {@link OutputStream} so it is transparently decoded by the
	 * current {@link CryptUtils}.
	 * 
	 * @param out
	 *            the {@link OutputStream} to wrap
	 * @return the auto-decode {@link OutputStream}
	 */
	public OutputStream decrypt(OutputStream out) {
		Cipher dcipher = newCipher(Cipher.DECRYPT_MODE);
		return new CipherOutputStream(out, dcipher);
	}

	/**
	 * Wrap the given {@link OutputStream} so it is transparently decoded by the
	 * current {@link CryptUtils} and decoded from base64.
	 * 
	 * @param out
	 *            the {@link OutputStream} to wrap
	 * 
	 * @return the auto-decode {@link OutputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public OutputStream decrypt64(OutputStream out) throws IOException {
		return new Base64OutputStream(decrypt(out), false);
	}

	/**
	 * Create a new {@link Cipher}of the given mode (see
	 * {@link Cipher#ENCRYPT_MODE} and {@link Cipher#ENCRYPT_MODE}).
	 * 
	 * @param mode
	 *            the mode ({@link Cipher#ENCRYPT_MODE} or
	 *            {@link Cipher#ENCRYPT_MODE})
	 * 
	 * @return the new {@link Cipher}
	 */
	private Cipher newCipher(int mode) {
		try {
			Cipher cipher = Cipher.getInstance(symmetric ? AES_NAME : RSA_NAME);

			Key key = null;
			if (symmetric) {
				key = this.key;
			} else {
				key = mode == Cipher.ENCRYPT_MODE ? encKey : decKey;
			}

			// IV only for sym
			IvParameterSpec ivspec = null;
			if (true || symmetric) {
				byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
				ivspec = new IvParameterSpec(iv);
			}

			cipher.init(mode, key, ivspec);
			return cipher;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					"Cannot initialize encryption sub-system", e);
		}
	}

	/**
	 * Encrypt the data.
	 * 
	 * @param data
	 *            the data to encrypt
	 * 
	 * @return the encrypted data
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public byte[] encrypt(byte[] data) throws SSLException {
		synchronized (ecipher) {
			try {
				return ecipher.doFinal(data);
			} catch (IllegalBlockSizeException e) {
				throw new SSLException(e);
			} catch (BadPaddingException e) {
				throw new SSLException(e);
			}
		}
	}

	/**
	 * Encrypt the data.
	 * 
	 * @param data
	 *            the data to encrypt
	 * 
	 * @return the encrypted data
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public byte[] encrypt(String data) throws SSLException {
		return encrypt(StringUtils.getBytes(data));
	}

	/**
	 * Encrypt the data, then encode it into Base64.
	 * 
	 * @param data
	 *            the data to encrypt
	 * @param zip
	 *            TRUE to also compress the data in GZIP format; remember that
	 *            compressed and not-compressed content are different; you need
	 *            to know which is which when decoding
	 * 
	 * @return the encrypted data, encoded in Base64
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public String encrypt64(String data) throws SSLException {
		return encrypt64(StringUtils.getBytes(data));
	}

	/**
	 * Encrypt the data, then encode it into Base64.
	 * 
	 * @param data
	 *            the data to encrypt
	 * 
	 * @return the encrypted data, encoded in Base64
	 * 
	 * @throws SSLException
	 *             in case of I/O error (i.e., the data is not what you assumed
	 *             it was)
	 */
	public String encrypt64(byte[] data) throws SSLException {
		try {
			return StringUtils.base64(encrypt(data));
		} catch (IOException e) {
			// not exactly true, but we consider here that this error is a crypt
			// error, not a normal I/O error
			throw new SSLException(e);
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities.
	 * 
	 * @param data
	 *            the encrypted data to decode
	 * 
	 * @return the original, decoded data
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public byte[] decrypt(byte[] data) throws SSLException {
		synchronized (dcipher) {
			try {
				return dcipher.doFinal(data);
			} catch (IllegalBlockSizeException e) {
				throw new SSLException(e);
			} catch (BadPaddingException e) {
				throw new SSLException(e);
			}
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities
	 * and to be a {@link String}.
	 * 
	 * @param data
	 *            the encrypted data to decode
	 * 
	 * @return the original, decoded data,as a {@link String}
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public String decrypts(byte[] data) throws SSLException {
		try {
			return new String(decrypt(data), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is required in all confirm JVMs
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities
	 * and is a Base64 encoded value.
	 * 
	 * @param data
	 *            the encrypted data to decode in Base64 format
	 * @param zip
	 *            TRUE to also uncompress the data from a GZIP format
	 *            automatically; if set to FALSE, zipped data can be returned
	 * 
	 * @return the original, decoded data
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public byte[] decrypt64(String data) throws SSLException {
		try {
			return decrypt(StringUtils.unbase64(data));
		} catch (IOException e) {
			// not exactly true, but we consider here that this error is a crypt
			// error, not a normal I/O error
			throw new SSLException(e);
		}
	}

	/**
	 * Decode the data which is assumed to be encrypted with the same utilities
	 * and is a Base64 encoded value, then convert it into a String (this method
	 * assumes the data <b>was</b> indeed a UTF-8 encoded {@link String}).
	 * 
	 * @param data
	 *            the encrypted data to decode in Base64 format
	 * @param zip
	 *            TRUE to also uncompress the data from a GZIP format
	 *            automatically; if set to FALSE, zipped data can be returned
	 * 
	 * @return the original, decoded data
	 * 
	 * @throws SSLException
	 *             in case of I/O error
	 */
	public String decrypt64s(String data) throws SSLException {
		try {
			return new String(decrypt(StringUtils.unbase64(data)), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is required in all confirm JVMs
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// not exactly true, but we consider here that this error is a crypt
			// error, not a normal I/O error
			throw new SSLException(e);
		}
	}

	/**
	 * Small and lazy-easy way to initialize a key with {@link CryptUtils}.
	 * <p>
	 * <b>Some</b> part of the key will be used to generate an encryption key
	 * for symmetric encryption and initialize the {@link CryptUtils}; even NULL
	 * will generate something.
	 * <p>
	 * <b>This is most probably not secure. Do not use if you actually care
	 * about security.</b>
	 * 
	 * @param key
	 *            the {@link String} to use as a base for the key, can be NULL
	 *            (which will generate a key for NULL)
	 * 
	 * @return the new instance
	 */
	static public CryptUtils generateSymmetric(String key) {
		return generateSymmetric(StringUtils.getMd5Hash("" + key).getBytes());
	}

	/**
	 * Create a new instance of {@link CryptUtils} with the given keys.
	 * <p>
	 * The key <b>must</b> be exactly the right size.
	 * 
	 * @param bytes
	 *            the array, which <b>must</b> be of 128 bits (32 bytes) for
	 *            symmetric encryption
	 * 
	 * @return the new instance
	 */
	static public CryptUtils generateSymmetric(byte[] bytes) {
		try {
			CryptUtils me = new CryptUtils(true);

			if (bytes == null || bytes.length != 32) {
				throw new InvalidKeySpecException(
						"The size of the key must be of 128 bits (32 bytes) for symmetric encryption, it is: "
								+ (bytes == null ? "null" : "" + bytes.length)
								+ " bytes");
			}

			me.key = new SecretKeySpec(bytes, "AES");
			me.ecipher = me.newCipher(Cipher.ENCRYPT_MODE);
			me.dcipher = me.newCipher(Cipher.DECRYPT_MODE);

			return me;
		} catch (InvalidKeySpecException e) {
			// We made sure that the key is correct, so nothing here
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Create a new instance of {@link CryptUtils} with the given keys.
	 * <p>
	 * The key <b>must</b> be correctly encoded as (sa1, sa2, sb1, sb2, a, a,
	 * a... b, b, b...) where :
	 * <ul>
	 * <li>a: is the decryption key</li>
	 * <li>b: the encryption key</li>
	 * <li>sa1, sa2: the two bytes specifying the size of a (size = sa1 * 100 +
	 * sa2)</li>
	 * <li>sb1, sb2: the two bytes specifying the size of b (size = sb1 * 100 +
	 * sb2)</li>
	 * </ul>
	 * 
	 * @param bytes
	 *            the array, which <b>must</b> be correctly encoded
	 * 
	 * @throws InvalidKeySpecException
	 *             if the key is badly encoded
	 */
	static public CryptUtils generateAsymmetric(byte[] bytes)
			throws InvalidKeySpecException {
		CryptUtils me = new CryptUtils(false);

		if (bytes == null || bytes.length < 6) {
			throw new InvalidKeySpecException("Invalid for asymmetric, it is: "
					+ (bytes == null ? "null" : "" + bytes.length) + " bytes");
		}

		int sizeDec = 100 * bytes[0] + bytes[1];
		int sizeEnc = 100 * bytes[2] + bytes[3];

		if (bytes.length != sizeDec + sizeEnc + 4) {
			throw new InvalidKeySpecException("Invalid for asymmetric, it is: "
					+ bytes.length + " bytes, but is described as "
					+ (sizeDec + sizeEnc + 4) + " bytes");
		}

		try {
			KeyFactory kf = KeyFactory.getInstance("RSA");
			me.decKey = kf.generatePublic(new X509EncodedKeySpec(Arrays
					.copyOfRange(bytes, 4, 4 + sizeDec)));
			me.encKey = kf
					.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(
							Arrays.copyOfRange(bytes, 4 + sizeDec, 4 + sizeDec
									+ sizeEnc)));
		} catch (NoSuchAlgorithmException e) {
			// All conforming JVM implementations support RSA
			e.printStackTrace();
		}

		me.ecipher = me.newCipher(Cipher.ENCRYPT_MODE);
		me.dcipher = me.newCipher(Cipher.DECRYPT_MODE);

		return me;
	}

	/**
	 * Create a new instance of {@link CryptUtils} with a randomly generated key
	 * pair.
	 * 
	 */
	static public CryptUtils generateAsymmetric() {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(1024);
			KeyPair kp = gen.generateKeyPair();
			byte[] pub = kp.getPublic().getEncoded();
			byte[] priv = kp.getPrivate().getEncoded();

			byte[] bytes = new byte[4 + pub.length + priv.length];
			bytes[0] = (byte) (pub.length / 100);
			bytes[1] = (byte) (pub.length % 100);
			bytes[2] = (byte) (priv.length / 100);
			bytes[3] = (byte) (priv.length % 100);
			System.arraycopy(pub, 0, bytes, 4, pub.length);
			System.arraycopy(priv, 0, bytes, 4 + pub.length, priv.length);

			return generateAsymmetric(bytes);
		} catch (Exception e) {
			// Keys should always have this size
			e.printStackTrace();
			return null;
		}
	}
}
