package org.openlca.app.editors.graphical.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.ExchangeEditPart;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.util.Labels;
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
		if (object instanceof GraphEditPart)
			return false;
		setText(M.OpenInEditor + ": " + getObjectName());

		return ((NodeEditPart.class.isAssignableFrom(object.getClass()))
				|| (object instanceof ExchangeEditPart));
	}

	protected String getObjectName() {
		if (object == null)
			return "";

		if (NodeEditPart.class.isAssignableFrom(object.getClass())) {
			var model = ((NodeEditPart) object).getModel();
			return Labels.of(model.descriptor.type);
		}
		else if (object instanceof ExchangeEditPart)
			return M.Flow;
		else return "";
	}

	@Override
	public void run() {
		try {
			var b = editor.isDirty();
			if (editor.promptSaveIfNecessary()) {
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
			log.error("Failed to open the editor. ", e);
		}
	}

}
