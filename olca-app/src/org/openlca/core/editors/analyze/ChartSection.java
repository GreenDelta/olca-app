package org.openlca.core.editors.analyze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.editors.ContributionItem;
import org.openlca.core.editors.charts.ContributionChart;
import org.openlca.core.editors.io.ImageExportAction;

/**
 * Chart section of the first page in the analysis editor. Can contain flow or
 * impact category contributions.
 */
class ChartSection<T> {

	private int maxItems = 5;

	private String sectionTitle = "#no title";
	private String selectionName = "#no item name";
	private IProcessContributionProvider<T> provider;
	private AbstractViewer<T, TableComboViewer> itemViewer;
	private Combo modeCombo;
	private ContributionChart chart;

	public ChartSection(IProcessContributionProvider<T> provider) {
		this.provider = provider;
	}

	public void setSectionTitle(String sectionTitle) {
		this.sectionTitle = sectionTitle;
	}

	public void setSelectionName(String selectionName) {
		this.selectionName = selectionName;
	}

	public void render(Composite parent, FormToolkit toolkit) {
		Section section = UI.section(parent, toolkit, sectionTitle);
		Composite sectionClient = UI.sectionClient(section, toolkit);
		UI.gridLayout(sectionClient, 1);
		Composite header = toolkit.createComposite(sectionClient);
		UI.gridLayout(header, 4);
		createItemCombo(toolkit, header);
		createModeCombo(toolkit, header);
		Composite chartComposite = toolkit.createComposite(sectionClient);
		UI.gridData(chartComposite, true, false);
		chart = new ContributionChart(chartComposite, toolkit);
		Actions.bind(section, new ImageExportAction(sectionClient));
		refresh();
	}

	@SuppressWarnings("unchecked")
	private void createItemCombo(FormToolkit toolkit, Composite header) {
		toolkit.createLabel(header, selectionName);
		if (provider instanceof FlowContributionProvider) {
			FlowContributionProvider _p = (FlowContributionProvider) provider;
			FlowViewer itemViewer = new FlowViewer(header, Database.getCache());
			itemViewer.setInput(_p.getElements());
			this.itemViewer = (AbstractViewer<T, TableComboViewer>) itemViewer;
		} else if (provider instanceof ImpactContributionProvider) {
			ImpactContributionProvider _p = (ImpactContributionProvider) provider;
			ImpactCategoryViewer itemViewer = new ImpactCategoryViewer(header);
			itemViewer.setInput(_p.getElements());
			this.itemViewer = (AbstractViewer<T, TableComboViewer>) itemViewer;
		} else
			throw new IllegalStateException("Unknown contribution provider");
		itemViewer.selectFirst();
		itemViewer.addSelectionChangedListener(new ViewerSelectionChange<T>());
	}

	private void createModeCombo(FormToolkit toolkit, Composite header) {
		toolkit.createLabel(header, Messages.OrderBy);
		modeCombo = new Combo(header, SWT.NONE);
		toolkit.adapt(modeCombo);
		modeCombo.setItems(new String[] { Messages.HotSpots,
				Messages.TotalContributions });
		modeCombo.select(0);
		modeCombo.addSelectionListener(new ComboSelectionChange());
	}

	private void refresh() {
		T selection = itemViewer.getSelected();
		if (selection == null)
			return;
		boolean totals = modeCombo.getSelectionIndex() == 1;
		List<ProcessContributionItem> items = totals ? provider.getItems(
				selection, 0) : provider.getHotSpots(selection, 0);
		Collections.sort(items);
		List<ContributionItem> data = createContributions(items, totals);
		chart.setData(data);
	}

	private List<ContributionItem> createContributions(
			List<ProcessContributionItem> items, boolean totals) {
		List<ContributionItem> contributions = new ArrayList<>();
		ContributionItem restItem = null;
		for (int i = 0; i < items.size(); i++) {
			ProcessContributionItem item = items.get(i);
			double amount = totals ? item.getTotalAmount() : item
					.getSingleAmount();
			if (i < maxItems)
				addContribution(contributions, item, amount);
			else {
				if (restItem == null)
					restItem = initRest(contributions, item);
				restItem.setAmount(restItem.getAmount() + amount);
				restItem.setContribution(restItem.getContribution()
						+ item.getContribution());
			}
		}
		return contributions;
	}

	private void addContribution(List<ContributionItem> contributions,
			ProcessContributionItem item, double amount) {
		ContributionItem c = new ContributionItem();
		c.setAmount(amount);
		c.setContribution(item.getContribution());
		c.setLabel(item.getProcessName());
		c.setUnit(item.getUnit());
		contributions.add(c);
	}

	private ContributionItem initRest(List<ContributionItem> contributions,
			ProcessContributionItem item) {
		ContributionItem restItem;
		restItem = new ContributionItem();
		restItem.setLabel(Messages.Rest);
		restItem.setUnit(item.getUnit());
		restItem.setRest(true);
		contributions.add(restItem);
		return restItem;
	}

	private class ComboSelectionChange implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			refresh();
		}

	}

	private class ViewerSelectionChange<U> implements
			ISelectionChangedListener<U> {

		@Override
		public void selectionChanged(U selection) {
			refresh();
		}

	}

}
