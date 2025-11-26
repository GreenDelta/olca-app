package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.components.graphics.BasicGraphicalEditor;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.editors.sd.editor.graph.actions.AddAuxiliaryAction;
import org.openlca.app.editors.sd.editor.graph.actions.AddRateAction;
import org.openlca.app.editors.sd.editor.graph.actions.AddStockAction;
import org.openlca.app.editors.sd.editor.graph.edit.SdEditPartFactory;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;

/**
 * Graphical editor for system dynamics models.
 * Allows editing stocks, rates, auxiliaries and their connections.
 */
public class SdGraphEditor extends BasicGraphicalEditor {

	public static final String ID = "SdGraphEditor";

	private SdModelEditor modelEditor;
	private SdGraph graph;

	public SdGraphEditor(SdModelEditor modelEditor) {
		this.modelEditor = modelEditor;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		super.init(site, input);
		if (input instanceof SdGraphEditorInput graphInput) {
			setPartName(graphInput.getName());
		}
	}

	@Override
	public Theme getTheme() {
		if (theme == null) {
			// TODO: load theme
			// theme = Themes.get(ID).getDefault();
		}
		return theme;
	}

	@Override
	protected void loadConfig() {
		// TODO: Load graph configuration from file if needed
	}

	@Override
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		var viewer = getGraphicalViewer();
		viewer.setContents(getModel());
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		var viewer = getGraphicalViewer();

		viewer.setEditPartFactory(new SdEditPartFactory());

		// Set up context menu
		var menuProvider = new SdContextMenuProvider(viewer, getActionRegistry());
		viewer.setContextMenu(menuProvider);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void createActions() {
		super.createActions();
		var registry = getActionRegistry();

		IAction action;

		action = new AddStockAction(this);
		registry.registerAction(action);

		action = new AddRateAction(this);
		registry.registerAction(action);

		action = new AddAuxiliaryAction(this);
		registry.registerAction(action);
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		// Create an empty graph - will be populated when activated
		graph = new SdGraph(this);
		setModel(graph);
	}

	@Override
	public SdGraph getModel() {
		return graph;
	}

	public void setModel(SdGraph graph) {
		this.graph = graph;
		super.setModel(graph);
	}

	public SdModelEditor getModelEditor() {
		return modelEditor;
	}

	/**
	 * Make super.getActionRegistry() public.
	 */
	@Override
	public ActionRegistry getActionRegistry() {
		return super.getActionRegistry();
	}

	/**
	 * Called when the graph page is first activated.
	 * Loads the graph from the model.
	 */
	public void onFirstActivation() {
		// TODO: Initialize the graph from the actual SD model (XMILE)
		// For now, create an empty graph
		graph = new SdGraph(this);
		setModel(graph);
		getGraphicalViewer().setContents(graph);

		// TODO: Load nodes from model variables
		// var vars = modelEditor.vars();
		// for (var v : vars) {
		//     var nodeType = determineNodeType(v);
		//     var node = new SdNode(nodeType, v.name().label());
		//     graph.addNode(node);
		// }
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// Handle selection changes
		updateActions(getSelectionActions());
	}

	@Override
	public boolean isDirty() {
		return modelEditor.isDirty();
	}

	public void setDirty() {
		modelEditor.setDirty();
	}

	/**
	 * Context menu provider for the SD graph editor.
	 */
	private static class SdContextMenuProvider extends ContextMenuProvider {

		private final ActionRegistry registry;

		public SdContextMenuProvider(org.eclipse.gef.EditPartViewer viewer,
				ActionRegistry registry) {
			super(viewer);
			this.registry = registry;
		}

		@Override
		public void buildContextMenu(IMenuManager menu) {
			// Add node creation actions
			menu.add(registry.getAction(AddStockAction.ID));
			menu.add(registry.getAction(AddRateAction.ID));
			menu.add(registry.getAction(AddAuxiliaryAction.ID));

			menu.add(new Separator());

			// Add standard edit actions
			var deleteAction = registry.getAction(ActionFactory.DELETE.getId());
			if (deleteAction != null) {
				menu.add(deleteAction);
			}
		}
	}
}
