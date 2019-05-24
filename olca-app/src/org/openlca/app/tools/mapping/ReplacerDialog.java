package org.openlca.app.tools.mapping;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.replacer.ReplacerConfig;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.io.maps.FlowMap;

class ReplacerDialog extends Dialog {

	/**
	 * Open the replacer dialog and return a configuration. If this function
	 * returns `None` it means that the user cancelled the dialog or that the
	 * settings result in no possible replacements.
	 */
	static Optional<ReplacerConfig> open(
			FlowMap mapping, IProvider provider) {
		if (mapping == null || provider == null)
			return Optional.empty();
		ReplacerConfig conf = new ReplacerConfig(mapping, provider);
		ReplacerDialog dialog = new ReplacerDialog(conf);
		if (dialog.open() != Dialog.OK)
			return Optional.empty();
		if (!conf.processes && !conf.methods)
			return Optional.empty();
		return Optional.of(conf);
	}

	private final ReplacerConfig conf;

	private ReplacerDialog(ReplacerConfig conf) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.conf = conf;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Apply flow mapping");
		UI.center(UI.shell(), shell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite root = (Composite) super.createDialogArea(parent);
		UI.gridLayout(root, 1, 10, 10);

		Button processes = check(
				root, "Replace flows in processes", conf.processes);
		Button methods = check(
				root, "Replace flows in LCIA methods", conf.methods);
		Button delete = check(
				root, "Delete mapped and unused flows", conf.deleteMapped);

		Runnable setState = () -> {
			conf.processes = processes.getSelection();
			conf.methods = methods.getSelection();

			if (conf.processes && conf.methods) {
				delete.setEnabled(true);
				conf.deleteMapped = delete.getSelection();
			} else {
				delete.setEnabled(false);
				delete.setSelection(false);
				conf.deleteMapped = false;
			}
		};
		setState.run();

		Arrays.asList(processes, methods, delete).forEach(b -> {
			Controls.onSelect(b, e -> setState.run());
		});
		return root;
	}

	private Button check(Composite root, String label, boolean selected) {
		Button b = new Button(root, SWT.CHECK);
		b.setText(label);
		b.setSelection(selected);
		return b;
	}

}
