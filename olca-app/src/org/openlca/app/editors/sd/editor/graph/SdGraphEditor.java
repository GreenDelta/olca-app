package org.openlca.app.editors.sd.editor.graph;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.sd.eqn.Id;
import org.openlca.sd.eqn.Var.Stock;
import org.openlca.sd.xmile.view.XmiStockView;

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

		var stockViews = new HashMap<Id, XmiStockView>();
		var xmile = parent.xmile();
		if (xmile.model() != null) {
			for (var view : xmile.model().views()) {
				for (var stockView : view.stocks()) {
					stockViews.putIfAbsent(Id.of(stockView.name()), stockView);
				}
			}
		}

		int i = 0;
		for (var variable : vars) {
			if (variable instanceof Stock s) {
				var stock = new StockModel(s);
				var view = stockViews.get(s.name());
				if (view != null) {
					int x = (int) view.x();
					int y = (int) view.y();
					int w = view.width() != null ? view.width().intValue() : 100;
					int h = view.height() != null ? view.height().intValue() : 50;
					stock.bounds.setBounds(x, y, w, h);
				} else {
					int pos = 10 + i * 20;
					stock.bounds.setBounds(pos, pos, 100, 50);
				}
				model.stocks.add(stock);
				i++;
			}
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

}
