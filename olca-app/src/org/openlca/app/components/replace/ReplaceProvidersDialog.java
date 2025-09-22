package org.openlca.app.components.replace;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ProcessCombo;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.ProviderReplacer;
import org.openlca.util.Strings;

public class ReplaceProvidersDialog extends FormDialog {

	private final ProviderReplacer replacer;
	private final List<ProcessDescriptor> usedProviders;

	private ProcessCombo sourceCombo;
	private FlowViewer flowCombo;
	private ProcessCombo targetCombo;
	private Button excludeDataPackageDatasets;

	public static void openDialog() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}

		record Cons(
				ProviderReplacer replacer,
				List<ProcessDescriptor> providers) {
		}
		var cons = App.exec("Collect used providers...", () -> {
			var replacer = ProviderReplacer.of(db);
			var providers = replacer.getUsedProviders();
			if (!providers.isEmpty()) {
				providers.sort((p1, p2) -> Strings.compare(
						Labels.name(p1), Labels.name(p2)));
			}
			return new Cons(replacer, providers);
		});

		if (cons.providers.isEmpty()) {
			MsgBox.info("No replaceable providers found",
					"There are no default providers in the database that" +
							"could be replaced with another provider.");
			return;
		}

		new ReplaceProvidersDialog(cons.replacer, cons.providers).open();
	}

	private ReplaceProvidersDialog(
			ProviderReplacer replacer, List<ProcessDescriptor> usedProviders) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.replacer = replacer;
		this.usedProviders = usedProviders;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.BulkreplaceProviders);
		newShell.setSize(800, 400);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 2, 20, 20);
		UI.gridData(body, true, false);

		UI.label(body, tk, M.ReplaceProvider);
		sourceCombo = new ProcessCombo(body);
		sourceCombo.addSelectionChangedListener(this::updateProducts);

		UI.label(body, tk, M.OfProduct);
		flowCombo = new FlowViewer(body);
		flowCombo.addSelectionChangedListener(this::updateCandidates);
		flowCombo.setEnabled(false);

		UI.label(body, tk, M.With);
		targetCombo = new ProcessCombo(body);
		targetCombo.addSelectionChangedListener($ -> updateButtons());
		targetCombo.setEnabled(false);

		if (!Database.dataPackages().isEmpty()) {
			excludeDataPackageDatasets = tk.createButton(
					body, M.FilterDataSetsFromDataPackage, SWT.CHECK);
		}

		App.runInUI("Render providers", () -> sourceCombo.setInput(usedProviders));
	}

	private void updateProducts(ProcessDescriptor source) {
		var flows = replacer.getProviderFlowsOf(source);
		flowCombo.setInput(flows);
		targetCombo.setInput(Collections.emptyList());
		flowCombo.setEnabled(flows.size() > 1);
		if (flows.size() == 1) {
			flowCombo.select(flows.getFirst());
		}
		updateButtons();
	}

	private void updateCandidates(FlowDescriptor flow) {
		var source = sourceCombo.getSelected();
		var providers = replacer.getProvidersOf(flow);
		providers.remove(source);

		targetCombo.setInput(providers);
		targetCombo.setEnabled(providers.size() > 1);
		if (providers.size() == 1) {
			targetCombo.select(providers.getFirst());
		}
		updateButtons();
	}

	private void updateButtons() {
		var source = sourceCombo.getSelected();
		var flow = flowCombo.getSelected();
		boolean enabled = source != null
				&& source.id != 0L
				&& flow != null
				&& flow.id != 0L;
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	@Override
	protected void okPressed() {
		var origin = sourceCombo.getSelected();
		var flow = flowCombo.getSelected();
		var target = targetCombo.getSelected();

		if (excludeDataPackageDatasets != null && excludeDataPackageDatasets.getSelection()) {
			replacer.excludeDataPackageDatasets();
		}
		super.okPressed();
		App.runWithProgress(
				"Replace providers...",
				() -> replacer.replace(origin, target, flow));
	}

}
