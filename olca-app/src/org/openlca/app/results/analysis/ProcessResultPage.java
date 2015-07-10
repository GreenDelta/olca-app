package org.openlca.app.results.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.combo.ProcessViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.matrix.FlowIndex;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResultProvider;

/**
 * Shows the single and upstream results of the processes in an analysis result.
 */
public class ProcessResultPage extends FormPage {

	private EntityCache cache = Cache.getEntityCache();
	private CalculationSetup setup;
	private FullResultProvider result;
	private ResultProvider flowResult;
	private ResultProvider impactResult;

	private FormToolkit toolkit;
	private ProcessViewer flowProcessViewer;
	private ProcessViewer impactProcessCombo;
	private TableViewer inputTable;
	private TableViewer outputTable;
	private TableViewer impactTable;
	private Spinner flowSpinner;
	private Spinner impactSpinner;
	private ContributionImage image = new ContributionImage(
			Display.getCurrent());
	private double flowCutOff = 0.01;
	private double impactCutOff = 0.01;

	private final static String[] EXCHANGE_COLUMN_LABELS = {
			Messages.Contribution,
			Messages.Flow, Messages.UpstreamTotal, Messages.DirectContribution,
			Messages.Unit };
	private final static String[] IMPACT_COLUMN_LABELS = {
			Messages.Contribution,
			Messages.ImpactCategory, Messages.UpstreamTotal,
			Messages.DirectImpact, Messages.Unit };

	public ProcessResultPage(FormEditor editor, FullResultProvider result,
			CalculationSetup setup) {
		super(editor, ProcessResultPage.class.getName(),
				Messages.ProcessResults);
		this.setup = setup;
		this.result = result;
		this.flowResult = new ResultProvider(result);
		this.impactResult = new ResultProvider(result);
	}

	@Override
	public void dispose() {
		image.dispose();
		super.dispose();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		toolkit = managedForm.getToolkit();
		ScrolledForm form = UI.formHeader(managedForm, Messages.ProcessResults);
		Composite body = UI.formBody(form, toolkit);
		createFlowSection(body);
		if (result.hasImpactResults())
			createImpactSection(body);
		form.reflow(true);
		setInputs();
	}

	private void setInputs() {
		fillFlows(inputTable);
		fillFlows(outputTable);
		ProcessDescriptor p = cache.get(ProcessDescriptor.class, result
				.getResult().getProductIndex().getRefProduct().getFirst());
		flowProcessViewer.select(p);
		if (result.hasImpactResults()) {
			impactProcessCombo.select(p);
			impactTable.setInput(result.getImpactDescriptors());
		}
	}

	private void fillFlows(TableViewer table) {
		boolean input = table == inputTable;
		FlowIndex idx = result.getResult().getFlowIndex();
		List<FlowDescriptor> list = new ArrayList<>();
		for (FlowDescriptor f : result.getFlowDescriptors()) {
			if (idx.isInput(f.getId()) == input)
				list.add(f);
		}
		Collections.sort(list);
		table.setInput(list);
	}

	private void createFlowSection(Composite parent) {
		Section section = UI.section(parent, toolkit, Messages.FlowResults);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		Composite container = new Composite(composite, SWT.NONE);
		UI.gridData(container, true, false);
		UI.gridLayout(container, 5);
		UI.formLabel(container, toolkit, Messages.Process);
		flowProcessViewer = new ProcessViewer(container, cache);
		flowProcessViewer.setInput(setup.getProductSystem());
		flowProcessViewer.addSelectionChangedListener((selection) -> {
			flowResult.setProcess(selection);
			inputTable.refresh();
			outputTable.refresh();
		});

		UI.formLabel(container, toolkit, Messages.Cutoff);
		flowSpinner = new Spinner(container, SWT.BORDER);
		flowSpinner.setValues(1, 0, 10000, 2, 1, 100);
		toolkit.adapt(flowSpinner);
		toolkit.createLabel(container, "%");
		Controls.onSelect(flowSpinner, (e) -> {
			flowCutOff = flowSpinner.getSelection();
			inputTable.refresh();
			outputTable.refresh();
		});

		Composite resultContainer = new Composite(composite, SWT.NONE);
		resultContainer.setLayout(new GridLayout(2, true));
		UI.gridData(resultContainer, true, true);
		UI.formLabel(resultContainer, Messages.Inputs);
		UI.formLabel(resultContainer, Messages.Outputs);
		inputTable = createFlowTable(resultContainer);
		outputTable = createFlowTable(resultContainer);
	}

	private TableViewer createFlowTable(Composite parent) {
		TableViewer table = new TableViewer(parent);
		decorateResultViewer(table, EXCHANGE_COLUMN_LABELS);
		FlowLabel label = new FlowLabel();
		table.setLabelProvider(label);
		Tables.sortByLabels(table, label, 1, 4);
		Tables.sortByDouble(table, (FlowDescriptor f) -> flowResult
				.getUpstreamContribution(f), 0);
		Tables.sortByDouble(table, (FlowDescriptor f) -> flowResult
				.getUpstreamTotal(f), 2);
		Tables.sortByDouble(table, (FlowDescriptor f) -> flowResult
				.getDirectResult(f), 3);
		return table;
	}

