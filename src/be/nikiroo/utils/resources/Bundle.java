package be.nikiroo.utils.resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * This class encapsulate a {@link ResourceBundle} in UTF-8. It only allows to
 * retrieve values associated to an enumeration, and allows some additional
 * methods.
 * 
 * @author niki
 * 
 * @param <E>
 *            the enum to use to get values out of this class
 */
public class Bundle<E extends Enum<E>> {
	protected Class<E> type;
	protected Enum<?> name;
	private ResourceBundle map;

	/**
	 * Create a new {@link Bundles} of the given name.
	 * 
	 * @param type
	 *            a runtime instance of the class of E
	 * 
	 * @param name
	 *            the name of the {@link Bundles}
	 */
	protected Bundle(Class<E> type, Enum<?> name) {
		this.type = type;
		this.name = name;
		setBundle(name, Locale.getDefault());
	}

	/**
	 * Return the value associated to the given id as a {@link String}.
	 * 
	 * @param mame
	 *            the id of the value to get
	 * 
	 * @return the associated value, or NULL if not found (not present in the
	 *         resource file)
	 */
	public String getString(E id) {
		return getStringX(id, "");
	}

	/**
	 * Return the value associated to the given id as a {@link String} suffixed
	 * with the runtime value "_suffix" (that is, "_" and suffix).
	 * 
	 * @param mame
	 *            the id of the value to get
	 * @param suffix
	 *            the runtime suffix
	 * 
	 * @return the associated value, or NULL if not found (not present in the
	 *         resource file)
	 */
	public String getStringX(E id, String suffix) {
		String key = id.name()
				+ ((suffix == null || suffix.isEmpty()) ? "" : "_"
						+ suffix.toUpperCase());

		if (containsKey(key)) {
			return getString(key).trim();
		}

		return null;
	}

	/**
	 * Return the value associated to the given id as a {@link Boolean}.
	 * 
	 * @param mame
	 *            the id of the value to get
	 * 
	 * @return the associated value
	 */
	public Boolean getBoolean(E id) {
		String str = getString(id);
		if (str != null && str.length() > 0) {
			if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("on")
					|| str.equalsIgnoreCase("yes"))
				return true;
			if (str.equalsIgnoreCase("false") || str.equalsIgnoreCase("off")
					|| str.equalsIgnoreCase("no"))
				return false;

		}

