package org.openlca.app.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.GroupingContribution;
import org.openlca.core.results.ProcessGrouping;

class GroupResultSection {

	private final int FLOW = 0;
	private final int IMPACT = 1;
	private int resultType = 0;

	private EntityCache cache = Cache.getEntityCache();
	private List<ProcessGrouping> groups;
	private ContributionResultProvider<?> result;
	private FlowViewer flowViewer;
	private ImpactCategoryViewer impactViewer;
	private ContributionChart chart;
	private GroupResultTable table;

	public GroupResultSection(List<ProcessGrouping> groups,
			ContributionResultProvider<?> result) {
		this.groups = groups;
		this.result = result;
	}

	public void update() {
		Object selection;
		String unit;
		if (resultType == FLOW) {
			FlowDescriptor flow = flowViewer.getSelected();
			unit = Labels.getRefUnit(flow, result.getCache());
			selection = flow;
		} else {
			ImpactCategoryDescriptor impact = impactViewer.getSelected();
			unit = impact.getReferenceUnit();
			selection = impact;
		}
		updateResults(selection, unit);
	}

	private void updateResults(Object selection, String unit) {
		if (selection != null && table != null) {
			List<ContributionItem<ProcessGrouping>> items = calculate(selection);
			Contributions.sortDescending(items);
			table.setInput(items, unit);
			List<ContributionItem<?>> chartData = new ArrayList<>();
			chartData.addAll(items);
			chart.setData(chartData, unit);
		}
	}

	private List<ContributionItem<ProcessGrouping>> calculate(Object selection) {
		GroupingContribution calculator = new GroupingContribution(result,
				groups);
		if (selection instanceof FlowDescriptor)
			return calculator.calculate((FlowDescriptor) selection)
					.getContributions();
		if (selection instanceof ImpactCategoryDescriptor)
			return calculator.calculate((ImpactCategoryDescriptor) selection)
					.getContributions();
		return Collections.emptyList();
	}

	public void render(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit, Messages.Results);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		UI.gridLayout(client, 1);
		createCombos(toolkit, client);
		table = new GroupResultTable(client);
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
		Button flowsCheck = toolkit.createButton(parent, Messages.Flows,
				SWT.RADIO);
		flowsCheck.setSelection(true);
		flowViewer = new FlowViewer(parent, cache);
		Set<FlowDescriptor> flows = result.getFlowDescriptors();
		flowViewer.setInput(flows.toArray(new FlowDescriptor[flows.size()]));
		flowViewer.addSelectionChangedListener((e) -> update());
		if (flows.size() > 0)
			flowViewer.select(flows.iterator().next());
		new ResultTypeCheck(flowViewer, flowsCheck, FLOW);
	}

	private void createImpact(FormToolkit toolkit, Composite parent) {
		Button impactCheck = toolkit.createButton(parent,
				Messages.ImpactCategories, SWT.RADIO);
		impactViewer = new ImpactCategoryViewer(parent);
		impactViewer.setEnabled(false);
		Set<ImpactCategoryDescriptor> impacts = result.getImpactDescriptors();
		impactViewer.setInput(impacts);
		impactViewer.addSelectionChangedListener((e) -> update());
		if (impacts.size() > 0)
			impactViewer.select(impacts.iterator().next());
		new ResultTypeCheck(impactViewer, impactCheck, IMPACT);
	}

	private class ResultTypeCheck implements SelectionListener {

		private AbstractComboViewer<?> viewer;
		private Button check;
		private int type;

		public ResultTypeCheck(AbstractComboViewer<?> viewer, Button check,
				int type) {
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
