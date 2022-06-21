package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
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
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.core.results.FullResult;

/**
 * Shows the single and upstream results of the processes in an analysis result.
 */
public class ProcessResultPage extends FormPage {

	private final Map<Long, ProcessDescriptor> processes = new HashMap<>();
	private final CalculationSetup setup;
	private final FullResult result;
	private final ResultProvider flowResult;
	private final ResultProvider impactResult;
	private final ContributionImage image = new ContributionImage();

	private FormToolkit toolkit;
	private ProcessViewer flowProcessViewer;
	private ProcessViewer impactProcessCombo;
	private TableViewer inputTable;
	private TableViewer outputTable;
	private TableViewer impactTable;
	private Spinner flowSpinner;
	private Spinner impactSpinner;
	private double flowCutOff = 0.01;
	private double impactCutOff = 0.01;

	private final static String[] EXCHANGE_COLUMN_LABELS = {
		M.Contribution,
		M.Flow, M.UpstreamInclDirect, M.Direct,
		M.Unit};
	private final static String[] IMPACT_COLUMN_LABELS = {
		M.Contribution,
		M.ImpactCategory, M.UpstreamInclDirect,
		M.Direct, M.Unit};

	public ProcessResultPage(FormEditor editor,
													 FullResult result, CalculationSetup setup) {
		super(editor, ProcessResultPage.class.getName(), M.ProcessResults);
		this.result = result;
		this.setup = setup;
		for (var desc : result.getProcesses()) {
			if (desc instanceof ProcessDescriptor) {
				processes.put(desc.id, (ProcessDescriptor) desc);
			}
		}
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
		toolkit = mForm.getToolkit();
		var form = UI.formHeader(mForm,
			Labels.name(setup.target()),
			Images.get(result));
		var body = UI.formBody(form, toolkit);
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
		fillFlows(inputTable);
		fillFlows(outputTable);
		long refProcessId = result.demand().techFlow().providerId();
		ProcessDescriptor p = processes.get(refProcessId);
		flowProcessViewer.select(p);
		if (result.hasImpacts()) {
			impactProcessCombo.select(p);
			impactTable.setInput(result.getImpacts());
		}
	}

	private void fillFlows(TableViewer table) {
		boolean input = table == inputTable;
		var list = new ArrayList<EnviFlow>();
		for (var f : result.getFlows()) {
			if (!f.isVirtual() && f.isInput() == input) {
				list.add(f);
			}
		}
		table.setInput(list);
	}

	private void createFlowSection(Composite parent) {
		var section = UI.section(parent, toolkit,
			M.FlowContributionsToProcessResults);
		UI.gridData(section, true, true);
		Composite comp = toolkit.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 1);

		Composite container = new Composite(comp, SWT.NONE);
		UI.gridData(container, true, false);
		UI.gridLayout(container, 5);
		UI.formLabel(container, toolkit, M.Process);
		flowProcessViewer = new ProcessViewer(container);
		flowProcessViewer.setInput(processes.values());
		flowProcessViewer.addSelectionChangedListener((selection) -> {
			flowResult.setProcess(selection);
			inputTable.refresh();
			outputTable.refresh();
		});

		UI.formLabel(container, toolkit, M.DontShowSmallerThen);
		flowSpinner = new Spinner(container, SWT.BORDER);
		flowSpinner.setValues(1, 0, 10000, 2, 1, 100);
		toolkit.adapt(flowSpinner);
		toolkit.createLabel(container, "%");
		Controls.onSelect(flowSpinner, (e) -> {
			flowCutOff = flowSpinner.getSelection();
			inputTable.refresh();
			outputTable.refresh();
		});

