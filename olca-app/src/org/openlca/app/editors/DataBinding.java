package org.openlca.app.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.component.ISingleModelDrop;
import org.openlca.app.component.TextDropComponent;
import org.openlca.app.db.Database;
import org.openlca.app.util.Bean;
import org.openlca.app.util.Colors;
import org.openlca.core.database.BaseDao;
import org.openlca.core.editors.IEditor;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBinding {

	public enum BindingType {

		STRING, DOUBLE, INT, SHORT;

	}

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private IEditor editor;

	public DataBinding() {
	}

	public DataBinding(IEditor editor) {
		this.editor = editor;
	}

	/** Removes *all* modify listeners from the given text. */
	public void release(Text text) {
		Listener[] listeners = text.getListeners(SWT.Modify);
		log.trace("release {} listeners from text", listeners.length);
		for (Listener listener : listeners) {
			if (!(listener instanceof ModifyListener))
				continue;
			ModifyListener mod = (ModifyListener) listener;
			text.removeModifyListener(mod);
		}
	}

	public void onModel(final Object bean, final String property,
			final TextDropComponent text) {
		log.trace("Register data binding - base descriptor - {} - {}", bean,
				property);
		if (bean == null || property == null || text == null)
			return;

		checkType(bean, property, text);

		initValue(bean, property, text);
		text.setHandler(new ISingleModelDrop() {

			@Override
			public void handle(BaseDescriptor descriptor) {
				setModel(bean, property, text);
				editorChange();
			}

		});
	}

	private void checkType(final Object bean, final String property,
			final TextDropComponent text) {
		try {
			Class<?> propertyType = Bean.getType(bean, property);
			if (propertyType != text.getModelType().getModelClass())
				throw new IllegalArgumentException("Cannot bind "
						+ text.getModelType().getModelClass()
								.getCanonicalName() + " to "
						+ propertyType.getCanonicalName());
		} catch (Exception e) {
			error("Cannot bind bean", e);
		}
	}

	public void on(final Object bean, final String property, BindingType type,
			final Text text) {
		switch (type) {
		case STRING:
			onString(bean, property, text);
			break;
		case DOUBLE:
			onDouble(bean, property, text);
			break;
		case INT:
			onInt(bean, property, text);
			break;
		case SHORT:
			onShort(bean, property, text);
			break;
		}
	}

	private void onString(final Object bean, final String property,
			final Text text) {
		log.trace("Register data binding - string - {} - {}", bean, property);
		if (bean == null || property == null || text == null)
			return;
		initValue(bean, property, text);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setStringValue(bean, property, text);
				editorChange();
			}
		});
	}

	private void onShort(final Object bean, final String property,
			final Text text) {
		log.trace("Register data binding - short - {} - {}", bean, property);
		if (bean == null || property == null || text == null)
			return;
		initValue(bean, property, text);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setShortValue(bean, property, text);
				editorChange();
			}
		});
	}

	private void onInt(final Object bean, final String property, final Text text) {
		log.trace("Register data binding - int - {} - {}", bean, property);
		if (bean == null || property == null || text == null)
			return;
		initValue(bean, property, text);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setIntValue(bean, property, text);
				editorChange();
			}
		});
	}

	private void onDouble(final Object bean, final String property,
			final Text text) {
		log.trace("Register data binding - double - {} - {}", bean, property);
		if (bean == null || property == null || text == null)
			return;
		initValue(bean, property, text);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDoubleValue(bean, property, text);
				editorChange();
			}
		});
	}

	private void initValue(Object bean, String property, Text text) {
		try {
			Object val = Bean.getValue(bean, property);
			if (val != null) {
				text.setText(val.toString());
			}
		} catch (Exception e) {
			error("Cannot set text value", e);
		}
	}

	private void initValue(Object bean, String property, TextDropComponent text) {
		try {
			Object val = Bean.getValue(bean, property);
			if (val instanceof RootEntity) {
				BaseDescriptor descriptor = Descriptors
						.toDescriptor((RootEntity) val);
				text.setContent(descriptor);
			}
		} catch (Exception e) {
			error("Cannot set text value", e);
		}
	}

	private void setStringValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String val = text.getText();
		try {
			Bean.setValue(bean, property, val);
		} catch (Exception e) {
			error("Cannot set bean value", e);
		}
	}

	private void setShortValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String stringVal = text.getText();
		try {
			Short s = Short.parseShort(stringVal);
			Bean.setValue(bean, property, s);
			text.setToolTipText(null);
			text.setBackground(Colors.getWhite());
		} catch (NumberFormatException e) {
			text.setToolTipText("" + stringVal + " is not a valid number");
			text.setBackground(Colors.getErrorColor());
		} catch (Exception e) {
			error("Cannot set bean value", e);
		}
	}

	private void setIntValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String stringVal = text.getText();
		try {
			Integer s = Integer.parseInt(stringVal);
			Bean.setValue(bean, property, s);
			text.setToolTipText(null);
			text.setBackground(Colors.getWhite());
		} catch (NumberFormatException e) {
			text.setToolTipText("" + stringVal + " is not a valid number");
			text.setBackground(Colors.getErrorColor());
		} catch (Exception e) {
			error("Cannot set bean value", e);
		}
	}

	private void setDoubleValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String stringVal = text.getText();
		try {
			Double d = Double.parseDouble(stringVal);
			Bean.setValue(bean, property, d);
			text.setToolTipText(null);
			text.setBackground(Colors.getWhite());
		} catch (NumberFormatException e) {
			text.setToolTipText("" + stringVal + " is not a valid number");
			text.setBackground(Colors.getErrorColor());
		} catch (Exception e) {
			error("Cannot set bean value", e);
		}
	}

	private void setModel(Object bean, String property, TextDropComponent text) {
		log.trace("Change value {} @ {}", property, bean);
		try {
			BaseDescriptor descriptor = text.getContent();
			Object model = new BaseDao<>(descriptor.getModelType()
					.getModelClass(), Database.get()).getForId(descriptor
					.getId());
			Bean.setValue(bean, property, model);
		} catch (Exception e) {
			error("Cannot set bean value", e);
		}
	}

	private void error(String message, Exception e) {
		Logger log = LoggerFactory.getLogger(DataBinding.class);
		log.error(message, e);
	}

	private void editorChange() {
		if (editor != null)
			editor.setDirty(true);
	}

}
