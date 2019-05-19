package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import be.nikiroo.utils.Image;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.StringUtils.Alignment;
import be.nikiroo.utils.resources.Bundle;
import be.nikiroo.utils.resources.Meta.Format;
import be.nikiroo.utils.resources.MetaInfo;

/**
 * A graphical item that reflect a configuration option from the given
 * {@link Bundle}.
 * <p>
 * This graphical item can be edited, and the result will be saved back into the
 * linked {@link MetaInfo}; you still have to save the {@link MetaInfo} should
 * you wish to, of course.
 * 
 * @author niki
 * 
 * @param <E>
 *            the type of {@link Bundle} to edit
 */
public class ConfigItem<E extends Enum<E>> extends JPanel {
	private static final long serialVersionUID = 1L;

	/** A small (?) blue in PNG, base64 encoded. */
	private static String infoImage64 = //
	""
			+ "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBI"
			+ "WXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4wURFRg6IrtcdgAAATdJREFUOMvtkj8sQ1EUxr9z/71G"
			+ "m1RDogYxq7WDDYMYTSajSG4n6YRYzSaSLibWbiaDIGwdiLIYDFKDNJEgKu969xi8UNHy7H7LPcN3"
			+ "v/Odcy+hG9oOIeIcBCJS9MAvlZtOMtHxsrFrJHGqe0RVGnHAHpcIbPlng8BS3HmKBJYzabGUzcrJ"
			+ "XK+ckIrqANYR2JEv2nYDEVck0WKGfHzyq82Go+btxoX3XAcAIqTj8wPqOH6mtMeM4bGCLhyfhTMA"
			+ "qlLhKHqujCfaweCAmV0p50dPzsNpEKpK01V/n55HIvTnfDC2odKlfeYadZN/T+AqDACUsnkhqaU1"
			+ "LRIVuX1x7ciuSWQxVIrunONrfq3dI6oh+T94Z8453vEem/HTqT8ZpFJ0qDXtGkPbAGAMeSRngQCA"
			+ "eUvgn195AwlZWyvjtQdhAAAAAElFTkSuQmCC";

	/**
	 * Create a new {@link ConfigItem} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 * @param nhgap
	 *            negative horisontal gap in pixel to use for the label, i.e.,
	 *            the step lock sized labels will start smaller by that amount
	 *            (the use case would be to align controls that start at a
	 *            different horisontal position)
	 */
	public ConfigItem(MetaInfo<E> info, int nhgap) {
		this.setLayout(new BorderLayout());

		// TODO: support arrays
		Format fmt = info.getFormat();
		if (info.isArray()) {
			fmt = Format.STRING;
		}

		switch (fmt) {
		case BOOLEAN:
			addBooleanField(info, nhgap);
			break;
		case COLOR:
			addColorField(info, nhgap);
			break;
		case FILE:
			addBrowseField(info, nhgap, false);
			break;
		case DIRECTORY:
			addBrowseField(info, nhgap, true);
			break;
		case COMBO_LIST:
			addComboboxField(info, nhgap, true);
			break;
		case FIXED_LIST:
			addComboboxField(info, nhgap, false);
			break;
		case INT:
			addIntField(info, nhgap);
			break;
		case PASSWORD:
			addPasswordField(info, nhgap);
			break;
		case STRING:
		case LOCALE: // TODO?
		default:
			addStringField(info, nhgap);
			break;
		}
	}

