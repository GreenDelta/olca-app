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
import org.eclipse.jface.action.IAction;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;

public class ActionFactory {

	public static IAction buildSupplyChain() {
		return new BuildSupplyChainAction();
	}

	public static IAction buildNextTier() {
		return new BuildNextTierAction();
	}

	public static IAction buildSupplyChainMenu(ProductSystemGraphEditor editor) {
		BuildSupplyChainMenuAction action = new BuildSupplyChainMenuAction();
		action.editor = editor;
		return action;
	}

	public static IAction minimizeAll(ProductSystemGraphEditor editor) {
		ChangeAllStateAction action = new ChangeAllStateAction(MINIMIZE);
		action.editor = editor;
		return action;
	}

	public static IAction maximizeAll(ProductSystemGraphEditor editor) {
		ChangeAllStateAction action = new ChangeAllStateAction(MAXIMIZE);
		action.editor = editor;
		return action;
	}

	public static IAction expandAll(ProductSystemGraphEditor editor) {
		MassExpansionAction action = new MassExpansionAction(EXPAND);
		action.editor = editor;
		return action;
	}

	public static IAction collapseAll(ProductSystemGraphEditor editor) {
		MassExpansionAction action = new MassExpansionAction(COLLAPSE);
		action.editor = editor;
		return action;
	}

	public static IAction show(ProductSystemGraphEditor editor, TreeViewer viewer) {
		return new HideShowAction(editor, viewer, SHOW);
	}

	public static IAction hide(ProductSystemGraphEditor editor, TreeViewer viewer) {
		return new HideShowAction(editor, viewer, HIDE);
	}

	public static IAction layoutMenu(ProductSystemGraphEditor editor) {
		LayoutMenuAction action = new LayoutMenuAction();
		action.setEditor(editor);
		return action;
	}

	public static IAction openMiniatureView(ProductSystemGraphEditor editor) {
		OpenMiniatureViewAction action = new OpenMiniatureViewAction();
		action.editor = editor;
		return action;
	}

	public static IAction removeAllConnections(ProductSystemGraphEditor editor) {
		RemoveAllConnectionsAction action = new RemoveAllConnectionsAction();
		action.editor = editor;
		return action;
	}

	public static IAction removeSupplyChain(ProductSystemGraphEditor editor) {
		RemoveSupplyChainAction action = new RemoveSupplyChainAction();
		action.editor = editor;
		return action;
	}

	public static IAction saveImage(ProductSystemGraphEditor editor) {
		SaveImageAction action = new SaveImageAction();
		action.editor = editor;
		return action;
	}

	public static IAction mark(ProductSystemGraphEditor editor) {
		MarkingAction action = new MarkingAction(MARK);
		action.editor = editor;
		return action;
	}

	public static IAction unmark(ProductSystemGraphEditor editor) {
		MarkingAction action = new MarkingAction(UNMARK);
		action.editor = editor;
		return action;
	}

	public static IAction searchProviders(ProductSystemGraphEditor editor) {
		SearchConnectorsAction action = new SearchConnectorsAction(PROVIDER);
		action.editor = editor;
		return action;
	}

	public static IAction searchRecipients(ProductSystemGraphEditor editor) {
		SearchConnectorsAction action = new SearchConnectorsAction(RECIPIENTS);
		action.editor = editor;
		return action;
	}

	public static IAction open(ProductSystemGraphEditor editor) {
		OpenAction action = new OpenAction();
		action.editor = editor;
		return action;
	}

	public static IAction showOutline() {
		return new ShowOutlineAction();
	}

}