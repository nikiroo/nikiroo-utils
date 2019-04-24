package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import be.nikiroo.utils.StringUtils;

/**
 * A simple class to serialise objects to {@link String}.
 * <p>
 * This class does not support inner classes (it does support nested classes,
 * though).
 * 
 * @author niki
 */
public class Exporter {
	private Map<Integer, Object> map;
	private OutputStream out;

	/**
	 * Create a new {@link Exporter}.
	 */
	public Exporter(OutputStream out) {
		if (out == null) {
			throw new NullPointerException(
					"Cannot create an be.nikiroo.utils.serials.Exporter that will export to NULL");
		}

		this.out = out;
		map = new HashMap<Integer, Object>();
	}

	/**
	 * Serialise the given object and add it to the list.
	 * <p>
	 * <b>Important: </b>If the operation fails (with a
	 * {@link NotSerializableException}), the {@link Exporter} will be corrupted
	 * (will contain bad, most probably not importable data).
	 * 
	 * @param o
	 *            the object to serialise
	 * @return this (for easier appending of multiple values)
	 * 
	 * @throws NotSerializableException
	 *             if the object cannot be serialised (in this case, the
	 *             {@link Exporter} can contain bad, most probably not
	 *             importable data)
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Exporter append(Object o) throws NotSerializableException,
			IOException {
		SerialUtils.append(out, o, map);
		return this;
	}

	/**
	 * Append the exported items in a serialised form into the given
	 * {@link OutputStream}.
	 * 
	 * @param out
	 *            the {@link OutputStream}
	 * @param b64
	 *            TRUE to have BASE64-coded content, FALSE to have raw content,
	 *            NULL to let the system decide
	 * @param zip
	 *            TRUE to zip the BASE64 output if the output is indeed in
	 *            BASE64 format, FALSE not to
	 */
	public void appendTo(OutputStream out, Boolean b64, boolean zip) {
		if (b64 == null && out.length() < 128) {
			b64 = false;
		}

		if (b64 == null || b64) {
			try {
				String zipped = StringUtils.base64(out.toString(), zip);
				if (b64 != null || zipped.length() < out.length() - 4) {
					SerialUtils.write(out, zip ? "ZIP:" : "B64:");
					SerialUtils.write(out, zipped);
					return;
				}
			} catch (IOException e) {
				throw new RuntimeException(
						"Base64 conversion of data failed, maybe not enough memory?",
						e);
			}
		}

		out.append(out);
	}
}