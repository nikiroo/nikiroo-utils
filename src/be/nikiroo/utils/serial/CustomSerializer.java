package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.NextableInputStream;
import be.nikiroo.utils.NextableInputStreamStep;

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
		SerialUtils.write(out, "custom^");
		SerialUtils.write(out, getType());
		SerialUtils.write(out, "^");
		// TODO: manage ENTER
		toStream(out, value);

		return true;
	}

	public Object decode(InputStream in) throws IOException {
		// TODO: manage ENTER
		// TODO read and skip "custom^......^": next(), next(), nextAll() ?
		NextableInputStream stream = new NextableInputStream(in,
				new NextableInputStreamStep('^'));

		try {
			if (!stream.next()) {
				throw new IOException("Cannot find the first custom^ element");
			}

			String custom = IOUtils.readSmallStream(stream);
			if (!"custom".equals(custom)) {
				throw new IOException(
						"Cannot find the first custom^ element, it is: "
								+ custom + "^");
			}

			if (!stream.next()) {
				throw new IOException("Cannot find the second custom^"
						+ getType() + " element");
			}

			String type = IOUtils.readSmallStream(stream);
			if (!getType().equals(type)) {
				throw new IOException("Cannot find the second custom^"
						+ getType() + " element, it is: custom^" + type + "^");
			}

			if (!stream.nextAll()) {
				throw new IOException("Cannot find the third custom^"
						+ getType() + "^value element");
			}

			// TODO: manage ENTER
			return fromStream(stream);
		} finally {
			stream.close(false);
		}
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
