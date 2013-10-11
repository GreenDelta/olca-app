package org.openlca.app.analysis;

import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.analysis.sankey.SankeyDiagram;
import org.openlca.app.analysis.sankey.SankeySelectionAction;
import org.openlca.app.editors.graphical.action.ActionFactory;
import org.openlca.app.editors.graphical.action.OpenMiniatureViewAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisEditorActions extends EditorActionBarContributor implements
		IPageChangedListener {

	private Logger log = LoggerFactory.getLogger(getClass());
	private static AnalysisEditorActions instance; // TODO: delete!
	private SaveImageAction saveImageAction = new SaveImageAction();
	private final SankeySelectionAction propertySelectionAction = new SankeySelectionAction();
	private final OpenMiniatureViewAction viewAction = ActionFactory
			.createOpenMiniatureViewAction();
	private IToolBarManager toolBarManager;
	private ZoomManager zoomManager;
	private Action[] zoomActions = new Action[2];

	public AnalysisEditorActions() {
		instance = this;
	}

	public static AnalysisEditorActions getInstance() {
		return instance;
	}

	public SankeySelectionAction getPropertySelectionAction() {
		return propertySelectionAction;
	}

	public void updateExportAction() {
		for (final IContributionItem item : toolBarManager.getItems()) {
			item.update();
		}
	}

	@Override
	public void contributeToToolBar(IToolBarManager toolBar) {
		toolBar.add(viewAction);
		toolBar.add(propertySelectionAction);
		toolBar.add(saveImageAction);
		this.toolBarManager = toolBar;
	}

	private void updateZoomItems() {

		if (zoomActions[0] != null && zoomActions[1] != null) {
			for (final IContributionItem item : toolBarManager.getItems()) {
				if (item.getId() != null) {
					if (item.getId().equals(zoomActions[0].getId())
							|| item.getId().equals(zoomActions[1].getId())) {
						toolBarManager.remove(item);
					}
				}
			}
		}

		if (zoomManager != null) {
			final Action zoomIn = new ZoomInAction(zoomManager);
			final Action zoomOut = new ZoomOutAction(zoomManager);
			zoomActions[0] = zoomIn;
			zoomActions[1] = zoomOut;
			toolBarManager.add(zoomIn);
			toolBarManager.add(zoomOut);
		} else {
			zoomActions[0] = null;
			zoomActions[1] = null;
		}
		toolBarManager.update(true);
	}

	@Override
	public void pageChanged(final PageChangedEvent event) {
		boolean enabled = event.getSelectedPage() instanceof SankeyDiagram;
		updateEnabledState(enabled);
	}

	private void updateEnabledState(final boolean enabledState) {
		propertySelectionAction.setEnabled(enabledState);
		saveImageAction.setEnabled(enabledState);
		viewAction.setEnabled(enabledState);
		if (zoomActions[0] != null) {
			boolean zoomEnabled = enabledState && zoomManager != null
					&& zoomManager.getMaxZoom() != zoomManager.getZoom();
			zoomActions[0].setEnabled(zoomEnabled);
		}
		if (zoomActions[1] != null) {
			boolean zoomEnabled = enabledState && zoomManager != null
					&& zoomManager.getMinZoom() != zoomManager.getZoom();
			zoomActions[1].setEnabled(zoomEnabled);
		}
	}

	@Override
	public void setActiveEditor(final IEditorPart targetEditor) {
		super.setActiveEditor(targetEditor);
		log.trace("activate editor: {}", targetEditor);

		if (targetEditor instanceof AnalyzeEditor) {
			final AnalyzeEditor editor = (AnalyzeEditor) targetEditor;
			final IFormPage page = editor.getActivePageInstance();
			if (page != null) {
				page.setActive(true);
			}
			editor.addPageChangedListener(this);
			boolean enabled = editor.getActivePageInstance() == null;
			propertySelectionAction.setSankeyDiagram(editor.getDiagram());
			saveImageAction.setSankeyDiagram(editor.getDiagram());
			zoomManager = ((ScalableRootEditPart) editor.getDiagram()
					.getGraphicalViewer().getRootEditPart()).getZoomManager();
			viewAction.update((Viewport) ((ScalableRootEditPart) editor
					.getDiagram().getGraphicalViewer().getRootEditPart())
					.getFigure(), ((ScalableRootEditPart) editor.getDiagram()
					.getGraphicalViewer().getRootEditPart())
					.getLayer(LayerConstants.PRINTABLE_LAYERS), editor
					.getDiagram().getGraphicalViewer().getControl(),
					((ScalableRootEditPart) editor.getDiagram()
							.getGraphicalViewer().getRootEditPart())
							.getZoomManager());
			updateZoomItems();
			updateEnabledState(enabled);
		} else {
			updateZoomItems();
			updateEnabledState(false);
			zoomManager = null;
		}
	}
}
