package org.openlca.app.viewers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageManager;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.AbstractTableViewer.IModelChangedListener.Type;
import org.openlca.app.viewers.modify.CellModifySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTableViewer<T> extends AbstractViewer<T, TableViewer> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private List<IModelChangedListener<T>> changeListener = new ArrayList<>();
	private List<Action> actions;
	private CellModifySupport<T> cellModifySupport;

	protected AbstractTableViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		TableViewer viewer = new TableViewer(parent, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI);

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(getLabelProvider());
		viewer.setSorter(getSorter());

		Table table = viewer.getTable();
		String[] columnHeaders = getColumnHeaders();
		if (!useColumnHeaders()) {
			table.setLinesVisible(false);
			table.setHeaderVisible(false);
		} else {
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			for (String p : columnHeaders)
				new TableColumn(table, SWT.NULL).setText(p);
			for (TableColumn c : table.getColumns())
				c.pack();
		}
		if (useColumnHeaders())
			viewer.setColumnProperties(columnHeaders);
		UI.gridData(table, true, true);

		actions = new ArrayList<>();
		if (supports(OnCreate.class))
			actions.add(new CreateAction());
		if (supports(OnRemove.class))
			actions.add(new RemoveAction());
		UI.bindActions(viewer, actions.toArray(new Action[actions.size()]));

		cellModifySupport = new CellModifySupport<>(viewer);

		return viewer;
	}

	protected CellModifySupport<T> getCellModifySupport() {
		return cellModifySupport;
	}

	/**
	 * Subclasses may override this for support of column headers for the table
	 * combo, if null or empty array is returned, the headers are not visible
	 * and the combo behaves like a standard combo
	 */
	protected String[] getColumnHeaders() {
		return null;
	}

	private boolean useColumnHeaders() {
		return getColumnHeaders() != null && getColumnHeaders().length > 0;
	}

	public void bindTo(Section section) {
		UI.bindActions(section, actions.toArray(new Action[actions.size()]));
	}

	@SuppressWarnings("unchecked")
	public List<T> getAllSelected() {
		List<Object> list = Viewers.getAllSelected(getViewer());
		List<T> result = new ArrayList<>();
		for (Object value : list)
			if (!(value instanceof AbstractViewer.Null))
				result.add((T) value);
		return result;
	}

	public void addDoubleClickListener(IDoubleClickListener listener) {
		getViewer().addDoubleClickListener(listener);
	}

	public void removeDoubleClickListener(IDoubleClickListener listener) {
		getViewer().removeDoubleClickListener(listener);
	}

	public void addModelChangedListener(IModelChangedListener<T> listener) {
		if (!changeListener.contains(listener))
			changeListener.add(listener);
	}

	public void removeModelChangedListener(IModelChangedListener<T> listener) {
		if (changeListener.contains(listener))
			changeListener.remove(listener);
	}

	protected void fireModelChanged(Type type, T element) {
		for (IModelChangedListener<T> listener : changeListener)
			listener.modelChanged(type, element);
	}

	private boolean supports(Class<? extends Annotation> clazz) {
		for (Method method : this.getClass().getDeclaredMethods())
			if (method.isAnnotationPresent(clazz))
				return true;
		return false;
	}

	private void call(Class<? extends Annotation> clazz) {
		for (Method method : this.getClass().getDeclaredMethods())
			if (method.isAnnotationPresent(clazz))
				try {
					method.invoke(this);
				} catch (Exception e) {
					log.error("Cannot call onAdd method", e);
				}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	protected @interface OnCreate {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	protected @interface OnRemove {
	}

	public interface IModelChangedListener<T> {

		public enum Type {

			CREATE, REMOVE, CHANGE;

		}

		void modelChanged(Type type, T element);

	}

	private class CreateAction extends Action {

		private CreateAction() {
			setText(Messages.AddAction_Text);
			setImageDescriptor(ImageManager
					.getImageDescriptor(ImageType.ADD_ICON));
			setDisabledImageDescriptor(ImageManager
					.getImageDescriptor(ImageType.ADD_ICON_DISABLED));
		}

		@Override
		public void run() {
			call(OnCreate.class);
		}

	}

	private class RemoveAction extends Action {

		private RemoveAction() {
			setText(Messages.RemoveAction_Text);
			setImageDescriptor(ImageManager
					.getImageDescriptor(ImageType.DELETE_ICON));
			setDisabledImageDescriptor(ImageManager
					.getImageDescriptor(ImageType.DELETE_ICON_DISABLED));
		}

		@Override
		public void run() {
			call(OnRemove.class);
		}

	}

}
