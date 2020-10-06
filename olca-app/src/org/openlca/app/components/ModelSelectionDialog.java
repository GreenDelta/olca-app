package org.openlca.app.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

import com.google.common.base.Strings;

public class ModelSelectionDialog extends FormDialog {

	/**
	 * Indicates whether multiple elements can be selected in the dialog or not.
	 */
	public boolean forMultiple = false;

	/**
	 * Indicates whether the OK button can be clicked on an empty selection.
	 */
	public boolean isEmptyOk = false;

	/**
	 * The selected elements. Note that this array can be null when nothing was
	 * selected.
	 */
	public CategorizedDescriptor[] selection;

	private final ModelType modelType;
	private TreeViewer viewer;
	private Text filterText;

	public ModelSelectionDialog(ModelType modelType) {
		super(UI.shell());
		this.modelType = modelType;
		setBlockOnOpen(true);
	}

	public static CategorizedDescriptor select(ModelType type) {
		if (type == null || !type.isCategorized())
			return null;
		ModelSelectionDialog d = new ModelSelectionDialog(type);
		return d.open() == OK
				? d.first()
				: null;
	}

	public static CategorizedDescriptor[] multiSelect(ModelType type) {
		if (type == null || !type.isCategorized())
			return null;
		ModelSelectionDialog d = new ModelSelectionDialog(type);
		d.forMultiple = true;
		if (d.open() == OK)
			return d.selection;
		return null;
	}

	/**
	 * Returns the first element from the selection or null when the selection is
	 * empty.
	 */
	public CategorizedDescriptor first() {
		if (selection == null || selection.length == 0)
			return null;
		return selection[0];
	}

	@Override
	protected void createButtonsForButtonBar(Composite comp) {
		createButton(comp, IDialogConstants.OK_ID, M.OK, false);
		getButton(IDialogConstants.OK_ID).setEnabled(isEmptyOk);
		createButton(comp, IDialogConstants.CANCEL_ID, M.Cancel, true);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		FormToolkit tk = form.getToolkit();
		UI.formHeader(form, getTitle());
		Composite body = UI.formBody(form.getForm(), tk);
		UI.gridLayout(body, 1);
		Label filterLabel = UI.formLabel(body, tk, M.Filter);
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
			return forMultiple ? M.Actors : M.Actor;
		case FLOW:
			return forMultiple ? M.Flows : M.Flow;
		case FLOW_PROPERTY:
			return forMultiple ? M.FlowProperties : M.FlowProperty;
		case IMPACT_METHOD:
			return forMultiple
					? M.ImpactAssessmentMethods
					: M.ImpactAssessmentMethod;
		case PROCESS:
			return forMultiple ? M.Processes : M.Process;
		case PRODUCT_SYSTEM:
			return forMultiple ? M.ProductSystems : M.ProductSystem;
		case PROJECT:
			return forMultiple ? M.Projects : M.Project;
		case SOCIAL_INDICATOR:
			return forMultiple ? M.SocialIndicators : M.SocialIndicator;
		case SOURCE:
			return forMultiple ? M.Sources : M.Source;
		case UNIT_GROUP:
			return forMultiple ? M.UnitGroups : M.UnitGroup;
		case CATEGORY:
			return forMultiple ? "Categories" : M.Category;
		case CURRENCY:
			return forMultiple ? M.Currencies : M.Currency;
		case DQ_SYSTEM:
			return forMultiple ? M.DataQualitySystems : M.DataQualitySystem;
		case IMPACT_CATEGORY:
			return forMultiple ? M.ImpactCategories : M.ImpactCategory;
		case LOCATION:
			return forMultiple ? M.Locations : M.Location;
		case PARAMETER:
			return forMultiple ? M.Parameters : M.Parameter;
		case NW_SET:
			return forMultiple
					? M.NormalizationWeightingSets
					: M.NormalizationWeighting;
		case UNIT:
			return forMultiple ? M.Units : M.Unit;
		default:
			return "unknown?";
		}
	}

	private void createViewer(Composite comp) {
		viewer = forMultiple
				? NavigationTree.forMultiSelection(comp, modelType)
				: NavigationTree.forSingleSelection(comp, modelType);
		ModelTextFilter filter = new ModelTextFilter(filterText, viewer);
		viewer.setFilters(filter);
		UI.gridData(viewer.getTree(), true, true);
		viewer.addSelectionChangedListener(new SelectionChangedListener());
		viewer.addDoubleClickListener(new DoubleClickListener());
	}

	@Override
	protected Point getInitialSize() {
		var shell = getShell().getDisplay().getBounds();
		int width = shell.x > 0 && shell.x < 600
				? shell.x
				: 600;
		int height = shell.y > 0 && shell.y < 600
				? shell.y
				: 600;
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
	protected Point getInitialLocation(Point size) {
		Point loc = super.getInitialLocation(size);
		int marginTop = (getParentShell().getSize().y - size.y) / 3;
		if (marginTop < 0) {
			marginTop = 0;
		}
		return new Point(loc.x, loc.y + marginTop);
	}

	private class SelectionChangedListener
			implements ISelectionChangedListener {

		@Override
		public void selectionChanged(SelectionChangedEvent e) {

			List<Object> allSelected = Viewers.getAllSelected(viewer);
			List<CategorizedDescriptor> descriptors = new ArrayList<>();
			String filter = filterText.getText().toLowerCase();

			for (Object obj : allSelected) {
				if (!(obj instanceof ModelElement))
					continue;
				CategorizedDescriptor d = ((ModelElement) obj).getContent();
				if (d == null || d.type != modelType)
					continue;

				// make sure that the selected element
				// is visible and not hidden because of
				// the filter
				if (!Strings.isNullOrEmpty(filter)) {
					String label = Labels.name(d);
					if (label == null ||
							!label.toLowerCase().contains(filter))
						continue;
				}

				descriptors.add(d);
			}
			selection = descriptors.toArray(new CategorizedDescriptor[0]);
			if (!isEmptyOk) {
				getButton(IDialogConstants.OK_ID).setEnabled(
						selection != null && selection.length > 0);
			}
		}
	}

	private class DoubleClickListener implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent e) {
			Object obj = Viewers.getFirstSelected(viewer);
			if (obj instanceof ModelElement) {
				ModelElement elem = (ModelElement) obj;
				selection = new CategorizedDescriptor[] {
						elem.getContent()
				};
				okPressed();
			}
		}
	}

}
