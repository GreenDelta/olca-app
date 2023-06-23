package org.openlca.app.viewers.tables;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.util.Actions;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of AbstractViewer for SWT table viewer.
 * <p>
 * There are three extensions that can be implemented by annotating the methods
 * of implementing classes. To enable creation and removal actions use
 * annotations {@link OnAdd} and {@link OnRemove}. The run methods of each
 * action will call all annotated methods. Implementations are responsible to
 * update the input. To enable drop feature use {@link OnDrop} and specify the
 * type of accepted elements by the input parameter of the annotated method.
 */
public class AbstractTableViewer<T> extends AbstractViewer<T, TableViewer> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private List<Action> actions;
	private ModifySupport<T> cellModifySupport;

	protected AbstractTableViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected TableViewer createViewer(Composite parent) {
		TableViewer viewer = Tables.createViewer(parent, getColumnHeaders(), getLabelProvider());
		decorateViewer(viewer);
		return viewer;
	}

	protected void decorateViewer(TableViewer viewer) {
		createActions(viewer);
		if (supports(OnDrop.class))
			addDropSupport(viewer);
		if (useColumnHeaders())
			cellModifySupport = new ModifySupport<>(viewer);
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
		List<Action> additionalActions = getAdditionalActions();
		Action[] tableActions = new Action[actions.size() + additionalActions.size() + 1];
		for (int i = 0; i < actions.size(); i++)
			tableActions[i] = actions.get(i);
		for (int i = 0; i < additionalActions.size(); i++)
			tableActions[i + actions.size()] = additionalActions.get(i);
		tableActions[tableActions.length - 1] = TableClipboard.onCopySelected(viewer);
		Actions.bind(viewer, tableActions);
	}

	protected List<Action> getAdditionalActions() {
		return Collections.emptyList();
	}

	private void addDropSupport(TableViewer viewer) {
		Transfer transferType = ModelTransfer.getInstance();
		DropTarget dropTarget = new DropTarget(viewer.getTable(), DND.DROP_COPY
				| DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(transferType);
		AbstractTableViewer<T> thisObject = this;
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent event) {
				if (transferType.isSupportedType(event.currentDataType))
					if (event.data != null)
						for (Method method : getMethods(OnDrop.class))
							tryInvoke(method, event.data);
			}

			private void tryInvoke(Method method, Object arg) {
				if (method == null || arg == null)
					return;
				var paramTypes = method.getParameterTypes();
				if (paramTypes.length != 1) {
					log.error("OnDrop methods must have a single parameter: {}", method);
					return;
				}
				var paramType = paramTypes[0];

				Consumer<Object> invoke = val -> {
					var argType = val.getClass();
					if (!paramType.isAssignableFrom(argType)) {
						log.error("called OnDrop with {} on {}", argType, paramType);
						return;
					}
					try {
						method.setAccessible(true);
						method.invoke(thisObject, val);
					} catch (Exception e) {
						log.error("invoking OnDrop method failed", e);
					}
				};

				var argType = arg.getClass();
				if (argType.isArray()) {
					for (Object ai : (Object[]) arg) {
						invoke.accept(ai);
					}
				} else {
					invoke.accept(arg);
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
	public void bindTo(Section section, Action... additionalActions) {
		List<Action> all = actions;
		if (additionalActions != null && additionalActions.length > 0) {
			all = new ArrayList<>(actions);
			all.addAll(Arrays.asList(additionalActions));
		}
		Actions.bind(section, all.toArray(new Action[0]));
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
