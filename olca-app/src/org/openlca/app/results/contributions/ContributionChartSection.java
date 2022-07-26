package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ResultFlowCombo;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.ImageExportAction;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;

/**
 * Chart section of the first page in the analysis editor. Can contain flow or
 * impact category contributions.
 */
public class ContributionChartSection {

	private final LcaResult result;
	private final ResultItemOrder items;
	private final boolean forFlows;
	private String sectionTitle = "";
	private String selectionName = "";

	private AbstractViewer<?, TableComboViewer> itemViewer;
	private ContributionChart chart;

	public static ContributionChartSection forFlows(ResultEditor editor) {
		var s = new ContributionChartSection(editor, true);
		s.sectionTitle = M.DirectContributionsFlowResultsOverview;
		s.selectionName = M.Flow;
		return s;
	}

	public static ContributionChartSection forImpacts(ResultEditor editor) {
		var s = new ContributionChartSection(editor, false);
		s.sectionTitle = M.DirectContributionsImpactCategoryResultsOverview;
		s.selectionName = M.ImpactCategory;
		return s;
	}

	private ContributionChartSection(ResultEditor editor, boolean forFlows) {
		this.result = editor.result;
		this.items = editor.items;
		this.forFlows = forFlows;
	}

	public void render(Composite parent, FormToolkit tk) {
		Section section = UI.section(parent, tk, sectionTitle);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		Composite header = tk.createComposite(comp);
		UI.gridLayout(header, 2);
		createCombo(tk, header);
		chart = ContributionChart.create(comp, tk);
		Actions.bind(section, new ImageExportAction(comp));
		refresh();
	}

	private void createCombo(FormToolkit tk, Composite comp) {
		tk.createLabel(comp, selectionName);
		if (forFlows) {
			var combo = new ResultFlowCombo(comp);
			combo.setInput(items.enviFlows());
			this.itemViewer = combo;
		} else {
			var combo = new ImpactCategoryViewer(comp);
			combo.setInput(items.impacts());
			this.itemViewer = combo;
		}
		itemViewer.addSelectionChangedListener(_e -> refresh());
		itemViewer.selectFirst();
	}

	private void refresh() {
		if (chart == null)
			return;
		Object e = itemViewer.getSelected();
		String unit = null;
		List<Contribution<TechFlow>> cons = null;
		if (e instanceof EnviFlow flow) {
			unit = Labels.refUnit(flow);
			cons = result.getProcessContributions(flow);
		} else if (e instanceof ImpactDescriptor impact) {
			unit = impact.referenceUnit;
			cons = result.getProcessContributions(impact);
		}
		if (cons == null)
			return;
		chart.setData(new ArrayList<>(cons), unit);
	}
}
