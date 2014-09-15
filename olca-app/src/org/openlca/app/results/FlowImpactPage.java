package org.openlca.app.results;

import java.util.List;

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
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
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
			Messages.Flow, Messages.Amount, Messages.Unit };

	private ContributionResultProvider<?> result;
	private ImpactCategoryViewer impactCombo;
	private TableViewer flowViewer;
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

		Composite selectionContainer = toolkit.createComposite(composite);
		UI.gridData(selectionContainer, true, false);
		UI.gridLayout(selectionContainer, 5);
		UI.formLabel(selectionContainer, toolkit, Messages.ImpactCategory);
		impactCombo = new ImpactCategoryViewer(selectionContainer);
		impactCombo.setInput(result.getImpactDescriptors());
		impactCombo.addSelectionChangedListener((impact) -> {
			ContributionSet<FlowDescriptor> contributions = result
					.getFlowContributions(impact);
			List<ContributionItem<FlowDescriptor>> items = contributions
					.getContributions();
			Contributions.sortDescending(items);
			flowViewer.setInput(items);
		});
		UI.formLabel(selectionContainer, toolkit, Messages.Cutoff);
		spinner = new Spinner(selectionContainer, SWT.BORDER);
		spinner.setValues(1, 0, 10000, 2, 1, 100);
		toolkit.adapt(spinner);
		toolkit.createLabel(selectionContainer, "%");
		Controls.onSelect(spinner, (e) -> {
			cutOff = spinner.getSelection();
			flowViewer.refresh();
		});

		createFlowViewer(composite);

		form.reflow(true);

		impactCombo.selectFirst();
	}

	private void createFlowViewer(Composite parent) {
		flowViewer = Tables.createViewer(parent, COLUMN_LABELS);
		flowViewer.setLabelProvider(new FlowImpactLabelProvider());
		flowViewer.setFilters(new ViewerFilter[] { new CutOffFilter() });
		Tables.bindColumnWidths(flowViewer.getTable(), new double[] { 0.1, 0.4,
				0.3, 0.2 });
	}

	private class FlowImpactLabelProvider extends BaseLabelProvider implements
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
			ContributionItem<FlowDescriptor> contribution = (ContributionItem<FlowDescriptor>) element;
			return image.getForTable(contribution.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ContributionItem))
				return null;
			ContributionItem<FlowDescriptor> contribution = (ContributionItem<FlowDescriptor>) element;
			switch (columnIndex) {
			case 0:
				return Numbers.percent(contribution.getShare());
			case 1:
				return Labels.getDisplayName(contribution.getItem());
			case 2:
				return Numbers.format(contribution.getAmount());
			case 3:
				return impactCombo.getSelected().getReferenceUnit();
			}
			return null;
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
