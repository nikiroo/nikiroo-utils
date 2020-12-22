package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A graphical item that can be presented in a list and supports user
 * interaction.
 * <p>
 * Can be selected, hovered...
 * 
 * @author niki
 */
abstract public class Item extends JPanel {
	static private final long serialVersionUID = 1L;

	static private Map<Dimension, BufferedImage> empty = new HashMap<Dimension, BufferedImage>();
	static private Map<Dimension, BufferedImage> error = new HashMap<Dimension, BufferedImage>();
	static private Map<Color, JComponent> statuses = new HashMap<Color, JComponent>();

	private String id;
	private boolean selected;
	private boolean hovered;

	private String mainTemplate;
	private String secondaryTemplate;

	private boolean hasImage;
	private JLabel title;
	private JLabel secondary;
	private JLabel statusIndicatorOn;
	private JLabel statusIndicatorOff;
	private JLabel statusIndicatorUnknown;
	private Image image;
	private boolean imageError;

	private String cachedMain;
	private String cachedOptSecondary;
	private Integer cachedStatus;

	/**
	 * Create a new {@link Item}
	 * 
	 * @param id
	 *            an ID that represents this {@link Item} (can be NULL)
	 * @param hasImage
	 *            this {@link Item} will contain an image
	 */
	public Item(String id, boolean hasImage) {
		this.id = id;
		this.hasImage = hasImage;
		init(hasImage);
	}

	// Configuration :

	protected int getMaxDisplaySize() {
		return 40;
	}

	protected int getCoverWidth() {
		return 100;
	}

	protected int getCoverHeight() {
		return 150;
	}

	protected int getTextWidth() {
		return getCoverWidth() + 40;
	}

	protected int getTextHeight() {
		return 50;
	}

	protected int getCoverVOffset() {
		return 20;
	}

	protected int getCoverHOffset() {
		return 0;
	}

	protected int getHGap() {
		return 10;
	}

	/** Colour used for the secondary item (author/word count). */
	protected Color getSecondaryColor() {
		return new Color(128, 128, 128);
	}

	/**
	 * Return a display-ready version of the main information to show.
	 * <p>
	 * Note that you can make use of {@link Item#limit(String)}.
	 * 
	 * @return the main info in a ready-to-display version, cannot be NULL
	 */
	abstract protected String getMainInfoDisplay();

	/**
	 * Return a display-ready version of the secondary information to show.
	 * <p>
	 * Note that you can make use of {@link Item#limit(String)}.
	 * 
	 * @return the main info in a ready-to-display version, cannot be NULL
	 */
	abstract protected String getSecondaryInfoDisplay();

	/**
	 * The current status for the status indicator.
	 * <p>
	 * Note that NULL and negative values will create "hollow" indicators, while
	 * other values will create "filled" indicators.
	 * 
	 * @return the status which can be NULL, presumably for "Unknown"
	 */
	abstract protected Integer getStatus();

	/**
	 * Get the background colour to use according to the given state.
	 * <p>
	 * Since it is an overlay, an opaque colour will of course mask everything.
	 * 
	 * @param enabled
	 *            the item is enabled
	 * @param selected
	 *            the item is selected
	 * @param hovered
	 *            the mouse cursor currently hovers over the item
	 * 
	 * @return the correct background colour to use
	 */
	abstract protected Color getOverlayColor(boolean enabled, boolean selected,
			boolean hovered);

	/**
	 * Get the colour to use for the status indicator.
	 * <p>
	 * Return NULL if you don't want a status indicator for this state.
	 * 
	 * @param status
	 *            the current status as returned by {@link Item#getStatus()}
	 * 
	 * @return the base colour to use, or NULL for no status indicator
	 */
	abstract protected Color getStatusIndicatorColor(Integer status);

