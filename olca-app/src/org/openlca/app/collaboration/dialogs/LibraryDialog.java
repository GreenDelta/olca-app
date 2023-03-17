package org.openlca.app.collaboration.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

public class LibraryDialog extends FormDialog {

	private final String library;
	private Button urlCheck;
	private Button fileCheck;
	private Mode selectedMode = Mode.URL;
	private String location;

	public LibraryDialog(String library) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.library = library;
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var formBody = UI.header(form, form.getToolkit(),
				"Locate library",
				"Please specify a location for the missing library '" + library + "'");
		var body = UI.composite(formBody, form.getToolkit());
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true).widthHint = 500;
		createContent(body, form.getToolkit());
		form.getForm().reflow(true);
	}

	private void createContent(Composite parent, FormToolkit tk) {
		urlCheck = createCheckboxSection(parent, tk, "From url", Mode.URL, (composite, check) -> {
			var text = UI.text(composite, tk);
			text.addModifyListener(e -> {
				location = text.getText();
				updateButtons();
			});
			text.addFocusListener(FocusListener.focusGainedAdapter(e -> select(check, text)));
			return text;
		});
		fileCheck = createCheckboxSection(parent, tk, "From file", Mode.FILE, (composite, check) -> {
			UI.gridLayout(composite, 2);
			var text = UI.text(composite, tk, SWT.READ_ONLY | SWT.BORDER);
			text.addFocusListener(FocusListener.focusGainedAdapter(e -> select(check, text)));
			UI.gridData(text, true, false);
			var browseButton = UI.button(composite, tk);
			browseButton.setText("Browse");
			Controls.onSelect(browseButton, e -> {
				select(check, text);
				var zolca = FileChooser.open("*.zip");
				if (zolca == null) {
					location = null;
					return;
				}
				text.setText(zolca.getAbsolutePath());
				location = zolca.getAbsolutePath();
				updateButtons();
			});
			return text;
		});
	}

	private void select(Button check, Text text) {
		urlCheck.setSelection(check == urlCheck);
		fileCheck.setSelection(check == fileCheck);
		location = text.getText();
		updateButtons();
	}

	private Button createCheckboxSection(Composite parent, FormToolkit tk, String label, Mode mode, Renderer renderer) {
		var check = UI.radio(parent, tk, label);
		check.setSelection(mode == selectedMode);
		var composite = UI.composite(parent, tk);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, false);
		var text = renderer.render(composite, check);
		Controls.onSelect(check, e -> {
			selectedMode = mode;
			location = text.getText();
		});
		return check;
	}

	private boolean isComplete() {
		return !Strings.nullOrEmpty(location);
	}

	private void updateButtons() {
		getButton(IDialogConstants.OK_ID).setEnabled(isComplete());
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		var ok = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		ok.setEnabled(isComplete());
		setButtonLayoutData(ok);
	}

	public boolean isFileSelected() {
		return selectedMode == Mode.FILE;
	}

	public String getLocation() {
		return location;
	}

	private enum Mode {

		URL,
		FILE;

	}

	private interface Renderer {

		Text render(Composite composite, Button check);

	}

}
