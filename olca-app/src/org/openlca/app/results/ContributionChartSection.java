package org.openlca.app.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;

/**
 * Chart section of the first page in the analysis editor. Can contain flow or
 * impact category contributions.
 */
public class ContributionChartSection {

	private boolean forFlows = true;

	private int maxItems = 5;

	private String sectionTitle = "";
	private String selectionName = "";

	private ContributionResultProvider<?> provider;
	private AbstractViewer<?, TableComboViewer> itemViewer;
	private ContributionChart chart;

	public static ContributionChartSection forFlows(
			ContributionResultProvider<?> provider) {
		ContributionChartSection section = new ContributionChartSection(
				provider, true);
		section.sectionTitle = Messages.FlowContributions;
		section.selectionName = Messages.Flow;
		return section;
	}

	public static ContributionChartSection forImpacts(
			ContributionResultProvider<?> provider) {
		ContributionChartSection section = new ContributionChartSection(
				provider, false);
		section.sectionTitle = Messages.ImpactContributions;
		section.selectionName = Messages.ImpactCategory;
		return section;
	}

	private ContributionChartSection(ContributionResultProvider<?> provider,
			boolean forFlows) {
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
		Composite chartComposite = toolkit.createComposite(sectionClient);
		UI.gridData(chartComposite, true, false);
		chart = new ContributionChart(chartComposite, toolkit);
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
		FlowViewer viewer = new FlowViewer(header, Cache.getEntityCache());
		Set<FlowDescriptor> set = provider.getFlowDescriptors();
		FlowDescriptor[] flows = set.toArray(new FlowDescriptor[set.size()]);
		viewer.setInput(flows);
		viewer.addSelectionChangedListener((selection) -> refresh());
		this.itemViewer = viewer;
	}

	private void createImpactViewer(Composite header) {
		ImpactCategoryViewer viewer = new ImpactCategoryViewer(header);
		Set<ImpactCategoryDescriptor> set = provider.getImpactDescriptors();
		ImpactCategoryDescriptor[] impacts = set
				.toArray(new ImpactCategoryDescriptor[set.size()]);
		viewer.setInput(impacts);
		viewer.addSelectionChangedListener((selection) -> refresh());
		this.itemViewer = viewer;
	}

	private void refresh() {
		if (chart == null)
			return;
		Object selection = itemViewer.getSelected();
		String unit = null;
		ContributionSet<ProcessDescriptor> contributionSet = null;
		if (selection instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) selection;
			unit = Labels.getRefUnit(flow, provider.getCache());
			contributionSet = provider.getProcessContributions(flow);
		} else if (selection instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor impact = (ImpactCategoryDescriptor) selection;
			unit = impact.getReferenceUnit();
			contributionSet = provider.getProcessContributions(impact);
		}
		if (contributionSet == null)
			return;
		List<ContributionItem<ProcessDescriptor>> items = Contributions
				.topWithRest(contributionSet.getContributions(), maxItems);
		List<ContributionItem<?>> chartData = new ArrayList<>();
		chartData.addAll(items);
		chart.setData(chartData, unit);
	}
}
