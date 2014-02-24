package org.openlca.app.editors;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.components.ISingleModelDrop;
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.db.Database;
import org.openlca.app.util.Bean;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.IModelChangedListener;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBinding {

	public enum TextBindType {

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

	/** Removes *all* selection listeners from the given date time. */
	public void release(DateTime dateTime) {
		Listener[] listeners = dateTime.getListeners(SWT.Selection);
		log.trace("release {} listeners from date time", listeners.length);
		for (Listener listener : listeners) {
			if (!(listener instanceof SelectionListener))
				continue;
			SelectionListener sel = (SelectionListener) listener;
			dateTime.removeSelectionListener(sel);
		}
	}

	/** Removes *all* selection listeners from the given button. */
	public void release(Button button) {
		Listener[] listeners = button.getListeners(SWT.Selection);
		log.trace("release {} listeners from button", listeners.length);
		for (Listener listener : listeners) {
			if (!(listener instanceof SelectionListener))
				continue;
			SelectionListener sel = (SelectionListener) listener;
			button.removeSelectionListener(sel);
		}
	}

	/** Unsets the handler of the given text drop component. */
	public void release(TextDropComponent component) {
		component.setHandler(null);
	}

	/** Removes *all* selection changed listeners from the given viewer. */
	public <T> void release(AbstractComboViewer<T> viewer) {
		ISelectionChangedListener<T>[] listeners = viewer
				.getSelectionChangedListeners();
		log.trace("release {} listeners from viewer", listeners.length);
		for (ISelectionChangedListener<T> listener : listeners)
			viewer.removeSelectionChangedListener(listener);
	}

	/** Removes *all* selection changed listeners from the given viewer. */
	public <T> void release(AbstractTableViewer<T> viewer) {
		ISelectionChangedListener<T>[] listeners = viewer
				.getSelectionChangedListeners();
		log.trace("release {} listeners from viewer", listeners.length);
		for (ISelectionChangedListener<T> listener : listeners)
			viewer.removeSelectionChangedListener(listener);

		IModelChangedListener<T>[] modelListeners = viewer
				.getModelChangedListeners();
		log.trace("release {} listeners from viewer", listeners.length);
		for (IModelChangedListener<T> listener : modelListeners)
			viewer.removeModelChangedListener(listener);
	}

	@SuppressWarnings("unchecked")
	public <T> void on(final Object bean, final String property,
			AbstractTableViewer<T> viewer) {
		List<T> modelList = null;
		try {
			modelList = (List<T>) Bean.getValue(bean, property);
		} catch (Exception e) {
			log.error("Cannot find property " + property
					+ ", is not a list or generic type does not match");
			return;
		}
		try {
			Method setInput = viewer.getClass().getDeclaredMethod("setInput",
					bean.getClass());
			setInput.setAccessible(true);
			setInput.invoke(viewer, bean);
		} catch (Exception e) {
			log.error(
					"Cannot set viewer input for type " + bean.getClass()
							+ " on viewer " + viewer.getClass()
							+ ". Note that there must be" + "a setInput<"
							+ bean.getClass() + "> method in the viewer.", e);
			return;
		}

		viewer.addModelChangedListener(new BoundModelChangedListener<T>(
				modelList));
	}

	private class BoundModelChangedListener<T> implements
			IModelChangedListener<T> {

		private List<T> list;

		private BoundModelChangedListener(List<T> list) {
			this.list = list;
		}

		@Override
		public void modelChanged(ModelChangeType type, T element) {
			if (type == ModelChangeType.CREATE)
				list.add(element);
			if (type == ModelChangeType.REMOVE)
				list.remove(element);
			editorChange();
		}

	}

	public <T> void on(final Object bean, final String property,
			final AbstractComboViewer<T> viewer) {
		log.trace("Register data binding - base descriptor - {} - {}", bean,
				property);
		if (bean == null || property == null || viewer == null)
			return;

		checkType(bean, property, viewer);
		initValue(bean, property, viewer);
		viewer.addSelectionChangedListener(new ISelectionChangedListener<T>() {
			@Override
			public void selectionChanged(T selection) {
				setModel(bean, property, viewer);
				editorChange();
			}
		});
	}

	public void on(final Object bean, final String property,
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

	public void on(final Object bean, final String property, final Button button) {
		log.trace("Register data binding - string - {} - {}", bean, property);
		if (bean == null || property == null || button == null)
			return;
		initValue(bean, property, button);
		button.addSelectionListener(new SelectionListener() {

			private void selected() {
				setBooleanValue(bean, property, button);
				editorChange();
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				selected();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				selected();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <T> void checkType(final Object bean, final String property,
			final AbstractComboViewer<T> viewer) {
		try {
			Class<?> propertyType = Bean.getType(bean, property);
			if (propertyType != viewer.getType())
				if (RootEntity.class.isAssignableFrom(propertyType)
						&& BaseDescriptor.class.isAssignableFrom(viewer
								.getType()))
					if (propertyType != Descriptors
							.getModelClass((Class<? extends BaseDescriptor>) viewer
									.getType()))
						throw new IllegalArgumentException("Cannot bind "
								+ viewer.getType().getCanonicalName() + " to "
								+ propertyType.getCanonicalName());
		} catch (Exception e) {
			error("Cannot bind bean", e);
		}
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

	public void on(final Object bean, final String property,
			final DateTime dateTime) {
		log.trace("Register data binding - string - {} - {}", bean, property);
		if (bean == null || property == null || dateTime == null)
			return;
		initValue(bean, property, dateTime);
		dateTime.addSelectionListener(new SelectionListener() {
			private void selected() {
				setDateValue(bean, property, dateTime);
				editorChange();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				selected();
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				selected();
			}
		});
	}

	public void on(final Object bean, final String property, TextBindType type,
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
		default:
			// Enum values are read only
			break;
		}
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

	private void initValue(Object bean, String property, DateTime dateTime) {
		try {
			Object val = Bean.getValue(bean, property);
			if (val != null) {
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
			}
		} catch (Exception e) {
			error("Cannot set text value", e);
		}
	}

	private void initValue(Object bean, String property, Text text) {
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
		if (val != null)
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
		return "";
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
			if (val != null) {
				if (BaseDescriptor.class.isAssignableFrom(viewer.getType())
						&& !BaseDescriptor.class.isAssignableFrom(val
								.getClass())) {
					val = Descriptors.toDescriptor((RootEntity) val);
				}
				viewer.select((T) val);
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
