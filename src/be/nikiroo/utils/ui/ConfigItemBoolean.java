package be.nikiroo.utils.ui;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import be.nikiroo.utils.resources.MetaInfo;

public class ConfigItemBoolean<E extends Enum<E>> extends ConfigItem<E> {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@link ConfigItemBoolean} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 */
	public ConfigItemBoolean(MetaInfo<E> info) {
		super(info);
	}

	@Override
	protected Object getFromField(int item) {
		JCheckBox field = (JCheckBox) getField(item);
		if (field != null) {
			return field.isSelected();
		}

		return null;
	}

	@Override
	protected Object getFromInfo(int item) {
		return info.getBoolean(item, false);
	}

	@Override
	protected void setToField(Object value, int item) {
		JCheckBox field = (JCheckBox) getField(item);
		if (field != null) {
			// Should not happen if config enum is correct
			// (but this is not enforced)
			if (value == null) {
				value = false;
			}

			field.setSelected((Boolean) value);
		}
	}

	@Override
	protected void setToInfo(Object value, int item) {
		info.setBoolean((Boolean) value, item);
	}

	@Override
	protected JComponent createField(Object value) {
		// Should not happen!
		if (value == null) {
			System.err
					.println("No default value given for BOOLEAN parameter \""
							+ info.getName() + "\", we consider it is FALSE");
		}

		return new JCheckBox();
	}
}
