package org.openlca.app.editors;

import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.components.ModelLink;
import org.openlca.app.db.Database;
import org.openlca.app.util.Bean;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.Daos;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBinding {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private ModelEditor<?> editor;

	public DataBinding() {
	}

	public DataBinding(ModelEditor<?> editor) {
		this.editor = editor;
	}

	public <T> void onModel(Supplier<?> supplier, String property,
		AbstractComboViewer<T> viewer) {
		log.trace("Register data binding - base descriptor - {}", property);
		if (supplier == null || property == null || viewer == null)
			return;
		initValue(supplier.get(), property, viewer);
		viewer.addSelectionChangedListener((selection)
			-> setModel(supplier.get(), property, viewer));
	}

	private void setModel(Object bean, String property,
		AbstractComboViewer<?> viewer) {
		log.trace("Change value {} @ {}", property, bean);
		try {
			Object newValue = viewer.getSelected();
			if (newValue instanceof Descriptor descriptor) {
				var modelClass = descriptor.type.getModelClass();
				newValue = Daos.base(Database.get(), modelClass).getForId(descriptor.id);
			}
			Object oldValue = Bean.getValue(bean, property);
			if (Objects.equals(newValue, oldValue))
				return;
			Bean.setValue(bean, property, newValue);
			editorChange();
		} catch (Exception e) {
			ErrorReporter.on("Cannot set bean value", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends RootEntity> void onModel(
		Supplier<?> supplier, String property, ModelLink<T> link) {
		if (supplier == null || property == null || link == null)
			return;

		// try to set the initial value
		try {
			var val = Bean.getValue(supplier.get(), property);
			if (val instanceof RootEntity model) {
				link.setModel((T) model);
			}
		} catch (Exception e) {
			ErrorReporter.on("failed to set value in model link", e);
		}

		link.onChange(next -> {
			try {
				var obj = supplier.get();
				var current = Bean.getValue(obj, property);
				if (Objects.equals(current, next))
					return;
				Bean.setValue(obj, property, next);
				editorChange();
			} catch (Exception e) {
				ErrorReporter.on("failed to set value in model link", e);
			}
		});
	}

	public void onBoolean(Supplier<?> supplier, String property, Button button) {
		log.trace("Register data binding - boolean - {}", property);
		if (supplier == null || property == null || button == null)
			return;
		initValue(supplier.get(), property, button);
		Controls.onSelect(button, (e) -> {
			setBooleanValue(supplier.get(), property, button);
			editorChange();
		});
	}

	public void readOnly(Object bean, String property, Label label) {
		log.trace("Register data binding - string - {} - {}", bean, property);
		if (bean == null || property == null || label == null)
			return;
		initValue(bean, property, label);
	}

	public void readOnly(Object bean, String property, CLabel label) {
		log.trace("Register data binding - string - {} - {}", bean, property);
		if (bean == null || property == null || label == null)
			return;
		initValue(bean, property, label);
	}

	public void onString(Supplier<?> supplier, String property, Text text) {
		log.trace("Register data binding - string - {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setStringValue(supplier.get(), property, text);
			editorChange();
		});
	}

	public void onShort(Supplier<?> supplier, String property, Text text) {
		log.trace("Register data binding - short - {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setShortValue(supplier.get(), property, text);
			editorChange();
		});
	}

	public void onInt(Supplier<?> supplier, String property, Text text) {
		log.trace("Register data binding - int - {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setIntValue(supplier.get(), property, text);
			editorChange();
		});
	}

	public void onDouble(Supplier<?> supplier, String property, Text text) {
		log.trace("Register data binding - double -  {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setDoubleValue(supplier.get(), property, text);
			editorChange();
		});
	}

	private void initValue(Object bean, String property, Text text) {
		if (bean == null)
			return;
		try {
			Object val = Bean.getValue(bean, property);
			String value = getValueAsString(val);
			text.setText(value);
		} catch (Exception e) {
			ErrorReporter.on("Cannot set text value", e);
		}
	}

	private void initValue(Object bean, String property, Label label) {
		try {
			Object val = Bean.getValue(bean, property);
			String value = getValueAsString(val);
			label.setText(value);
		} catch (Exception e) {
			ErrorReporter.on("Cannot set label value", e);
		}
	}

	private void initValue(Object bean, String property, CLabel label) {
		try {
			Object val = Bean.getValue(bean, property);
			String value = getValueAsString(val);
			label.setText(value);
		} catch (Exception e) {
			ErrorReporter.on("Cannot set label value", e);
		}
	}

	private String getValueAsString(Object val) {
		if (val == null)
			return "";
		if (val.getClass().isEnum())
			return Labels.getEnumText(val);
		else if (val instanceof RefEntity e)
			return e.name;
		else if (val instanceof Descriptor)
			return ((Descriptor) val).name;
		else if (val instanceof Date)
			return DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT).format((Date) val);
		else if (val instanceof GregorianCalendar)
			return DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT).format(
				((GregorianCalendar) val).getTime());
		else
			return val.toString();
	}

	private void initValue(Object bean, String property, Button button) {
		try {
			Object val = Bean.getValue(bean, property);
			if (val != null)
				if (val instanceof Boolean)
					button.setSelection((Boolean) val);
		} catch (Exception e) {
			ErrorReporter.on("Cannot set check state", e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void initValue(Object bean, String property,
		AbstractComboViewer<T> viewer) {
		try {
			Object val = Bean.getValue(bean, property);
			if (val == null)
				return;
			if (Descriptor.class.isAssignableFrom(viewer.getType())
				&& !Descriptor.class.isAssignableFrom(val.getClass())) {
				val = Descriptor.of((RefEntity) val);
			}
			viewer.select((T) val);
		} catch (Exception e) {
			ErrorReporter.on("Cannot set text value", e);
		}
	}

	private void setStringValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String val = text.getText();
		try {
			Bean.setValue(bean, property, val);
		} catch (Exception e) {
			ErrorReporter.on("Cannot set bean value", e);
		}
	}

	private void setBooleanValue(Object bean, String property, Button button) {
		log.trace("Change value {} @ {}", property, bean);
		boolean val = button.getSelection();
		try {
			Bean.setValue(bean, property, val);
		} catch (Exception e) {
			ErrorReporter.on("Cannot set bean value", e);
		}
	}

	private void setShortValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String stringVal = text.getText();
		try {
			Short s = Short.parseShort(stringVal);
			Bean.setValue(bean, property, s);
			text.setToolTipText(null);
			text.setBackground(Colors.white());
		} catch (NumberFormatException e) {
			text.setToolTipText("" + stringVal + " is not a valid number");
			text.setBackground(Colors.errorColor());
		} catch (Exception e) {
			ErrorReporter.on("Cannot set bean value", e);
		}
	}

	private void setIntValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String stringVal = text.getText();
		try {
			Integer s = Integer.parseInt(stringVal);
			Bean.setValue(bean, property, s);
			text.setToolTipText(null);
			text.setBackground(Colors.white());
		} catch (NumberFormatException e) {
			text.setToolTipText("" + stringVal + " is not a valid number");
			text.setBackground(Colors.errorColor());
		} catch (Exception e) {
			ErrorReporter.on("Cannot set bean value", e);
		}
	}

	private void setDoubleValue(Object bean, String property, Text text) {
		log.trace("Change value {} @ {}", property, bean);
		String stringVal = text.getText();
		try {
			Double d = Double.parseDouble(stringVal);
			Bean.setValue(bean, property, d);
			text.setToolTipText(null);
			text.setBackground(Colors.white());
		} catch (NumberFormatException e) {
			text.setToolTipText("" + stringVal + " is not a valid number");
			text.setBackground(Colors.errorColor());
		} catch (Exception e) {
			ErrorReporter.on("Cannot set bean value", e);
		}
	}

	private void editorChange() {
		if (editor != null)
			editor.setDirty(true);
	}

}
