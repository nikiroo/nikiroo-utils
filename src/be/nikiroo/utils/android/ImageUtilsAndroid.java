package be.nikiroo.utils.android;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import be.nikiroo.utils.Image;
import be.nikiroo.utils.ImageUtils;
import be.nikiroo.utils.StringUtils;

/**
 * This class offer some utilities based around images and uses the Android
 * framework.
 * 
 * @author niki
 */
public class ImageUtilsAndroid extends ImageUtils {
	@Override
	protected boolean check() {
		// If we can get the class, it means we have access to it
		Config.ALPHA_8;
		return true;
	}

	@Override
	public void saveAsImage(Image img, File target, String format)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(target);
		try {
			Bitmap image = fromImage(img);

			boolean ok = false;
			try {
				ok = image.compress(
						Bitmap.CompressFormat.valueOf(format.toUpperCase()),
						90, fos);
			} catch (Exception e) {
				ok = false;
			}

			// Some formats are not reliable
			// Second change: PNG
			if (!ok && !format.equals("png")) {
				ok = image.compress(Bitmap.CompressFormat.PNG, 90, fos);
			}

			if (!ok) {
				throw new IOException(
						"Cannot find a writer for this image and format: "
								+ format);
			}
		} catch (IOException e) {
			throw new IOException("Cannot write image to " + target, e);
		} finally {
			fos.close();
		}
	}

	/**
	 * Convert the given {@link Image} into a {@link Bitmap} object.
	 * 
	 * @param img
	 *            the {@link Image}
	 * @return the {@link Image} object
	 * @throws IOException
	 *             in case of IO error
	 */
	static public Bitmap fromImage(Image img) throws IOException {
		InputStream stream = img.newInputStream();
		try {
			Bitmap image = BitmapFactory.decodeStream(stream);
			if (image == null) {
				String ssize = StringUtils.formatNumber(img.getSize());
				throw new IOException(
						"Failed to convert input to image, size was: " + ssize);
			}
		} finally {
			stream.close();
		}
		
		return image;
	}
}
