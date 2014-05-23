package org.openlca.app.editors.graphical.action;

import static org.openlca.app.editors.graphical.action.ChangeAllStateAction.MAXIMIZE;
import static org.openlca.app.editors.graphical.action.ChangeAllStateAction.MINIMIZE;
import static org.openlca.app.editors.graphical.action.HideShowAction.HIDE;
import static org.openlca.app.editors.graphical.action.HideShowAction.SHOW;
import static org.openlca.app.editors.graphical.action.MarkingAction.MARK;
import static org.openlca.app.editors.graphical.action.MarkingAction.UNMARK;
import static org.openlca.app.editors.graphical.action.MassExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical.action.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical.action.SearchConnectorsAction.PROVIDER;
import static org.openlca.app.editors.graphical.action.SearchConnectorsAction.RECIPIENTS;

import org.eclipse.gef.ui.parts.TreeViewer;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;

public class ActionFactory {

	public static BuildSupplyChainAction createBuildSupplyChainAction() {
		return new BuildSupplyChainAction();
	}

	public static BuildNextTierAction createBuildNextTierAction() {
		return new BuildNextTierAction();
	}

	public static BuildSupplyChainMenuAction createBuildSupplyChainMenuAction(
			ProductSystemGraphEditor editor) {
		BuildSupplyChainMenuAction action = new BuildSupplyChainMenuAction();
		action.setEditor(editor);
		return action;
	}

	public static ChangeAllStateAction createMinimizeAllAction(
			ProductSystemGraphEditor editor) {
		ChangeAllStateAction action = new ChangeAllStateAction(MINIMIZE);
		action.setEditor(editor);
		return action;
	}

	public static ChangeAllStateAction createMaximizeAllAction(
			ProductSystemGraphEditor editor) {
		ChangeAllStateAction action = new ChangeAllStateAction(MAXIMIZE);
		action.setEditor(editor);
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

	public static HideShowAction createShowAction(
			ProductSystemGraphEditor editor, TreeViewer viewer) {
		HideShowAction action = new HideShowAction(SHOW);
		action.setEditor(editor);
		action.setViewer(viewer);
		return action;
	}

	public static HideShowAction createHideAction(
			ProductSystemGraphEditor editor, TreeViewer viewer) {
		HideShowAction action = new HideShowAction(HIDE);
		action.setEditor(editor);
		action.setViewer(viewer);
		return action;
	}

	public static LayoutMenuAction createLayoutMenuAction(
			ProductSystemGraphEditor editor) {
		LayoutMenuAction action = new LayoutMenuAction();
		action.setEditor(editor);
		return action;
	}

	public static OpenMiniatureViewAction createOpenMiniatureViewAction(
			ProductSystemGraphEditor editor) {
		OpenMiniatureViewAction action = new OpenMiniatureViewAction();
		action.setEditor(editor);
		return action;
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

	public static SaveImageAction createSaveImageAction(
			ProductSystemGraphEditor editor) {
		SaveImageAction action = new SaveImageAction();
		action.setEditor(editor);
		return action;
	}

	public static MarkingAction createMarkAction(ProductSystemGraphEditor editor) {
		MarkingAction action = new MarkingAction(MARK);
		action.setEditor(editor);
		return action;
	}

	public static MarkingAction createUnmarkAction(
			ProductSystemGraphEditor editor) {
		MarkingAction action = new MarkingAction(UNMARK);
		action.setEditor(editor);
		return action;
	}

	public static SearchConnectorsAction createSearchProvidersAction(
			ProductSystemGraphEditor editor) {
		SearchConnectorsAction action = new SearchConnectorsAction(PROVIDER);
		action.setEditor(editor);
		return action;
	}

	public static SearchConnectorsAction createSearchRecipientsAction(
			ProductSystemGraphEditor editor) {
		SearchConnectorsAction action = new SearchConnectorsAction(RECIPIENTS);
		action.setEditor(editor);
		return action;
	}

	public static OpenAction createOpenAction(ProductSystemGraphEditor editor) {
		OpenAction action = new OpenAction();
		action.setEditor(editor);
		return action;
	}

	public static ShowOutlineAction createShowOutlineAction() {
		ShowOutlineAction action = new ShowOutlineAction();
		return action;
	}

}