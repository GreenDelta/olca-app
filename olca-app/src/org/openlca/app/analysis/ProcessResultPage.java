package org.openlca.app.analysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ProcessViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrices.FlowIndex;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.AnalysisResult;

/** Shows the single results of the processes in an analysis result. */
public class ProcessResultPage extends FormPage {

	private EntityCache cache = Database.getCache();
	private AnalyzeEditor editor;
	private AnalysisResult result;
	private ResultProvider flowResultProvider;
	private ResultProvider impactResultProvider;

	private FormToolkit toolkit;
	private ProcessViewer flowProcessViewer;
	private ProcessViewer impactProcessViewer;
	private TableViewer inputViewer;
	private TableViewer outputViewer;
	private TableViewer impactViewer;
	private Spinner flowSpinner;
	private Spinner impactSpinner;
	private ContributionImage image = new ContributionImage(
			Display.getCurrent());
	private double flowCutOff = 0.01;
	private double impactCutOff = 0.01;

	private final static String[] EXCHANGE_COLUMN_LABELS = { "Contribution",
			"Flow", "Upstream total", "Direct contribution", "Unit" };
	private final static String[] IMPACT_COLUMN_LABELS = { "Contribution",
			"Impact category", "Upstream total", "Direct impact", "Unit" };

	public ProcessResultPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, ProcessResultPage.class.getName(), "Process results");
		this.editor = editor;
		this.result = result;
		this.flowResultProvider = new ResultProvider(result);
		this.impactResultProvider = new ResultProvider(result);
	}

	@Override
	public void dispose() {
		image.dispose();
		super.dispose();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		toolkit = managedForm.getToolkit();
		ScrolledForm form = UI.formHeader(managedForm, "Process results");
		Composite body = UI.formBody(form, toolkit);
		createFlowSection(body);
		if (result.hasImpactResults())
			createImpactSection(body);
		form.reflow(true);
		setInputs();
	}

	private void setInputs() {
		FlowIndex flowIndex = result.getFlowIndex();
		List<FlowDescriptor> inputFlows = new ArrayList<>();
		List<FlowDescriptor> outputFlows = new ArrayList<>();
		for (FlowDescriptor flow : result.getFlowResults().getFlows(cache)) {
			if (flowIndex.isInput(flow.getId()))
				inputFlows.add(flow);
			else
				outputFlows.add(flow);
		}
		inputViewer.setInput(inputFlows);
		outputViewer.setInput(outputFlows);

		ProcessDescriptor p = cache.get(ProcessDescriptor.class, result
				.getProductIndex().getRefProduct().getFirst());
		flowProcessViewer.select(p);
		if (result.hasImpactResults()) {
			impactProcessViewer.select(p);
			impactViewer.setInput(result.getImpactResults().getImpacts(cache));
		}
	}

	private void createFlowSection(Composite parent) {
		Section section = UI.section(parent, toolkit, "Flow results");
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		Composite container = new Composite(composite, SWT.NONE);
		UI.gridData(container, true, false);
		UI.gridLayout(container, 5);
		UI.formLabel(container, toolkit, "Process");
		flowProcessViewer = new ProcessViewer(container, cache);
		flowProcessViewer.setInput(editor.getSetup().getProductSystem());
		flowProcessViewer
				.addSelectionChangedListener(new ISelectionChangedListener<ProcessDescriptor>() {
					@Override
					public void selectionChanged(ProcessDescriptor selection) {
						flowResultProvider.setProcess(selection);
						inputViewer.refresh();
						outputViewer.refresh();
					}
				});
		UI.formLabel(container, toolkit, "Cut-Off");
		flowSpinner = new Spinner(container, SWT.BORDER);
		flowSpinner.setValues(1, 0, 10000, 2, 1, 100);
		toolkit.adapt(flowSpinner);
		toolkit.createLabel(container, "%");
		flowSpinner.addSelectionListener(new FlowCutOffChange());

		Composite resultContainer = new Composite(composite, SWT.NONE);
		resultContainer.setLayout(new GridLayout(2, true));
		UI.gridData(resultContainer, true, true);

		UI.formLabel(resultContainer, "Inputs");
		UI.formLabel(resultContainer, "Outputs");

		inputViewer = new TableViewer(resultContainer);
		inputViewer.setLabelProvider(new FlowLabel());
		decorateResultViewer(inputViewer, EXCHANGE_COLUMN_LABELS);

		outputViewer = new TableViewer(resultContainer);
		outputViewer.setLabelProvider(new FlowLabel());
		decorateResultViewer(outputViewer, EXCHANGE_COLUMN_LABELS);

	}

	private void createImpactSection(Composite parent) {
		Section section = UI.section(parent, toolkit,
				"Impact assessment results");
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		Composite container = new Composite(composite, SWT.NONE);
		UI.gridLayout(container, 5);
		UI.gridData(container, true, false);
		UI.formLabel(container, toolkit, "Process");
		impactProcessViewer = new ProcessViewer(container, cache);
		impactProcessViewer.setInput(editor.getSetup().getProductSystem());
		impactProcessViewer
				.addSelectionChangedListener(new ISelectionChangedListener<ProcessDescriptor>() {
					@Override
					public void selectionChanged(ProcessDescriptor selection) {
						impactResultProvider.setProcess(selection);
						impactViewer.refresh();
					}
				});
		UI.formLabel(container, toolkit, "Cut-Off");
		impactSpinner = new Spinner(container, SWT.BORDER);
		impactSpinner.setValues(1, 0, 10000, 2, 1, 100);
		toolkit.adapt(impactSpinner);
		toolkit.createLabel(container, "%");
		impactSpinner.addSelectionListener(new ImpactCutOffChange());

		impactViewer = new TableViewer(composite);
		impactViewer.setLabelProvider(new ImpactLabel());
		decorateResultViewer(impactViewer, IMPACT_COLUMN_LABELS);
	}

	private void decorateResultViewer(TableViewer viewer, String[] columnLabels) {
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);

		for (int i = 0; i < columnLabels.length; i++) {
			final TableColumn c = new TableColumn(viewer.getTable(), SWT.NULL);
			c.setText(columnLabels[i]);
		}
		viewer.setColumnProperties(columnLabels);
		viewer.setSorter(new ContributionSorter());
		viewer.setFilters(new ViewerFilter[] { new CutOffFilter() });
		UI.gridData(viewer.getTable(), true, true);
		Tables.bindColumnWidths(viewer.getTable(), new double[] { 0.20, 0.30,
				0.20, 0.20, 0.10 });
	}

	private class FlowCutOffChange implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			flowCutOff = flowSpinner.getSelection();
			inputViewer.refresh();
			outputViewer.refresh();
		}
	}

	private class ImpactCutOffChange implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			impactCutOff = impactSpinner.getSelection();
			impactViewer.refresh();
		}
	}

	private class FlowLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof FlowDescriptor) || columnIndex != 0)
				return null;
			FlowDescriptor flow = (FlowDescriptor) element;
			return image.getForTable(flowResultProvider
					.getUpstreamContribution(flow));
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) element;
			switch (columnIndex) {
			case 0:
				return Numbers.percent(flowResultProvider
						.getUpstreamContribution(flow));
			case 1:
				return getFlowLabel(flow);
			case 2:
				return Numbers
						.format(flowResultProvider.getUpstreamTotal(flow));
			case 3:
				return Numbers.format(flowResultProvider.getDirectResult(flow));
			case 4:
				return Labels.getRefUnit(flow, cache);
			default:
				return null;
			}
		}

		private String getFlowLabel(FlowDescriptor flow) {
			String val = flow.getName();
			if (flow.getCategory() == null && flow.getLocation() == null)
				return val;
			Pair<String, String> cat = Labels.getFlowCategory(flow, cache);
			val += "(";
			if (cat.getLeft() != null && !cat.getLeft().isEmpty())
				val += cat.getLeft();
			if (cat.getRight() != null && !cat.getRight().isEmpty())
				val += "/" + cat.getRight();
			val += ")";
			return val;
		}
	}

	private class ContributionSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!((e1 instanceof FlowDescriptor && e2 instanceof FlowDescriptor) || (e1 instanceof ImpactCategoryDescriptor && e2 instanceof ImpactCategoryDescriptor)))
				return 0;

			double contribution1 = 0;
			if (e1 instanceof FlowDescriptor)
				contribution1 = flowResultProvider
						.getUpstreamContribution((FlowDescriptor) e1);
			else if (e1 instanceof ImpactCategoryDescriptor)
				contribution1 = impactResultProvider
						.getUpstreamContribution((ImpactCategoryDescriptor) e1);

			double contribution2 = 0;
			if (e2 instanceof FlowDescriptor)
				contribution2 = flowResultProvider
						.getUpstreamContribution((FlowDescriptor) e2);
			else if (e2 instanceof ImpactCategoryDescriptor)
				contribution2 = impactResultProvider
						.getUpstreamContribution((ImpactCategoryDescriptor) e2);

			return -1 * Double.compare(contribution1, contribution2);
		}
	}

	private class ImpactLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof ImpactCategoryDescriptor)
					|| element == null)
				return null;
			if (columnIndex != 0)
				return null;

			return image
					.getForTable(impactResultProvider
							.getUpstreamContribution((ImpactCategoryDescriptor) element));
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ImpactCategoryDescriptor)
					|| element == null)
				return null;

			ImpactCategoryDescriptor category = (ImpactCategoryDescriptor) element;
			switch (columnIndex) {
			case 0:
				return Numbers.percent(impactResultProvider
						.getUpstreamContribution(category));
			case 1:
				return category.getName();
			case 2:
				return Numbers.format(impactResultProvider
						.getUpstreamTotal(category));
			case 3:
				return Numbers.format(impactResultProvider
						.getDirectResult(category));
			case 4:
				return category.getReferenceUnit();
			}
			return null;
		}

	}

	private class CutOffFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof FlowDescriptor || element instanceof ImpactCategoryDescriptor))
				return false;
			boolean forFlow = element instanceof FlowDescriptor;
			double cutoff = forFlow ? flowCutOff : impactCutOff;
			if (cutoff == 0)
				return true;
			double contribution = 0;
			if (forFlow)
				contribution = flowResultProvider
						.getUpstreamContribution((FlowDescriptor) element);
			else
				contribution = impactResultProvider
						.getUpstreamContribution((ImpactCategoryDescriptor) element);
			return contribution * 100 > cutoff;
		}
	}

	private class ResultProvider {

		private ProcessDescriptor process;
		private AnalysisResult result;

		public ResultProvider(AnalysisResult result) {
			this.process = cache.get(ProcessDescriptor.class, result
					.getProductIndex().getRefProduct().getFirst());
			this.result = result;
		}

		public void setProcess(ProcessDescriptor process) {
			this.process = process;
		}

		private double getUpstreamContribution(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			double total = result.getFlowResults().getTotalResult(flow);
			if (total == 0)
				return 0;
			double val = result.getTotalFlowResult(process.getId(),
					flow.getId());
			double contribution = val / total;
			if (contribution > 1)
				return 1;
			return contribution;
		}

		private double getDirectResult(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			return result.getSingleFlowResult(process.getId(), flow.getId());
		}

		private double getUpstreamTotal(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			return result.getTotalFlowResult(process.getId(), flow.getId());
		}

		private double getUpstreamContribution(ImpactCategoryDescriptor category) {
			if (process == null || category == null)
				return 0;
			double total = result.getImpactResults().getTotalResult(category);
			if (total == 0)
				return 0;
			double val = result.getTotalImpactResult(process.getId(),
					category.getId());
			double contribution = val / total;
			if (contribution > 1)
				return 1;
			return contribution;
		}

		private double getDirectResult(ImpactCategoryDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getSingleImpactResult(process.getId(),
					category.getId());
		}

		private double getUpstreamTotal(ImpactCategoryDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getTotalImpactResult(process.getId(),
					category.getId());
		}
	}

}
