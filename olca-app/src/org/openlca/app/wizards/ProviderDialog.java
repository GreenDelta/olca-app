package org.openlca.app.wizards;

import java.util.List;

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
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.cache.ExchangeTable.Linkable;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProviderDialog extends Dialog {

	private final Options options;
	private final Linkable exchange;
	private final List<TechFlow> providers;

	private Button saveCheck;
	private Button autoContinueCheck;
	private Button cancelCheck;

	public static Options select(Linkable e, List<TechFlow> providers) {
		Options opts = new Options();
		if (providers == null || providers.isEmpty())
			return opts;
		opts.selected = providers.get(0);
		if (e == null)
			return opts;
		try {
			App.runInUI(M.SelectProvider, () -> {
				var dialog = new ProviderDialog(opts, e, providers);
				dialog.open();
			}).join();
		} catch (Exception ex) {
			Logger log = LoggerFactory.getLogger(ProviderDialog.class);
			log.error("Failed to wait for dialog thread", ex);
		}
		return opts;
	}

	public ProviderDialog(Options options,
			Linkable e, List<TechFlow> providers) {
		super(UI.shell());
		this.options = options;
		this.exchange = e;
		this.providers = providers;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		UI.gridLayout(c, 2);

		UI.label(c, M.Process);
		String processLabel = getLabel(
				ProcessDescriptor.class, exchange.processId());
		UI.label(c, Strings.cut(processLabel, 120))
				.setToolTipText(processLabel);

		UI.label(c, M.Flow);
		String flowLabel = getLabel(
				FlowDescriptor.class, exchange.flowId());
		UI.label(c, Strings.cut(flowLabel, 120))
				.setToolTipText(flowLabel);

		Combo combo = UI.labeledCombo(c, M.Provider);
		UI.gridData(combo, true, false).widthHint = 80;
		String[] labels = new String[providers.size()];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = Labels.name(providers.get(i).provider());
		}
		combo.setItems(labels);
		combo.select(0);
		combo.setToolTipText(labels[0]);
		Controls.onSelect(combo, e -> {
			int i = combo.getSelectionIndex();
			options.selected = providers.get(i);
			combo.setToolTipText(labels[i]);
		});
		createChecks(c);
		return c;
	}

	private void createChecks(Composite comp) {
		UI.filler(comp);
		Button b = new Button(comp, SWT.RADIO);
		b.setText(M.ContinueMultiProviders);
		b.setSelection(true);

		UI.filler(comp);
		saveCheck = new Button(comp, SWT.RADIO);
		saveCheck.setText(M.AlwaysUseProvider);

		UI.filler(comp);
		autoContinueCheck = new Button(comp, SWT.RADIO);
		autoContinueCheck.setText(M.ContinueAutoSelect);

		UI.filler(comp);
		cancelCheck = new Button(comp, SWT.RADIO);
		cancelCheck.setText(M.CancelSystemCreation);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.SelectProvider);
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		int x = p.x;
		int y = p.y;
		if (x < 450) {
			x = 450;
		}
		if (y < 300) {
			y = 300;
		}
		return new Point(x, y);
	}

	private <T extends Descriptor> String getLabel(Class<T> clazz, long id) {
		EntityCache cache = AppContext.getEntityCache();
		if (cache == null)
			return "?";
		T d = cache.get(clazz, id);
		if (d == null)
			return "?";
		return Labels.name(d);
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
		TechFlow selected;
		boolean saveSelected;
		boolean autoContinue;
		boolean cancel;
	}
}
