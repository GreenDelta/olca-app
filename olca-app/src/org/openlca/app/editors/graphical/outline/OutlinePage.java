package org.openlca.app.editors.graphical.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.EditDomain;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.action.ActionFactory;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class OutlinePage extends ContentOutlinePage {

	private IAction showAction;
	private IAction hideAction;

	private ProductSystemNode model;
	private EditDomain editDomain;
	private SelectionSynchronizer selectionSynchronizer;
	private SashForm sash;
	private Text searchText;

	public OutlinePage(ProductSystemNode model) {
		super(new TreeViewer());
		this.model = model;
		initializeActions();
	}

	@Override
	protected TreeViewer getViewer() {
		return (TreeViewer) super.getViewer();
	}

	private void initializeActions() {
		showAction = ActionFactory.show(model.editor, getViewer());
		hideAction = ActionFactory.hide(model.editor, getViewer());
	}

	public void setEditDomain(EditDomain editDomain) {
		this.editDomain = editDomain;
	}

	public void setSelectionSynchronizer(SelectionSynchronizer selectionSynchronizer) {
		this.selectionSynchronizer = selectionSynchronizer;
	}

	private MenuManager createContextMenu() {
		MenuManager menuManager = new MenuManager();
		menuManager.add(showAction);
		menuManager.add(hideAction);
		return menuManager;
	}

	@Override
	public void createControl(Composite parent) {
		sash = new SashForm(parent, SWT.VERTICAL);
		new Label(sash, SWT.BORDER_SOLID).setText(M.FilterByName);
		searchText = new Text(sash, SWT.BORDER_SOLID);
		searchText.addPaintListener(new SearchPaintListener());
		getViewer().createControl(sash);
		getViewer().setEditDomain(editDomain);
		getViewer().setEditPartFactory(new AppTreeEditPartFactory(model));
		getViewer().setContents(model.getProductSystem());
		getViewer().setContextMenu(createContextMenu());
		selectionSynchronizer.addViewer(getViewer());
		searchText.addModifyListener(new SearchModifyListener());
	}

	@Override
	public Control getControl() {
		return sash;
	}

	public void refresh() {
		getViewer().setEditPartFactory(new AppTreeEditPartFactory(model));
		getViewer().setContents(model.getProductSystem());
		getViewer().getControl().redraw();
		getViewer().getControl().update();
	}

	private class SearchPaintListener implements PaintListener {

		@Override
		public void paintControl(PaintEvent e) {
			if (sash.getSize().y < 30)
				return;
			int[] weights = sash.getWeights();
			weights[0] = 18;
			weights[1] = 18;
			weights[2] = sash.getSize().y - 30;
			sash.setWeights(weights);
		}

	}

	private class SearchModifyListener implements ModifyListener {

		@Override
		public void modifyText(ModifyEvent e) {
			List<ProcessTreeEditPart> selection = filterSelection();
			if (selection.size() == 0) {
				getViewer().deselectAll();
				return;
			}
			ProcessTreeEditPart[] selectionArray = selection.toArray(new ProcessTreeEditPart[selection.size()]);
			StructuredSelection result = new StructuredSelection(selectionArray);
			getViewer().setSelection(result);
		}

		private List<ProcessTreeEditPart> filterSelection() {
			if (searchText.getText() == null || searchText.getText().length() == 0) {
				getViewer().deselectAll();
				return Collections.emptyList();
			}
			List<ProcessTreeEditPart> filtered = new ArrayList<>();
			for (Object part : getViewer().getContents().getChildren()) {
				if (!(part instanceof ProcessTreeEditPart))
					continue;
				String name = ((ProcessTreeEditPart) part).getModel().name.toLowerCase();
				String value = searchText.getText().toLowerCase();
				if (!name.contains(value))
					continue;
				filtered.add((ProcessTreeEditPart) part);
			}
			return filtered;
		}

	}

}