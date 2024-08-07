package org.openlca.app.tools.mapping;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.components.ModelCheckBoxTree;
import org.openlca.app.tools.mapping.model.FlowProvider;
import org.openlca.app.tools.mapping.replacer.ReplacerConfig;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

class ReplacerDialog extends FormDialog {

	/**
	 * Open the replacer dialog and return a configuration. If this function
	 * returns `None` it means that the user cancelled the dialog or that the
	 * settings result in no possible replacements.
	 */
	static Optional<ReplacerConfig> open(
			FlowMap mapping, FlowProvider target) {
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
		shell.setText(M.ReplaceFlowsInDatabase);
		UI.center(UI.shell(), shell);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var comp = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(comp, 1, 10, 10);
		UI.label(comp, tk, M.ThisWillReplaceFlowsInTheDb);
		tree = new ModelCheckBoxTree(
				ModelType.PROCESS,
				ModelType.IMPACT_METHOD);
		tree.drawOn(comp, tk);
		Button delete = tk.createButton(
				comp, M.DeleteReplacedAndUnusedFlows, SWT.CHECK);
		Controls.onSelect(delete,
			_e -> conf.deleteMapped = delete.getSelection());
	}

	@Override
	protected void okPressed() {
		this.selection = tree.getSelection();
		super.okPressed();
	}

}
