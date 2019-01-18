package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.results.ImageExportAction;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.ContributionItem;
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

	private ContributionResult provider;
	private AbstractViewer<?, TableComboViewer> itemViewer;
	private ContributionChart chart;

	public static ContributionChartSection forFlows(ContributionResult provider) {
		ContributionChartSection section = new ContributionChartSection(provider, true);
		section.sectionTitle = M.DirectContributionsFlowResultsOverview;
		section.selectionName = M.Flow;
		return section;
	}

	public static ContributionChartSection forImpacts(ContributionResult provider) {
		ContributionChartSection section = new ContributionChartSection(provider, false);
		section.sectionTitle = M.DirectContributionsImpactCategoryResultsOverview;
		section.selectionName = M.ImpactCategory;
		return section;
	}

	private ContributionChartSection(ContributionResult provider, boolean forFlows) {
		this.provider = provider;
		this.forFlows = forFlows;
	}

	public void render(Composite parent, FormToolkit toolkit) {
		Section section = UI.section(parent, toolkit, sectionTitle);
		Composite sectionClient = UI.sectionClient(section, toolkit);
		UI.gridLayout(sectionClient, 1);
		Composite header = toolkit.createComposite(sectionClient);
		UI.gridLayout(header, 2);
		createItemCombo(toolkit, header);
		chart = ContributionChart.create(sectionClient, toolkit);
		Actions.bind(section, new ImageExportAction(sectionClient));
		refresh();
	}

	private void createItemCombo(FormToolkit toolkit, Composite header) {
		toolkit.createLabel(header, selectionName);
		if (forFlows)
			createFlowViewer(header);
		else
			createImpactViewer(header);
		itemViewer.selectFirst();
	}

	private void createFlowViewer(Composite header) {
		FlowViewer viewer = new FlowViewer(header);
		Set<FlowDescriptor> set = provider.getFlows();
		FlowDescriptor[] flows = set.toArray(new FlowDescriptor[set.size()]);
		viewer.setInput(flows);
		viewer.addSelectionChangedListener((selection) -> refresh());
		this.itemViewer = viewer;
	}

	private void createImpactViewer(Composite header) {
		ImpactCategoryViewer viewer = new ImpactCategoryViewer(header);
		Set<ImpactCategoryDescriptor> set = provider.getImpacts();
		ImpactCategoryDescriptor[] impacts = set.toArray(new ImpactCategoryDescriptor[set.size()]);
		viewer.setInput(impacts);
		viewer.addSelectionChangedListener((selection) -> refresh());
		this.itemViewer = viewer;
	}

	private void refresh() {
		if (chart == null)
			return;
		Object selection = itemViewer.getSelected();
		String unit = null;
		ContributionSet<CategorizedDescriptor> contributionSet = null;
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) selection;
			unit = Labels.getRefUnit(flow);
			contributionSet = provider.getProcessContributions(flow);
		} else if (selection instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor impact = (ImpactCategoryDescriptor) selection;
			unit = impact.referenceUnit;
			contributionSet = provider.getProcessContributions(impact);
		}
		if (contributionSet == null)
			return;
		chart.setData(new ArrayList<ContributionItem<?>>(contributionSet.contributions), unit);
	}
}
