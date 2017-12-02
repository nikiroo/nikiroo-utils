package be.nikiroo.fanfix.library;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.server.ConnectActionClientObject;

/**
 * This {@link BasicLibrary} will access a remote server to list the available
 * stories, and download the ones you try to load to the local directory
 * specified in the configuration.
 * 
 * @author niki
 */
public class RemoteLibrary extends BasicLibrary {
	private String host;
	private int port;
	private final String key;

	/**
	 * Create a {@link RemoteLibrary} linked to the given server.
	 * 
	 * @param key
	 *            the key that will allow us to exchange information with the
	 *            server
	 * @param host
	 *            the host to contact or NULL for localhost
	 * @param port
	 *            the port to contact it on
	 */
	public RemoteLibrary(String key, String host, int port) {
		this.key = key;
		this.host = host;
		this.port = port;
	}

	@Override
	public String getLibraryName() {
		return host + ":" + port;
	}

	@Override
	public BufferedImage getCover(final String luid) {
		final BufferedImage[] result = new BufferedImage[1];

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = send(new Object[] { key, "GET_COVER", luid });
					result[0] = (BufferedImage) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public BufferedImage getSourceCover(final String source) {
		final BufferedImage[] result = new BufferedImage[1];

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = send(new Object[] { key, "GET_SOURCE_COVER",
							source });
					result[0] = (BufferedImage) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public synchronized Story getStory(final String luid, Progress pg) {
		final Progress pgF = pg;
		final Story[] result = new Story[1];

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;
					if (pg == null) {
						pg = new Progress();
					}

					Object rep = send(new Object[] { key, "GET_STORY", luid });

					MetaData meta = null;
					if (rep instanceof MetaData) {
						meta = (MetaData) rep;
						if (meta.getWords() <= Integer.MAX_VALUE) {
							pg.setMinMax(0, (int) meta.getWords());
						}
					}

					List<Object> list = new ArrayList<Object>();
					for (Object obj = send(null); obj != null; obj = send(null)) {
						list.add(obj);
						pg.add(1);
					}

					result[0] = RemoteLibraryServer.rebuildStory(list);
					pg.done();
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public synchronized Story save(final Story story, final String luid,
			Progress pg) throws IOException {
		final Progress pgF = pg;

		new ConnectActionClientObject(host, port, true) {
			@Override
			public void action(Version serverVersion) throws Exception {
				Progress pg = pgF;
				if (pg == null) {
					pg = new Progress();
				}

				if (story.getMeta().getWords() <= Integer.MAX_VALUE) {
					pg.setMinMax(0, (int) story.getMeta().getWords());
				}

				send(new Object[] { key, "SAVE_STORY", luid });

				List<Object> list = RemoteLibraryServer.breakStory(story);
				for (Object obj : list) {
					send(obj);
					pg.add(1);
				}

				send(null);
				pg.done();
			}

			@Override
			protected void onError(Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}.connect();

		// because the meta changed:
		clearCache();
		story.setMeta(getInfo(luid));

		return story;
	}

	@Override
	public synchronized void delete(final String luid) throws IOException {
		new ConnectActionClientObject(host, port, true) {
			@Override
			public void action(Version serverVersion) throws Exception {
				send(new Object[] { key, "DELETE_STORY", luid });
			}

			@Override
			protected void onError(Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}.connect();
	}

	@Override
	public void setSourceCover(final String source, final String luid) {
		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					send(new Object[] { key, "SET_SOURCE_COVER", source, luid });
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	@Override
	public synchronized File getFile(final String luid, Progress pg) {
		throw new java.lang.InternalError(
				"Operation not supportorted on remote Libraries");
	}

	/**
	 * Check if this {@link RemoteLibraryServer} is able to connect and identify
	 * to the remote server.
	 * 
	 * @return TRUE if it is online
	 */
	public boolean isOnline() {
		final Boolean[] result = new Boolean[1];

		result[0] = false;
		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = send(new Object[] { key, "PING" });
					result[0] = "PONG".equals(rep);
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) {
		final Progress pgF = pg;
		final List<MetaData> metas = new ArrayList<MetaData>();

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;
					if (pg == null) {
						pg = new Progress();
					}

					Object rep = send(new Object[] { key, "GET_METADATA", "*" });

					while (true) {
						if (!RemoteLibraryServer.updateProgress(pg, rep)) {
							break;
						}

						rep = send(null);
					}

					for (MetaData meta : (MetaData[]) rep) {
						metas.add(meta);
					}
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return metas;
	}

	@Override
	protected void clearCache() {
	}

	// The following methods are only used by Save and Delete in BasicLibrary:

	@Override
	protected int getNextId() {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected void doDelete(String luid) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected Story doSave(Story story, Progress pg) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}
}
