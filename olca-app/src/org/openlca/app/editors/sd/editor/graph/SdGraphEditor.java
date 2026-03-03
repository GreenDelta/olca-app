package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Themes;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.editors.sd.editor.graph.actions.AddRateAction;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;

public class SdGraphEditor extends GraphicalEditor {

	private final SdModelEditor parent;
	private final Theme theme = Themes.get(Themes.CONTEXT_MODEL);
	private SdGraph graph;

	public SdGraphEditor(SdModelEditor parent) {
		this.parent = parent;
	}

	public SdModelEditor parent() {
		return parent;
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
		graph = SdGraph.of(parent.model());
		viewer.setContents(graph);

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
		var viewer = getGraphicalViewer();
		var control = viewer.getControl();
		control.setBackground(theme.backgroundColor());
		control.addPaintListener(e -> {
			if (!control.isDisposed()) {
				control.setBackground(theme.backgroundColor());
			}
		});

		var menu = new ContextMenu(viewer, getActionRegistry());
		viewer.setContextMenu(menu);
	}

	@Override
	protected void createActions() {
		super.createActions();
		var registry = getActionRegistry();
		registry.registerAction(new AddRateAction(this));
	}

	public SdGraph graph() {
		return graph;
	}

	public Point getCursorLocation() {
		var loc = Display.getCurrent().getCursorLocation();
		return getGraphicalViewer().getControl().toControl(loc);
	}

	public void exec(Command cmd) {
		getCommandStack().execute(cmd);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getCommandStack().markSaveLocation();
	}
}
