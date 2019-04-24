package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class CustomSerializer {

	protected abstract void toStream(OutputStream out, Object value)
			throws IOException;

	protected abstract Object fromStream(InputStream in) throws IOException;

	protected abstract String getType();

	/**
	 * Encode the object into the given {@link OutputStream} if supported.
	 * 
	 * @param out
	 *            the builder to append to
	 * @param value
	 *            the object to encode
	 * 
	 * @return FALSE if the value is not supported, TRUE if the operation was
	 *         successful (if the value is supported by the operation was not
	 *         successful, you will get an {@link IOException})
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public boolean encode(OutputStream out, Object value) throws IOException {
		if (!isSupported(value)) {
			return false;
		}

		SerialUtils.write(out, "custom^");
		SerialUtils.write(out, getType());
		SerialUtils.write(out, "^");
		toStream(out, value);

		return true;
	}

	public Object decode(String encodedValue) throws IOException {
		return fromString((String) SerialUtils.decode(contentOf(encodedValue)));
	}

	public static boolean isCustom(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);

		return pos1 >= 0 && pos2 >= 0 && encodedValue.startsWith("custom^");
	}

	public static String typeOf(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);
		String type = encodedValue.substring(pos1 + 1, pos2);

		return type;
	}

	public static String contentOf(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);
		String encodedContent = encodedValue.substring(pos2 + 1);

		return encodedContent;
	}
}
