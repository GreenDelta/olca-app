package org.openlca.app.editors.graphical_legacy.action;

import static org.openlca.app.editors.graphical_legacy.action.MinMaxAllAction.MAXIMIZE;
import static org.openlca.app.editors.graphical_legacy.action.MinMaxAllAction.MINIMIZE;
import static org.openlca.app.editors.graphical_legacy.action.HideShowAction.HIDE;
import static org.openlca.app.editors.graphical_legacy.action.HideShowAction.SHOW;
import static org.openlca.app.editors.graphical_legacy.action.MassExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical_legacy.action.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical_legacy.action.SearchConnectorsAction.PROVIDER;
import static org.openlca.app.editors.graphical_legacy.action.SearchConnectorsAction.RECIPIENTS;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.viewers.Selections;

public class GraphActions {

	private GraphActions() {
	}

	public static <T> T firstSelectedOf(GraphEditor editor, Class<T> type) {
		return editor == null || type == null
				? null
				: firstSelectedOf(editor.getSelection(), type);
	}

	public static <T> T firstSelectedOf(ISelection s, Class<T> type) {
		var obj = Selections.firstOf(s);
		if (obj == null || type == null)
			return null;
		if (type.isInstance(s))
			return type.cast(s);
		if (!(obj instanceof EditPart))
			return null;
		var model = ((EditPart) obj).getModel();
		return type.isInstance(model)
				? type.cast(model)
				: null;
	}

	public static <T> List<T> allSelectedOf(GraphEditor editor, Class<T> type) {
		return editor == null || type == null
				? Collections.emptyList()
				: allSelectedOf(editor.getSelection(), type);
	}

	public static <T> List<T> allSelectedOf(ISelection s, Class<T> type) {
		var objects = Selections.allOf(s);
		if (objects.isEmpty() || type == null)
			return Collections.emptyList();
		return objects.stream()
				.map(obj -> obj instanceof EditPart
						? ((EditPart) obj).getModel()
						: obj)
				.filter(type::isInstance)
				.map(type::cast)
				.collect(Collectors.toList());
	}

	public static IAction minimizeAll(GraphEditor editor) {
		MinMaxAllAction action = new MinMaxAllAction(MINIMIZE);
		action.editor = editor;
		return action;
	}

	public static IAction maximizeAll(GraphEditor editor) {
		MinMaxAllAction action = new MinMaxAllAction(MAXIMIZE);
		action.editor = editor;
		return action;
	}

	public static IAction expandAll(GraphEditor editor) {
		var action = new MassExpansionAction(EXPAND);
		action.editor = editor;
		return action;
	}

	public static IAction collapseAll(GraphEditor editor) {
		var action = new MassExpansionAction(COLLAPSE);
		action.editor = editor;
		return action;
	}

	public static IAction show(GraphEditor editor, TreeViewer viewer) {
		return new HideShowAction(editor, viewer, SHOW);
	}

	public static IAction hide(GraphEditor editor, TreeViewer viewer) {
		return new HideShowAction(editor, viewer, HIDE);
	}

	public static IAction openMiniatureView(GraphEditor editor) {
		OpenMiniatureViewAction action = new OpenMiniatureViewAction();
		action.editor = editor;
		return action;
	}

	public static IAction removeAllConnections(GraphEditor editor) {
		RemoveAllConnectionsAction action = new RemoveAllConnectionsAction();
		action.editor = editor;
		return action;
	}

	public static IAction removeSupplyChain(GraphEditor editor) {
		RemoveSupplyChainAction action = new RemoveSupplyChainAction();
		action.editor = editor;
		return action;
	}

	public static IAction searchProviders(GraphEditor editor) {
		SearchConnectorsAction action = new SearchConnectorsAction(PROVIDER);
		action.editor = editor;
		return action;
	}

	public static IAction searchRecipients(GraphEditor editor) {
		SearchConnectorsAction action = new SearchConnectorsAction(RECIPIENTS);
		action.editor = editor;
		return action;
	}

	public static IAction showOutline() {
		return new ShowOutlineAction();
	}

}
