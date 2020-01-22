package org.openlca.app.results.contributions;

import java.util.ArrayList;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ResultFlowCombo;
import org.openlca.app.results.ImageExportAction;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.matrix.IndexFlow;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionResult;
import org.openlca.core.results.ContributionSet;
import org.openlca.util.Strings;

/**
 * Chart section of the first page in the analysis editor. Can contain flow or
 * impact category contributions.
 */
public class ContributionChartSection {

	private boolean forFlows = true;
	private String sectionTitle = "";
	private String selectionName = "";

	private ContributionResult result;
	private AbstractViewer<?, TableComboViewer> itemViewer;
	private ContributionChart chart;

	public static ContributionChartSection forFlows(ContributionResult r) {
		ContributionChartSection s = new ContributionChartSection(r, true);
		s.sectionTitle = M.DirectContributionsFlowResultsOverview;
		s.selectionName = M.Flow;
		return s;
	}

	public static ContributionChartSection forImpacts(ContributionResult r) {
		ContributionChartSection s = new ContributionChartSection(r, false);
		s.sectionTitle = M.DirectContributionsImpactCategoryResultsOverview;
		s.selectionName = M.ImpactCategory;
		return s;
	}

	private ContributionChartSection(ContributionResult r, boolean forFlows) {
		this.result = r;
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
			ResultFlowCombo combo = new ResultFlowCombo(comp);
			IndexFlow[] flows = result.getFlows().stream()
					.sorted((f1, f2) -> {
						if (f1.flow == null || f2.flow == null)
							return 0;
						return Strings.compare(f1.flow.name, f2.flow.name);
					}).toArray(IndexFlow[]::new);
			combo.setInput(flows);
			this.itemViewer = combo;

		} else {
			ImpactCategoryViewer combo = new ImpactCategoryViewer(comp);
			ImpactCategoryDescriptor[] impacts = result.getImpacts().stream()
					.sorted((i1, i2) -> Strings.compare(i1.name, i2.name))
					.toArray(ImpactCategoryDescriptor[]::new);
			combo.setInput(impacts);
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
		ContributionSet<CategorizedDescriptor> cons = null;
		if (e instanceof IndexFlow) {
			IndexFlow flow = (IndexFlow) e;
			unit = Labels.getRefUnit(flow.flow);
			cons = result.getProcessContributions(flow);
		} else if (e instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor impact = (ImpactCategoryDescriptor) e;
			unit = impact.referenceUnit;
			cons = result.getProcessContributions(impact);
		}
		if (cons == null)
			return;
		chart.setData(new ArrayList<>(cons.contributions), unit);
	}
}
