package org.openlca.app.results.contributions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Controls;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.CostResults;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.CostResultViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionResultProvider;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;

/**
 * A section which shows the process contributions to flow or impact results of
 * an analysis result.
 */
public class ContributionTableSection {

	private ModelType type;

	private String sectionTitle = "";
	private String selectionName = "";
	private ContributionResultProvider<?> provider;
	private AbstractComboViewer<?> itemViewer;
	private ContributionTable table;
	private Spinner spinner;

	public static ContributionTableSection forFlows(
			ContributionResultProvider<?> provider) {
		ContributionTableSection section = new ContributionTableSection(
				provider, ModelType.FLOW);
		section.sectionTitle = M.FlowContributions;
		section.selectionName = M.Flow;
		return section;
	}

	public static ContributionTableSection forImpacts(
			ContributionResultProvider<?> provider) {
		ContributionTableSection section = new ContributionTableSection(
				provider, ModelType.IMPACT_CATEGORY);
		section.sectionTitle = M.ImpactContributions;
		section.selectionName = M.ImpactCategory;
		return section;
	}

	public static ContributionTableSection forCosts(
			ContributionResultProvider<?> provider) {
		ContributionTableSection section = new ContributionTableSection(
				provider, ModelType.CURRENCY);
		section.sectionTitle = M.CostsAddedValues;
		section.selectionName = M.Costs;
		return section;
	}

	private ContributionTableSection(ContributionResultProvider<?> provider,
			ModelType type) {
		this.provider = provider;
		this.type = type;
	}

	public void render(Composite parent, FormToolkit toolkit) {
		Section section = UI.section(parent, toolkit, sectionTitle);
		UI.gridData(section, true, true);
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);
		Composite header = toolkit.createComposite(composite);
		UI.gridData(header, true, false);
		UI.gridLayout(header, 5);
		createItemCombo(toolkit, header);
		createSpinner(toolkit, header);
		table = new ContributionTable(composite);
		UI.gridData(table.getTable(), true, true);
	}

	private void createItemCombo(FormToolkit toolkit, Composite header) {
		toolkit.createLabel(header, selectionName);
		switch (type) {
		case FLOW:
			createFlowViewer(header);
			break;
		case IMPACT_CATEGORY:
			createImpactViewer(header);
			break;
		case CURRENCY:
			createCostViewer(header);
			break;
		default:
			break;
		}
		itemViewer.selectFirst();
	}

	private void createFlowViewer(Composite header) {
		FlowViewer viewer = new FlowViewer(header, Cache.getEntityCache());
		Set<FlowDescriptor> set = provider.getFlowDescriptors();
		FlowDescriptor[] flows = set.toArray(new FlowDescriptor[set.size()]);
		viewer.setInput(flows);
		viewer.addSelectionChangedListener((selection) -> refreshValues());
		this.itemViewer = viewer;
	}

	private void createImpactViewer(Composite header) {
		ImpactCategoryViewer viewer = new ImpactCategoryViewer(header);
		Set<ImpactCategoryDescriptor> set = provider.getImpactDescriptors();
		ImpactCategoryDescriptor[] impacts = set
				.toArray(new ImpactCategoryDescriptor[set.size()]);
		viewer.setInput(impacts);
		viewer.addSelectionChangedListener((selection) -> refreshValues());
		this.itemViewer = viewer;
	}

	private void createCostViewer(Composite header) {
		CostResultViewer viewer = new CostResultViewer(header);
		CostResultDescriptor[] costs = CostResults.getDescriptors(provider)
				.toArray(new CostResultDescriptor[2]);
		viewer.setInput(costs);
		viewer.addSelectionChangedListener(selection -> refreshValues());
		this.itemViewer = viewer;
	}

	private void createSpinner(FormToolkit toolkit, Composite header) {
		toolkit.createLabel(header, M.Cutoff);
		spinner = new Spinner(header, SWT.BORDER);
		spinner.setValues(2, 0, 100, 0, 1, 10);
		toolkit.adapt(spinner);
		toolkit.createLabel(header, "%");
		Controls.onSelect(spinner, (e) -> refreshValues());
	}

	void refreshValues() {
		if (table == null)
			return;
		Object selected = itemViewer.getSelected();
		if (selected == null)
			return;
		String unit = null;
		List<ContributionItem<ProcessDescriptor>> items = null;
		if (selected instanceof FlowDescriptor) {
			FlowDescriptor flow = (FlowDescriptor) selected;
			unit = Labels.getRefUnit(flow, provider.cache);
			items = provider.getProcessContributions(flow).contributions;
		} else if (selected instanceof ImpactCategoryDescriptor) {
			ImpactCategoryDescriptor impact = (ImpactCategoryDescriptor) selected;
			unit = impact.getReferenceUnit();
			items = provider.getProcessContributions(impact).contributions;
		} else if (selected instanceof CostResultDescriptor) {
			CostResultDescriptor cost = (CostResultDescriptor) selected;
			unit = Labels.getReferenceCurrencyCode();
			ContributionSet<ProcessDescriptor> set = provider
					.getProcessCostContributions();
			if (cost.forAddedValue)
				CostResults.forAddedValues(set);
			items = set.contributions;
		}
		setTableData(items, unit);
	}

	private void setTableData(List<ContributionItem<ProcessDescriptor>> items,
			String unit) {
		if (items == null)
			return;
		double cutOff = spinner.getSelection() / 100.0;
		Contributions.sortDescending(items);
		List<ContributionItem<?>> tableData = new ArrayList<>();
		for (ContributionItem<?> item : items) {
			if (Math.abs(cutOff) < 1e-5 || Math.abs(item.share) >= cutOff)
				tableData.add(item);
		}
		table.setInput(tableData, unit);
	}
}
