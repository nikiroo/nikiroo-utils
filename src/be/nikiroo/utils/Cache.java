package be.nikiroo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * A generic cache system, with special support for {@link URL}s.
 * <p>
 * This cache also manages timeout information.
 * 
 * @author niki
 */
public class Cache {
	private File dir;
	private long tooOldChanging;
	private long tooOldStable;

	/**
	 * Create a new {@link Cache} object.
	 * 
	 * @param dir
	 *            the directory to use as cache
	 * @param hoursChanging
	 *            the number of hours after which a cached file that is thought
	 *            to change ~often is considered too old (or -1 for
	 *            "never too old")
	 * @param hoursStable
	 *            the number of hours after which a cached file that is thought
	 *            to change rarely is considered too old (or -1 for
	 *            "never too old")
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Cache(File dir, int hoursChanging, int hoursStable)
			throws IOException {
		this.dir = dir;
		this.tooOldChanging = 1000 * 60 * 60 * hoursChanging;
		this.tooOldStable = 1000 * 60 * 60 * hoursStable;

		if (dir != null && !dir.exists()) {
			dir.mkdirs();
		}

		if (dir == null || !dir.exists()) {
			throw new IOException("Cannot create the cache directory: "
					+ (dir == null ? "null" : dir.getAbsolutePath()));
		}
	}

	/**
	 * Check the resource to see if it is in the cache.
	 * 
	 * @param url
	 *            the resource to check
	 * @param allowTooOld
	 *            allow files even if they are considered too old
	 * @param stable
	 *            a stable file (that dones't change too often) -- parameter
	 *            used to check if the file is too old to keep or not
	 * 
	 * @return TRUE if it is
	 * 
	 */
	public boolean check(URL url, boolean allowTooOld, boolean stable) {
		File file = getCached(url);
		if (file.exists()) {
			if (allowTooOld || !isOld(file, stable)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Clean the cache (delete the cached items).
	 * 
	 * @param onlyOld
	 *            only clean the files that are considered too old for a stable
	 *            resource
	 * 
	 * @return the number of cleaned items
	 */
	public int clean(boolean onlyOld) {
		return clean(onlyOld, dir);
	}

	/**
	 * Trace information (info/error) generated by this class.
	 * <p>
	 * You can override it if you don't want the default sysout/syserr.
	 * 
	 * @param message
	 *            the message
	 * @param error
	 *            TRUE for error messages, FALSE for information messages
	 */
	protected void trace(String message, boolean error) {
		if (error) {
			System.err.println(message);
		} else {
			System.out.println(message);
		}
	}

	/**
	 * Clean the cache (delete the cached items) in the given cache directory.
	 * 
	 * @param onlyOld
	 *            only clean the files that are considered too old for stable
	 *            resources
	 * @param cacheDir
	 *            the cache directory to clean
	 * 
	 * @return the number of cleaned items
	 */
	private int clean(boolean onlyOld, File cacheDir) {
		int num = 0;
		for (File file : cacheDir.listFiles()) {
			if (file.isDirectory()) {
				num += clean(onlyOld, file);
			} else {
				if (!onlyOld || isOld(file, true)) {
					if (file.delete()) {
						num++;
					} else {
						trace("Cannot delete temporary file: "
								+ file.getAbsolutePath(), true);
					}
				}
			}
		}

		return num;
	}

	/**
	 * Open a resource from the cache if it exists.
	 * 
	 * @param uniqueID
	 *            the unique ID
	 * @param allowTooOld
	 *            allow files even if they are considered too old
	 * @param stable
	 *            a stable file (that dones't change too often) -- parameter
	 *            used to check if the file is too old to keep or not
	 * 
	 * @return the opened resource if found, NULL if not
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream load(String uniqueID, boolean allowTooOld, boolean stable) {
		return load(getCached(uniqueID), allowTooOld, stable);
	}

	/**
	 * Open a resource from the cache if it exists.
	 * 
	 * @param url
	 *            the resource to open
	 * @param allowTooOld
	 *            allow files even if they are considered too old
	 * @param stable
	 *            a stable file (that dones't change too often) -- parameter
	 *            used to check if the file is too old to keep or not
	 * 
	 * @return the opened resource if found, NULL if not
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream load(URL url, boolean allowTooOld, boolean stable)
			throws IOException {
		return load(getCached(url), allowTooOld, stable);
	}

	/**
	 * Open a resource from the cache if it exists.
	 * 
	 * @param url
	 *            the resource to open
	 * @param allowTooOld
	 *            allow files even if they are considered too old
	 * @param stable
	 *            a stable file (that dones't change too often) -- parameter
	 *            used to check if the file is too old to keep or not
	 * 
	 * @return the opened resource if found, NULL if not
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private InputStream load(File cached, boolean allowTooOld, boolean stable) {
		if (cached.exists() && (allowTooOld || !isOld(cached, stable))) {
			try {
				return new MarkableFileInputStream(new FileInputStream(cached));
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		return null;
	}

	/**
	 * Save the given resource to the cache.
	 * 
	 * @param in
	 *            the input data
	 * @param uniqueID
	 *            a unique ID used to locate the cached resource
	 * 
	 * @return the resulting {@link File}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public File save(InputStream in, String uniqueID) throws IOException {
		File cached = getCached(uniqueID);
		cached.getParentFile().mkdirs();
		return save(in, cached);
	}

	/**
	 * Save the given resource to the cache.
	 * 
	 * @param in
	 *            the input data
	 * @param url
	 *            the {@link URL} used to locate the cached resource
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public File save(InputStream in, URL url) throws IOException {
		File cached = getCached(url);
		return save(in, cached);
	}

	/**
	 * Save the given resource to the cache.
	 * 
	 * @param in
	 *            the input data
	 * @param cached
	 *            the cached {@link File} to save to
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private File save(InputStream in, File cached) throws IOException {
		IOUtils.write(in, cached);
		return cached;
	}

	/**
	 * Check if the {@link File} is too old according to
	 * {@link Cache#tooOldChanging}.
	 * 
	 * @param file
	 *            the file to check
	 * @param stable
	 *            TRUE to denote stable files, that are not supposed to change
	 *            too often
	 * 
	 * @return TRUE if it is
	 */
	private boolean isOld(File file, boolean stable) {
		long max = tooOldChanging;
		if (stable) {
			max = tooOldStable;
		}

		if (max < 0) {
			return false;
		}

		long time = new Date().getTime() - file.lastModified();
		if (time < 0) {
			trace("Timestamp in the future for file: " + file.getAbsolutePath(),
					true);
		}

		return time < 0 || time > max;
	}

	/**
	 * Return the associated cache {@link File} from this {@link URL}.
	 * 
	 * @param url
	 *            the {@link URL}
	 * 
	 * @return the cached {@link File} version of this {@link URL}
	 */
	private File getCached(URL url) {
		File subdir;

		String name = url.getHost();
		if (name == null || name.isEmpty()) {
			// File
			File file = new File(url.getFile());
			subdir = new File(file.getParent().replace("..", "__"));
			subdir = new File(dir, allowedChars(subdir.getPath()));
			name = allowedChars(url.getFile());
		} else {
			// URL
			File subsubDir = new File(dir, allowedChars(url.getHost()));
			subdir = new File(subsubDir, "_" + allowedChars(url.getPath()));
			name = allowedChars("_" + url.getQuery());
		}

		File cacheFile = new File(subdir, name);
		subdir.mkdirs();

		return cacheFile;
	}

	/**
	 * Get the basic cache resource file corresponding to this unique ID.
	 * <p>
	 * Note that you may need to add a sub-directory in some cases.
	 * 
	 * @param uniqueID
	 *            the id
	 * 
	 * @return the cached version if present, NULL if not
	 */
	private File getCached(String uniqueID) {
		File file = new File(dir, allowedChars(uniqueID));
		File subdir = new File(file.getParentFile(), "_");
		return new File(subdir, file.getName());
	}

	/**
	 * Replace not allowed chars (in a {@link File}) by "_".
	 * 
	 * @param raw
	 *            the raw {@link String}
	 * 
	 * @return the sanitised {@link String}
	 */
	private String allowedChars(String raw) {
		return raw.replace('/', '_').replace(':', '_').replace("\\", "_");
	}
}