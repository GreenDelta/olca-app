package org.openlca.app.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class ModelSelectionDialog extends FormDialog {

	private final ModelType modelType;
	private boolean multiSelection = false;
	private TreeViewer viewer;
	private Text filterText;
	private BaseDescriptor[] selection;
	private Font boldLabelFont;

	public static BaseDescriptor select(ModelType type) {
		ModelSelectionDialog dialog = new ModelSelectionDialog(UI.shell(), type);
		if (dialog.open() == OK) {
			BaseDescriptor[] selection = dialog.getSelection();
			if (selection == null || selection.length == 0)
				return null;
			return selection[0];
		}
		return null;
	}

	public static BaseDescriptor[] multiSelect(ModelType type) {
		ModelSelectionDialog dialog = new ModelSelectionDialog(UI.shell(), type);
		dialog.multiSelection = true;
		if (dialog.open() == OK)
			return dialog.getSelection();
		return null;
	}

	private ModelSelectionDialog(Shell parentShell, ModelType modelType) {
		super(parentShell);
		this.modelType = modelType;
		setBlockOnOpen(true);
	}

	@Override
	public boolean close() {
		if (boldLabelFont != null && !boldLabelFont.isDisposed())
			boldLabelFont.dispose();
		return super.close();
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		FormToolkit toolkit = form.getToolkit();
		UI.formHeader(form, getTitle());
		Composite body = UI.formBody(form.getForm(), form.getToolkit());
		UI.gridLayout(body, 1);
		Label filterLabel = UI.formLabel(body, form.getToolkit(),
				Messages.Filter);
		boldLabelFont = UI.boldFont(filterLabel);
		filterLabel.setFont(boldLabelFont);
		filterText = UI.formText(body, SWT.SEARCH);
		Section section = UI.section(body, toolkit, Messages.Content);
		addSectionActions(section);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		createViewer(composite);
	}

	private String getTitle() {
		if (modelType == null)
			return "unknown?";
		switch (modelType) {
		case ACTOR:
			return multiSelection ? Messages.Actors : Messages.Actor;
		case FLOW:
			return multiSelection ? Messages.Flows : Messages.Flow;
		case FLOW_PROPERTY:
			return multiSelection ? Messages.FlowProperties
					: Messages.FlowProperty;
		case IMPACT_METHOD:
			return multiSelection ? Messages.ImpactAssessmentMethods
					: Messages.ImpactAssessmentMethod;
		case PROCESS:
			return multiSelection ? Messages.Processes : Messages.Process;
		case PRODUCT_SYSTEM:
			return multiSelection ? Messages.ProductSystems
					: Messages.ProductSystem;
		case PROJECT:
			return multiSelection ? Messages.Projects : Messages.Project;
		case SOURCE:
			return multiSelection ? Messages.Sources : Messages.Source;
		case UNIT_GROUP:
			return multiSelection ? Messages.UnitGroups : Messages.UnitGroup;
		default:
			return "unknown?";
		}
	}

	private void createViewer(Composite composite) {
		if (multiSelection)
			viewer = NavigationTree.forMultiSelection(composite, modelType);
		else
			viewer = NavigationTree.forSingleSelection(composite, modelType);
		ModelTextFilter filter = new ModelTextFilter(filterText, viewer);
		viewer.setFilters(new ViewerFilter[] { filter });
		UI.gridData(viewer.getTree(), true, true);
		viewer.addSelectionChangedListener(new SelectionChangedListener());
		viewer.addDoubleClickListener(new DoubleClickListener());
	}

	@Override
	protected Point getInitialSize() {
		int width = 600;
		int height = 600;
		Rectangle shellBounds = getShell().getDisplay().getBounds();
		int shellWidth = shellBounds.x;
		int shellHeight = shellBounds.y;
		if (shellWidth > 0 && shellWidth < width)
			width = shellWidth;
		if (shellHeight > 0 && shellHeight < height)
			height = shellHeight;
		return new Point(width, height);
	}

	private void addSectionActions(Section section) {
		Action expand = new Action() {
			{
				setImageDescriptor(ImageType.EXPAND_ICON.getDescriptor());
			}

			@Override
			public void run() {
				viewer.expandAll();
			}
		};
		Action collapse = new Action() {
			{
				setImageDescriptor(ImageType.COLLAPSE_ICON.getDescriptor());
			}

			@Override
			public void run() {
				viewer.collapseAll();
			}
		};
		Actions.bind(section, expand, collapse);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point loc = super.getInitialLocation(initialSize);
		int marginTop = (getParentShell().getSize().y - initialSize.y) / 3;
		if (marginTop < 0)
			marginTop = 0;
		return new Point(loc.x, loc.y + marginTop);
	}

	private BaseDescriptor[] getSelection() {
		return selection;
	}

	private boolean matches(ModelElement element) {
		return element.getContent().getName().toLowerCase()
				.contains(filterText.getText().toLowerCase());
	}

	private class SelectionChangedListener implements ISelectionChangedListener {

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection() != null && !event.getSelection().isEmpty()) {
				IStructuredSelection s = (IStructuredSelection) event
						.getSelection();

				List<BaseDescriptor> currentSelection = new ArrayList<>();
				for (Object selected : s.toArray())
					currentSelection
							.addAll(getSelection((INavigationElement<?>) selected));
				selection = currentSelection
						.toArray(new BaseDescriptor[currentSelection.size()]);
			}
			getButton(IDialogConstants.OK_ID).setEnabled(
					selection != null && selection.length > 0);
		}
	}

	private List<BaseDescriptor> getSelection(INavigationElement<?> element) {
		List<BaseDescriptor> currentSelection = new ArrayList<>();
		if (element instanceof ModelElement) {
			ModelElement modelElement = (ModelElement) element;
			if (matches(modelElement))
				currentSelection.add(modelElement.getContent());
		} else
			for (INavigationElement<?> child : element.getChildren())
				currentSelection.addAll(getSelection(child));
		return currentSelection;
	}

	private class DoubleClickListener implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection currentSelection = (IStructuredSelection) event
					.getSelection();
			if (currentSelection.getFirstElement() instanceof ModelElement) {
				ModelElement element = (ModelElement) currentSelection
						.getFirstElement();
				BaseDescriptor modelComponent = element.getContent();
				selection = new BaseDescriptor[] { modelComponent };
				okPressed();
			}
		}
	}

}
