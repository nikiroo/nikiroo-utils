package be.nikiroo.utils.streams;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This {@link OutputStream} will change some of its content by replacing it
 * with something else.
 * 
 * @author niki
 */
public class ReplaceOutputStream extends BufferedOutputStream {
	private byte[][] froms;
	private byte[][] tos;

	/**
	 * Create a {@link ReplaceOutputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param out
	 *            the under-laying {@link OutputStream}
	 * @param from
	 *            the {@link String} to replace
	 * @param to
	 *            the {@link String} to replace with
	 */
	public ReplaceOutputStream(OutputStream out, String from, String to) {
		this(out, StreamUtils.bytes(from), StreamUtils.bytes(to));
	}

	/**
	 * Create a {@link ReplaceOutputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param out
	 *            the under-laying {@link OutputStream}
	 * @param from
	 *            the value to replace
	 * @param to
	 *            the value to replace with
	 */
	public ReplaceOutputStream(OutputStream out, byte[] from, byte[] to) {
		this(out, new byte[][] { from }, new byte[][] { to });
	}

	/**
	 * Create a {@link ReplaceOutputStream} that will replace all <tt>froms</tt>
	 * with <tt>tos</tt>.
	 * <p>
	 * Note that they will be replaced in order, and that for each <tt>from</tt>
	 * a <tt>to</tt> must correspond.
	 * 
	 * @param out
	 *            the under-laying {@link OutputStream}
	 * @param froms
	 *            the values to replace
	 * @param tos
	 *            the values to replace with
	 */
	public ReplaceOutputStream(OutputStream out, String[] froms, String[] tos) {
		this(out, StreamUtils.bytes(froms), StreamUtils.bytes(tos));
	}

	/**
	 * Create a {@link ReplaceOutputStream} that will replace all <tt>froms</tt>
	 * with <tt>tos</tt>.
	 * <p>
	 * Note that they will be replaced in order, and that for each <tt>from</tt>
	 * a <tt>to</tt> must correspond.
	 * 
	 * @param out
	 *            the under-laying {@link OutputStream}
	 * @param froms
	 *            the values to replace
	 * @param tos
	 *            the values to replace with
	 */
	public ReplaceOutputStream(OutputStream out, byte[][] froms, byte[][] tos) {
		super(out);
		bypassFlush = false;

		if (froms.length != tos.length) {
			throw new IllegalArgumentException(
					"For replacing, each FROM must have a corresponding TO");
		}

		this.froms = froms;
		this.tos = tos;
	}

	@Override
	public void flush(boolean includingSubStream) throws IOException {
		// Note: very simple, not efficient implementation, sorry.
		while (start < stop) {
			boolean replaced = false;
			for (int i = 0; i < froms.length; i++) {
				if (froms[i] != null
						&& froms[i].length > 0
						&& StreamUtils
								.startsWith(froms[i], buffer, start, stop)) {
					if (tos[i] != null && tos[i].length > 0) {
						out.write(tos[i]);
						bytesWritten += tos[i].length;
					}

					start += froms[i].length;
					replaced = true;
					break;
				}
			}

			if (!replaced) {
				out.write(buffer[start++]);
				bytesWritten++;
			}
		}

		start = 0;
		stop = 0;

		if (includingSubStream) {
			out.flush();
		}
	}
}
