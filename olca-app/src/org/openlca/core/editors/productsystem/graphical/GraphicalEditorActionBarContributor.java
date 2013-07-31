/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.FeatureFlag;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.core.editors.productsystem.CalculateCostsAction;
import org.openlca.core.editors.productsystem.MatrixExportAction;
import org.openlca.core.editors.productsystem.OpenCalculationWizardAction;
import org.openlca.core.editors.productsystem.ProductSystemEditor;
import org.openlca.core.editors.productsystem.SystemExportAction;
import org.openlca.core.editors.productsystem.graphical.actions.BuildSupplyChainMenuAction;
import org.openlca.core.editors.productsystem.graphical.actions.ExpandAllAction;
import org.openlca.core.editors.productsystem.graphical.actions.FoldAllAction;
import org.openlca.core.editors.productsystem.graphical.actions.LayoutAction;
import org.openlca.core.editors.productsystem.graphical.actions.MaximizeAllProcessesAction;
import org.openlca.core.editors.productsystem.graphical.actions.MinimizeAllProcessesAction;
import org.openlca.core.editors.productsystem.graphical.actions.OpenMiniatureViewAction;
import org.openlca.core.editors.productsystem.graphical.actions.SaveImageAction;
import org.openlca.core.model.ProductSystem;

/**
 * Action bar contributor for the product system editor
 * 
 * @author Sebastian Greve
 * 
 */
