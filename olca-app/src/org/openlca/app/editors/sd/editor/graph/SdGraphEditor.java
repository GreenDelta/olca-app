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

		var bounds = new HashMap<Id, Rectangle>();
		var xmile = parent.xmile();
		if (xmile.model() != null) {
			for (var view : xmile.model().views()) {
				for (var v : view.stocks()) {
					bounds.putIfAbsent(Id.of(v.name()), boundsOf(v));
				}
				for (var v : view.auxiliaries()) {
					bounds.putIfAbsent(Id.of(v.name()), boundsOf(v));
				}
				for (var v : view.flows()) {
					bounds.putIfAbsent(Id.of(v.name()), boundsOf(v));
				}
			}
		}

		int i = 0;
		for (var variable : vars) {
			var m = new VarModel(variable);
			var b = bounds.getOrDefault(
				variable.name(), new Rectangle(10 + i * 20, 10 + i * 20, 100, 50));
			m.bounds.setBounds(b);
			model.vars.add(m);
			i++;
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	private Rectangle boundsOf(XmiStockView v) {
		return boundsOf(v.x(), v.y(), v.width(), v.height());
	}

	private Rectangle boundsOf(XmiAuxView v) {
		return boundsOf(v.x(), v.y(), v.width(), v.height());
	}

	private Rectangle boundsOf(XmiFlowView v) {
		return boundsOf(v.x(), v.y(), null, null);
	}

	private Rectangle boundsOf(double x, double y, Double w, Double h) {
		return new Rectangle(
			(int) x,
			(int) y,
			w != null ? w.intValue() : 80,
			h != null ? h.intValue() : 45);
	}
}
