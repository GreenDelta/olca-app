package org.openlca.app.results.contributions;

import java.util.ArrayList;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;

/**
 * Shows the single and upstream results of the processes in an analysis result.
 */
public class ProcessResultPage extends FormPage {

	private final CalculationSetup setup;
	private final LcaResult result;
	private final ResultItemOrder items;
	private final ResultProvider flowResult;
	private final ResultProvider impactResult;
	private final ContributionImage image = new ContributionImage();

	private FormToolkit tk;
	private TechFlowCombo flowProcessViewer;
	private TechFlowCombo impactCombo;
	private TableViewer inputTable;
	private TableViewer outputTable;
	private TableViewer impactTable;
	private Spinner flowSpinner;
	private Spinner impactSpinner;
	private double flowCutOff = 0.01;
	private double impactCutOff = 0.01;

	public ProcessResultPage(ResultEditor editor) {
		super(editor, ProcessResultPage.class.getName(), M.ProcessResults);
		this.result = editor.result;
		this.setup = editor.setup;
		this.items = editor.items;
		this.flowResult = new ResultProvider(result);
		this.impactResult = new ResultProvider(result);
	}

	@Override
	public void dispose() {
		image.dispose();
		super.dispose();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		tk = mForm.getToolkit();
		var form = UI.formHeader(mForm,
				Labels.name(setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var body = UI.formBody(form, tk);
		if (result.hasEnviFlows()) {
			createFlowSection(body);
		}
		if (result.hasImpacts()) {
			createImpactSection(body);
		}
		form.reflow(true);
		setInputs();
	}

	private void setInputs() {
		if (!result.hasEnviFlows())
			return;

		fillFlows(inputTable);
		fillFlows(outputTable);
		var refFlow = result.demand().techFlow();
		flowProcessViewer.select(refFlow);

		if (result.hasImpacts()) {
			impactCombo.select(refFlow);
			impactTable.setInput(items.impacts());
		}
	}

	private void fillFlows(TableViewer table) {
		boolean input = table == inputTable;
		var list = new ArrayList<EnviFlow>();
		for (var f : items.enviFlows()) {
			if (!f.isVirtual() && f.isInput() == input) {
				list.add(f);
			}
		}
		table.setInput(list);
	}

	private void createFlowSection(Composite parent) {
		var section = UI.section(parent, tk, M.FlowContributionsToProcessResults);
		UI.gridData(section, true, true);
		var comp = tk.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 1);

		var container = tk.createComposite(comp);
		UI.gridData(container, true, false);
		UI.gridLayout(container, 5);
		UI.formLabel(container, tk, M.Process);
		flowProcessViewer = new TechFlowCombo(container);
		flowProcessViewer.setInput(items.techFlows());
		flowProcessViewer.addSelectionChangedListener((selection) -> {
			flowResult.setTechFlow(selection);
			inputTable.refresh();
			outputTable.refresh();
		});

		UI.formLabel(container, tk, M.DontShowSmallerThen);
		flowSpinner = new Spinner(container, SWT.BORDER);
		flowSpinner.setValues(1, 0, 10000, 2, 1, 100);
		tk.adapt(flowSpinner);
		tk.createLabel(container, "%");
		Controls.onSelect(flowSpinner, (e) -> {
			flowCutOff = flowSpinner.getSelection();
			inputTable.refresh();
			outputTable.refresh();
		});

		var resultComp = tk.createComposite(comp);
		resultComp.setLayout(new GridLayout(2, true));
		UI.gridData(resultComp, true, true);
		UI.formLabel(resultComp, tk, M.Inputs);
		UI.formLabel(resultComp, tk, M.Outputs);
		inputTable = createFlowTable(resultComp);
		outputTable = createFlowTable(resultComp);
	}

	private TableViewer createFlowTable(Composite parent) {
		var label = new FlowLabel();
		String[] headers = {
				M.Contribution,
				M.Flow,
				M.Category,
				M.UpstreamInclDirect,
				M.Direct,
				M.Unit};
		var table = Tables.createViewer(parent, headers, label);
		Tables.bindColumnWidths(table, 0.1, 0.3, 0.2, 0.15, 0.15, 0.1);
		decorateResultViewer(table);
		Viewers.sortByLabels(table, label, 1, 2, 5);
		Viewers.sortByDouble(table,
				(EnviFlow f) -> flowResult.getTotalContribution(f), 0);
		Viewers.sortByDouble(table,
				(EnviFlow f) -> flowResult.getTotalResult(f), 3);
		Viewers.sortByDouble(table,
				(EnviFlow f) -> flowResult.getDirectResult(f), 4);
		Actions.bind(table, TableClipboard.onCopySelected(table));
		table.getTable().getColumns()[3].setAlignment(SWT.RIGHT);
		table.getTable().getColumns()[4].setAlignment(SWT.RIGHT);
		return table;
	}

	private void createImpactSection(Composite parent) {
		var section = UI.section(parent, tk, M.ImpactAssessmentResults);
		UI.gridData(section, true, true);
		var comp = tk.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 1);

		var container = tk.createComposite(comp);
		UI.gridLayout(container, 5);
		UI.gridData(container, true, false);
		UI.formLabel(container, tk, M.Process);
		impactCombo = new TechFlowCombo(container);
		impactCombo.setInput(items.techFlows());
		impactCombo.addSelectionChangedListener((selection) -> {
			impactResult.setTechFlow(selection);
			impactTable.refresh();
		});
		UI.formLabel(container, tk, M.DontShowSmallerThen);
		impactSpinner = new Spinner(container, SWT.BORDER);
		impactSpinner.setValues(1, 0, 10000, 2, 1, 100);
		tk.adapt(impactSpinner);
		tk.createLabel(container, "%");
		Controls.onSelect(impactSpinner, (e) -> {
			impactCutOff = impactSpinner.getSelection();
			impactTable.refresh();
		});
		impactTable = createImpactTable(comp);
	}