		Composite resultContainer = new Composite(comp, SWT.NONE);
		resultContainer.setLayout(new GridLayout(2, true));
		UI.gridData(resultContainer, true, true);
		UI.formLabel(resultContainer, M.Inputs);
		UI.formLabel(resultContainer, M.Outputs);
		inputTable = createFlowTable(resultContainer);
		outputTable = createFlowTable(resultContainer);
	}

	private TableViewer createFlowTable(Composite parent) {
		FlowLabel label = new FlowLabel();
		TableViewer table = Tables.createViewer(parent, EXCHANGE_COLUMN_LABELS, label);
		decorateResultViewer(table);
		Viewers.sortByLabels(table, label, 1, 4);
		Viewers.sortByDouble(table, (EnviFlow f) -> flowResult.getUpstreamContribution(f), 0);
		Viewers.sortByDouble(table, (EnviFlow f) -> flowResult.getUpstreamTotal(f), 2);
		Viewers.sortByDouble(table, (EnviFlow f) -> flowResult.getDirectResult(f), 3);
		Actions.bind(table, TableClipboard.onCopySelected(table));
		table.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
		table.getTable().getColumns()[3].setAlignment(SWT.RIGHT);
		return table;
	}

	private void createImpactSection(Composite parent) {
		Section section = UI.section(parent, toolkit,
			M.ImpactAssessmentResults);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		Composite container = new Composite(composite, SWT.NONE);
		UI.gridLayout(container, 5);
		UI.gridData(container, true, false);
		UI.formLabel(container, toolkit, M.Process);
		impactProcessCombo = new ProcessViewer(container);
		impactProcessCombo.setInput(processes.values());
		impactProcessCombo.addSelectionChangedListener((selection) -> {
			impactResult.setProcess(selection);
			impactTable.refresh();
		});
		UI.formLabel(container, toolkit, M.DontShowSmallerThen);
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
		ImpactLabel label = new ImpactLabel();
		TableViewer table = Tables.createViewer(composite, IMPACT_COLUMN_LABELS, label);
		decorateResultViewer(table);
		Viewers.sortByLabels(table, label, 1, 4);
		Viewers.sortByDouble(table, (ImpactDescriptor i) -> impactResult.getUpstreamContribution(i), 0);
		Viewers.sortByDouble(table, (ImpactDescriptor i) -> impactResult.getUpstreamTotal(i), 2);
		Viewers.sortByDouble(table, (ImpactDescriptor i) -> impactResult.getDirectResult(i), 3);
		Actions.bind(table, TableClipboard.onCopySelected(table));
		table.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
		table.getTable().getColumns()[3].setAlignment(SWT.RIGHT);
		return table;
	}

	private void decorateResultViewer(TableViewer table) {
		table.setFilters(new CutOffFilter());
		UI.gridData(table.getTable(), true, true);
		Tables.bindColumnWidths(table.getTable(), 0.20, 0.30, 0.20, 0.20, 0.10);
	}

	private static class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

		public ProcessViewer(Composite parent) {
			super(parent);
			setInput(new ProcessDescriptor[0]);
		}

		@Override
		public Class<ProcessDescriptor> getType() {
			return ProcessDescriptor.class;
		}

	}

	private class FlowLabel extends BaseLabelProvider implements
		ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof EnviFlow flow) || col != 0)
				return null;
			double c = flowResult.getUpstreamContribution(flow);
			return image.get(c);
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof EnviFlow f))
				return null;
			return switch (col) {
				case 0 -> Numbers.percent(flowResult.getUpstreamContribution(f));
				case 1 -> getFlowLabel(f.flow());
				case 2 -> Numbers.format(flowResult.getUpstreamTotal(f));
				case 3 -> Numbers.format(flowResult.getDirectResult(f));
				case 4 -> Labels.refUnit(f);
				default -> null;
			};
		}

		private String getFlowLabel(FlowDescriptor flow) {
			if (flow == null)
				return "";
			String name = flow.name;
			return flow.category != null
				? name + " (" + Labels.getShortCategory(flow) + ")"
				: name;
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
			return image.get(impactResult.getUpstreamContribution(d));
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ImpactDescriptor d))
				return null;
			return switch (col) {
				case 0 -> Numbers.percent(impactResult.getUpstreamContribution(d));
				case 1 -> d.name;
				case 2 -> Numbers.format(impactResult.getUpstreamTotal(d));
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
				? flowResult.getUpstreamContribution((EnviFlow) o)
				: impactResult.getUpstreamContribution((ImpactDescriptor) o);
			return c * 100 > cutoff;
		}
	}

	private static class ResultProvider {

		private final FullResult result;
		private RootDescriptor process;

		public ResultProvider(FullResult result) {
			this.process = result.demand().techFlow().provider();
			this.result = result;
		}

		public void setProcess(ProcessDescriptor process) {
			this.process = process;
		}

		private double getUpstreamContribution(EnviFlow flow) {
			if (process == null || flow == null)
				return 0;
			double total = result.getTotalFlowResult(flow);
			if (total == 0)
				return 0;
			double val = result.getUpstreamFlowResult(process, flow);
			double c = val / Math.abs(total);
			return c > 1 ? 1 : c;
		}

		private double getDirectResult(EnviFlow flow) {
			if (process == null || flow == null)
				return 0;
			return result.getDirectFlowResult(process, flow);
		}

		private double getUpstreamTotal(EnviFlow flow) {
			if (process == null || flow == null)
				return 0;
			return result.getUpstreamFlowResult(process, flow);
		}

		private double getUpstreamContribution(ImpactDescriptor d) {
			if (process == null || d == null)
				return 0;
			double total = result.getTotalImpactResult(d);
			if (total == 0)
				return 0;
			double val = result.getUpstreamImpactResult(process, d);
			double c = val / Math.abs(total);
			return c > 1 ? 1 : c;
		}

		private double getDirectResult(ImpactDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getDirectImpactResult(process, category);
		}

		private double getUpstreamTotal(ImpactDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getUpstreamImpactResult(process, category);
		}
	}

}
