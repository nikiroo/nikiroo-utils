package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * This class will help you download content from Internet Sites ({@link URL}
 * based).
 * <p>
 * It allows you to control some options often required on web sites that do not
 * want to simply serve HTML, but actively makes your life difficult with stupid
 * checks.
 * 
 * @author niki
 */
public class Downloader {
	private String UA;
	private CookieManager cookies;

	/**
	 * Create a new {@link Downloader}.
	 * 
	 * @param UA
	 *            the User-Agent to use to download the resources -- note that
	 *            some websites require one, some actively blacklist real UAs
	 *            like the one from wget, some whitelist a couple of browsers
	 *            only (!)
	 */
	public Downloader(String UA) {
		this.UA = UA;

		cookies = new CookieManager();
		cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookies);
	}

	/**
	 * Clear all the cookies currently in the jar.
	 * <p>
	 * As long as you don't, the cookies are kept.
	 */
	public void clearCookies() {
		cookies.getCookieStore().removeAll();
	}

	/**
	 * Open the given {@link URL} and update the cookies.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 **/
	public InputStream open(URL url) throws IOException {
		return open(url, url, url, null, null, null, null);
	}

	/**
	 * Open the given {@link URL} and update the cookies.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * @param postParams
	 *            the POST parameters
	 * @param getParams
	 *            the GET parameters (priority over POST)
	 * @param oauth
	 *            OAuth authorization (aka, "bearer XXXXXXX")
	 * 
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream open(URL url, URL currentReferer,
			Map<String, String> cookiesValues, Map<String, String> postParams,
			Map<String, String> getParams, String oauth) throws IOException {
		return open(url, url, currentReferer, cookiesValues, postParams,
				getParams, oauth);
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
	 * Open the given {@link URL} and update the cookies.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * @param originalUrl
	 *            the original {@link URL} before any redirection occurs
	 * @param postParams
	 *            the POST parameters
	 * @param getParams
	 *            the GET parameters (priority over POST)
	 * @param oauth
	 *            OAuth authorisation (aka, "bearer XXXXXXX")
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private InputStream open(URL url, final URL originalUrl,
			URL currentReferer, Map<String, String> cookiesValues,
			Map<String, String> postParams, Map<String, String> getParams,
			String oauth) throws IOException {

		trace("Download: " + url, false);

		URLConnection conn = openConnectionWithCookies(url, currentReferer,
				cookiesValues);

		// Priority: GET over POST
		Map<String, String> params = getParams;
		if (getParams == null) {
			params = postParams;
		}

		if ((params != null || oauth != null)
				&& conn instanceof HttpURLConnection) {
			StringBuilder requestData = null;
			if (params != null) {
				requestData = new StringBuilder();
				for (Map.Entry<String, String> param : params.entrySet()) {
					if (requestData.length() != 0)
						requestData.append('&');
					requestData.append(URLEncoder.encode(param.getKey(),
							"UTF-8"));
					requestData.append('=');
					requestData.append(URLEncoder.encode(
							String.valueOf(param.getValue()), "UTF-8"));
				}

				conn.setDoOutput(true);

				if (getParams == null && postParams != null) {
					((HttpURLConnection) conn).setRequestMethod("POST");
				}

				conn.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				conn.setRequestProperty("charset", "utf-8");
			}

			if (oauth != null) {
				conn.setRequestProperty("Authorization", oauth);
			}

			if (requestData != null) {
				OutputStreamWriter writer = new OutputStreamWriter(
						conn.getOutputStream());

				writer.write(requestData.toString());
				writer.flush();
				writer.close();
			}
		}

		conn.connect();

		// Check if redirect
		if (conn instanceof HttpURLConnection
				&& ((HttpURLConnection) conn).getResponseCode() / 100 == 3) {
			String newUrl = conn.getHeaderField("Location");
			return open(new URL(newUrl), originalUrl, currentReferer,
					cookiesValues, postParams, getParams, oauth);
		}

		InputStream in = conn.getInputStream();
		if ("gzip".equals(conn.getContentEncoding())) {
			in = new GZIPInputStream(in);
		}

		return in;
	}

	/**
	 * Open a connection on the given {@link URL}, and manage the cookies that
	 * come with it.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * 
	 * @return the connection
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private URLConnection openConnectionWithCookies(URL url,
			URL currentReferer, Map<String, String> cookiesValues)
			throws IOException {
		URLConnection conn = url.openConnection();

		conn.setRequestProperty("User-Agent", UA);
		conn.setRequestProperty("Cookie", generateCookies(cookiesValues));
		conn.setRequestProperty("Accept-Encoding", "gzip");
		if (currentReferer != null) {
			conn.setRequestProperty("Referer", currentReferer.toString());
			conn.setRequestProperty("Host", currentReferer.getHost());
		}

		return conn;
	}

	/**
	 * Generate the cookie {@link String} from the local {@link CookieStore} so
	 * it is ready to be passed.
	 * 
	 * @return the cookie
	 */
	private String generateCookies(Map<String, String> cookiesValues) {
		StringBuilder builder = new StringBuilder();
		for (HttpCookie cookie : cookies.getCookieStore().getCookies()) {
			if (builder.length() > 0) {
				builder.append(';');
			}

			// TODO: check if format is ok
			builder.append(cookie.toString());
		}

		if (cookiesValues != null) {
			for (Map.Entry<String, String> set : cookiesValues.entrySet()) {
				if (builder.length() > 0) {
					builder.append(';');
				}
				builder.append(set.getKey());
				builder.append('=');
				builder.append(set.getValue());
			}
		}

		return builder.toString();
	}
}