	/**
	 * Initialise this {@link Item}.
	 */
	private void init(boolean hasImage) {
		if (!hasImage) {
			title = new JLabel();
			mainTemplate = "${MAIN}";
			secondary = new JLabel();
			secondaryTemplate = "${SECONDARY}";
			secondary.setForeground(getSecondaryColor());

			JPanel idTitle = null;
			if (id != null && !id.isEmpty()) {
				JLabel idLabel = new JLabel(id);
				idLabel.setPreferredSize(new JLabel(" 999 ").getPreferredSize());
				idLabel.setForeground(Color.gray);
				idLabel.setHorizontalAlignment(SwingConstants.CENTER);

				idTitle = new JPanel(new BorderLayout());
				idTitle.setOpaque(false);
				idTitle.add(idLabel, BorderLayout.WEST);
				idTitle.add(title, BorderLayout.CENTER);
			}

			setLayout(new BorderLayout());
			if (idTitle != null)
				add(idTitle, BorderLayout.CENTER);
			add(secondary, BorderLayout.EAST);
		} else {
			image = null;
			title = new JLabel();
			secondary = new JLabel();
			secondaryTemplate = "";

			String color = String.format("#%X%X%X", getSecondaryColor()
					.getRed(), getSecondaryColor().getGreen(),
					getSecondaryColor().getBlue());
			mainTemplate = String
					.format("<html>"
							+ "<body style='width: %d px; height: %d px; text-align: center;'>"
							+ "${MAIN}" + "<br>" + "<span style='color: %s;'>"
							+ "${SECONDARY}" + "</span>" + "</body>"
							+ "</html>", getTextWidth(), getTextHeight(), color);

			int ww = Math.max(getCoverWidth(), getTextWidth());
			int hh = getCoverHeight() + getCoverVOffset() + getHGap()
					+ getTextHeight();

			JPanel placeholder = new JPanel();
			placeholder
					.setPreferredSize(new Dimension(ww, hh - getTextHeight()));
			placeholder.setOpaque(false);

			JPanel titlePanel = new JPanel(new BorderLayout());
			titlePanel.setOpaque(false);
			titlePanel.add(title, BorderLayout.NORTH);

			titlePanel.setBorder(BorderFactory.createEmptyBorder());

			setLayout(new BorderLayout());
			add(placeholder, BorderLayout.NORTH);
			add(titlePanel, BorderLayout.CENTER);
		}

		// Cached values are NULL, so it will be updated
		updateData();
	}

