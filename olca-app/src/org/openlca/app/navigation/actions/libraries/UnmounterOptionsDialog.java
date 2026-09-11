package org.openlca.app.navigation.actions.libraries;

import java.util.EnumSet;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.library.Unmounter;

/// Lets the user select how a library should be removed from a database. All
/// [Unmounter.Retention] options are shown, but only the ones that are possible
/// for the given library can be selected.
class UnmounterOptionsDialog extends FormDialog {

	private final EnumSet<Unmounter.Retention> options;
	private Unmounter.Retention selected;

	private UnmounterOptionsDialog(EnumSet<Unmounter.Retention> options) {
		super(UI.shell());
		this.options = options;
		// KEEP_NONE is the preferred option; when it is not possible we
		// default to KEEP_USED which then is always possible
		this.selected = options.contains(Unmounter.Retention.KEEP_NONE)
			? Unmounter.Retention.KEEP_NONE
			: Unmounter.Retention.KEEP_USED;
	}

	/// Opens the dialog and returns the retention option that was selected by
	/// the user. Returns an empty option when the user cancels the dialog or
	/// when there is no option to select.
	static Optional<Unmounter.Retention> show(
		EnumSet<Unmounter.Retention> options
	) {
		if (options == null || options.isEmpty())
			return Optional.empty();
		var dialog = new UnmounterOptionsDialog(options);
		return dialog.open() == OK
			? Optional.of(dialog.selected)
			: Optional.empty();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Remove library from database");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 250);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 1, 10, 20);

		addOption(body, tk, Unmounter.Retention.KEEP_NONE,
			"Keep none: completely remove the library.");
		addOption(body, tk, Unmounter.Retention.KEEP_USED,
			"Keep used: copy used library data sets to the database.");
		addOption(body, tk, Unmounter.Retention.KEEP_ALL,
			"Keep all: copy all library data sets to the database.");
	}

	private void addOption(
		Composite body, FormToolkit tk,
		Unmounter.Retention retention, String label
	) {
		var radio = UI.button(body, tk, label, SWT.RADIO);
		radio.setEnabled(options.contains(retention));
		radio.setSelection(selected == retention);
		Controls.onSelect(radio, _ -> {
			if (radio.getSelection()) {
				selected = retention;
			}
		});
	}
}
