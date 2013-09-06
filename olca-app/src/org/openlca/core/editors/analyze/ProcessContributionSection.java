package org.openlca.core.editors.analyze;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.editors.io.ui.FileChooser;
import org.openlca.core.model.descriptors.FlowDescriptor;

/**
 * A section which shows the process contributions to flow or impact results of
 * an analysis result.
 */
class ProcessContributionSection<T> {

	private EntityCache cache = Database.getCache();
	private String sectionTitle = "#no title";
	private String selectionName = "#no item name";
	private IProcessContributionProvider<T> provider;
	private AbstractComboViewer<T> itemViewer;
	private ProcessContributionViewer viewer;
	private Spinner spinner;
	private Combo modeCombo;

	public ProcessContributionSection(IProcessContributionProvider<T> provider) {
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
		UI.gridData(section, true, true);
		Actions.bind(section, new ExportAction());
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		UI.gridLayout(composite, 1);

		Composite header = toolkit.createComposite(composite);
		UI.gridData(header, true, false);
		UI.gridLayout(header, 7);
		ComboSelectionChange selectionChange = new ComboSelectionChange();
		createItemCombo(toolkit, header);
		createModeCombo(toolkit, header, selectionChange);
		createSpinner(toolkit, header, selectionChange);
		createTable(toolkit, composite);
	}

	@SuppressWarnings("unchecked")
	private void createItemCombo(FormToolkit toolkit, Composite header) {
		toolkit.createLabel(header, selectionName);
		if (provider instanceof FlowContributionProvider) {
			FlowViewer itemViewer = new FlowViewer(header, cache);
			Set<FlowDescriptor> flows = provider.getAnalysisResult()
					.getFlowResults().getFlows(cache);
			itemViewer
					.setInput(flows.toArray(new FlowDescriptor[flows.size()]));
			this.itemViewer = (AbstractComboViewer<T>) itemViewer;
		} else if (provider instanceof ImpactContributionProvider) {
			ImpactCategoryViewer itemViewer = new ImpactCategoryViewer(header);
			itemViewer.setInput(provider.getAnalysisResult().getImpactResults()
					.getImpacts(cache));
			this.itemViewer = (AbstractComboViewer<T>) itemViewer;
		} else
			throw new IllegalStateException("Unknown contribution provider");
		itemViewer.selectFirst();
		itemViewer.addSelectionChangedListener(new ViewerSelectionChange<T>());
	}

	private void createModeCombo(FormToolkit toolkit, Composite header,
			ComboSelectionChange selectionChange) {
		toolkit.createLabel(header, Messages.OrderBy);
		modeCombo = new Combo(header, SWT.NONE);
		toolkit.adapt(modeCombo);
		modeCombo.setItems(new String[] { Messages.TotalContributions,
				Messages.HotSpots });
		modeCombo.select(0);
		modeCombo.addSelectionListener(selectionChange);
	}

	private void createSpinner(FormToolkit toolkit, Composite header,
			ComboSelectionChange selectionChange) {
		toolkit.createLabel(header, Messages.CutOff);
		spinner = new Spinner(header, SWT.BORDER);
		spinner.setValues(2, 0, 100, 0, 1, 10);
		toolkit.adapt(spinner);
		toolkit.createLabel(header, "%");
		spinner.addSelectionListener(selectionChange);
	}

	private void createTable(FormToolkit toolkit, Composite composite) {
		viewer = new ProcessContributionViewer(composite);
		UI.gridData(viewer.getTable(), true, true);
	}

	void refreshValues() {
		T item = itemViewer.getSelected();
		if (item == null)
			return;
		double cutOff = spinner.getSelection() / 100.0;
		List<ProcessContributionItem> items = null;
		if (modeCombo.getSelectionIndex() == 0)
			items = provider.getItems(item, cutOff);
		else
			items = provider.getHotSpots(item, cutOff);
		Collections.sort(items);
		viewer.setInput(items);
	}

	private class ViewerSelectionChange<U> implements
			ISelectionChangedListener<U> {

		@Override
		public void selectionChanged(U selection) {
			refreshValues();
		}
	}

	private class ComboSelectionChange implements SelectionListener {

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			refreshValues();
		}
	}

	private class ExportAction extends Action {
		public ExportAction() {
			setToolTipText(Messages.ExportToExcel);
			setImageDescriptor(ImageType.EXCEL_ICON.getDescriptor());
		}

		@Override
		@SuppressWarnings("unchecked")
		public void run() {
			File file = FileChooser.forExport(".xls", sectionTitle + ".xls");
			if (file == null)
				return;
			ContributionExportData data = new ContributionExportData();
			data.setCutoff(spinner.getSelection());
			data.setFile(file);
			data.setItemName(selectionName);
			data.setItems((List<ProcessContributionItem>) viewer.getInput());
			data.setOrderType(modeCombo.getText());
			data.setSelectedItem(itemViewer.getSelected().toString());
			data.setTitle(sectionTitle);
			ContributionExport export = new ContributionExport(data);
			App.run("Export of process contributions", export);
		}
	}

}
