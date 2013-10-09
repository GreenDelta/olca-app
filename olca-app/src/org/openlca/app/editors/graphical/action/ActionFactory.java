package org.openlca.app.editors.graphical.action;

import static org.openlca.app.editors.graphical.action.ChangeAllStateAction.MAXIMIZE;
import static org.openlca.app.editors.graphical.action.ChangeAllStateAction.MINIMIZE;
import static org.openlca.app.editors.graphical.action.MassExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical.action.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical.action.HideShowAction.HIDE;
import static org.openlca.app.editors.graphical.action.HideShowAction.SHOW;

import org.eclipse.gef.ui.parts.TreeViewer;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessType;

public class ActionFactory {

	public static BuildSupplyChainAction createBuildSupplyChainAction(
			ProcessType preferredType) {
		return new BuildSupplyChainAction(preferredType);
	}

	public static BuildSupplyChainMenuAction createBuildSupplyChainMenuAction(
			ProductSystemGraphEditor editor) {
		BuildSupplyChainMenuAction action = new BuildSupplyChainMenuAction();
		action.setEditor(editor);
		return action;
	}

	public static ChangeAllStateAction createMinimizeAllAction(
			ProductSystemNode model) {
		ChangeAllStateAction action = new ChangeAllStateAction(MINIMIZE);
		action.setModel(model);
		return action;
	}

	public static ChangeAllStateAction createMaximizeAllAction(
			ProductSystemNode model) {
		ChangeAllStateAction action = new ChangeAllStateAction(MAXIMIZE);
		action.setModel(model);
		return action;
	}

	public static MassExpansionAction createExpandAllAction(
			ProductSystemGraphEditor editor) {
		MassExpansionAction action = new MassExpansionAction(EXPAND);
		action.setEditor(editor);
		return action;
	}

	public static MassExpansionAction createCollapseAllAction(
			ProductSystemGraphEditor editor) {
		MassExpansionAction action = new MassExpansionAction(COLLAPSE);
		action.setEditor(editor);
		return action;
	}

	public static HideShowAction createShowAction(ProductSystemNode model,
			TreeViewer viewer) {
		HideShowAction action = new HideShowAction(SHOW);
		action.setModel(model);
		action.setViewer(viewer);
		return action;
	}

	public static HideShowAction createHideAction(ProductSystemNode model,
			TreeViewer viewer) {
		HideShowAction action = new HideShowAction(HIDE);
		action.setModel(model);
		action.setViewer(viewer);
		return action;
	}

	public static LayoutAction createLayoutAction(ProductSystemNode model,
			GraphLayoutType layoutType) {
		LayoutAction action = new LayoutAction(layoutType);
		action.setModel(model);
		return action;
	}

	public static OpenMiniatureViewAction createOpenMiniatureViewAction() {
		return new OpenMiniatureViewAction();
	}

	public static RemoveAllConnectionsAction createRemoveAllConnectionsAction(
			ProductSystemGraphEditor editor) {
		RemoveAllConnectionsAction action = new RemoveAllConnectionsAction();
		action.setEditor(editor);
		return action;
	}

	public static RemoveSupplyChainAction createRemoveSupplyChainAction(
			ProductSystemGraphEditor editor) {
		RemoveSupplyChainAction action = new RemoveSupplyChainAction();
		action.setEditor(editor);
		return action;
	}

	public static SaveImageAction createSaveImageAction(ProductSystemNode model) {
		SaveImageAction action = new SaveImageAction();
		action.setModel(model);
		return action;
	}

}
