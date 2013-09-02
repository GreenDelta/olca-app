package org.openlca.app.editors.graphical.outline;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.EditDomain;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.parts.TreeViewer;
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
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.action.ActionFactory;
import org.openlca.app.editors.graphical.action.HideShowAction;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class OutlinePage extends ContentOutlinePage implements
		PropertyChangeListener {

	private HideShowAction showAction;
	private HideShowAction hideAction;

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
		showAction = ActionFactory.createShowAction(model, getViewer());
		hideAction = ActionFactory.createHideAction(model, getViewer());
	}

	public void setEditDomain(EditDomain editDomain) {
		this.editDomain = editDomain;
	}

	public void setSelectionSynchronizer(
			SelectionSynchronizer selectionSynchronizer) {
		this.selectionSynchronizer = selectionSynchronizer;
	}

	private MenuManager createContextMenu() {
		final MenuManager menuManager = new MenuManager();
		menuManager.add(showAction);
		menuManager.add(hideAction);
		return menuManager;
	}

	@Override
	public void createControl(Composite parent) {
		sash = new SashForm(parent, SWT.VERTICAL);
		new Label(sash, SWT.BORDER_SOLID)
				.setText(Messages.Systems_ProductSystemGraphEditor_FilterLabel);
		searchText = new Text(sash, SWT.BORDER_SOLID);
		searchText.addPaintListener(new SearchPaintListener());
		getViewer().createControl(sash);
		getViewer().setEditDomain(editDomain);
		getViewer().setEditPartFactory(new AppTreeEditPartFactory(model));
		getViewer().setContents(model.getProductSystem());
		getViewer().setContextMenu(createContextMenu());
		selectionSynchronizer.addViewer(getViewer());
		searchText.addModifyListener(new SearchModifyListener());
		model.addPropertyChangeListener(this);
	}

	@Override
	public Control getControl() {
		return sash;
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("processes")
				|| arg0.getPropertyName().equals("processLinks")
				|| arg0.getPropertyName().equals("refreshTree")) {
			getViewer().setEditPartFactory(new AppTreeEditPartFactory(model));
			getViewer().setContents(model.getProductSystem());
			getViewer().getControl().redraw();
			getViewer().getControl().update();
		}
	}

	public void refresh() {
		propertyChange(new PropertyChangeEvent(this, "refreshTree", "not null",
				null));
	}

	private class SearchPaintListener implements PaintListener {

		@Override
		public void paintControl(PaintEvent e) {
			if (sash.getSize().y >= 30) {
				int[] weights = sash.getWeights();
				weights[0] = 18;
				weights[1] = 18;
				weights[2] = sash.getSize().y - 30;
				sash.setWeights(weights);
			}
		}

	}

	private class SearchModifyListener implements ModifyListener {

		@Override
		public void modifyText(ModifyEvent e) {
			List<ProcessTreeEditPart> selection = filterSelection();
			if (selection.size() == 0)
				getViewer().deselectAll();
			else {
				ProcessTreeEditPart[] selectionArray = selection
						.toArray(new ProcessTreeEditPart[selection.size()]);
				StructuredSelection result = new StructuredSelection(
						selectionArray);
				getViewer().setSelection(result);
			}

		}

		private List<ProcessTreeEditPart> filterSelection() {
			List<ProcessTreeEditPart> filtered = new ArrayList<>();
			if (searchText.getText() == null
					|| searchText.getText().length() == 0)
				getViewer().deselectAll();
			else {
				for (Object part : getViewer().getContents().getChildren()) {
					if (part instanceof ProcessTreeEditPart) {
						String name = ((ProcessTreeEditPart) part).getModel()
								.getName().toLowerCase();
						String value = searchText.getText().toLowerCase();
						if (name.contains(value))
							filtered.add((ProcessTreeEditPart) part);
					}
				}
			}
			return filtered;
		}

	}

}
