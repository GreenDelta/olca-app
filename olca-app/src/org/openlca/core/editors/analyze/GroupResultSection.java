package org.openlca.core.editors.analyze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.app.viewer.AbstractViewer;
import org.openlca.app.viewer.FlowViewer;
import org.openlca.app.viewer.ISelectionChangedListener;
import org.openlca.app.viewer.ImpactCategoryViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.ContributionItem;
import org.openlca.core.editors.charts.ContributionChart;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.core.model.results.Contribution;
import org.openlca.core.model.results.GroupingContribution;
import org.openlca.core.model.results.ProcessGrouping;

class GroupResultSection {

	private int FLOW = 0;
	private int IMPACT = 1;
	private int resultType = 0;

	private List<ProcessGrouping> groups;
	private AnalysisResult result;
	private TableViewer tableViewer;
	private FlowViewer flowViewer;
	private ImpactCategoryViewer impactViewer;
	private ContributionChart chart;
	private IDatabase database;

	public GroupResultSection(List<ProcessGrouping> groups,
			AnalysisResult result, IDatabase database) {
		this.groups = groups;
		this.result = result;
		this.database = database;
	}

	public void update() {
		Object selection = null;
		if (resultType == FLOW)
			selection = flowViewer.getSelected();
		else
			selection = impactViewer.getSelected();
		if (selection != null && tableViewer != null) {
			List<Contribution<ProcessGrouping>> items = calculate(selection);
			tableViewer.setInput(items);
			List<ContributionItem> chartData = createChartData(items);
			chart.setData(chartData);
		}
	}

	private List<Contribution<ProcessGrouping>> calculate(Object selection) {
		GroupingContribution calculator = new GroupingContribution(result,
				groups);
		if (selection instanceof Flow)
			return calculator.calculate((Flow) selection).getContributions();
		if (selection instanceof ImpactCategoryDescriptor)
			return calculator.calculate((ImpactCategoryDescriptor) selection)
					.getContributions();
		return Collections.emptyList();
	}

	private List<ContributionItem> createChartData(
			List<Contribution<ProcessGrouping>> items) {
		List<ContributionItem> data = new ArrayList<>();
		for (Contribution<ProcessGrouping> item : items) {
			ContributionItem dataItem = new ContributionItem();
			dataItem.setAmount(item.getAmount());
			dataItem.setContribution(item.getShare());
			dataItem.setLabel(item.getItem().getName());
			dataItem.setRest(item.getItem().isRest());
			data.add(dataItem);
			// dataItem.setUnit(); TODO: units in chart
		}
		return data;
	}

	public void render(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit, Messages.Common_Results);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		UI.gridLayout(client, 1);
		createCombos(toolkit, client);
		GroupResultTable table = new GroupResultTable(client);
		tableViewer = table.getViewer();
		UI.gridData(tableViewer.getControl(), true, false).heightHint = 200;
		createChartSection(client, toolkit);
		update();
	}

	private void createChartSection(Composite parent, FormToolkit toolkit) {
		Composite composite = toolkit.createComposite(parent);
		UI.gridData(composite, true, true);
		chart = new ContributionChart(composite, toolkit);
	}

	private void createCombos(FormToolkit toolkit, Composite client) {
		Composite composite = toolkit.createComposite(client);
		UI.gridData(composite, true, false);
		UI.gridLayout(composite, 2);
		createFlowViewer(toolkit, composite);
		if (result.hasImpactResults())
			createImpact(toolkit, composite);
	}

	private void createFlowViewer(FormToolkit toolkit, Composite parent) {
		Button flowsCheck = toolkit.createButton(parent, Messages.Common_Flows,
				SWT.RADIO);
		flowsCheck.setSelection(true);
		flowViewer = new FlowViewer(parent);
		flowViewer.setInput(result);
		flowViewer.addSelectionChangedListener(new SelectionChange<Flow>());
		if (result.getFlowIndex().getFlows().length > 0)
			flowViewer.select(result.getFlowIndex().getFlows()[0]);
		new ResultTypeCheck(flowViewer, flowsCheck, FLOW);
	}

	private void createImpact(FormToolkit toolkit, Composite parent) {
		Button impactCheck = toolkit.createButton(parent,
				Messages.Common_ImpactCategories, SWT.RADIO);
		impactViewer = new ImpactCategoryViewer(parent);
		impactViewer.setEnabled(false);
		impactViewer.setInput(result);
		impactViewer
				.addSelectionChangedListener(new SelectionChange<ImpactCategoryDescriptor>());
		if (result.getImpactCategories().length > 0)
			impactViewer.select(result.getImpactCategories()[0]);
		new ResultTypeCheck(impactViewer, impactCheck, IMPACT);
	}

	private class SelectionChange<T> implements ISelectionChangedListener<T> {

		@Override
		public void selectionChanged(T value) {
			update();
		}
	}

	private class ResultTypeCheck implements SelectionListener {

		private AbstractViewer<?> viewer;
		private Button check;
		private int type;

		public ResultTypeCheck(AbstractViewer<?> viewer, Button check, int type) {
			this.viewer = viewer;
			this.check = check;
			this.type = type;
			check.addSelectionListener(this);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (check.getSelection()) {
				viewer.setEnabled(true);
				resultType = this.type;
				update();
			} else
				viewer.setEnabled(false);
		}
	}
}
