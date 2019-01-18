package org.openlca.app.components.replace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ProcessViewer;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ReplaceProvidersDialog extends FormDialog {

	private ProcessViewer processViewer;
	private FlowViewer productViewer;
	private ProcessViewer replacementViewer;

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
		newShell.setSize(800, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		FormToolkit toolkit = mForm.getToolkit();
		Composite body = UI.formBody(mForm.getForm(), toolkit);
		UI.gridLayout(body, 2, 20, 20);
		UI.gridData(body, true, false);
		processViewer = createProcessViewer(body, toolkit, M.ReplaceProvider, this::updateProducts);
		productViewer = createFlowViewer(body, toolkit, M.OfProduct, this::updateReplacementCandidates);
		productViewer.setEnabled(false);
		replacementViewer = createProcessViewer(body, toolkit, M.With, selected -> updateButtons());
		replacementViewer.setEnabled(false);
		processViewer.setInput(getUsedInExchanges());
	}

	private ProcessViewer createProcessViewer(Composite parent, FormToolkit toolkit, String label,
			Consumer<ProcessDescriptor> onChange) {
		UI.formLabel(parent, toolkit, label);
		ProcessViewer viewer = new ProcessViewer(parent, Cache.getEntityCache());
		viewer.addSelectionChangedListener(onChange);
		return viewer;
	}

	private FlowViewer createFlowViewer(Composite parent, FormToolkit toolkit, String label,
			Consumer<FlowDescriptor> onChange) {
		UI.formLabel(parent, toolkit, label);
		FlowViewer viewer = new FlowViewer(parent);
		viewer.addSelectionChangedListener(onChange);
		return viewer;
	}

	private void updateProducts(ProcessDescriptor selected) {
		List<FlowDescriptor> outputs = getProductOutputs(selected);
		productViewer.setInput(outputs);
		replacementViewer.setInput(Collections.emptyList());
		productViewer.setEnabled(outputs.size() > 1);
		if (outputs.size() == 1) {
			productViewer.select(outputs.get(0));
		}
		updateButtons();
	}

	private void updateReplacementCandidates(FlowDescriptor product) {
		List<ProcessDescriptor> providers = getProviders(product);
		replacementViewer.setInput(providers);
		replacementViewer.setEnabled(providers.size() > 1);
		if (providers.size() == 1) {
			replacementViewer.select(providers.get(0));
		}
		updateButtons();
	}

	private void updateButtons() {
		ProcessDescriptor first = processViewer.getSelected();
		FlowDescriptor second = productViewer.getSelected();
		boolean enabled = first != null
				&& first.id != 0L
				&& second != null
				&& second.id != 0L;
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
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
		if (process == null || process.id == 0L)
			return Collections.emptyList();
		ProcessDao dao = new ProcessDao(Database.get());
		List<FlowDescriptor> products = dao.getTechnologyOutputs(process);
		for (FlowDescriptor flow : new ArrayList<>(products))
			if (flow.flowType != FlowType.PRODUCT_FLOW)
				products.remove(flow);
		return products;
	}

	private List<ProcessDescriptor> getProviders(FlowDescriptor product) {
		List<ProcessDescriptor> result = new ArrayList<>();
		// TODO: search for processes and waste flows
		FlowDao flowDao = new FlowDao(Database.get());
		Set<Long> ids = flowDao.getWhereOutput(product.id);
		ProcessDao processDao = new ProcessDao(Database.get());
		result.addAll(processDao.getDescriptors(ids));
		result.remove(processViewer.getSelected());
		return result;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	@Override
	protected void okPressed() {
		ProcessDescriptor oldProcess = processViewer.getSelected();
		FlowDescriptor product = productViewer.getSelected();
		ProcessDescriptor newProcess = replacementViewer.getSelected();
		ProcessDao dao = new ProcessDao(Database.get());
		dao.replace(oldProcess.id, product.id, newProcess != null ? newProcess.id : null);
		Database.get().getEntityFactory().getCache().evictAll();
		super.okPressed();
	}

}
