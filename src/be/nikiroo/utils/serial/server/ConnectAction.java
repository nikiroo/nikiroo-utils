package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.ssl.SSLException;

import be.nikiroo.utils.CryptUtils;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.serial.Exporter;
import be.nikiroo.utils.serial.Importer;
import be.nikiroo.utils.streams.NextableInputStream;
import be.nikiroo.utils.streams.NextableInputStreamStep;

/**
 * Base class used for the client/server basic handling.
 * <p>
 * It represents a single action: a client is expected to only execute one
 * action, while a server is expected to execute one action for each client
 * action.
 * 
 * @author niki
 */
abstract class ConnectAction {
	private Socket s;
	private boolean server;

	private CryptUtils crypt;

	private Object lock = new Object();
	private NextableInputStream in;
	private OutputStream out;
	private boolean contentToSend;

	private long bytesReceived;
	private long bytesSent;

	/**
	 * Method that will be called when an action is performed on either the
	 * client or server this {@link ConnectAction} represent.
	 * 
	 * @throws Exception
	 *             in case of I/O error
	 */
	abstract protected void action() throws Exception;

	/**
	 * Handler called when an unexpected error occurs in the code.
	 * 
	 * @param e
	 *            the exception that occurred, SSLException usually denotes a
	 *            crypt error
	 */
	abstract protected void onError(Exception e);

	/**
	 * Create a new {@link ConnectAction}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param server
	 *            TRUE for a server action, FALSE for a client action (will
	 *            impact the process)
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 */
	protected ConnectAction(Socket s, boolean server, String key) {
		this.s = s;
		this.server = server;
		if (key != null) {
			crypt = new CryptUtils(key);
		}
	}

	/**
	 * The total amount of bytes received.
	 * 
	 * @return the amount of bytes received
	 */
	public long getBytesReceived() {
		return bytesReceived;
	}

	/**
	 * The total amount of bytes sent.
	 * 
	 * @return the amount of bytes sent
	 */
	public long getBytesSent() {
		return bytesSent;
	}

	/**
	 * Actually start the process (this is synchronous).
	 */
	public void connect() {
		try {
			in = new NextableInputStream(s.getInputStream(),
					new NextableInputStreamStep('\b'));

			try {
				out = s.getOutputStream();
				try {
					action();
				} finally {
					out.close();
					out = null;
				}
			} finally {
				in.close();
				in = null;
			}
		} catch (Exception e) {
			onError(e);
		} finally {
			try {
				s.close();
			} catch (Exception e) {
				onError(e);
			}
		}
	}

	/**
	 * Serialise and send the given object to the counter part (and, only for
	 * client, return the deserialised answer -- the server will always receive
	 * NULL).
	 * 
	 * @param data
	 *            the data to send
	 * 
	 * @return the answer (which can be NULL if no answer, or NULL for an answer
	 *         which is NULL) if this action is a client, always NULL if it is a
	 *         server
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws NoSuchFieldException
	 *             if the serialised data contains information about a field
	 *             which does actually not exist in the class we know of
	 * @throws NoSuchMethodException
	 *             if a class described in the serialised data cannot be created
	 *             because it is not compatible with this code
	 * @throws ClassNotFoundException
	 *             if a class described in the serialised data cannot be found
	 */
	protected Object sendObject(Object data) throws IOException,
			NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
		synchronized (lock) {

			new Exporter(out).append(data);
			out.write('\b');

			if (server) {
				out.flush();
				return null;
			}

			contentToSend = true;
			try {
				return recObject();
			} catch (NullPointerException e) {
				// We accept no data here
			}

			return null;
		}
	}

	/**
	 * Reserved for the server: flush the data to the client and retrieve its
	 * answer.
	 * <p>
	 * Also used internally for the client (only do something if there is
	 * contentToSend).
	 * <p>
	 * Will only flush the data if there is contentToSend.
	 * 
	 * @return the deserialised answer (which can actually be NULL)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws NoSuchFieldException
	 *             if the serialised data contains information about a field
	 *             which does actually not exist in the class we know of
	 * @throws NoSuchMethodException
	 *             if a class described in the serialised data cannot be created
	 *             because it is not compatible with this code
	 * @throws ClassNotFoundException
	 *             if a class described in the serialised data cannot be found
	 * @throws java.lang.NullPointerException
	 *             if the counter part has no data to send
	 */
	protected Object recObject() throws IOException, NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException,
			java.lang.NullPointerException {
		synchronized (lock) {
			if (server || contentToSend) {
				if (contentToSend) {
					out.flush();
					contentToSend = false;
				}

				if (in.next()) {
					return new Importer().read(in).getValue();
				}

				throw new NullPointerException();
			}

			return null;
		}
	}

	/**
	 * Send the given string to the counter part (and, only for client, return
	 * the answer -- the server will always receive NULL).
	 * 
	 * @param line
	 *            the data to send (we will add a line feed)
	 * 
	 * @return the answer if this action is a client (without the added line
	 *         feed), NULL if it is a server
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws SSLException
	 *             in case of crypt error
	 */
	protected String sendString(String line) throws IOException {
		synchronized (lock) {
			writeLine(out, line);

			if (server) {
				out.flush();
				return null;
			}

			contentToSend = true;
			return recString();
		}
	}

	/**
	 * Reserved for the server (externally): flush the data to the client and
	 * retrieve its answer.
	 * <p>
	 * Also used internally for the client (only do something if there is
	 * contentToSend).
	 * <p>
	 * Will only flush the data if there is contentToSend.
	 * 
	 * @return the answer (which can be NULL)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws SSLException
	 *             in case of crypt error
	 */
	protected String recString() throws IOException {
		synchronized (lock) {
			if (server || contentToSend) {
				if (contentToSend) {
					out.flush();
					contentToSend = false;
				}

				return readLine(in);
			}

			return null;
		}
	}

	/**
	 * Read a possibly encrypted line.
	 * 
	 * @param in
	 *            the stream to read from
	 * @return the unencrypted line
	 * 
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws SSLException
	 *             in case of crypt error
	 */
	private String readLine(NextableInputStream in) throws IOException {
		String line = null;
		if (in.next()) {
			line = IOUtils.readSmallStream(in);
		}

		if (line != null) {
			bytesReceived += line.length();
			if (crypt != null) {
				line = crypt.decrypt64s(line, false);
			}
		}

		return line;
	}

	/**
	 * Write a line, possible encrypted.
	 * 
	 * @param out
	 *            the stream to write to
	 * @param line
	 *            the line to write
	 * @throws IOException
	 *             in case of I/O error
	 * @throws SSLException
	 *             in case of crypt error
	 */
	private void writeLine(OutputStream out, String line) throws IOException {
		if (crypt == null) {
			out.write(line.getBytes("UTF-8"));
			bytesSent += line.length();
		} else {
			// TODO: how NOT to create so many big Strings?
			String b64 = crypt.encrypt64(line, false);
			out.write(b64.getBytes("UTF-8"));
			bytesSent += b64.length();
		}
		out.write('\b');
		bytesSent++;
	}
}