package org.openlca.app.tools.mapping;

import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.tools.mapping.model.FlowMap;
import org.openlca.app.tools.mapping.model.IMapProvider;
import org.openlca.app.tools.mapping.model.ReplacerConfig;
import org.openlca.app.util.UI;

class ReplacerDialog extends Dialog {

	/**
	 * Open the replacer dialog and return a configuration. If this function returns
	 * `None` it means that the user cancelled the dialog or that the settings
	 * result in no possible replacements.
	 */
	static Optional<ReplacerConfig> open(
			FlowMap mapping, IMapProvider provider) {
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
		shell.setText("Open flow mapping");
		UI.center(UI.shell(), shell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite root = (Composite) super.createDialogArea(parent);
		UI.gridLayout(root, 2, 10, 10);
		UI.formCheckBox(root, "Replace flows in processes");
		UI.formCheckBox(root, "Replace flows in LCIA methods");
		UI.formCheckBox(root, "Delete mapped and unused flows");
		return root;
	}

}
