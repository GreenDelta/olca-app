package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.systems.ProductSystemInfoPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenEditorAction extends SelectionAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final GraphEditor editor;
	/**
	 * The selected on which the action is called.
	 */
	private Object object;

	public OpenEditorAction(GraphEditor part) {
		super(part);
		editor = part;
		setId(ActionIds.OPEN_EDITOR);
		setImageDescriptor(Icon.OPEN_FOLDER.descriptor());
	}

	@Override
	protected boolean calculateEnabled() {
		var objects = getSelectedObjects();
		if (objects.size() != 1)
			return false;
		object = objects.get(0);
		setText(M.OpenInEditor + ": " + getObjectName());

		return ((object instanceof GraphEditPart)
				|| (NodeEditPart.class.isAssignableFrom(object.getClass()))
				|| (object instanceof ExchangeEditPart));
	}

	protected String getObjectName() {
		if (object == null)
			return "";

		if (object instanceof GraphEditPart)
			return M.ProductSystem;
		else if (NodeEditPart.class.isAssignableFrom(object.getClass()))
			return M.Process;
		else if (object instanceof ExchangeEditPart)
			return M.Flow;
		else return "";
	}

	@Override
	public void run() {
		try {
			var b = editor.isDirty();
			if (editor.promptSaveIfNecessary()) {
				if (object instanceof GraphEditPart) {
					var systemEditor = editor.getProductSystemEditor();
					systemEditor.setActivePage(ProductSystemInfoPage.ID);
				}
				if (NodeEditPart.class.isAssignableFrom(object.getClass())) {
					var node = ((NodeEditPart) object).getModel();
					if (b)
						App.close(node.descriptor);
					App.open(node.descriptor);
				}
				if (object instanceof ExchangeEditPart exchangeEditPart) {
					if (b)
						App.close(exchangeEditPart.getModel().exchange.flow);
					App.open(exchangeEditPart.getModel().exchange.flow);
				}
			}
		} catch (Exception e) {
			log.error("Failed to complete product system. ", e);
		}
	}

}