		return null;
	}

	/**
	 * Return the value associated to the given id as a {@link boolean}.
	 * 
	 * @param mame
	 *            the id of the value to get
	 * @param def
	 *            the default value when it is not present in the config file or
	 *            if it is not a boolean value
	 * 
	 * @return the associated value
	 */
	public boolean getBoolean(E id, boolean def) {
		Boolean b = getBoolean(id);
		if (b != null)
			return b;

		return def;
	}

	/**
	 * Return the value associated to the given id as an {@link Integer}.
	 * 
	 * @param mame
	 *            the id of the value to get
	 * 
	 * @return the associated value
	 */
	public Integer getInteger(E id) {
		try {
			return Integer.parseInt(getString(id));
		} catch (Exception e) {
		}

		return null;
	}

	/**
	 * Return the value associated to the given id as a {@link int}.
	 * 
	 * @param mame
	 *            the id of the value to get
	 * @param def
	 *            the default value when it is not present in the config file or
	 *            if it is not a int value
	 * 
	 * @return the associated value
	 */
	public int getInteger(E id, int def) {
		Integer i = getInteger(id);
		if (i != null)
			return i;

		return def;
	}

	/**
	 * Return the value associated to the given id as a {@link Character}.
	 * 
	 * @param mame
	 *            the id of the value to get
	 * 
	 * @return the associated value
	 */
	public char getChar(E id) {
		String s = getString(id).trim();
		if (s.length() > 0) {
			return s.charAt(0);
		}

		return ' ';
	}

	/**
	 * Create/update the .properties file. Will use the most likely candidate as
	 * base if the file does not already exists and this resource is
	 * translatable (for instance, "en_US" will use "en" as a base if the
	 * resource is a translation file).
	 * 
	 * @param path
	 *            the path where the .properties files are
	 * 
	 * @throws IOException
	 *             in case of IO errors
	 */
	public void updateFile(String path) throws IOException {
		File file = getUpdateFile(path);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file), "UTF-8"));

		writeHeader(writer);
		writer.write("\n");
		writer.write("\n");

		for (Field field : type.getDeclaredFields()) {
			Meta meta = field.getAnnotation(Meta.class);
			if (meta != null) {
				E id = E.valueOf(type, field.getName());
				String info = getMetaInfo(meta);

				if (info != null) {
					writer.write(info);
					writer.write("\n");
				}

				writeValue(writer, id);
			}
		}

		writer.close();
	}

	/**
	 * Check if the internal map contains the given key.
	 * 
	 * @param key
	 *            the key to check for
	 * 
	 * @return true if it does
	 */
	protected boolean containsKey(String key) {
		try {
			map.getObject(key);
			return true;
		} catch (MissingResourceException e) {
			return false;
		}
	}

	/**
	 * Get the value for the given key if it exists in the internal map, or NULL
	 * if not.
	 * 
	 * @param key
	 *            the key to check for
	 * 
	 * @return the value, or NULL
	 */
	protected String getString(String key) {
		if (containsKey(key)) {
			return map.getString(key);
		}

		return null;
	}

	/**
	 * Return formated, display-able information from the {@link Meta} field
	 * given. Each line will always starts with a "#" character.
	 * 
	 * @param meta
	 *            the {@link Meta} field
	 * 
	 * @return the information to display or NULL if none
	 */
	protected String getMetaInfo(Meta meta) {
		String what = meta.what();
		String where = meta.where();
		String format = meta.format();
		String info = meta.info();

		int opt = what.length() + where.length() + format.length();
		if (opt + info.length() == 0)
			return null;

		StringBuilder builder = new StringBuilder();
		builder.append("# ");

		if (opt > 0) {
			builder.append("(");
			if (what.length() > 0) {
				builder.append("WHAT: " + what);
				if (where.length() + format.length() > 0)
					builder.append(", ");
			}

			if (where.length() > 0) {
				builder.append("WHERE: " + where);
				if (format.length() > 0)
					builder.append(", ");
			}

			if (format.length() > 0) {
				builder.append("FORMAT: " + format);
			}

			builder.append(")");
			if (info.length() > 0) {
				builder.append("\n# ");
			}
		}

		builder.append(info);

		return builder.toString();
	}

	/**
	 * The display name used in the <tt>.properties file</tt>.
	 * 
	 * @return the name
	 */
	protected String getBundleDisplayName() {
		return name.toString();
	}

	/**
	 * Write the header found in the configuration <tt>.properties</tt> file of
	 * this {@link Bundles}.
	 * 
	 * @param writer
	 *            the {@link Writer} to write the header in
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	protected void writeHeader(Writer writer) throws IOException {
		writer.write("# " + getBundleDisplayName() + "\n");
		writer.write("#\n");
	}

	/**
	 * Write the given id to the config file, i.e., "MY_ID = my_curent_value"
	 * followed by a new line
	 * 
	 * @param writer
	 *            the {@link Writer} to write into
	 * @param id
	 *            the id to write
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	protected void writeValue(Writer writer, E id) throws IOException {
		writeValue(writer, id.name(), getString(id));
	}

	/**
	 * Write the given data to the config file, i.e., "MY_ID = my_curent_value"
	 * followed by a new line
	 * 
	 * @param writer
	 *            the {@link Writer} to write into
	 * @param id
	 *            the id to write
	 * @param value
	 *            the id's value
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	protected void writeValue(Writer writer, String id, String value)
			throws IOException {
		writer.write(id);
		writer.write(" = ");

		if (value == null) {
			value = "";
		}

		String[] lines = value.replaceAll("\\\t", "\\\\\\t").split("\n");
		for (int i = 0; i < lines.length; i++) {
			writer.write(lines[i]);
			if (i < lines.length - 1) {
				writer.write("\\n\\");
			}
			writer.write("\n");
		}
	}

	/**
	 * Return the source file for this {@link Bundles} from the given path.
	 * 
	 * @param path
	 *            the path where the .properties files are
	 * 
	 * @return the source {@link File}
	 * 
	 * @throws IOException
	 *             in case of IO errors
	 */
	protected File getUpdateFile(String path) {
		return new File(path, name.name() + ".properties");
	}

	/**
	 * Change the currently used bundle.
	 * 
	 * @param name
	 *            the name of the bundle to load
	 * @param locale
	 *            the {@link Locale} to use
	 */
	protected void setBundle(Enum<?> name, Locale locale) {
		map = null;
		String dir = Bundles.getDirectory();

		if (dir != null) {
			try {
				File file = getPropertyFile(dir, name.name(), locale);
				if (file != null) {
					Reader reader = new InputStreamReader(new FileInputStream(
							file), "UTF8");
					map = new PropertyResourceBundle(reader);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (map == null) {
			map = ResourceBundle.getBundle(type.getPackage().getName() + "."
					+ name.name(), locale, new FixedResourceBundleControl());
		}
	}

	/**
	 * Return the resource file that is closer to the {@link Locale}.
	 * 
	 * @param dir
	 *            the dirctory to look into
	 * @param name
	 *            the file basename (without <tt>.properties</tt>)
	 * @param locale
	 *            the {@link Locale}
	 * 
	 * @return the closest match or NULL if none
	 */
	private File getPropertyFile(String dir, String name, Locale locale) {
		List<String> locales = new ArrayList<String>();
		if (locale != null) {
			String country = locale.getCountry() == null ? "" : locale
					.getCountry();
			String language = locale.getLanguage() == null ? "" : locale
					.getLanguage();
			if (!language.isEmpty() && !country.isEmpty()) {
				locales.add("_" + language + "-" + country);
			}
			if (!language.isEmpty()) {
				locales.add("_" + language);
			}
		}

		locales.add("");

		File file = null;
		for (String loc : locales) {
			file = new File(dir, name + loc + ".properties");
			if (file.exists()) {
				break;
			} else {
				file = null;
			}
		}

		return file;
	}
}
