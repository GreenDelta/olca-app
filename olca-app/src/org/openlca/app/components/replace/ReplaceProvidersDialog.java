package org.openlca.app.components.replace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.util.Info;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.BaseNameComparator;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ReplaceProvidersDialog extends FormDialog {

	private List<ProcessDescriptor> usedInExchanges;
	private List<ProcessDescriptor> replacementCandidates;
	private ComboViewer processViewer;
	private ComboViewer productViewer;
	private ComboViewer replacementViewer;

	public static void openDialog() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		new ReplaceProvidersDialog().open();
	}

	public ReplaceProvidersDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.BulkreplaceProviders);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		FormToolkit toolkit = mForm.getToolkit();
		Composite body = UI.formBody(mForm.getForm(), toolkit);
		UI.gridLayout(body, 2, 20, 20);
		usedInExchanges = getUsedInExchanges();
		processViewer = createProcessViewer(body, toolkit);
		productViewer = createProductViewer(body, toolkit);
		replacementViewer = createReplacementViewer(body, toolkit);
	}

	private ComboViewer createProcessViewer(Composite parent, FormToolkit toolkit) {
		UI.formLabel(parent, toolkit, M.ReplaceProvider);
		ComboViewer viewer = new ComboViewer(new CCombo(parent, SWT.DROP_DOWN));
		decorateViewer(viewer);
		viewer.setInput(usedInExchanges);
		NameFilter filter = new NameFilter();
		viewer.addFilter(filter);
		UI.gridData(viewer.getCCombo(), true, false).widthHint = 300;
		viewer.getCCombo().addKeyListener(new FilterOnKey(viewer, filter, () -> usedInExchanges));
		viewer.addSelectionChangedListener((e) -> {
			ProcessDescriptor selected = Viewers.getFirstSelected(viewer);
			if (selected == null || selected.getId() == 0l) {
				productViewer.setInput(new ArrayList<>());
			} else {
				productViewer.setInput(getProductOutputs(selected));
			}
			replacementCandidates = new ArrayList<>();
			replacementViewer.setInput(replacementCandidates);
			updateButtons();
		});
		return viewer;
	}

	private ComboViewer createProductViewer(Composite parent, FormToolkit toolkit) {
		UI.formLabel(parent, toolkit, M.OfProduct);
		ComboViewer viewer = new ComboViewer(new Combo(parent, SWT.NONE));
		decorateViewer(viewer);
		viewer.setLabelProvider(new BaseLabelProvider());
		UI.gridData(viewer.getCombo(), true, false).widthHint = 300;
		viewer.addSelectionChangedListener((e) -> {
			ProcessDescriptor process = Viewers.getFirstSelected(processViewer);
			FlowDescriptor product = Viewers.getFirstSelected(productViewer);
			if (process == null || process.getId() == 0l || product == null) {
				replacementCandidates = new ArrayList<>();
			} else {
				replacementCandidates = getProviders(product);
			}
			replacementViewer.setInput(replacementCandidates);
			updateButtons();
		});
		return viewer;
	}

	private ComboViewer createReplacementViewer(Composite parent, FormToolkit toolkit) {
		UI.formLabel(parent, toolkit, M.With);
		ComboViewer viewer = new ComboViewer(new CCombo(parent, SWT.NONE));
		decorateViewer(viewer);
		NameFilter filter = new NameFilter();
		viewer.addFilter(filter);
		UI.gridData(viewer.getCCombo(), true, false).widthHint = 300;
		viewer.getCCombo().addKeyListener(new FilterOnKey(viewer, filter, () -> replacementCandidates));
		viewer.addSelectionChangedListener((e) -> {
			updateButtons();
		});
		return viewer;
	}

	private void updateButtons() {
		ProcessDescriptor first = Viewers.getFirstSelected(processViewer);
		FlowDescriptor second = Viewers.getFirstSelected(productViewer);
		boolean enabled = first != null && first.getId() != 0l && second != null && second.getId() != 0l;
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	private void decorateViewer(ComboViewer viewer) {
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setComparator(new BaseNameComparator());
	}

	private List<ProcessDescriptor> getUsedInExchanges() {
		ProcessDao dao = new ProcessDao(Database.get());
		Set<Long> ids = dao.getUsed();
		List<ProcessDescriptor> result = new ArrayList<>();
		result.add(new ProcessDescriptor());
		result.addAll(dao.getDescriptors(ids));
		return result;
	}

	private List<FlowDescriptor> getProductOutputs(ProcessDescriptor process) {
		if (process == null || process.getId() == 0l)
			return Collections.emptyList();
		ProcessDao dao = new ProcessDao(Database.get());
		List<FlowDescriptor> products = dao.getTechnologyOutputs(process);
		for (FlowDescriptor flow : new ArrayList<>(products))
			if (flow.getFlowType() != FlowType.PRODUCT_FLOW)
				products.remove(flow);
		return products;
	}

	private List<ProcessDescriptor> getProviders(FlowDescriptor product) {
		List<ProcessDescriptor> result = new ArrayList<>();
		// TODO: search for processes and waste flows
		FlowDao flowDao = new FlowDao(Database.get());
		Set<Long> ids = flowDao.getWhereOutput(product.getId());
		result.add(new ProcessDescriptor());
		ProcessDao processDao = new ProcessDao(Database.get());
		result.addAll(processDao.getDescriptors(ids));
		return result;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	@Override
	protected void okPressed() {
		ProcessDescriptor oldProcess = Viewers.getFirstSelected(processViewer);
		FlowDescriptor product = Viewers.getFirstSelected(productViewer);
		ProcessDescriptor newProcess = Viewers.getFirstSelected(replacementViewer);
		ProcessDao dao = new ProcessDao(Database.get());
		dao.replace(oldProcess.getId(), product.getId(), newProcess != null ? newProcess.getId() : null);
		Database.get().getEntityFactory().getCache().evictAll();
		super.okPressed();
	}

	private class LabelProvider extends BaseLabelProvider {

		private Category getCategory(ProcessDescriptor process) {
			if (process == null || process.getCategory() == null)
				return null;
			return Cache.getEntityCache().get(Category.class, process.getCategory());
		}

		private String getCategoryText(ProcessDescriptor process) {
			Category category = getCategory(process);
			if (category == null)
				return "";
			String text = "";
			while (category.getCategory() != null) {
				text = category.getCategory().getName() + "/" + text;
				category = category.getCategory();
			}
			return text;
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof ProcessDescriptor))
				return null;
			ProcessDescriptor process = (ProcessDescriptor) element;
			String processText = Labels.getDisplayName(process);
			if (processText == null)
				processText = "";
			String categoryText = getCategoryText(process);
			return categoryText + processText;

		}

	}

}
