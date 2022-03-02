package org.openlca.app.editors.epds;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.components.FileChooser;
import org.openlca.app.tools.openepd.model.Ec3InternalEpd;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.jsonld.Json;

import java.util.Objects;

class ExportDialog extends FormDialog  {

	private final Ec3InternalEpd epd;

	public static void show(Ec3InternalEpd epd) {
		if (epd == null)
			return;
		new ExportDialog(epd).open();
	}

	private ExportDialog(Ec3InternalEpd epd) {
		super(UI.shell());
		this.epd = Objects.requireNonNull(epd);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Export an openEPD document");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Upload", true);
		createButton(parent, 999, "Save as file", false);
		createButton(parent, IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {


	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			okPressed();
			return;
		}
		if (buttonId == IDialogConstants.CANCEL_ID) {
			cancelPressed();
			return;
		}

		// save as file
		var json = epd.toJson();
		var file = FileChooser.forSavingFile(
			"Save openEPD document", epd.name + ".json");
		if (file == null)
			return;
		try {
			Json.write(json, file);
			super.okPressed();
		} catch (Exception e) {
			ErrorReporter.on("Failed to save openEPD document", e);
		}
	}
}
