package org.openlca.app.editors;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.function.Supplier;

import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.db.Database;
import org.openlca.app.util.Bean;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBinding {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private IEditor editor;

	public DataBinding() {
	}

	public DataBinding(IEditor editor) {
		this.editor = editor;
	}

	public <T> void on(final Object bean, final String property,
			final AbstractComboViewer<T> viewer) {
		log.trace("Register data binding - base descriptor - {} - {}", bean,
				property);
		if (bean == null || property == null || viewer == null)
			return;
		initValue(bean, property, viewer);
		viewer.addSelectionChangedListener(new ISelectionChangedListener<T>() {
			@Override
			public void selectionChanged(T selection) {
				setModel(bean, property, viewer);
				editorChange();
			}
		});
	}

	public void onModel(final Supplier<?> supplier, final String property,
			final TextDropComponent text) {
		log.trace("Register data binding - base descriptor - {} ", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.setHandler((descriptor) -> {
			setModel(supplier.get(), property, text);
			editorChange();
		});
	}

	public void onBoolean(final Supplier<?> supplier, final String property,
			final Button button) {
		log.trace("Register data binding - boolean - {}", property);
		if (supplier == null || property == null || button == null)
			return;
		initValue(supplier.get(), property, button);
		button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setBooleanValue(supplier.get(), property, button);
				editorChange();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	public void onDate(final Supplier<?> supplier, final String property,
			final DateTime dateTime) {
		log.trace("Register data binding - date - {}", property);
		if (supplier == null || property == null || dateTime == null)
			return;
		initValue(supplier.get(), property, dateTime);
		dateTime.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				setDateValue(supplier.get(), property, dateTime);
				editorChange();
			}
		});
	}

	public void readOnly(final Object bean, final String property,
			final Label label) {
		log.trace("Register data binding - string - {} - {}", bean, property);
		if (bean == null || property == null || label == null)
			return;
		initValue(bean, property, label);
	}

	public void readOnly(final Object bean, final String property,
			final CLabel label) {
		log.trace("Register data binding - string - {} - {}", bean, property);
		if (bean == null || property == null || label == null)
			return;
		initValue(bean, property, label);
	}

	public void onString(final Supplier<?> supplier, final String property,
			final Text text) {
		log.trace("Register data binding - string - {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setStringValue(supplier.get(), property, text);
			editorChange();
		});
	}

	public void onShort(final Supplier<?> supplier, final String property,
			final Text text) {
		log.trace("Register data binding - short - {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setShortValue(supplier.get(), property, text);
			editorChange();
		});
	}

	public void onInt(final Supplier<?> supplier, final String property,
			final Text text) {
		log.trace("Register data binding - int - {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setIntValue(supplier.get(), property, text);
			editorChange();
		});
	}

	public void onDouble(final Supplier<?> supplier, final String property,
			final Text text) {
		log.trace("Register data binding - double -  {}", property);
		if (supplier == null || property == null || text == null)
			return;
		initValue(supplier.get(), property, text);
		text.addModifyListener((e) -> {
			setDoubleValue(supplier.get(), property, text);
			editorChange();
		});
	}

	private void initValue(Object bean, String property, DateTime dateTime) {
		try {
			Object val = Bean.getValue(bean, property);
			if (val == null)
				return;
			GregorianCalendar calendar = null;
			if (val instanceof Date) {
				calendar = new GregorianCalendar();
				calendar.setTime((Date) val);
			} else if (val instanceof GregorianCalendar)
				calendar = (GregorianCalendar) val;
			if (calendar != null) {
				dateTime.setDay(calendar.get(Calendar.DAY_OF_MONTH));
				dateTime.setMonth(calendar.get(Calendar.MONTH));
				dateTime.setYear(calendar.get(Calendar.YEAR));
			}
		} catch (Exception e) {
			error("Cannot set text value", e);
		}
	}

	private void initValue(Object bean, String property, Text text) {
		if (bean == null)
			return;
		try {
			Object val = Bean.getValue(bean, property);
			String value = getValueAsString(val);
			text.setText(value);
		} catch (Exception e) {
			error("Cannot set text value", e);
		}
	}

	private void initValue(Object bean, String property, Label label) {
		try {
			Object val = Bean.getValue(bean, property);
			String value = getValueAsString(val);
			label.setText(value);
		} catch (Exception e) {
			error("Cannot set label value", e);
		}
	}

	private void initValue(Object bean, String property, CLabel label) {
		try {
			Object val = Bean.getValue(bean, property);
			String value = getValueAsString(val);
			label.setText(value);
		} catch (Exception e) {
			error("Cannot set label value", e);
		}
	}

	private String getValueAsString(Object val) {
		if (val == null)
			return "";
		if (val.getClass().isEnum())
			return Labels.getEnumText(val);
		else if (val instanceof RootEntity)
			return ((RootEntity) val).getName();
		else if (val instanceof BaseDescriptor)
			return ((BaseDescriptor) val).getName();
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
			error("Cannot set check state", e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void initValue(Object bean, String property,
			AbstractComboViewer<T> viewer) {
		try {
			Object val = Bean.getValue(bean, property);
			if (val == null)
				return;
			if (BaseDescriptor.class.isAssignableFrom(viewer.getType())
					&& !BaseDescriptor.class.isAssignableFrom(val.getClass())) {
				val = Descriptors.toDescriptor((RootEntity) val);
			}
			viewer.select((T) val);
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

	private void setDateValue(Object bean, String property, DateTime dateTime) {
		log.trace("Change value {} @ {}", property, bean);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.DAY_OF_MONTH, dateTime.getDay());
		calendar.set(Calendar.YEAR, dateTime.getYear());
		calendar.set(Calendar.MONTH, dateTime.getMonth());
		try {
			if (Bean.getType(bean, property) == Date.class)
				Bean.setValue(bean, property, calendar.getTime());
			else if (Bean.getType(bean, property) == GregorianCalendar.class)
				Bean.setValue(bean, property, calendar);
			else
				log.error("Cannot set bean value");
		} catch (Exception e) {
			error("Cannot set bean value", e);
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

	private void setBooleanValue(Object bean, String property, Button button) {
		log.trace("Change value {} @ {}", property, bean);
		boolean val = button.getSelection();
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

	private <T> void setModel(Object bean, String property,
			AbstractComboViewer<T> viewer) {
		log.trace("Change value {} @ {}", property, bean);
		try {
			T value = viewer.getSelected();
			Object model = null;
			if (value != null)
				if (value instanceof BaseDescriptor) {
					BaseDescriptor descriptor = (BaseDescriptor) value;
					Class<?> modelClass = descriptor.getModelType()
							.getModelClass();
					model = new BaseDao<>(modelClass, Database.get())
							.getForId(descriptor.getId());
				} else
					model = value;
			Bean.setValue(bean, property, model);
		} catch (Exception e) {
			error("Cannot set bean value", e);
		}
	}

	private void setModel(Object bean, String property, TextDropComponent text) {
		log.trace("Change value {} @ {}", property, bean);
		try {
			BaseDescriptor descriptor = text.getContent();
			Object model = null;
			if (descriptor != null)
				model = new BaseDao<>(
						descriptor.getModelType().getModelClass(),
						Database.get()).getForId(descriptor.getId());
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
