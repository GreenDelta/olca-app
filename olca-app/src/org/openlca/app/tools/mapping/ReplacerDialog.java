package org.openlca.app.tools.mapping;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.replacer.ReplacerConfig;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.io.maps.FlowMap;

class ReplacerDialog extends FormDialog {

	/**
	 * Open the replacer dialog and return a configuration. If this function
	 * returns `None` it means that the user cancelled the dialog or that the
	 * settings result in no possible replacements.
	 */
	static Optional<ReplacerConfig> open(
			FlowMap mapping, IProvider source) {
		if (mapping == null || source == null)
			return Optional.empty();
		ReplacerConfig conf = new ReplacerConfig(mapping, source);
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
		shell.setText("Replace flows in database");
		UI.center(UI.shell(), shell);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		Composite root = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(root, 1, 10, 10);

		UI.formLabel(root, tk, "This will replace the flows in the database " +
				"(the source system) with the flows in the target system.");

		Button processes = tk.createButton(root,
				"Replace flows in processes", SWT.CHECK);
		processes.setSelection(conf.processes);

		Button methods = tk.createButton(root,
				"Replace flows in LCIA methods", SWT.CHECK);
		methods.setSelection(conf.methods);

		Button delete = tk.createButton(root,
				"Delete replaced and unused flows", SWT.CHECK);
		delete.setSelection(conf.deleteMapped);

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
	}
}