	private void addStringField(final MetaInfo<E> info, int nhgap) {
		final JTextField field = new JTextField();
		field.setToolTipText(info.getDescription());
		field.setText(info.getString(false));

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				field.setText(info.getString(false));
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setString(field.getText());
			}
		});

		this.add(label(info, nhgap), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);

		setPreferredSize(field);
	}

	private void addBooleanField(final MetaInfo<E> info, int nhgap) {
		final JCheckBox field = new JCheckBox();
		field.setToolTipText(info.getDescription());
		Boolean state = info.getBoolean(true);

		// Should not happen!
		if (state == null) {
			System.err
					.println("No default value given for BOOLEAN parameter \""
							+ info.getName() + "\", we consider it is FALSE");
			state = false;
		}

		field.setSelected(state);

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				Boolean state = info.getBoolean(true);
				if (state == null) {
					state = false;
				}

				field.setSelected(state);
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setBoolean(field.isSelected());
			}
		});

		this.add(label(info, nhgap), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);

		setPreferredSize(field);
	}

	private void addColorField(final MetaInfo<E> info, int nhgap) {
		final JTextField field = new JTextField();
		field.setToolTipText(info.getDescription());
		field.setText(info.getString(false));

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				field.setText(info.getString(false));
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setString(field.getText());
			}
		});

		this.add(label(info, nhgap), BorderLayout.WEST);
		JPanel pane = new JPanel(new BorderLayout());

		final JButton colorWheel = new JButton();
		colorWheel.setIcon(getIcon(17, info.getColor(true)));
		colorWheel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color initialColor = new Color(info.getColor(true), true);
				Color newColor = JColorChooser.showDialog(ConfigItem.this,
						info.getName(), initialColor);
				if (newColor != null) {
					info.setColor(newColor.getRGB());
					field.setText(info.getString(false));
					colorWheel.setIcon(getIcon(17, info.getColor(true)));
				}
			}
		});
		pane.add(colorWheel, BorderLayout.WEST);
		pane.add(field, BorderLayout.CENTER);
		this.add(pane, BorderLayout.CENTER);

		setPreferredSize(pane);
	}

	private void addBrowseField(final MetaInfo<E> info, int nhgap,
			final boolean dir) {
		final JTextField field = new JTextField();
		field.setToolTipText(info.getDescription());
		field.setText(info.getString(false));

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				field.setText(info.getString(false));
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setString(field.getText());
			}
		});

		JButton browseButton = new JButton("...");
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(null);
				chooser.setFileSelectionMode(dir ? JFileChooser.DIRECTORIES_ONLY
						: JFileChooser.FILES_ONLY);
				if (chooser.showOpenDialog(ConfigItem.this) == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					if (file != null) {
						info.setString(file.getAbsolutePath());
						field.setText(info.getString(false));
					}
				}
			}
		});

		JPanel pane = new JPanel(new BorderLayout());
		this.add(label(info, nhgap), BorderLayout.WEST);
		pane.add(browseButton, BorderLayout.WEST);
		pane.add(field, BorderLayout.CENTER);
		this.add(pane, BorderLayout.CENTER);

		setPreferredSize(pane);
	}

	private void addComboboxField(final MetaInfo<E> info, int nhgap,
			boolean editable) {
		// rawtypes for Java 1.6 (and 1.7 ?) support
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final JComboBox field = new JComboBox(info.getAllowedValues());
		field.setEditable(editable);
		field.setSelectedItem(info.getString(false));

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				field.setSelectedItem(info.getString(false));
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setString(field.getSelectedItem().toString());
			}
		});

		this.add(label(info, nhgap), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);

		setPreferredSize(field);
	}

	private void addPasswordField(final MetaInfo<E> info, int nhgap) {
		final JPasswordField field = new JPasswordField();
		field.setToolTipText(info.getDescription());
		field.setText(info.getString(true));

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				field.setText(info.getString(false));
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setString(new String(field.getPassword()));
			}
		});

		this.add(label(info, nhgap), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);

		setPreferredSize(field);
	}

	private void addIntField(final MetaInfo<E> info, int nhgap) {
		final JSpinner field = new JSpinner();
		field.setToolTipText(info.getDescription());
		field.setValue(info.getInteger(true) == null ? 0 : info
				.getInteger(true));

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				field.setValue(info.getInteger(true) == null ? 0 : info
						.getInteger(true));
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setInteger((Integer) field.getValue());
				Integer value = info.getInteger(false);
				if (value == null) {
					field.setValue(0);
				}
			}
		});

		this.add(label(info, nhgap), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);

		setPreferredSize(field);
	}

	/**
	 * Create a label which width is constrained in lock steps.
	 * 
	 * @param info
	 *            the {@link MetaInfo} for which we want to add a label
	 * @param nhgap
	 *            negative horisontal gap in pixel to use for the label, i.e.,
	 *            the step lock sized labels will start smaller by that amount
	 *            (the use case would be to align controls that start at a
	 *            different horisontal position)
	 * 
	 * @return the label
	 */
	private JComponent label(final MetaInfo<E> info, int nhgap) {
		final JLabel label = new JLabel(info.getName());

		Dimension ps = label.getPreferredSize();
		if (ps == null) {
			ps = label.getSize();
		}

		int w = ps.width;
		int step = 150;
		for (int i = 2 * step - nhgap; i < 10 * step; i += step) {
			if (w < i) {
				w = i;
				break;
			}
		}

		final Runnable showInfo = new Runnable() {
			@Override
			public void run() {
				StringBuilder builder = new StringBuilder();
				String text = (info.getDescription().replace("\\n", "\n"))
						.trim();
				for (String line : StringUtils.justifyText(text, 80,
						Alignment.LEFT)) {
					if (builder.length() > 0) {
						builder.append("\n");
					}
					builder.append(line);
				}
				text = builder.toString();
				JOptionPane.showMessageDialog(ConfigItem.this, text,
						info.getName(), JOptionPane.INFORMATION_MESSAGE);
			}
		};

		JLabel help = new JLabel("");
		help.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		try {
			Image img = new Image(infoImage64);
			try {
				BufferedImage bImg = ImageUtilsAwt.fromImage(img);
				help.setIcon(new ImageIcon(bImg));
			} finally {
				img.close();
			}
		} catch (IOException e) {
			// This is an hard-coded image, should not happen
			help.setText("?");
		}

		help.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showInfo.run();
			}
		});

		JPanel pane2 = new JPanel(new BorderLayout());
		pane2.add(help, BorderLayout.WEST);
		pane2.add(new JLabel(" "), BorderLayout.CENTER);

		JPanel pane = new JPanel(new BorderLayout());
		pane.add(label, BorderLayout.WEST);
		pane.add(pane2, BorderLayout.CENTER);

		ps.width = w + 30; // 30 for the (?) sign
		pane.setSize(ps);
		pane.setPreferredSize(ps);

		return pane;
	}

	/**
	 * Return an {@link Icon} to use as a colour badge for the colour field
	 * controls.
	 * 
	 * @param size
	 *            the size of the badge
	 * @param color
	 *            the colour of the badge
	 * 
	 * @return the badge
	 */
	private Icon getIcon(int size, int color) {
		Color c = new Color(color, true);
		int avg = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
		Color border = (avg >= 128 ? Color.BLACK : Color.WHITE);

		BufferedImage img = new BufferedImage(size, size,
				BufferedImage.TYPE_4BYTE_ABGR);

		Graphics2D g = img.createGraphics();
		try {
			g.setColor(c);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.setColor(border);
			g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
		} finally {
			g.dispose();
		}

		return new ImageIcon(img);
	}

	private void setPreferredSize(JComponent field) {
		JTextField a = new JTextField("Test");
		int height = Math.max(a.getMinimumSize().height,
				field.getMinimumSize().height);
		setPreferredSize(new Dimension(200, height));
	}
}
