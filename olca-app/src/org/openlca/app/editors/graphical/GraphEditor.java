package org.openlca.app.editors.graphical;

import static org.openlca.app.components.graphics.themes.Themes.CONTEXT_MODEL;
import static org.openlca.app.editors.graphical.GraphFile.*;
import static org.openlca.app.editors.graphical.actions.MassExpansionAction.*;
import static org.openlca.app.editors.graphical.actions.SearchConnectorsAction.*;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.openlca.app.M;
import org.openlca.app.components.graphics.actions.SaveImageAction;
import org.openlca.app.components.graphics.frame.GraphicalEditorWithFrame;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Themes;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.actions.AddExchangeAction;
import org.openlca.app.editors.graphical.actions.AddProcessAction;
import org.openlca.app.editors.graphical.actions.AddStickyNoteAction;
import org.openlca.app.editors.graphical.actions.BuildNextTierAction;
import org.openlca.app.editors.graphical.actions.BuildSupplyChainAction;
import org.openlca.app.editors.graphical.actions.BuildSupplyChainMenuAction;
import org.openlca.app.editors.graphical.actions.EditExchangeAction;
import org.openlca.app.editors.graphical.actions.EditGraphConfigAction;
import org.openlca.app.editors.graphical.actions.EditModeAction;
import org.openlca.app.editors.graphical.actions.EditStickyNoteAction;
import org.openlca.app.editors.graphical.actions.LayoutAction;
import org.openlca.app.editors.graphical.actions.LinkUpdateAction;
import org.openlca.app.editors.graphical.actions.MassExpansionAction;
import org.openlca.app.editors.graphical.actions.MinMaxAction;
import org.openlca.app.editors.graphical.actions.MinMaxAllAction;
import org.openlca.app.editors.graphical.actions.OpenEditorAction;
import org.openlca.app.editors.graphical.actions.RemoveAllConnectionsAction;
import org.openlca.app.editors.graphical.actions.RemoveSupplyChainAction;
import org.openlca.app.editors.graphical.actions.SearchConnectorsAction;
import org.openlca.app.editors.graphical.actions.SetProcessGroupAction;
import org.openlca.app.editors.graphical.actions.SetReferenceAction;
import org.openlca.app.editors.graphical.actions.ShowElementaryFlowsAction;
import org.openlca.app.editors.graphical.edit.GraphEditPartFactory;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphFactory;
import org.openlca.app.editors.graphical.model.GraphLink;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Version;
import org.openlca.jsonld.Json;

import com.google.gson.JsonArray;

/**
 * A {@link GraphEditor} is the starting point of the graphical interface of a
 * product system. It creates an <code>Editor</code> containing a single
 * <code>GraphicalViewer</code> as its control.
 * The <code>GraphModel</code>  is the head of the model to be further
 * displayed.
 */
public class GraphEditor extends GraphicalEditorWithFrame {

	public static final String ID = "GraphicalEditor";

	private final ProductSystemEditor systemEditor;

	// TODO: save this in the same way as the layout is currently stored
	public final GraphConfig config = new GraphConfig();
	private final GraphFactory graphFactory = new GraphFactory(this);
	private final Set<RootEntity> dirtyEntities = new HashSet<>();

