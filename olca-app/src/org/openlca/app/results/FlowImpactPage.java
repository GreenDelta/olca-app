package org.openlca.app.results;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.TableColumnSorter;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;

/**
 * Shows the contributions of flows to an LCIA result.
 */
public class FlowImpactPage extends FormPage {

	private final static String[] COLUMN_LABELS = { Messages.Contribution,
			Messages.Flow, Messages.Category, Messages.SubCategory,
			Messages.Amount, Messages.Unit };

	private ContributionResultProvider<?> result;
	private ImpactCategoryViewer impactCombo;
	private TableViewer table;
	private Spinner spinner;
	private double cutOff = 1;

	public FlowImpactPage(FormEditor editor,
			ContributionResultProvider<?> result) {
		super(editor, "FlowImpactPage", Messages.FlowContributions);
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.FlowContributions);
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		makeSelectionElements(toolkit, composite);
		makeTable(composite);
		form.reflow(true);
		impactCombo.selectFirst();
	}

	private void makeSelectionElements(FormToolkit toolkit, Composite parent) {
		Composite composite = toolkit.createComposite(parent);
		UI.gridData(composite, true, false);
		UI.gridLayout(composite, 5);
		UI.formLabel(composite, toolkit, Messages.ImpactCategory);
		createImpactViewer(composite);
		UI.formLabel(composite, toolkit, Messages.Cutoff);
		spinner = new Spinner(composite, SWT.BORDER);
		spinner.setValues(1, 0, 100, 0, 1, 10);
		toolkit.adapt(spinner);
		toolkit.createLabel(composite, "%");
		Controls.onSelect(spinner, (e) -> {
			cutOff = spinner.getSelection();
			table.refresh();
		});
	}

	private void createImpactViewer(Composite parent) {
		impactCombo = new ImpactCategoryViewer(parent);
		impactCombo.setInput(result.getImpactDescriptors());
		impactCombo.addSelectionChangedListener((impact) -> {
			ContributionSet<FlowDescriptor> contributions = result
					.getFlowContributions(impact);
			List<ContributionItem<FlowDescriptor>> items = contributions
					.getContributions();
			Contributions.sortDescending(items);
			table.setInput(items);
		});
	}

	private void makeTable(Composite parent) {
		table = Tables.createViewer(parent, COLUMN_LABELS);
		Label label = new Label();
		table.setLabelProvider(label);
		table.setFilters(new ViewerFilter[] { new CutOffFilter() });
		Tables.bindColumnWidths(table.getTable(), new double[] { 0.1, 0.3, 0.2,
				0.2, 0.1, 0.1 });
		Actions.bind(table, TableClipboard.onCopy(table));
		Tables.registerSorters(table,
				ContributionSorter.forShare(0),
				new TableColumnSorter<>(ContributionItem.class, 1, label),
				new TableColumnSorter<>(ContributionItem.class, 2, label),
				new TableColumnSorter<>(ContributionItem.class, 3, label),
				ContributionSorter.forAmount(4),
				new TableColumnSorter<>(ContributionItem.class, 5, label));
	}

	private class Label extends BaseLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		@SuppressWarnings("unchecked")
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			if (columnIndex != 0)
				return null;
			ContributionItem<FlowDescriptor> contribution = ContributionItem.class
					.cast(element);
			return image.getForTable(contribution.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			ContributionItem<FlowDescriptor> contribution = ContributionItem.class
					.cast(element);
			FlowDescriptor flow = contribution.getItem();
			Pair<String, String> category = Labels.getFlowCategory(flow,
					Cache.getEntityCache());
			switch (columnIndex) {
			case 0:
				return Numbers.percent(contribution.getShare());
			case 1:
				return Labels.getDisplayName(flow);
			case 2:
				return category.getLeft();
			case 3:
				return category.getRight();
			case 4:
				return Numbers.format(contribution.getAmount());
			case 5:
				return impactCombo.getSelected().getReferenceUnit();
			default:
				return null;
			}
		}

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}
	}

	private class CutOffFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof ContributionItem))
				return false;
			ContributionItem<?> item = (ContributionItem<?>) element;
			if ((item.getShare() * 100) < cutOff)
				return false;
			return true;
		}
	}

}
