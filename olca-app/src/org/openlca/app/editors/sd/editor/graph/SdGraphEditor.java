package org.openlca.app.editors.sd.editor.graph;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.sd.eqn.Id;
import org.openlca.sd.eqn.Var.Aux;
import org.openlca.sd.eqn.Var.Rate;
import org.openlca.sd.eqn.Var.Stock;
import org.openlca.sd.xmile.view.XmiAuxView;
import org.openlca.sd.xmile.view.XmiFlowView;
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
		var auxViews = new HashMap<Id, XmiAuxView>();
		var flowViews = new HashMap<Id, XmiFlowView>();
		var xmile = parent.xmile();
		if (xmile.model() != null) {
			for (var view : xmile.model().views()) {
				for (var stockView : view.stocks()) {
					stockViews.putIfAbsent(Id.of(stockView.name()), stockView);
				}
				for (var auxView : view.auxiliaries()) {
					auxViews.putIfAbsent(Id.of(auxView.name()), auxView);
				}
				for (var flowView : view.flows()) {
					flowViews.putIfAbsent(Id.of(flowView.name()), flowView);
				}
			}
		}

		int i = 0;
		for (var variable : vars) {
			var m = new VarModel(variable);
			Rectangle b = null;
			if (variable instanceof Stock s) {
				var v = stockViews.get(s.name());
				if (v != null) {
					b = new Rectangle((int) v.x(), (int) v.y(),
							v.width() != null ? v.width().intValue() : 100,
							v.height() != null ? v.height().intValue() : 50);
				}
			} else if (variable instanceof Aux a) {
				var v = auxViews.get(a.name());
				if (v != null) {
					b = new Rectangle((int) v.x(), (int) v.y(), 40, 40);
				}
			} else if (variable instanceof Rate r) {
				var v = flowViews.get(r.name());
				if (v != null) {
					b = new Rectangle((int) v.x(), (int) v.y(), 120, 40);
				}
			}

			if (b != null) {
				m.bounds.setBounds(b);
			} else {
				m.bounds.setBounds(10 + i * 20, 10 + i * 20, 100, 50);
			}
			model.vars.add(m);
			i++;
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

}