	private TableViewer createImpactTable(Composite composite) {
		ImpactLabel label = new ImpactLabel();
		String[] headers = {
				M.Contribution,
				M.ImpactCategory,
				M.UpstreamInclDirect,
				M.Direct,
				M.Unit};
		var table = Tables.createViewer(composite, headers, label);
		Tables.bindColumnWidths(table, 0.20, 0.30, 0.20, 0.20, 0.10);
		decorateResultViewer(table);
		Viewers.sortByLabels(table, label, 1, 4);
		Viewers.sortByDouble(table,
				(ImpactDescriptor i) -> impactResult.getTotalContribution(i), 0);
		Viewers.sortByDouble(table,
				(ImpactDescriptor i) -> impactResult.getTotalResult(i), 2);
		Viewers.sortByDouble(table,
				(ImpactDescriptor i) -> impactResult.getDirectResult(i), 3);
		Actions.bind(table, TableClipboard.onCopySelected(table));
		table.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
		table.getTable().getColumns()[3].setAlignment(SWT.RIGHT);
		return table;
	}

	private void decorateResultViewer(TableViewer table) {
		table.setFilters(new CutOffFilter());
		UI.gridData(table.getTable(), true, true);
	}

	private static class TechFlowCombo extends AbstractComboViewer<TechFlow> {

		public TechFlowCombo(Composite parent) {
			super(parent);
			setInput(new TechFlow[0]);
		}

		@Override
		public Class<TechFlow> getType() {
			return TechFlow.class;
		}

	}

	private class FlowLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof EnviFlow flow) || col != 0)
				return null;
			double c = flowResult.getTotalContribution(flow);
			return image.get(c);
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof EnviFlow flow))
				return null;
			return switch (col) {
				case 0 -> Numbers.percent(flowResult.getTotalContribution(flow));
				case 1 -> Labels.name(flow);
				case 2 -> Labels.category(flow);
				case 3 -> Numbers.format(flowResult.getTotalResult(flow));
				case 4 -> Numbers.format(flowResult.getDirectResult(flow));
				case 5 -> Labels.refUnit(flow);
				default -> null;
			};
		}
	}

	private class ImpactLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof ImpactDescriptor d))
				return null;
			if (col != 0)
				return null;
			return image.get(impactResult.getTotalContribution(d));
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ImpactDescriptor d))
				return null;
			return switch (col) {
				case 0 -> Numbers.percent(impactResult.getTotalContribution(d));
				case 1 -> d.name;
				case 2 -> Numbers.format(impactResult.getTotalResult(d));
				case 3 -> Numbers.format(impactResult.getDirectResult(d));
				case 4 -> d.referenceUnit;
				default -> null;
			};
		}

	}

	private class CutOffFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parent, Object o) {
			if (!(o instanceof EnviFlow || o instanceof ImpactDescriptor))
				return false;
			boolean forFlow = o instanceof EnviFlow;
			double cutoff = forFlow ? flowCutOff : impactCutOff;
			if (cutoff == 0)
				return true;
			double c = forFlow
					? flowResult.getTotalContribution((EnviFlow) o)
					: impactResult.getTotalContribution((ImpactDescriptor) o);
			return c * 100 > cutoff;
		}
	}

	private static class ResultProvider {

		private final LcaResult result;
		private TechFlow techFlow;

		public ResultProvider(LcaResult result) {
			this.techFlow = result.demand().techFlow();
			this.result = result;
		}

		public void setTechFlow(TechFlow techFlow) {
			this.techFlow = techFlow;
		}

		private double getTotalContribution(EnviFlow flow) {
			if (techFlow == null || flow == null)
				return 0;
			double total = result.getTotalFlowValueOf(flow);
			double value = result.getTotalFlowOf(flow, techFlow);
			return Contribution.shareOf(value, total);
		}

		private double getDirectResult(EnviFlow flow) {
			return techFlow == null || flow == null
					? 0
					: result.getDirectFlowOf(flow, techFlow);
		}

		private double getTotalResult(EnviFlow flow) {
			return techFlow == null || flow == null
					? 0
					: result.getTotalFlowOf(flow, techFlow);
		}

		private double getTotalContribution(ImpactDescriptor impact) {
			if (techFlow == null || impact == null)
				return 0;
			double total = result.getTotalImpactValueOf(impact);
			double val = result.getTotalImpactOf(impact, techFlow);
			return Contribution.shareOf(val, total);
		}

		private double getDirectResult(ImpactDescriptor impact) {
			return techFlow == null || impact == null
					? 0
					: result.getDirectImpactOf(impact, techFlow);
		}

		private double getTotalResult(ImpactDescriptor impact) {
			return techFlow == null || impact == null
					? 0
					: result.getTotalImpactOf(impact, techFlow);
		}
	}

}
