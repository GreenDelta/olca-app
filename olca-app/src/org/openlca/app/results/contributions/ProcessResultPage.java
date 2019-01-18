package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.util.Strings;

/**
 * Shows the single and upstream results of the processes in an analysis result.
 */
public class ProcessResultPage extends FormPage {

	private Map<Long, ProcessDescriptor> processes = new HashMap<>();
	private FullResult result;
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
	private CalculationSetup setup;
	private ContributionImage image = new ContributionImage(Display.getCurrent());
	private double flowCutOff = 0.01;
	private double impactCutOff = 0.01;

	private final static String[] EXCHANGE_COLUMN_LABELS = {
			M.Contribution,
			M.Flow, M.UpstreamInclDirect, M.Direct,
			M.Unit };
	private final static String[] IMPACT_COLUMN_LABELS = {
			M.Contribution,
			M.ImpactCategory, M.UpstreamInclDirect,
			M.Direct, M.Unit };

	public ProcessResultPage(FormEditor editor,
			FullResult result, CalculationSetup setup) {
		super(editor, ProcessResultPage.class.getName(), M.ProcessResults);
		this.result = result;
		this.setup = setup;
		for (CategorizedDescriptor desc : result.getProcesses()) {
			if (desc instanceof ProcessDescriptor) {
				processes.put(desc.id,
						(ProcessDescriptor) desc);
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
	protected void createFormContent(IManagedForm mform) {
		toolkit = mform.getToolkit();
		ScrolledForm form = UI.formHeader(mform,
				Labels.getDisplayName(setup.productSystem),
				Images.get(result));
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
		long refProcessId = result.techIndex.getRefFlow().id();
		ProcessDescriptor p = processes.get(refProcessId);
		flowProcessViewer.select(p);
		if (result.hasImpactResults()) {
			impactProcessCombo.select(p);
			impactTable.setInput(result.getImpacts());
		}
	}

	private void fillFlows(TableViewer table) {
		boolean input = table == inputTable;
		List<FlowDescriptor> list = new ArrayList<>();
		for (FlowDescriptor f : result.getFlows()) {
			if (result.isInput(f) == input)
				list.add(f);
		}
		Collections.sort(list,
				(f1, f2) -> Strings.compare(f1.name, f2.name));
		table.setInput(list);
	}

	private void createFlowSection(Composite parent) {
		Section section = UI.section(parent, toolkit, M.FlowContributionsToProcessResults);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		Composite container = new Composite(composite, SWT.NONE);
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

		UI.formLabel(container, toolkit, M.Cutoff);
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
		Viewers.sortByDouble(table, (FlowDescriptor f) -> flowResult.getUpstreamContribution(f), 0);
		Viewers.sortByDouble(table, (FlowDescriptor f) -> flowResult.getUpstreamTotal(f), 2);
		Viewers.sortByDouble(table, (FlowDescriptor f) -> flowResult.getDirectResult(f), 3);
		Actions.bind(table, TableClipboard.onCopy(table));
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
		UI.formLabel(container, toolkit, M.Cutoff);
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
		Viewers.sortByDouble(table, (ImpactCategoryDescriptor i) -> impactResult.getUpstreamContribution(i), 0);
		Viewers.sortByDouble(table, (ImpactCategoryDescriptor i) -> impactResult.getUpstreamTotal(i), 2);
		Viewers.sortByDouble(table, (ImpactCategoryDescriptor i) -> impactResult.getDirectResult(i), 3);
		Actions.bind(table, TableClipboard.onCopy(table));
		table.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
		table.getTable().getColumns()[3].setAlignment(SWT.RIGHT);
		return table;
	}

	private void decorateResultViewer(TableViewer table) {
		table.setFilters(new ViewerFilter[] { new CutOffFilter() });
		UI.gridData(table.getTable(), true, true);
		Tables.bindColumnWidths(table.getTable(), 0.20, 0.30, 0.20, 0.20, 0.10);
	}

	private class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

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
				return Labels.getRefUnit(flow);
			default:
				return null;
			}
		}

		private String getFlowLabel(FlowDescriptor flow) {
			String val = flow.name;
			if (flow.category == null)
				return val;
			return val + " (" + Labels.getShortCategory(flow) + ")";
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
				return d.name;
			case 2:
				return Numbers.format(impactResult.getUpstreamTotal(d));
			case 3:
				return Numbers.format(impactResult.getDirectResult(d));
			case 4:
				return d.referenceUnit;
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

		private CategorizedDescriptor process;
		private FullResult result;

		public ResultProvider(FullResult result) {
			this.process = result.techIndex.getRefFlow().process;
			this.result = result;
		}

		public void setProcess(ProcessDescriptor process) {
			this.process = process;
		}

		private double getUpstreamContribution(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			double total = result.getTotalFlowResult(flow);
			if (total == 0)
				return 0;
			double val = result.getUpstreamFlowResult(process, flow);
			double c = val / Math.abs(total);
			return c > 1 ? 1 : c;
		}

		private double getDirectResult(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			return result.getDirectFlowResult(process, flow);
		}

		private double getUpstreamTotal(FlowDescriptor flow) {
			if (process == null || flow == null)
				return 0;
			return result.getUpstreamFlowResult(process, flow);
		}

		private double getUpstreamContribution(ImpactCategoryDescriptor d) {
			if (process == null || d == null)
				return 0;
			double total = result.getTotalImpactResult(d);
			if (total == 0)
				return 0;
			double val = result.getUpstreamImpactResult(process, d);
			double c = val / Math.abs(total);
			return c > 1 ? 1 : c;
		}

		private double getDirectResult(ImpactCategoryDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getDirectImpactResult(process, category);
		}

		private double getUpstreamTotal(ImpactCategoryDescriptor category) {
			if (process == null || category == null)
				return 0;
			return result.getUpstreamImpactResult(process, category);
		}
	}

}