	private void createImpactSection(Composite parent) {
		Section section = UI.section(parent, toolkit,
				Messages.ImpactAssessmentResults);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		Composite container = new Composite(composite, SWT.NONE);
		UI.gridLayout(container, 5);
		UI.gridData(container, true, false);
		UI.formLabel(container, toolkit, Messages.Process);
		impactProcessCombo = new ProcessViewer(container, cache);
		impactProcessCombo.setInput(setup.getProductSystem());
		impactProcessCombo.addSelectionChangedListener((selection) -> {
			impactResult.setProcess(selection);
			impactTable.refresh();
		});
		UI.formLabel(container, toolkit, Messages.Cutoff);
		impactSpinner = new Spinner(container, SWT.BORDER);
		impactSpinner.setValues(1, 0, 10000, 2, 1, 100);
		toolkit.adapt(impactSpinner);
		toolkit.createLabel(container, "%");
		Controls.onSelect(impactSpinner, (e) -> {
			impactCutOff = impactSpinner.getSelection();
			impactTable.refresh();
		});
		impactTable = createImpactTable(composite);
	}

	private TableViewer createImpactTable(Composite composite) {
		TableViewer table = new TableViewer(composite);
		decorateResultViewer(table, IMPACT_COLUMN_LABELS);
		ImpactLabel label = new ImpactLabel();
		table.setLabelProvider(label);
		Tables.sortByLabels(table, label, 1, 4);
		Tables.sortByDouble(table, (ImpactCategoryDescriptor i) -> impactResult
				.getUpstreamContribution(i), 0);
		Tables.sortByDouble(table, (ImpactCategoryDescriptor i) -> impactResult
				.getUpstreamTotal(i), 2);
		Tables.sortByDouble(table, (ImpactCategoryDescriptor i) -> impactResult
				.getDirectResult(i), 3);
		return table;
	}

	private void decorateResultViewer(TableViewer table, String[] columns) {
		table.setContentProvider(ArrayContentProvider.getInstance());
		table.getTable().setLinesVisible(true);
		table.getTable().setHeaderVisible(true);
		for (int i = 0; i < columns.length; i++) {
			TableColumn c = new TableColumn(table.getTable(), SWT.NULL);
			c.setText(columns[i]);
		}
		table.setColumnProperties(columns);
		table.setFilters(new ViewerFilter[] { new CutOffFilter() });
		UI.gridData(table.getTable(), true, true);
		Tables.bindColumnWidths(table.getTable(), 0.20, 0.30, 0.20, 0.20, 0.10);
	}

	private class FlowLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof FlowDescriptor) || col != 0)
				return null;
			FlowDescriptor flow = (FlowDescriptor) o;
			double c = flowResult.getUpstreamContribution(flow);
			return image.getForTable(c);
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) o;
			switch (col) {
			case 0:
				return Numbers.percent(
						flowResult.getUpstreamContribution(flow));
			case 1:
				return getFlowLabel(flow);
			case 2:
				return Numbers
						.format(flowResult.getUpstreamTotal(flow));
			case 3:
				return Numbers.format(flowResult.getDirectResult(flow));
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

	private class ImpactLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof ImpactCategoryDescriptor))
				return null;
			if (col != 0)
				return null;
			ImpactCategoryDescriptor d = (ImpactCategoryDescriptor) o;
			return image.getForTable(impactResult.getUpstreamContribution(d));
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ImpactCategoryDescriptor))
				return null;
			ImpactCategoryDescriptor d = (ImpactCategoryDescriptor) o;
			switch (col) {
			case 0:
				return Numbers.percent(impactResult.getUpstreamContribution(d));
			case 1:
				return d.getName();
			case 2:
				return Numbers.format(impactResult.getUpstreamTotal(d));
			case 3:
				return Numbers.format(impactResult.getDirectResult(d));
			case 4:
				return d.getReferenceUnit();
			}
			return null;
		}

	}

	private class CutOffFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parent, Object o) {
			if (!(o instanceof FlowDescriptor
					|| o instanceof ImpactCategoryDescriptor))
				return false;
			boolean forFlow = o instanceof FlowDescriptor;
			double cutoff = forFlow ? flowCutOff : impactCutOff;
			if (cutoff == 0)
				return true;
			double c = 0;
			if (forFlow)
				c = flowResult
						.getUpstreamContribution((FlowDescriptor) o);
			else
				c = impactResult
						.getUpstreamContribution((ImpactCategoryDescriptor) o);
			return c * 100 > cutoff;
		}
	}

	private class ResultProvider {

		private ProcessDescriptor process;
		private FullResultProvider result;

		public ResultProvider(FullResultProvider result) {
			long refProcessId = result.getResult().getProductIndex()
					.getRefProduct().getFirst();
			this.process = cache.get(ProcessDescriptor.class, refProcessId);
			this.result = result;
		}

		public void setProcess(ProcessDescriptor process) {
			this.process = process;
		}

		private double getUpstreamContribution(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			double total = result.getTotalFlowResult(flow).getValue();
			if (total == 0)
				return 0;
			double val = result.getUpstreamFlowResult(process, flow).getValue();
			double c = val / Math.abs(total);
			return c > 1 ? 1 : c;
		}

		private double getDirectResult(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			return result.getSingleFlowResult(process, flow).getValue();
		}

		private double getUpstreamTotal(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			return result.getUpstreamFlowResult(process, flow).getValue();
		}

		private double getUpstreamContribution(ImpactCategoryDescriptor d) {
			if (process == null || d == null)
				return 0;
			double total = result.getTotalImpactResult(d).getValue();
			if (total == 0)
				return 0;
			double val = result.getUpstreamImpactResult(process, d).getValue();
			double c = val / Math.abs(total);
			return c > 1 ? 1 : c;
		}

		private double getDirectResult(ImpactCategoryDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getSingleImpactResult(process, category).getValue();
		}

		private double getUpstreamTotal(ImpactCategoryDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getUpstreamImpactResult(process, category).getValue();
		}
	}

}
