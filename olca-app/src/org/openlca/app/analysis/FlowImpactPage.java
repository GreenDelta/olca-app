package org.openlca.app.analysis;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.AnalysisResult;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.FlowImpactContribution;

public class FlowImpactPage extends FormPage {

	private final static String[] COLUMN_LABELS = { "Contribution", "Flow",
			"Total amount", "Single amount", "Unit" };

	private EntityCache cache = Cache.getEntityCache();
	private AnalysisResult result;
	private ImpactCategoryViewer impactCategoryViewer;
	private TableViewer flowViewer;
	private FlowImpactContribution flowImpactContribution;
	private Spinner spinner;
	private double cutOff = 1;

	public FlowImpactPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, ProcessResultPage.class.getName(), "Flow contributions");
		this.result = result;
		this.flowImpactContribution = new FlowImpactContribution(result,
				Cache.getEntityCache());
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();

		ScrolledForm form = UI.formHeader(managedForm, "Flow contributions");
		Composite body = UI.formBody(form, toolkit);

		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);

		Composite selectionContainer = toolkit.createComposite(composite);
		UI.gridData(selectionContainer, true, false);
		UI.gridLayout(selectionContainer, 5);
		UI.formLabel(selectionContainer, toolkit, "Impact category");
		impactCategoryViewer = new ImpactCategoryViewer(selectionContainer);
		impactCategoryViewer.setInput(result.getImpactResults().getImpacts(
				cache));
		impactCategoryViewer
				.addSelectionChangedListener(new ISelectionChangedListener<ImpactCategoryDescriptor>() {

					@Override
					public void selectionChanged(
							ImpactCategoryDescriptor selection) {
						ContributionSet<FlowDescriptor> contributions = flowImpactContribution
								.calculate(selection);
						flowViewer.setInput(contributions.getContributions());
					}
				});
		UI.formLabel(selectionContainer, toolkit, "Cut-Off");
		spinner = new Spinner(selectionContainer, SWT.BORDER);
		spinner.setValues(1, 0, 10000, 2, 1, 100);
		toolkit.adapt(spinner);
		toolkit.createLabel(selectionContainer, "%");
		spinner.addSelectionListener(new CutOffChange());

		createFlowViewer(composite);

		form.reflow(true);

		impactCategoryViewer.selectFirst();
	}

	private void createFlowViewer(Composite parent) {
		flowViewer = new TableViewer(parent);
		flowViewer.setLabelProvider(new FlowImpactLabelProvider());
		flowViewer.setContentProvider(ArrayContentProvider.getInstance());
		flowViewer.getTable().setLinesVisible(true);
		flowViewer.getTable().setHeaderVisible(true);

		for (int i = 0; i < COLUMN_LABELS.length; i++) {
			final TableColumn c = new TableColumn(flowViewer.getTable(),
					SWT.NULL);
			c.setText(COLUMN_LABELS[i]);
		}
		flowViewer.setColumnProperties(COLUMN_LABELS);
		flowViewer.setSorter(new ContributionSorter());
		flowViewer.setFilters(new ViewerFilter[] { new CutOffFilter() });
		UI.gridData(flowViewer.getTable(), true, true);
		Tables.bindColumnWidths(flowViewer.getTable(), new double[] { 0.17,
				0.38, 0.17, 0.17, 0.10 });
	}

	private double getSingleAmount(Contribution<FlowDescriptor> flowContribution) {
		return flowContribution.getAmount();
	}

	private double getTotalAmount() {
		return result.getImpactResults().getTotalResult(
				impactCategoryViewer.getSelected());
	}

	private double getContribution(Contribution<FlowDescriptor> flowContribution) {
		double singleResult = getSingleAmount(flowContribution);
		if (singleResult == 0)
			return 0;
		double referenceResult = getTotalAmount();
		if (referenceResult == 0)
			return 0;
		double contribution = singleResult / referenceResult;
		if (contribution > 1)
			return 1;
		return contribution;
	}

	private class CutOffChange implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			cutOff = spinner.getSelection();
			flowViewer.refresh();
		}
	}

	private class FlowImpactLabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof Contribution) || element == null)
				return null;
			if (columnIndex != 0)
				return null;

			@SuppressWarnings("unchecked")
			Contribution<FlowDescriptor> contribution = (Contribution<FlowDescriptor>) element;
			return image.getForTable(getContribution(contribution));
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Contribution) || element == null)
				return null;

			@SuppressWarnings("unchecked")
			Contribution<FlowDescriptor> contribution = (Contribution<FlowDescriptor>) element;
			switch (columnIndex) {
			case 0:
				return Numbers.percent(getContribution(contribution));
			case 1:
				return contribution.getItem().getName();
			case 2:
				return Numbers.format(getTotalAmount());
			case 3:
				return Numbers.format(getSingleAmount(contribution));
			case 4:
				return impactCategoryViewer.getSelected().getReferenceUnit();
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

		@SuppressWarnings("unchecked")
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof Contribution) || element == null)
				return false;
			if (getContribution((Contribution<FlowDescriptor>) element) * 100 < cutOff)
				return false;
			return true;
		}
	}

	private class ContributionSorter extends ViewerSorter {

		@SuppressWarnings("unchecked")
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			// just for safety, should never happen
			if (!(e1 instanceof Contribution && e2 instanceof Contribution)
					|| e1 == null || e2 == null)
				return 0;

			double contribution1 = 0;
			if (e1 instanceof Contribution)
				contribution1 = getContribution((Contribution<FlowDescriptor>) e1);
			double contribution2 = 0;
			if (e2 instanceof Contribution)
				contribution2 = getContribution((Contribution<FlowDescriptor>) e2);

			return -1 * Double.compare(contribution1, contribution2);
		}
	}

}
