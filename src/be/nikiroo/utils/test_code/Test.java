package be.nikiroo.utils.test_code;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import be.nikiroo.utils.Cache;
import be.nikiroo.utils.CacheMemory;
import be.nikiroo.utils.CryptUtils;
import be.nikiroo.utils.Downloader;
import be.nikiroo.utils.main.bridge;
import be.nikiroo.utils.main.img2aa;
import be.nikiroo.utils.main.justify;
import be.nikiroo.utils.test.TestLauncher;

/**
 * Tests for nikiroo-utils.
 * 
 * @author niki
 */
public class Test extends TestLauncher {
	/**
	 * Start the tests.
	 * 
	 * @param args
	 *            the arguments (which are passed as-is to the other test
	 *            classes)
	 */
	public Test(String[] args) {
		super("Nikiroo-utils", args);

		if (false) {
			try {
				KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
				gen.initialize(1024); // size
				KeyPair kp = gen.generateKeyPair();
				String s = kp.getPublic().getFormat() + ", "
						+ kp.getPublic().getEncoded().length + " bytes";
				System.out.println("pub: " + s);
				s = kp.getPrivate().getFormat() + ", "
						+ kp.getPrivate().getEncoded().length + " bytes";
				System.out.println("priv: " + s);

				byte[] pub = kp.getPublic().getEncoded();
				byte[] priv = kp.getPrivate().getEncoded();

				byte[] bytes = new byte[4 + pub.length + priv.length];
				bytes[0] = (byte) (pub.length / 100);
				bytes[1] = (byte) (pub.length % 100);
				bytes[2] = (byte) (priv.length / 100);
				bytes[3] = (byte) (priv.length % 100);
				System.arraycopy(pub, 0, bytes, 4, pub.length);
				System.arraycopy(priv, 0, bytes, 4 + pub.length, priv.length);

				CryptUtils.generateAsymmetric(bytes);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
		}

		setDetails(true);

		// addSeries(new ProgressTest(args));
		// addSeries(new BundleTest(args));
		// addSeries(new IOUtilsTest(args));
		// addSeries(new VersionTest(args));
		// addSeries(new SerialTest(args));
		// addSeries(new SerialServerTest(args));
		// addSeries(new StringUtilsTest(args));
		// addSeries(new TempFilesTest(args));
		addSeries(new CryptUtilsTest(args));
		// addSeries(new BufferedInputStreamTest(args));
		// addSeries(new NextableInputStreamTest(args));
		// addSeries(new ReplaceInputStreamTest(args));
		// addSeries(new BufferedOutputStreamTest(args));
		// addSeries(new ReplaceOutputStreamTest(args));

		// TODO: test cache and downloader
		Cache cache = null;
		CacheMemory memcache = null;
		Downloader downloader = null;

		// To include the sources:
		img2aa siu;
		justify ssu;
		bridge aa;
	}

	/**
	 * Main entry point of the program.
	 * 
	 * @param args
	 *            the arguments passed to the {@link TestLauncher}s.
	 */
	static public void main(String[] args) {
		System.exit(new Test(args).launch());
	}
}
