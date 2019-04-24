package be.nikiroo.utils.serial.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.ssl.SSLException;

import be.nikiroo.utils.CryptUtils;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.Exporter;
import be.nikiroo.utils.serial.Importer;

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
	private Version version;
	private Version clientVersion;

	private CryptUtils crypt;

	private Object lock = new Object();
	private InputStream in;
	private OutputStream out;
	private boolean contentToSend;

	private long bytesReceived;
	private long bytesSent;

	/**
	 * Method that will be called when an action is performed on either the
	 * client or server this {@link ConnectAction} represent.
	 * 
	 * @param version
	 *            the counter part version
	 * 
	 * @throws Exception
	 *             in case of I/O error
	 */
	abstract protected void action(Version version) throws Exception;

	/**
	 * Method called when we negotiate the version with the client.
	 * <p>
	 * Thus, it is only called on the server.
	 * <p>
	 * Will return the actual server version by default.
	 * 
	 * @param clientVersion
	 *            the client version
	 * 
	 * @return the version to send to the client
	 */
	abstract protected Version negotiateVersion(Version clientVersion);

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
	 * @param version
	 *            the version of this client-or-server
	 */
	protected ConnectAction(Socket s, boolean server, String key,
			Version version) {
		this.s = s;
		this.server = server;
		if (key != null) {
			crypt = new CryptUtils(key);
		}

		if (version == null) {
			this.version = new Version();
		} else {
			this.version = version;
		}

		clientVersion = new Version();
	}

	/**
	 * The version of this client-or-server.
	 * 
	 * @return the version
	 */
	public Version getVersion() {
		return version;
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
			in = s.getInputStream();
			try {
				out = s.getOutputStream();
				try {
					if (server) {
						String line;
						try {
							line = readLine(in);
						} catch (SSLException e) {
							out.write("Unauthorized\n".getBytes());
							throw e;
						}

						if (line != null && line.startsWith("VERSION ")) {
							// "VERSION client-version" (VERSION 1.0.0)
							Version clientVersion = new Version(
									line.substring("VERSION ".length()));
							this.clientVersion = clientVersion;
							Version v = negotiateVersion(clientVersion);
							if (v == null) {
								v = new Version();
							}

							sendString("VERSION " + v.toString());
						}

						action(clientVersion);
					} else {
						String v = sendString("VERSION " + version.toString());
						if (v != null && v.startsWith("VERSION ")) {
							v = v.substring("VERSION ".length());
						}

						action(new Version(v));
					}
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

				return new Importer().read(in).getValue();
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
	private String readLine(InputStream in) throws IOException {
		if (inReader == null) {
			inReader = new BufferedReader(new InputStreamReader(in));
		}
		String line = inReader.readLine();
		if (line != null) {
			bytesReceived += line.length();
			if (crypt != null) {
				line = crypt.decrypt64s(line, false);
			}
		}

		return line;
	}

	private BufferedReader inReader;

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
		out.write("\n".getBytes("UTF-8"));
		bytesSent++;
	}
}