package org.openlca.app.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.CalcExchange;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProviderDialog extends Dialog {

	public static Options select(CalcExchange e, long[] providers) {
		Options opts = new Options();
		if (providers == null || providers.length == 0)
			return opts;
		opts.selected = providers[0];
		if (e == null)
			return opts;
		try {
			App.runInUI("#Select a provider", () -> {
				ProviderDialog d = new ProviderDialog(
						opts, e, providers);
				d.open();
			}).join();
		} catch (Exception ex) {
			Logger log = LoggerFactory.getLogger(ProviderDialog.class);
			log.error("Failed to wait for dialog thread", ex);
		}
		return opts;
	}

	private final Options options;
	private final CalcExchange exchange;
	private final long[] providers;

	private Button saveCheck;
	private Button autoContinueCheck;
	private Button cancelCheck;

	public ProviderDialog(Options options,
			CalcExchange e, long[] providers) {
		super(UI.shell());
		this.options = options;
		this.exchange = e;
		this.providers = providers;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		UI.gridLayout(c, 2);
		UI.formLabel(c, M.Process);
		UI.formLabel(c, getLabel(
				ProcessDescriptor.class, exchange.processId));
		UI.formLabel(c, M.Flow);
		UI.formLabel(c, getLabel(
				FlowDescriptor.class, exchange.flowId));
		Combo combo = UI.formCombo(c, M.Providers);
		String[] labels = new String[providers.length];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = getLabel(ProcessDescriptor.class, providers[i]);
		}
		combo.setItems(labels);
		combo.select(0);
		Controls.onSelect(combo, e -> {
			options.selected = providers[combo.getSelectionIndex()];
		});
		createChecks(c);
		return c;
	}

	private void createChecks(Composite comp) {
		UI.filler(comp);
		Button b = new Button(comp, SWT.RADIO);
		b.setText("#Continue with multi-provider checks");
		b.setSelection(true);

		UI.filler(comp);
		saveCheck = new Button(comp, SWT.RADIO);
		saveCheck.setText("#Always use this provider for this flow");

		UI.filler(comp);
		autoContinueCheck = new Button(comp, SWT.RADIO);
		autoContinueCheck.setText("#Continue with auto-select");

		UI.filler(comp);
		cancelCheck = new Button(comp, SWT.RADIO);
		cancelCheck.setText("#Cancel the product system creation");
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("#Select a provider");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	private <T extends BaseDescriptor> String getLabel(Class<T> clazz, long id) {
		EntityCache cache = Cache.getEntityCache();
		if (cache == null)
			return "?";
		T d = cache.get(clazz, id);
		if (d == null)
			return "?";
		return Labels.getDisplayName(d);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected void okPressed() {
		options.saveSelected = saveCheck.getSelection();
		options.autoContinue = autoContinueCheck.getSelection();
		options.cancel = cancelCheck.getSelection();
		super.okPressed();
	}

	static class Options {
		long selected;
		boolean saveSelected;
		boolean autoContinue;
		boolean cancel;
	}
}
