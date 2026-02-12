package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.sd.editor.SdModelEditor;

public class SdGraphEditor extends GraphicalEditor {

	private final SdModelEditor parent;

	public SdGraphEditor(SdModelEditor parent) {
		this.parent = parent;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		super.init(site, input);
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
	}

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new PartFactory());
		var model = new GraphModel();
		populate(model);
		viewer.setContents(model);
	}

	private void populate(GraphModel model) {
		var vars = parent.vars();
		if (vars == null)
			return;
		for (var variable : vars) {
			if (variable instanceof org.openlca.sd.eqn.Var.Stock stock) {
				model.stocks.add(new StockModel(stock));
			}
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

}
