package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Themes;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.editors.sd.editor.graph.actions.AddVarAction;
import org.openlca.app.editors.sd.editor.graph.edit.PartFactory;
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
		for (var a : AddVarAction.allFor(this)) {
			registry.registerAction(a);
		}
	}

	/// We need to overwrite this otherwise the selection actions are not updated.
	/// The implementation of the super class does not handle graphical editors
	/// that are in a page of a parent editor.
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (parent.equals(part)) {
			updateActions(getSelectionActions());
		}
	}

	public SdGraph graph() {
		return graph;
	}

	public Point getCursorLocation() {
		var display = Display.getCurrent();
		if (display == null) return new Point(150, 150);
		return getGraphicalViewer()
			.getControl()
			.toControl(display.getCursorLocation());
	}

	public void exec(Command cmd) {
		getCommandStack().execute(cmd);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getCommandStack().markSaveLocation();
	}
}
