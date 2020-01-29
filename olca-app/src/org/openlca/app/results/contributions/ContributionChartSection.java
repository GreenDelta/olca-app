package org.openlca.app.results.contributions;

import java.util.ArrayList;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ResultFlowCombo;
import org.openlca.app.results.ImageExportAction;
import org.openlca.app.results.ResultEditor;
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

/**
 * Chart section of the first page in the analysis editor. Can contain flow or
 * impact category contributions.
 */
public class ContributionChartSection {

	private boolean forFlows = true;
	private String sectionTitle = "";
	private String selectionName = "";

	private final ContributionResult result;
	private AbstractViewer<?, TableComboViewer> itemViewer;
	private ContributionChart chart;

	public static ContributionChartSection forFlows(ResultEditor<?> editor) {
		ContributionChartSection s = new ContributionChartSection(editor, true);
		s.sectionTitle = M.DirectContributionsFlowResultsOverview;
		s.selectionName = M.Flow;
		return s;
	}

	public static ContributionChartSection forImpacts(ResultEditor<?> editor) {
		ContributionChartSection s = new ContributionChartSection(editor, false);
		s.sectionTitle = M.DirectContributionsImpactCategoryResultsOverview;
		s.selectionName = M.ImpactCategory;
		return s;
	}

	private ContributionChartSection(ResultEditor<?> editor, boolean forFlows) {
		this.result = editor.result;
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
			combo.setInput(result.getFlows());
			this.itemViewer = combo;
		} else {
			ImpactCategoryViewer combo = new ImpactCategoryViewer(comp);
			combo.setInput(result.getImpacts());
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
