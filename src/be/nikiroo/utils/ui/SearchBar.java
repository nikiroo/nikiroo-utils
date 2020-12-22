package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import be.nikiroo.utils.IOUtils;

/**
 * A generic search/filter bar.
 * 
 * @author niki
 */
public class SearchBar extends ListenerPanel {
	static private final long serialVersionUID = 1L;
	static private ImageIcon searchIcon;
	static private ImageIcon clearIcon;

	private JButton search;
	private JTextField text;
	private JButton clear;

	private boolean realTime;

	/**
	 * Create a new {@link SearchBar}.
	 */
	public SearchBar() {
		setLayout(new BorderLayout());

		// TODO: make an option to change the default setting here:
		// (can already be manually toggled by the user)
		realTime = true;

		if (searchIcon == null)
			searchIcon = getIcon("search-16x16.png");
		if (clearIcon == null)
			clearIcon = getIcon("clear-16x16.png");

		search = new JButton(searchIcon);
		if (searchIcon == null) {
			search.setText("[s]");
		}
		UIUtils.setButtonPressed(search, realTime);
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				realTime = !realTime;
				UIUtils.setButtonPressed(search, realTime);
				text.requestFocus();

				if (realTime) {
					fireActionPerformed(getText());
				}
			}
		});

		text = new JTextField();
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(final KeyEvent e) {
				super.keyTyped(e);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						boolean empty = (text.getText().isEmpty());
						clear.setVisible(!empty);

						if (realTime) {
							fireActionPerformed(getText());
						}
					}
				});
			}
		});
		text.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!realTime) {
					fireActionPerformed(getText());
				}
			}
		});

		clear = new JButton(clearIcon);
		if (clearIcon == null) {
			clear.setText("(c)");
		}
		clear.setBackground(text.getBackground());
		clear.setVisible(false);
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				text.setText("");
				clear.setVisible(false);
				text.requestFocus();

				fireActionPerformed(getText());
			}
		});

		add(search, BorderLayout.WEST);
		add(text, BorderLayout.CENTER);
		add(clear, BorderLayout.EAST);
	}

	/**
	 * Return the current text displayed by this {@link SearchBar}, or an empty
	 * {@link String} if none.
	 * 
	 * @return the text, cannot be NULL
	 */
	public String getText() {
		// Should usually not be NULL, but not impossible
		String text = this.text.getText();
		return text == null ? "" : text;
	}

	@Override
	public void setEnabled(boolean enabled) {
		search.setEnabled(enabled);
		clear.setEnabled(enabled);
		text.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	private ImageIcon getIcon(String name) {
		InputStream in = IOUtils.openResource(SearchBar.class, name);
		if (in != null) {
			try {
				return new ImageIcon(IOUtils.toByteArray(in));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}