public class GraphicalEditorActionBarContributor extends
		EditorActionBarContributor implements IPageChangedListener {

	private ProductSystemGraphEditor activeEditor;
	private BuildSupplyChainMenuAction buildAction = new BuildSupplyChainMenuAction();
	private OpenCalculationWizardAction calculateAction = new OpenCalculationWizardAction();
	private ExpandAllAction expandAllAction = new ExpandAllAction();
	private MatrixExportAction exportMatrixAction = new MatrixExportAction();
	private SystemExportAction exportProductSystemAction = new SystemExportAction();
	private FoldAllAction foldAllAction = new FoldAllAction();
	private CalculateCostsAction costAction = new CalculateCostsAction();

	private Action layoutAction;
	private MaximizeAllProcessesAction maximizeAllProcessesAction = new MaximizeAllProcessesAction();
	private MinimizeAllProcessesAction minimizeAllProcessesAction = new MinimizeAllProcessesAction();
	private SaveImageAction saveImageAction = new SaveImageAction();
	private OpenMiniatureViewAction viewAction = new OpenMiniatureViewAction();

	private void createLayoutAction() {
		layoutAction = new Action(
				Messages.Systems_AppActionBarContributorClass_LayoutActionText,
				IAction.AS_DROP_DOWN_MENU) {

			@Override
			public ImageDescriptor getImageDescriptor() {
				return ImageType.LAYOUT_ICON.getDescriptor();
			}

			@Override
			public void run() {
				// do nothing
			}

		};
		layoutAction.setId("graphLayoutAction");
		layoutAction.setMenuCreator(new LayoutMenuCreator());
	}

	/**
	 * Sets actions enabled/disabled
	 * 
	 * @param enabled
	 *            The new state of the actions
	 */
	private void setActionsEnabled(boolean enabled) {
		maximizeAllProcessesAction.setEnabled(enabled);
		minimizeAllProcessesAction.setEnabled(enabled);
		expandAllAction.setEnabled(enabled);
		foldAllAction.setEnabled(enabled);
		viewAction.setEnabled(enabled);
		saveImageAction.setEnabled(enabled);
		if (layoutAction == null) {
			createLayoutAction();
		}
		layoutAction.setEnabled(enabled);
		buildAction.setEnabled(enabled);
		if (enabled) {
			// update active editor
			buildAction.setProductSystemNode(activeEditor.getModel());
			maximizeAllProcessesAction.setProductSystemEditor(activeEditor);
			minimizeAllProcessesAction.setProductSystemEditor(activeEditor);
			expandAllAction.setModel(activeEditor.getModel());
			foldAllAction.setModel(activeEditor.getModel());
			viewAction.update((Viewport) ((ScalableRootEditPart) activeEditor
					.getGraphicalViewer().getRootEditPart()).getFigure(),
					((ScalableRootEditPart) activeEditor.getGraphicalViewer()
							.getRootEditPart())
							.getLayer(LayerConstants.PRINTABLE_LAYERS),
					activeEditor.getGraphicalViewer().getControl(),
					((ScalableRootEditPart) activeEditor.getGraphicalViewer()
							.getRootEditPart()).getZoomManager());
			saveImageAction.setEditor(activeEditor);
		}
		if (activeEditor != null
				&& activeEditor.getEditor() instanceof ProductSystemEditor) {
			calculateAction.setActiveEditor((ProductSystemEditor) activeEditor
					.getEditor());
			exportMatrixAction.setExportData((ProductSystem) activeEditor
					.getEditor().getModelComponent(), activeEditor
					.getDatabase());
			exportProductSystemAction.setExportData(
					(ProductSystem) activeEditor.getEditor()
							.getModelComponent(), activeEditor.getDatabase());
			costAction.setActiveEditor((ProductSystemEditor) activeEditor
					.getEditor());
		}
	}

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		setActionsEnabled(false);
		toolBarManager.add(maximizeAllProcessesAction);
		toolBarManager.add(minimizeAllProcessesAction);
		toolBarManager.add(expandAllAction);
		toolBarManager.add(foldAllAction);
		toolBarManager.add(viewAction);
		toolBarManager.add(layoutAction);
		toolBarManager.add(buildAction);
		toolBarManager.add(saveImageAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(exportMatrixAction);
		if (FeatureFlag.PRODUCT_SYSTEM_EXPORT.isEnabled())
			toolBarManager.add(exportProductSystemAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(costAction);
		toolBarManager.add(calculateAction);
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		// actions should only be enabled while the product system graph
		// editor is the selected page
		setActionsEnabled(event.getSelectedPage() instanceof ProductSystemGraphEditor);
	}

	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		super.setActiveEditor(targetEditor);
		if (targetEditor instanceof ProductSystemEditor) {
			activeEditor = ((ProductSystemEditor) targetEditor)
					.getGraphEditor();
			((ProductSystemEditor) targetEditor).addPageChangedListener(this);
			setActionsEnabled(((ProductSystemEditor) targetEditor)
					.getSelectedPage() instanceof ProductSystemGraphEditor);
		}
	}

	/**
	 * The menu creator of the layout drop down menu action
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class LayoutMenuCreator implements IMenuCreator {

		/**
		 * The layout action to show in the popup menu
		 */
		private List<LayoutAction> layoutActions = new ArrayList<>();

		/**
		 * Creates the popup menu
		 * 
		 * @param menu
		 *            The parent menu
		 * @return The parent menu
		 */
		private Menu createMenu(Menu menu) {
			for (GraphLayoutType t : GraphLayoutType.values()) {
				final LayoutAction la = new LayoutAction(t);
				la.setNode(activeEditor.getModel());
				layoutActions.add(la);
				MenuItem item = new MenuItem(menu, SWT.NONE);
				item.setText(la.getText());
				item.setImage(la.getImageDescriptor().createImage());
				item.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						// no action on default selection
					}

					@Override
					public void widgetSelected(SelectionEvent e) {
						la.run();
					}

				});
			}

			new MenuItem(menu, SWT.SEPARATOR);

			final MenuItem routeCheck = new MenuItem(menu, SWT.CHECK);
			routeCheck.setText(Messages.Systems_Route);
			routeCheck.setSelection(activeEditor.isRoute());
			routeCheck.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {

				}

				@Override
				public void widgetSelected(SelectionEvent e) {
					activeEditor.setRoute(routeCheck.getSelection());
				}
			});
			return menu;
		}

		@Override
		public void dispose() {
			for (LayoutAction la : layoutActions) {
				la.dispose();
			}
			layoutActions.clear();
		}

		@Override
		public Menu getMenu(Control control) {
			Menu menu = new Menu(control);
			createMenu(menu);
			control.setMenu(menu);
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return createMenu(parent);
		}

	}

}
