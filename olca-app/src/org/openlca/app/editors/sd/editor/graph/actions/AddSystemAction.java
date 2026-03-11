package org.openlca.app.editors.sd.editor.graph.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.graphics.Point;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.sd.model.EntityRef;
import org.openlca.sd.model.SystemBinding;

public class AddSystemAction extends WorkbenchPartAction {

	public static final String ID = "ADD-SYSTEM-ACTION";
	private final SdGraphEditor editor;
	private Point location = new Point(250, 250);

	public AddSystemAction(SdGraphEditor editor) {
		super(editor);
		this.editor = editor;
		setId(ID);
		setText("Add product system");
	}

	@Override
	protected boolean calculateEnabled() {
		location = editor.getCursorLocation();
		return true;
	}

	@Override
	public void run() {
		var d = ModelSelector.select(ModelType.PRODUCT_SYSTEM);
		if (d == null)
			return;
		var db = Database.get();
		if (db == null)
			return;
		var sys = db.get(ProductSystem.class, d.id);
		if (sys == null)
			return;
		var binding = new SystemBinding(EntityRef.of(sys));
		binding.setAmount(sys.targetAmount);
		SystemEditDialog.create(editor, binding, location);
	}
}
