package org.openlca.app.viewers.table;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.util.Actions;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of AbstractViewer for SWT table viewer.
 * 
 * There are three extensions that can be implemented by annotating the methods
 * of implementing classes. To enable creation and removal actions use
 * annotations {@link OnAdd} and {@link OnRemove}. The run methods of each
 * action will call all annotated methods. Implementations are responsible to
 * update the input. To enable drop feature use {@link OnDrop} and specify the
 * type of accepted elements by the input parameter of the annotated method.
 */
public class AbstractTableViewer<T> extends AbstractViewer<T, TableViewer> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private List<Action> actions;
	private ModifySupport<T> cellModifySupport;

	protected AbstractTableViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		TableViewer viewer = new TableViewer(parent, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.MULTI);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(getLabelProvider());
		initTable(viewer);
		Table table = viewer.getTable();
		UI.gridData(table, true, true);
		createActions(viewer);
		if (supports(OnDrop.class))
			addDropSupport(viewer);
		if (useColumnHeaders())
			cellModifySupport = new ModifySupport<>(viewer);
		return viewer;
	}

	private void initTable(TableViewer viewer) {
		Table table = viewer.getTable();
		if (!useColumnHeaders()) {
			table.setLinesVisible(false);
			table.setHeaderVisible(false);
		} else {
			String[] columnHeaders = getColumnHeaders();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			for (String p : columnHeaders)
				new TableColumn(table, SWT.NULL).setText(p);
			for (TableColumn c : table.getColumns())
				c.pack();
			viewer.setColumnProperties(columnHeaders);
		}
	}

	private void createActions(TableViewer viewer) {
		actions = new ArrayList<>();
		if (supports(OnAdd.class))
			actions.add(Actions.onAdd(() -> call(OnAdd.class)));
		if (supports(OnRemove.class)) {
			actions.add(Actions.onRemove(() -> call(OnRemove.class)));
			Tables.onDeletePressed(viewer, (e) -> call(OnRemove.class));
		}
		// we have to create this array, because we do not want to have the copy
		// action in the section menu
		Action[] tableActions = new Action[actions.size() +1];
		for(int i = 0; i < actions.size(); i++)
			tableActions[i] = actions.get(i);
		tableActions[actions.size()] = TableClipboard.onCopy(viewer);
		Actions.bind(viewer, tableActions);
	}

	private void addDropSupport(TableViewer viewer) {
		final Transfer transferType = ModelTransfer.getInstance();
		DropTarget dropTarget = new DropTarget(viewer.getTable(), DND.DROP_COPY
				| DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transferType });
		final AbstractTableViewer<T> thisObject = this;
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				if (transferType.isSupportedType(event.currentDataType))
					if (event.data != null)
						for (Method method : getMethods(OnDrop.class))
							tryInvoke(method, event.data);
			}

			private void tryInvoke(Method method, Object value) {
				Class<?> parameterType = method.getParameterTypes().length > 0 ? method
						.getParameterTypes()[0]
						: null;
				Class<?> dataType = value.getClass();
				if (dataType.isArray()) {
					for (Object object : (Object[]) value)
						if (parameterType == object.getClass()) {
							try {
								method.invoke(thisObject, object);
							} catch (Exception e) {
								log.error("Error invoking OnDrop method", e);
							}
						}
				} else {
					if (parameterType == dataType)
						try {
							method.invoke(thisObject, value);
						} catch (Exception e) {
							log.error("Error invoking OnDrop method", e);
						}
				}
			}
		});
	}

	protected ModifySupport<T> getModifySupport() {
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

	/**
	 * Binds the create and remove actions of the table viewer to the given
	 * section.
	 */
	public void bindTo(Section section) {
		Actions.bind(section, actions.toArray(new Action[actions.size()]));
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

	private boolean supports(Class<? extends Annotation> clazz) {
		for (Method method : this.getClass().getDeclaredMethods())
			if (method.isAnnotationPresent(clazz))
				return true;
		return false;
	}

	private void call(Class<? extends Annotation> clazz) {
		for (Method method : getMethods(clazz))
			try {
				method.setAccessible(true);
				method.invoke(this);
			} catch (Exception e) {
				log.error("Cannot call method for " + clazz.getSimpleName(), e);
			}
	}

	private List<Method> getMethods(Class<? extends Annotation> clazz) {
		List<Method> methods = new ArrayList<>();
		for (Method method : this.getClass().getDeclaredMethods())
			if (method.isAnnotationPresent(clazz))
				methods.add(method);
		return methods;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface OnAdd {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface OnRemove {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface OnDrop {
	}

}
