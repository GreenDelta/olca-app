package org.openlca.app.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import org.openlca.app.M;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class ModelSelectionDialog extends FormDialog {

	private final ModelType modelType;
	private boolean multiSelection = false;
	private TreeViewer viewer;
	private Text filterText;
	private CategorizedDescriptor[] selection;

	public static CategorizedDescriptor select(ModelType type) {
		if (type == null || !type.isCategorized())
			return null;
		ModelSelectionDialog dialog = new ModelSelectionDialog(UI.shell(), type);
		if (dialog.open() == OK) {
			CategorizedDescriptor[] selection = dialog.getSelection();
			if (selection == null || selection.length == 0)
				return null;
			return selection[0];
		}
		return null;
	}

	public static CategorizedDescriptor[] multiSelect(ModelType type) {
		if (type == null || !type.isCategorized())
			return null;
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
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		FormToolkit tk = form.getToolkit();
		UI.formHeader(form, getTitle());
		Composite body = UI.formBody(form.getForm(), tk);
		UI.gridLayout(body, 1);
		Label filterLabel = UI.formLabel(body, form.getToolkit(), M.Filter);
		filterLabel.setFont(UI.boldFont());
		filterText = UI.formText(body, SWT.SEARCH);
		Section section = UI.section(body, tk, M.Content);
		addSectionActions(section);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, tk, 1);
		createViewer(composite);
	}

	private String getTitle() {
		if (modelType == null)
			return "unknown?";
		switch (modelType) {
		case ACTOR:
			return multiSelection ? M.Actors : M.Actor;
		case FLOW:
			return multiSelection ? M.Flows : M.Flow;
		case FLOW_PROPERTY:
			return multiSelection ? M.FlowProperties : M.FlowProperty;
		case IMPACT_METHOD:
			return multiSelection ? M.ImpactAssessmentMethods : M.ImpactAssessmentMethod;
		case PROCESS:
			return multiSelection ? M.Processes : M.Process;
		case PRODUCT_SYSTEM:
			return multiSelection ? M.ProductSystems : M.ProductSystem;
		case PROJECT:
			return multiSelection ? M.Projects : M.Project;
		case SOCIAL_INDICATOR:
			return multiSelection ? M.SocialIndicators : M.SocialIndicator;
		case SOURCE:
			return multiSelection ? M.Sources : M.Source;
		case UNIT_GROUP:
			return multiSelection ? M.UnitGroups : M.UnitGroup;
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
		Action expand = Actions.create(
				M.ExpandAll,
				Icon.EXPAND.descriptor(),
				() -> viewer.expandAll());
		Action collapse = Actions.create(
				M.CollapseAll,
				Icon.COLLAPSE.descriptor(),
				() -> viewer.collapseAll());
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

	private CategorizedDescriptor[] getSelection() {
		return selection;
	}

	private class SelectionChangedListener
			implements ISelectionChangedListener {

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection() != null && !event.getSelection().isEmpty()) {
				IStructuredSelection s = (IStructuredSelection) event.getSelection();
				List<INavigationElement<?>> elements = new ArrayList<>();
				for (Object selected : s.toArray())
					elements.add((INavigationElement<?>) selected);
				Set<CategorizedDescriptor> descriptors = Navigator.collect(elements, this::unwrap);
				selection = descriptors.toArray(new CategorizedDescriptor[descriptors.size()]);
			}
			getButton(IDialogConstants.OK_ID).setEnabled(selection != null && selection.length > 0);
		}

		private CategorizedDescriptor unwrap(INavigationElement<?> element) {
			if (!(element instanceof ModelElement))
				return null;
			ModelElement modelElement = (ModelElement) element;
			if (!matches(modelElement))
				return null;
			return modelElement.getContent();
		}

		private boolean matches(ModelElement elem) {
			return elem.getContent().name.toLowerCase()
					.contains(filterText.getText().toLowerCase());
		}
	}

	private class DoubleClickListener implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection currentSelection = (IStructuredSelection) event
					.getSelection();
			if (currentSelection.getFirstElement() instanceof ModelElement) {
				ModelElement element = (ModelElement) currentSelection.getFirstElement();
				CategorizedDescriptor modelComponent = element.getContent();
				selection = new CategorizedDescriptor[] { modelComponent };
				okPressed();
			}
		}
	}

}
