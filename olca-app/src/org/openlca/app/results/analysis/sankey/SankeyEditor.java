package org.openlca.app.results.analysis.sankey;

import static org.openlca.app.results.analysis.sankey.SankeyConfig.CONFIG_PROP;
import static org.openlca.app.components.graphics.themes.Themes.CONTEXT_SANKEY;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.analysis.sankey.actions.EditSankeyConfigAction;
import org.openlca.app.results.analysis.sankey.actions.LayoutAction;
import org.openlca.app.results.analysis.sankey.actions.OpenEditorAction;
import org.openlca.app.results.analysis.sankey.edit.SankeyEditPartFactory;
import org.openlca.app.results.analysis.sankey.model.Diagram;
import org.openlca.app.results.analysis.sankey.model.SankeyFactory;
import org.openlca.app.components.graphics.actions.SaveImageAction;
import org.openlca.app.components.graphics.frame.GraphicalEditorWithFrame;
import org.openlca.app.components.graphics.frame.Splitter;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Themes;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.RootEntity;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;
import org.openlca.core.results.Sankey;

public class SankeyEditor extends GraphicalEditorWithFrame {

	public static final String ID = "editor.ProductSystemSankeyDiagram";

	public final ResultEditor resultEditor;
	public final DQResult dqResult;
	public final LcaResult result;
	public final ResultItemOrder items;
	public final RootEntity calculationTarget;

	private final SankeyFactory sankeyFactory = new SankeyFactory(this);
	public final SankeyConfig config;
	private Sankey<?> sankey;

	public SankeyEditor(ResultEditor parent) {
		this.resultEditor = parent;
		this.dqResult = parent.dqResult();
		this.result = parent.result();
		this.items = parent.items();
		this.calculationTarget = parent.setup().target();
		this.config = new SankeyConfig(this);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (calculationTarget != null) {
			setPartName(calculationTarget.name);
		}
		super.init(site, input);
	}

	@Override
	public Theme getTheme() {
			if (theme == null) {
				theme = Themes.get(CONTEXT_SANKEY);
			}
			return theme;
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();

		var viewer = getGraphicalViewer();

		ContextMenuProvider provider = new SankeyContextMenuProvider(viewer,
				getActionRegistry());
		viewer.setContextMenu(provider);

		viewer.setEditPartFactory(new SankeyEditPartFactory());
	}

	@Override
	public void createHeader(Splitter parent) {
		var header = new SankeyHeader(parent, SWT.NONE);
		setHeader(header);
		header.setRootEditPart(getRootEditPart());
		header.initialize();
	}

	@Override
	public void setInput(IEditorInput input) {
		super.setInput(input);
		// The model is initialized with an empty diagram so that it is only created
		// when the SankeyDiagram page is open (see ResultEditor).
		var diagram = new Diagram(this, config.orientation());
		setModel(diagram);
		if (getHeader() != null)
			getHeader().setModel(diagram);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void createActions() {
		super.createActions();
		var registry = getActionRegistry();
		var selectionActions = getSelectionActions();
		IAction action;

		action = new OpenEditorAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new EditSankeyConfigAction(this);
		registry.registerAction(action);

		action = new SaveImageAction(this, "sankey.png");
		registry.registerAction(action);

		action = new LayoutAction(this);
		registry.registerAction(action);
	}

	public SankeyFactory getSankeyFactory() {
		return sankeyFactory;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {}

	@Override
	protected void loadConfig() {}

	/**
	 * The <code>selectionChanged</code> method of <code>GraphicalEditor</code> is
	 * overridden due to the fact that this <code>GraphicalEditor</code> us part
	 * of a multipage editor.
	 * @param part      the workbench part containing the selection
	 * @param selection the current selection. This may be <code>null</code> if
	 *                  <code>INullSelectionListener</code> is implemented.
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)	{
		var activeEditor = getActiveEditor();
		if (activeEditor == null)
			return;

		if (activeEditor.equals(this.resultEditor))
			updateActions(getSelectionActions());
	}

	public void onFirstActivation() {
		var diagram = getSankeyFactory().createDiagram();
		setModel(diagram);
		if (getHeader() != null) {
			getHeader().setModel(diagram);
			diagram.firePropertyChange(CONFIG_PROP, null, diagram.getConfig());
		}
		getGraphicalViewer().setContents(diagram);
	}

	public void setSankey(Sankey<?> sankey) {
		this.sankey = sankey;
	}

	public Sankey<?> getSankey() {
		return sankey;
	}

}
