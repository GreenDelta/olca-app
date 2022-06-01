package org.openlca.app.tools.openepd.output;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.Ec3;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;

public record ExportState(State state, String id) {

	public enum State {
		/**
		 * Saved as local file.
		 */
		FILE,

		/**
		 * Created new EPD on EC3.
		 */
		CREATED,

		/**
		 * Updated existing EPD on EC3.
		 */
		UPDATED,

		/**
		 * User canceled the export or there as an error.
		 */
		CANCELED,

		/**
		 * Some unexpected error.
		 */
		ERROR,
	}

	static ExportState canceled() {
		return new ExportState(State.CANCELED, null);
	}

	static ExportState error() {
		return new ExportState(State.ERROR, null);
	}

	static ExportState created(String id) {
		return new ExportState(State.CREATED, id);
	}

	static ExportState updated(String id) {
		return new ExportState(State.UPDATED, id);
	}

	static ExportState file(String file) {
		return new ExportState(State.FILE, file);
	}

	public boolean isCreated() {
		return state == State.CREATED && id != null;
	}

	boolean isError() {
		return state == State.ERROR;
	}

	public void display() {
		if (state == null)
			return;
		switch (state) {
			case FILE -> Popup.info(
				"Exported as file", "The EPD was exported as file: " + id);
			case UPDATED, CREATED -> Ec3Dialog.show(this);
			default -> {
			}
		}
	}

	private static class Ec3Dialog extends FormDialog {

		private final ExportState state;
		private final String url;

		static void show(ExportState state) {
			if (state == null)
				return;
			new Ec3Dialog(state).open();
		}

		Ec3Dialog(ExportState state) {
			super(UI.shell());
			this.state = state;
			this.url = Ec3.displayUrl(state.id);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Uploaded to EC3");
		}

		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 575, 250);
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.formBody(mForm.getForm(), tk);
			var comp = tk.createComposite(body);
			UI.fillHorizontal(comp);
			UI.gridLayout(comp, 2);
			tk.createLabel(comp, "").setImage(Icon.EC3_WIZARD.get());

			var text = tk.createFormText(comp, false);
			UI.fillHorizontal(text).widthHint = 400;
			var prefix = state.isCreated()
				? "Uploaded a new EPD draft."
				: "Updated an existing EPD.";
			text.setText(prefix + " You can further edit"
				+ " and publish it on EC3 using the following URL: "
				+ url, false, true);
			text.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					Ec3Dialog.this.okPressed();
				}
			});
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, "Open EPD on EC3", true);
			createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CLOSE_LABEL, false);
		}

		@Override
		protected void okPressed() {
			try {
				Desktop.browse(url);
			} catch (Exception e) {
				ErrorReporter.on("Failed to open URL " + url, e);
			}
			super.okPressed();
		}
	}
}
