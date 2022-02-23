package org.openlca.app.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Strings;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

public class ModelSelector extends FormDialog {

	/**
	 * Indicates whether multiple elements can be selected in the dialog or not.
	 */
	public boolean forMultiple = false;

	/**
	 * Indicates whether the OK button can be clicked on an empty selection.
	 */
	public boolean isEmptyOk = false;

	/**
	 * The selected elements.
	 */
	private List<RootDescriptor> selection = Collections.emptyList();

	private TreeViewer viewer;
	private Text filterText;
	private ModelFilter modelFilter;
	private final ModelType modelType;

	public ModelSelector(ModelType modelType) {
		super(UI.shell());
		this.modelType = modelType;
		setBlockOnOpen(true);
	}

	public static RootDescriptor select(ModelType type) {
		if (type == null || !type.isRoot())
			return null;
		var dialog = new ModelSelector(type);
		return dialog.open() == OK
			? dialog.first()
			: null;
	}

	public static List<RootDescriptor> multiSelect(ModelType type) {
		if (type == null || !type.isRoot())
			return Collections.emptyList();
		var dialog = new ModelSelector(type);
		dialog.forMultiple = true;
		return dialog.open() == OK
			? dialog.selection
			: Collections.emptyList();
	}

	/**
	 * Returns the first element from the selection or null when the selection is
	 * empty.
	 */
	public RootDescriptor first() {
		if (selection == null || selection.isEmpty())
			return null;
		return selection.get(0);
	}

	public ModelSelector withFilter(Predicate<RootDescriptor> p) {
		this.modelFilter = p != null
			? new ModelFilter(p)
			: null;
		return this;
	}

	public <T> Optional<T> onOk(Function<ModelSelector, T> fn) {
		return open() == OK
			? Optional.of(fn.apply(this))
			: Optional.empty();
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
		return switch (modelType) {
			case ACTOR -> forMultiple ? M.Actors : M.Actor;
			case FLOW -> forMultiple ? M.Flows : M.Flow;
			case FLOW_PROPERTY -> forMultiple ? M.FlowProperties : M.FlowProperty;
			case IMPACT_METHOD -> forMultiple
				? M.ImpactAssessmentMethods
				: M.ImpactAssessmentMethod;
			case PROCESS -> forMultiple ? M.Processes : M.Process;
			case PRODUCT_SYSTEM -> forMultiple ? M.ProductSystems : M.ProductSystem;
			case PROJECT -> forMultiple ? M.Projects : M.Project;
			case SOCIAL_INDICATOR -> forMultiple ? M.SocialIndicators : M.SocialIndicator;
			case SOURCE -> forMultiple ? M.Sources : M.Source;
			case UNIT_GROUP -> forMultiple ? M.UnitGroups : M.UnitGroup;
			case CATEGORY -> forMultiple ? "Categories" : M.Category;
			case CURRENCY -> forMultiple ? M.Currencies : M.Currency;
			case DQ_SYSTEM -> forMultiple ? M.DataQualitySystems : M.DataQualitySystem;
			case IMPACT_CATEGORY -> forMultiple ? M.ImpactCategories : M.ImpactCategory;
			case LOCATION -> forMultiple ? M.Locations : M.Location;
			case PARAMETER -> forMultiple ? M.Parameters : M.Parameter;
			case NW_SET -> forMultiple
				? M.NormalizationWeightingSets
				: M.NormalizationWeighting;
			case UNIT -> forMultiple ? M.Units : M.Unit;
			default -> "unknown?";
		};
	}

	private void createViewer(Composite comp) {
		viewer = forMultiple
			? NavigationTree.forMultiSelection(comp, modelType)
			: NavigationTree.forSingleSelection(comp, modelType);
		var textFilter = new ModelTextFilter(filterText, viewer);
		if (modelFilter != null) {
			viewer.setFilters(modelFilter, textFilter);
		} else {
			viewer.setFilters(textFilter);
		}
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
			List<RootDescriptor> descriptors = new ArrayList<>();
			String filter = filterText.getText().toLowerCase();

			for (Object obj : allSelected) {
				if (!(obj instanceof ModelElement))
					continue;
				RootDescriptor d = ((ModelElement) obj).getContent();
				if (d == null || d.type != modelType)
					continue;

				// make sure that the selected element is visible and not
				// hidden because of the filter
				if (!Strings.isNullOrEmpty(filter)) {
					String label = Labels.name(d);
					if (label == null ||
						!label.toLowerCase().contains(filter))
						continue;
				}

				descriptors.add(d);
			}
			selection = descriptors;
			if (!isEmptyOk) {
				getButton(IDialogConstants.OK_ID).setEnabled(
					selection != null && selection.size() > 0);
			}
		}
	}

	private class DoubleClickListener implements IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent e) {
			Object obj = Viewers.getFirstSelected(viewer);
			if (obj instanceof ModelElement elem) {
				selection = Collections.singletonList(elem.getContent());
				okPressed();
			}
		}
	}

	private static class ModelFilter extends ViewerFilter {

		private final Predicate<RootDescriptor> predicate;

		ModelFilter(Predicate<RootDescriptor> p) {
			this.predicate = Objects.requireNonNull(p);
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object obj) {
			if (!(obj instanceof INavigationElement<?> elem))
				return false;
			if (elem instanceof ModelElement modElem)
				return predicate.test(modElem.getContent());

			// select the element when at least one of its child
			// elements is visible
			for (var child : elem.getChildren()) {
				if (select(viewer, elem, child))
					return true;
			}
			return false;
		}
	}
}
