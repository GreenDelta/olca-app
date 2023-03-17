package org.openlca.app.results.grouping;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ResultFlowCombo;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.contributions.ContributionChart;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.GroupingContribution;
import org.openlca.core.results.ProcessGrouping;

class GroupResultSection {

	private final int FLOW = 0;
	private final int IMPACT = 1;
	private int resultType = 0;

	private final List<ProcessGrouping> groups;
	private final ResultEditor editor;

	private ResultFlowCombo flowViewer;
	private ImpactCategoryViewer impactViewer;
	private GroupResultTable table;
	private ContributionChart chart;

	public GroupResultSection(List<ProcessGrouping> groups, ResultEditor editor) {
		this.groups = groups;
		this.editor = editor;
	}

	public void update() {
		Object selection;
		String unit;
		if (resultType == FLOW) {
			var flow = flowViewer.getSelected();
			unit = Labels.refUnit(flow);
			selection = flow;
		} else {
			var impact = impactViewer.getSelected();
			unit = impact.referenceUnit;
			selection = impact;
		}

		if (selection == null)
			return;
		var items = calculate(selection);
		Contributions.sortDescending(items);

		if (table != null) {
			table.setInput(items, unit);
		}
		if (chart != null) {
			chart.setData(items, unit);
		}
	}

	private List<Contribution<ProcessGrouping>> calculate(Object o) {
		var calc = new GroupingContribution(editor.result, groups);
		if (o instanceof EnviFlow)
			return calc.calculate((EnviFlow) o);
		if (o instanceof ImpactDescriptor)
			return calc.calculate((ImpactDescriptor) o);
		return Collections.emptyList();
	}

	public void render(Composite parent, FormToolkit tk) {
		Section section = UI.section(parent, tk, M.Results);
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		createCombos(tk, comp);
		table = new GroupResultTable(comp);
		chart = ContributionChart.create(comp, tk);
		chart.setLabel(new BaseLabelProvider() {
			@Override
			public String getText(Object obj) {
				return ((ProcessGrouping) obj).name;
			}
		});
		update();
	}

	private void createCombos(FormToolkit toolkit, Composite client) {
		Composite composite = UI.composite(client, toolkit);
		UI.gridData(composite, true, false);
		UI.gridLayout(composite, 2);
		createFlowViewer(toolkit, composite);
		if (editor.result.hasImpacts())
			createImpact(toolkit, composite);
	}

	private void createFlowViewer(FormToolkit toolkit, Composite parent) {
		Button flowsCheck = toolkit.createButton(parent, M.Flows, SWT.RADIO);
		flowsCheck.setSelection(true);
		flowViewer = new ResultFlowCombo(parent);
		var flows = editor.items.enviFlows();
		flowViewer.setInput(flows);
		flowViewer.addSelectionChangedListener(e -> update());
		if (flows.size() > 0) {
			flowViewer.select(flows.get(0));
		}
		new ResultTypeCheck(flowViewer, flowsCheck, FLOW);
	}

	private void createImpact(FormToolkit toolkit, Composite parent) {
		Button impactCheck = toolkit.createButton(parent, M.ImpactCategories, SWT.RADIO);
		impactViewer = new ImpactCategoryViewer(parent);
		impactViewer.setEnabled(false);
		var impacts = editor.items.impacts();
		impactViewer.setInput(impacts);
		impactViewer.addSelectionChangedListener((e) -> update());
		if (impacts.size() > 0) {
			impactViewer.select(impacts.get(0));
		}
		new ResultTypeCheck(impactViewer, impactCheck, IMPACT);
	}

	private class ResultTypeCheck implements SelectionListener {

		private final AbstractComboViewer<?> viewer;
		private final Button check;
		private final int type;

		public ResultTypeCheck(AbstractComboViewer<?> viewer, Button check, int type) {
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