	/**
	 * The book current selection state.
	 * 
	 * @return the selection state
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * The book current selection state,
	 * 
	 * @param selected
	 *            TRUE if it is selected
	 */
	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;
			repaint();
		}
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @return TRUE if it is mouse-hovered
	 */
	public boolean isHovered() {
		return this.hovered;
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @param hovered
	 *            TRUE if it is mouse-hovered
	 */
	public void setHovered(boolean hovered) {
		if (this.hovered != hovered) {
			this.hovered = hovered;
			repaint();
		}
	}

	/**
	 * Update the title, paint the item.
	 */
	@Override
	public void paint(Graphics g) {
		Rectangle clip = g.getClipBounds();
		if (clip == null || clip.getWidth() <= 0 || clip.getHeight() <= 0) {
			return;
		}

		updateData();

		super.paint(g);
		if (hasImage) {
			Image img = image == null ? getBlank(false) : image;
			if (isImageError())
				img = getBlank(true);

			int xOff = getCoverHOffset() + (getWidth() - getCoverWidth()) / 2;
			g.drawImage(img, xOff, getCoverVOffset(), null);

			Integer status = getStatus();
			boolean filled = status != null && status > 0;
			Color indicatorColor = getStatusIndicatorColor(status);
			if (indicatorColor != null) {
				UIUtils.drawEllipse3D(g, indicatorColor, getCoverWidth() + xOff
						+ 10, 10, 20, 20, filled);
			}
		}

		Color bg = getOverlayColor(isEnabled(), isSelected(), isHovered());
		g.setColor(bg);
		g.fillRect(clip.x, clip.y, clip.width, clip.height);
	}

	/**
	 * The image to display on image {@link Item} (NULL for non-image
	 * {@link Item}s).
	 * 
	 * @return the image or NULL for the empty image or for non image
	 *         {@link Item}s
	 */
	public Image getImage() {
		return hasImage ? image : null;
	}

	/**
	 * Change the image to display (does not work for non-image {@link Item}s).
	 * <p>
	 * NULL is allowed, an empty image will then be shown.
	 * 
	 * @param image
	 *            the new {@link Image} or NULL
	 * 
	 */
	public void setImage(Image image) {
		this.image = hasImage ? image : null;
	}

	/**
	 * Use the ERROR image instead of the real one or the empty one.
	 * 
	 * @return TRUE if we force use the error image
	 */
	public boolean isImageError() {
		return imageError;
	}

	/**
	 * Use the ERROR image instead of the real one or the empty one.
	 * 
	 * @param imageError
	 *            TRUE to force use the error image
	 */
	public void setImageError(boolean imageError) {
		this.imageError = imageError;
	}

	/**
	 * Make the given {@link String} display-ready (i.e., shorten it if it is
	 * too long).
	 * 
	 * @param value
	 *            the full value
	 * 
	 * @return the display-ready value
	 */
	protected String limit(String value) {
		if (value == null)
			value = "";

		if (value.length() > getMaxDisplaySize()) {
			value = value.substring(0, getMaxDisplaySize() - 3) + "...";
		}

		return value;
	}

	/**
	 * Update the title with the currently registered information.
	 */
	private void updateData() {
		String main = getMainInfoDisplay();
		String optSecondary = getSecondaryInfoDisplay();
		Integer status = getStatus();

		// Cached values can be NULL the first time
		if (!main.equals(cachedMain)
				|| !optSecondary.equals(cachedOptSecondary)
				|| status != cachedStatus) {
			title.setText(mainTemplate //
					.replace("${MAIN}", main) //
					.replace("${SECONDARY}", optSecondary) //
			);
			secondary.setText(secondaryTemplate//
					.replace("${MAIN}", main) //
					.replace("${SECONDARY}", optSecondary) //
					+ " ");

			Color bg = getOverlayColor(isEnabled(), isSelected(), isHovered());
			setBackground(bg);

			if (!hasImage) {
				remove(statusIndicatorUnknown);
				remove(statusIndicatorOn);
				remove(statusIndicatorOff);

				Color k = getStatusIndicatorColor(getStatus());
				JComponent statusIndicator = statuses.get(k);
				if (!statuses.containsKey(k)) {
					statusIndicator = generateStatusIndicator(k);
					statuses.put(k, statusIndicator);
				}

				if (statusIndicator != null)
					add(statusIndicator, BorderLayout.WEST);
			}

			validate();
		}

		this.cachedMain = main;
		this.cachedOptSecondary = optSecondary;
		this.cachedStatus = status;
	}

	/**
	 * Generate a status indicator for the given colour.
	 * 
	 * @param color
	 *            the colour to use
	 * 
	 * @return a status indicator ready to be used
	 */
	private JLabel generateStatusIndicator(final Color color) {
		JLabel indicator = new JLabel("   ") {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				super.paint(g);

				if (color != null) {
					Dimension sz = statusIndicatorOn.getSize();
					int s = Math.min(sz.width, sz.height);
					int x = Math.max(0, (sz.width - sz.height) / 2);
					int y = Math.max(0, (sz.height - sz.width) / 2);

					UIUtils.drawEllipse3D(g, color, x, y, s, s, true);
				}
			}
		};

		indicator.setBackground(color);
		return indicator;
	}

	private Image getBlank(boolean error) {
		Dimension key = new Dimension(getCoverWidth(), getCoverHeight());
		Map<Dimension, BufferedImage> images = error ? Item.error : Item.empty;

		BufferedImage blank = images.get(key);
		if (blank == null) {
			blank = new BufferedImage(getCoverWidth(), getCoverHeight(),
					BufferedImage.TYPE_4BYTE_ABGR);

			Graphics2D g = blank.createGraphics();
			try {
				g.setColor(Color.white);
				g.fillRect(0, 0, getCoverWidth(), getCoverHeight());

				g.setColor(error ? Color.red : Color.black);
				g.drawLine(0, 0, getCoverWidth(), getCoverHeight());
				g.drawLine(getCoverWidth(), 0, 0, getCoverHeight());
			} finally {
				g.dispose();
			}
			images.put(key, blank);
		}

		return blank;
	}
}
