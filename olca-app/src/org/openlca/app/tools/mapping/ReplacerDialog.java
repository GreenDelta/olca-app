package org.openlca.app.tools.mapping;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ModelCheckBoxTree;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.replacer.ReplacerConfig;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.maps.FlowMap;

class ReplacerDialog extends FormDialog {

	/**
	 * Open the replacer dialog and return a configuration. If this function
	 * returns `None` it means that the user cancelled the dialog or that the
	 * settings result in no possible replacements.
	 */
	static Optional<ReplacerConfig> open(
			FlowMap mapping, IProvider target) {
		if (mapping == null || target == null)
			return Optional.empty();
		ReplacerConfig conf = new ReplacerConfig(mapping, target);
		ReplacerDialog dialog = new ReplacerDialog(conf);
		if (dialog.open() != Dialog.OK || dialog.selection == null)
			return Optional.empty();
		conf.models.addAll(dialog.selection);
		if (conf.models.isEmpty())
			return Optional.empty();
		return Optional.of(conf);
	}

	private final ReplacerConfig conf;
	private ModelCheckBoxTree tree;
	private List<RootDescriptor> selection;

	private ReplacerDialog(ReplacerConfig conf) {
		super(UI.shell());
		setBlockOnOpen(true);
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
		Composite comp = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(comp, 1, 10, 10);
		UI.formLabel(comp, tk, "This will replace the flows in the database " +
				"(the source system) with the flows in the target system.");
		tree = new ModelCheckBoxTree(
				ModelType.PROCESS,
				ModelType.IMPACT_METHOD);
		tree.drawOn(comp, tk);
		Button delete = tk.createButton(comp,
				"Delete replaced and unused flows", SWT.CHECK);
		Controls.onSelect(delete,
			_e -> conf.deleteMapped = delete.getSelection());
	}

	@Override
	protected void okPressed() {
		this.selection = tree.getSelection();
		super.okPressed();
	}

}