	public GraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput graphInput) {
			if (graphInput.descriptor() != null) {
				setPartName(Labels.name(graphInput.descriptor()));
			}
		}
		super.init(site, input);
	}

	@Override
	public Theme getTheme() {
			if (theme == null) {
				theme = Themes.get(CONTEXT_MODEL);
			}
			return theme;
	}

	@Override
	protected void initializeGraphicalViewer() {
		super.initializeGraphicalViewer();
		GraphDropListener.on(this);
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();

		var viewer = getGraphicalViewer();

		ContextMenuProvider provider = new GraphContextMenuProvider(viewer,
				getActionRegistry());
		viewer.setContextMenu(provider);

		viewer.setEditPartFactory(new GraphEditPartFactory());
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void createActions() {
		super.createActions();
		var registry = getActionRegistry();
		var selectionActions = getSelectionActions();
		var stackActions = getStackActions();
		IAction action;

		action = new LayoutAction(this);
		registry.registerAction(action);

		action = new MinMaxAction(this, MINIMIZE);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new MinMaxAction(this, MAXIMIZE);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new MinMaxAllAction(this, MINIMIZE);
		registry.registerAction(action);
		stackActions.add(action.getId());

		action = new MinMaxAllAction(this, MAXIMIZE);
		registry.registerAction(action);
		stackActions.add(action.getId());

		action = new AddProcessAction(this);
		registry.registerAction(action);

		action = new AddStickyNoteAction(this);
		registry.registerAction(action);

		action = new AddExchangeAction(this, true);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new AddExchangeAction(this, false);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new EditExchangeAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new EditStickyNoteAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new EditGraphConfigAction(this);
		registry.registerAction(action);

		action = new ShowElementaryFlowsAction(this);
		registry.registerAction(action);

		action = new EditModeAction(this);
		registry.registerAction(action);

		action = new OpenEditorAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new SaveImageAction(this, "graph.png");
		registry.registerAction(action);

		action = new MassExpansionAction(this, EXPAND);
		registry.registerAction(action);
		stackActions.add(action.getId());

		action = new MassExpansionAction(this, COLLAPSE);
		registry.registerAction(action);
		stackActions.add(action.getId());

		action = new RemoveAllConnectionsAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new BuildSupplyChainMenuAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new BuildNextTierAction(this);
		registry.registerAction(action);

		action = new BuildSupplyChainAction(this);
		registry.registerAction(action);

		action = new RemoveSupplyChainAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new LinkUpdateAction(this);
		registry.registerAction(action);

		action = new SearchConnectorsAction(this, PROVIDER);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new SearchConnectorsAction(this, RECIPIENTS);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new SetReferenceAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new SetProcessGroupAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());
	}

	@Override
	protected void loadConfig() {
		// read GraphConfig object from file
		var config = GraphFile.getGraphConfig(this);
		if (config != null)
			config.copyTo(this.config);
	}

	/**
	 * The <code>selectionChanged</code> method of <code>GraphicalEditor</code> is
	 * overridden due to the fact that this <code>GraphicalEditor</code> us part
	 * of a multipage editor.
	 *
	 * @param part      the workbench part containing the selection
	 * @param selection the current selection. This may be <code>null</code> if
	 *                  <code>INullSelectionListener</code> is implemented.
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)	{
		var activeEditor = getActiveEditor();
		if (activeEditor == null)
			return;

		if (activeEditor.equals(this.systemEditor))
			updateActions(getSelectionActions());
	}

	@Override
	public Graph getModel() {
		return (Graph) super.getModel();
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		// The model is initialized with an empty graph so that it is only created
		// when the model graph page is open (see ProductSystemEditor).
		setModel(new Graph(this));
	}

	/**
	 * Set the product system editor dirty and add the eventual other dirty
	 * entities.
	 */
	public void setDirty(RootEntity... entities) {
		dirtyEntities.addAll(List.of(entities));
		systemEditor.setDirty(true);
	}

	@Override
	public boolean isDirty() {
		return systemEditor.isDirty();
	}

	public boolean isDirty(RootEntity entity) {
		return dirtyEntities.contains(entity);
	}

	public RootEntity getDirty(long id) {
		return dirtyEntities.stream()
				.filter(e -> e.id == id)
				.findFirst()
				.orElse(null);
	}

	public boolean removeDirty(RootEntity entity) {
		return dirtyEntities.remove(entity);
	}

	public ProductSystem getProductSystem() {
		return systemEditor.getModel();
	}

	public GraphFactory getGraphFactory() {
		return graphFactory;
	}

	/**
	 * Make super.getActionRegistry() public.
	 */
	public ActionRegistry getActionRegistry() {
		return super.getActionRegistry();
	}

	/**
	 * Save the dirty processes collected along the way and the product system.
	 * Compare to another editor, the GraphEditor is not editing a single type of
	 * entity.
	 */
	public void doSave() {
		// Copying the set to avoid concurrent modification.
		var entities = new HashSet<>(dirtyEntities);
		for (var entity : entities) {
			saveEntity(entity);
		}
		dirtyEntities.clear();
	}

	private void saveEntity(RootEntity entity) {
		var node = getModel().getNode(entity.id);
		var type = node.descriptor.type;

		// Map the exchanges of the process with the ProcessLinks.
		var mapPLinkToExchange = new HashMap<ProcessLink, Exchange>();
		node.getAllLinks().stream()
				.map(GraphLink.class::cast)
				.map(graphLink -> graphLink.processLink)
				.forEach(link -> GraphFactory.getConsumers(entity, type).stream()
						.filter(exchange -> exchange.internalId == link.exchangeId
								&& exchange.flow.id == link.flowId)
						.forEach(exchange -> mapPLinkToExchange.put(link, exchange)));

		// Update the entity
		entity.lastChange = Calendar.getInstance().getTimeInMillis();
		Version.incUpdate(entity);
		var db = Database.get();
		db.update(entity);

		var newEntity = db.get(entity.getClass(), entity.id);
		node.setEntity(newEntity);

		var newExchanges = GraphFactory.getExchanges(newEntity, type);
		// Update ProcessLink.exchangeId with the updated exchange.id.
		for (var exchange : newExchanges) {
			// Update the ProcessLink.exchangeId
			for (var entry : mapPLinkToExchange.entrySet()) {
				var oldExchange = entry.getValue();
				if (oldExchange.internalId == exchange.internalId) {
					entry.getKey().exchangeId = exchange.id;
				}
			}

			// Update the ExchangeItem.exchange
			for (var exchangeItem : node.getExchangeItems()) {
				if (exchangeItem.exchange.internalId == exchange.internalId) {
					exchangeItem.setExchange(exchange);
				}
			}
		}
	}

	public boolean promptSaveIfNecessary() throws Exception {
		if (!isDirty())
			return true;
		String question = M.SystemSaveProceedQ;
		if (Question.ask(M.SaveQ, question)) {
			new ProgressMonitorDialog(UI.shell()).run(false, false,
					systemEditor::doSave);
			return true;
		}
		return false;
	}

	public Graph updateModel() {
		systemEditor.updateModel();

		// Create new nodes with the new config.
		var rootObj = GraphFile.createJsonArray(this, getModel());
		var nodeArray = Json.getArray(rootObj, KEY_NODES);
		var stickyNoteArray = Json.getArray(rootObj, KEY_STICKY_NOTES);
		var newGraph = getGraphFactory().createGraph(this, nodeArray,
				stickyNoteArray);

		setModel(newGraph);
		getGraphicalViewer().setContents(newGraph);

		return newGraph;
	}

	public void onFirstActivation() {
		var nodeArray = GraphFile.getLayout(this, KEY_NODES);
		var stickyNoteArray = GraphFile.getLayout(this, KEY_STICKY_NOTES);

		var graph = nodeArray == null || isLayoutTooLarge(nodeArray)
				? getGraphFactory().createGraph(this)
				: getGraphFactory().createGraph(this, nodeArray, stickyNoteArray);

		setModel(graph);
		getGraphicalViewer().setContents(graph);
		getZoomManager().setZoom(config.zoom(), false);
		getZoomManager().getViewport().setViewLocation(config.viewLocation());
	}

	/**
	 * Checks if the layout size is too large to be displayed.
	 * Sometimes, users display too many nodes in the model graph and cannot open
	 * it again as it takes too much time to load. In this case, the user can
	 * reset the layout before opening the model graph.
	 */
	private boolean isLayoutTooLarge(JsonArray array) {
		if (array == null)
			return false;
		if (array.size() < 100)
			return false;
		return !Question.ask(M.RestoreLayout, M.RestoreLayoutQ);
	}

	public ProductSystemEditor getProductSystemEditor() {
		return systemEditor;
	}

}
