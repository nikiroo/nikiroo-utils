package be.nikiroo.fanfix.reader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.serial.SerialUtils;

/**
 * The class that handles the different {@link Story} readers you can use.
 * <p>
 * All the readers should be accessed via {@link BasicReader#getReader()}.
 * 
 * @author niki
 */
public abstract class BasicReader implements Reader {
	private static BasicLibrary defaultLibrary = Instance.getLibrary();
	private static ReaderType defaultType = ReaderType.GUI;

	private BasicLibrary lib;
	private MetaData meta;
	private Story story;
	private int chapter;

	/**
	 * Take the default reader type configuration from the config file.
	 */
	static {
		String typeString = Instance.getConfig().getString(Config.READER_TYPE);
		if (typeString != null && !typeString.isEmpty()) {
			try {
				ReaderType type = ReaderType.valueOf(typeString.toUpperCase());
				defaultType = type;
			} catch (IllegalArgumentException e) {
				// Do nothing
			}
		}
	}

	@Override
	public synchronized Story getStory(Progress pg) {
		if (story == null) {
			story = getLibrary().getStory(meta.getLuid(), pg);
		}

		return story;
	}

	@Override
	public BasicLibrary getLibrary() {
		if (lib == null) {
			lib = defaultLibrary;
		}

		return lib;
	}

	@Override
	public void setLibrary(BasicLibrary lib) {
		this.lib = lib;
	}

	@Override
	public synchronized MetaData getMeta() {
		return meta;
	}

	@Override
	public synchronized void setMeta(MetaData meta) throws IOException {
		setMeta(meta == null ? null : meta.getLuid()); // must check the library
	}

	@Override
	public synchronized void setMeta(String luid) throws IOException {
		story = null;
		meta = getLibrary().getInfo(luid);

		if (meta == null) {
			throw new IOException("Cannot retrieve story from library: " + luid);
		}
	}

	@Override
	public synchronized void setMeta(URL source, Progress pg)
			throws IOException {
		BasicSupport support = BasicSupport.getSupport(source);
		if (support == null) {
			throw new IOException("URL not supported: " + source.toString());
		}

		story = support.process(pg);
		if (story == null) {
			throw new IOException(
					"Cannot retrieve story from external source: "
							+ source.toString());
		}

		meta = story.getMeta();
	}

	@Override
	public int getChapter() {
		return chapter;
	}

	@Override
	public void setChapter(int chapter) {
		this.chapter = chapter;
	}

	/**
	 * Return a new {@link BasicReader} ready for use if one is configured.
	 * <p>
	 * Can return NULL if none are configured.
	 * 
	 * @return a {@link BasicReader}, or NULL if none configured
	 */
	public static Reader getReader() {
		try {
			if (defaultType != null) {
				return (Reader) SerialUtils.createObject(defaultType
						.getTypeName());
			}
		} catch (Exception e) {
			Instance.getTraceHandler().error(
					new Exception("Cannot create a reader of type: "
							+ defaultType + " (Not compiled in?)", e));
		}

		return null;
	}

	/**
	 * The default {@link Reader.ReaderType} used when calling
	 * {@link BasicReader#getReader()}.
	 * 
	 * @return the default type
	 */
	public static ReaderType getDefaultReaderType() {
		return defaultType;
	}

	/**
	 * The default {@link Reader.ReaderType} used when calling
	 * {@link BasicReader#getReader()}.
	 * 
	 * @param defaultType
	 *            the new default type
	 */
	public static void setDefaultReaderType(ReaderType defaultType) {
		BasicReader.defaultType = defaultType;
	}

	/**
	 * Change the default {@link LocalLibrary} to open with the
	 * {@link BasicReader}s.
	 * 
	 * @param lib
	 *            the new {@link LocalLibrary}
	 */
	public static void setDefaultLibrary(BasicLibrary lib) {
		BasicReader.defaultLibrary = lib;
	}

	/**
	 * Return an {@link URL} from this {@link String}, be it a file path or an
	 * actual {@link URL}.
	 * 
	 * @param sourceString
	 *            the source
	 * 
	 * @return the corresponding {@link URL}
	 * 
	 * @throws MalformedURLException
	 *             if this is neither a file nor a conventional {@link URL}
	 */
	public static URL getUrl(String sourceString) throws MalformedURLException {
		if (sourceString == null || sourceString.isEmpty()) {
			throw new MalformedURLException("Empty url");
		}

		URL source = null;
		try {
			source = new URL(sourceString);
		} catch (MalformedURLException e) {
			File sourceFile = new File(sourceString);
			source = sourceFile.toURI().toURL();
		}

		return source;
	}

	/**
	 * Open the {@link Story} with an external reader (the program will be
	 * passed the main file associated with this {@link Story}).
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to select the {@link Story} from
	 * @param luid
	 *            the {@link Story} LUID
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	@Override
	public void openExternal(BasicLibrary lib, String luid) throws IOException {
		MetaData meta = lib.getInfo(luid);
		File target = lib.getFile(luid, null);

		openExternal(meta, target);
	}

	/**
	 * Open the {@link Story} with an external reader (the program will be
	 * passed the given target file).
	 * 
	 * @param meta
	 *            the {@link Story} to load
	 * @param target
	 *            the target {@link File}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected void openExternal(MetaData meta, File target) throws IOException {
		String program = null;
		if (meta.isImageDocument()) {
			program = Instance.getUiConfig().getString(
					UiConfig.IMAGES_DOCUMENT_READER);
		} else {
			program = Instance.getUiConfig().getString(
					UiConfig.NON_IMAGES_DOCUMENT_READER);
		}

		if (program != null && program.trim().isEmpty()) {
			program = null;
		}

		start(target, program);
	}

	/**
	 * Start a file and open it with the given program if given or the first
	 * default system starter we can find.
	 * 
	 * @param target
	 *            the target to open
	 * @param program
	 *            the program to use or NULL for the default system starter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected void start(File target, String program) throws IOException {
		if (program == null) {
			boolean ok = false;
			for (String starter : new String[] { "xdg-open", "open", "see",
					"start", "run" }) {
				try {
					Instance.getTraceHandler().trace(
							"starting external program");
					Runtime.getRuntime().exec(
							new String[] { starter, target.getAbsolutePath() });
					ok = true;
					break;
				} catch (IOException e) {
				}
			}
			if (!ok) {
				throw new IOException("Cannot find a program to start the file");
			}
		} else {
			Instance.getTraceHandler().trace("starting external program");
			Runtime.getRuntime().exec(
					new String[] { program, target.getAbsolutePath() });
		}
	}
}
