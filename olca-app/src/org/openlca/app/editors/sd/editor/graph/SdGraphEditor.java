package org.openlca.app.editors.sd.editor.graph;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Themes;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.sd.eqn.EvaluationOrder;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Stock;
import org.openlca.sd.xmile.view.XmiAuxView;
import org.openlca.sd.xmile.view.XmiFlowView;
import org.openlca.sd.xmile.view.XmiStockView;

public class SdGraphEditor extends GraphicalEditor {

	private final SdModelEditor parent;
	private final Theme theme = Themes.get(Themes.CONTEXT_MODEL);
	private GraphModel model;

	public SdGraphEditor(SdModelEditor parent) {
		this.parent = parent;
	}

	@Override
	public void init(
		IEditorSite site, IEditorInput input
	) throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		super.init(site, input);

		// this sets the parent editor to dirty whenever there was a command
		// executed; when we have non-editing commands later, we may need to
		// filter them here
		getCommandStack().addCommandStackEventListener(e -> parent.setDirty());
	}

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new PartFactory(theme));
		model = new GraphModel();
		populate(model);
		viewer.setContents(model);

		var domain = viewer.getEditDomain();
		if (domain != null) {
			var tool = new PanningSelectionTool();
			domain.setActiveTool(tool);
			domain.setDefaultTool(tool);
		}
	}

	@Override
	protected void configureGraphicalViewer() {
		// it always falls back to the default background color
		// so we need to set it every time it is painted
		var control = getGraphicalViewer().getControl();
		control.setBackground(theme.backgroundColor());
		control.addPaintListener(e -> {
			if (!control.isDisposed()) {
				control.setBackground(theme.backgroundColor());
			}
		});
	}

	private void populate(GraphModel model) {
		var vars = parent.vars();
		if (vars == null)
			return;

		var bounds = new HashMap<Id, Rectangle>();
		var xmile = parent.xmile();
		if (xmile != null && xmile.model() != null) {
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

		var sdModel = parent.model();
		if (sdModel != null) {
			sdModel.positions().forEach((id, r) ->
				bounds.put(id, new Rectangle(r.x(), r.y(), r.width(), r.height())));
		}

		int i = 0;
		var modelMap = new HashMap<Id, VarModel>();
		for (var variable : vars) {
			var m = new VarModel(variable);
			modelMap.put(variable.name(), m);
			var b = bounds.getOrDefault(
				variable.name(), new Rectangle(10 + i * 20, 10 + i * 20, 100, 50));
			m.bounds.setBounds(b);
			model.vars.add(m);
			i++;
		}

		for (var m : model.vars) {
			if (m.variable instanceof Stock stock) {
				for (var flowId : stock.inFlows()) {
					link(modelMap.get(flowId), m, true);
				}
				for (var flowId : stock.outFlows()) {
					link(m, modelMap.get(flowId), true);
				}
			}
			var deps = EvaluationOrder.dependenciesOf(m.variable);
			for (var depId : deps) {
				link(modelMap.get(depId), m, false);
			}
		}
	}

	private void link(VarModel source, VarModel target, boolean isFlow) {
		if (source == null || target == null)
			return;
		var link = new LinkModel(source, target, isFlow);
		source.sourceLinks.add(link);
		target.targetLinks.add(link);
	}

	public void syncTo(SdModel sdModel) {
		if (sdModel == null || model == null)
			return;
		sdModel.positions().clear();
		for (var varModel : model.vars) {
			var b = varModel.bounds;
			sdModel.positions().put(varModel.variable.name(),
				new Rect(b.x, b.y, b.width, b.height));
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getCommandStack().markSaveLocation();
	}

	private Rectangle boundsOf(XmiStockView v) {
		return boundsOf(v.x(), v.y(), v.width(), v.height());
	}

	private Rectangle boundsOf(XmiAuxView v) {
		return boundsOf(v.x(), v.y(), v.width(), v.height());
	}

	private Rectangle boundsOf(XmiFlowView v) {
		return boundsOf(v.x(), v.y(), v.width(), v.height());
	}

	private Rectangle boundsOf(double x, double y, Double w, Double h) {
		int width = w != null ? w.intValue() : 80;
		int height = h != null ? h.intValue() : 45;
		// XMILE coordinates are often centers; GEF expects top-left.
		// We subtract half the size to center the figure on the coordinate.
		return new Rectangle(
			(int) x - width / 2,
			(int) y - height / 2,
			width,
			height);
	}
}
