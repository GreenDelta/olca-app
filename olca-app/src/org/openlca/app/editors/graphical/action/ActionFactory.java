package org.openlca.app.editors.graphical.action;

import static org.openlca.app.editors.graphical.action.ChangeAllStateAction.MAXIMIZE;
import static org.openlca.app.editors.graphical.action.ChangeAllStateAction.MINIMIZE;
import static org.openlca.app.editors.graphical.action.ExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical.action.ExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical.action.HideShowAction.SHOW;
import static org.openlca.app.editors.graphical.action.HideShowAction.HIDE;

import org.eclipse.gef.ui.parts.TreeViewer;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ActionFactory {

	public static BuildSupplyChainAction createBuildSupplyChainAction(
			ProcessType preferredType) {
		return new BuildSupplyChainAction(preferredType);
	}

	public static BuildSupplyChainMenuAction createBuildSupplyChainMenuAction(
			ProductSystemNode model) {
		return createBuildSupplyChainMenuAction(model, null);
	}

	public static BuildSupplyChainMenuAction createBuildSupplyChainMenuAction(
			ProductSystemNode model, ProcessDescriptor startProcess) {
		BuildSupplyChainMenuAction action = new BuildSupplyChainMenuAction();
		action.setModel(model);
		action.setStartProcess(startProcess);
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

	public static ExpansionAction createExpandAllAction(ProductSystemNode model) {
		ExpansionAction action = new ExpansionAction(EXPAND);
		action.setModel(model);
		return action;
	}

	public static ExpansionAction createCollapseAllAction(
			ProductSystemNode model) {
		ExpansionAction action = new ExpansionAction(COLLAPSE);
		action.setModel(model);
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

	public static RemoveAllConnectionsAction createRemoveAllConnectionsAction() {
		RemoveAllConnectionsAction action = new RemoveAllConnectionsAction();

		return action;
	}

	public static RemoveSupplyChainAction createRemoveSupplyChainAction(
			ProcessNode node) {
		RemoveSupplyChainAction action = new RemoveSupplyChainAction();
		action.setNode(node);
		return action;
	}

	public static SaveImageAction createSaveImageAction(ProductSystemNode model) {
		SaveImageAction action = new SaveImageAction();
		action.setModel(model);
		return action;
	}

}
