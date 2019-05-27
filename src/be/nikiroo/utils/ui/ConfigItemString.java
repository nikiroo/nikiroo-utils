package be.nikiroo.utils.ui;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JTextField;

import be.nikiroo.utils.resources.MetaInfo;

public class ConfigItemString<E extends Enum<E>> extends ConfigItem<E> {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@link ConfigItemString} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 */
	public ConfigItemString(MetaInfo<E> info) {
		super(info);
	}

	@Override
	protected Object getFrom(Object field) {
		JTextField field = (JTextField) getField(item);
		if (field != null) {
			return field.getText();
		}

		return null;
	}

	@Override
	protected Object getFromInfo(int item) {
		return info.getString(item, false);
	}

	@Override
	protected void setTo(int item, Object value) {

	}

	@Override
	protected void setToInfo(int item, Object value) {

	}

	// move back into ConfigItem
	// item = 0-based for array, -1 for no array
	protected void addField(final MetaInfo<E> info, final int item,
			JComponent addTo, int nhgap) {
		final JTextField field = new JTextField();

		setField(item, field);

		reload(item);

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				reload(item);
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				save(item);
			}
		});

		addTo.add(label(info, nhgap), BorderLayout.WEST);
		addTo.add(field, BorderLayout.CENTER);

		setPreferredSize(field);
	}
}
