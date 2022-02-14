package org.openlca.app.wizards.io;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.io.ImportLog;
import org.openlca.io.Import;

record ImportMonitor(IProgressMonitor monitor) {

	static ImportMonitor on(IProgressMonitor monitor) {
		return new ImportMonitor(monitor);
	}

	void run(Import imp) {

		imp.log().listen(message -> {
			if (message.state() == null)
				return;
			switch (message.state()) {
				case IMPORTED, INFO, UPDATED -> {
					if (message.hasMessage()) {
						monitor.subTask(message.message());
					} else if (message.hasDescriptor()) {
						var d = message.descriptor();
						monitor.subTask(Labels.of(d.type) + "; " + Labels.name(d));
					}
				}
				default -> {}
			}
		});

		monitor.beginTask("Import: ", IProgressMonitor.UNKNOWN);
		var watcher = new Thread(imp);
		watcher.start();

		var wasCanceled = new AtomicBoolean(false);
		while (watcher.isAlive()) {
			try {
				watcher.join(5000);
				if (monitor.isCanceled() && !wasCanceled.get()) {
					wasCanceled.set(true);
					imp.cancel();
					break;
				}
			} catch (InterruptedException e) {
				ErrorReporter.on("failed to join import thread", e);
			}
		}
		monitor.done();
		InfoDialog.show(imp);
	}

	private static class InfoDialog extends FormDialog {

		private final Import imp;
		private final ImportLog log;

		static void show(Import imp) {
			if (imp == null || imp.log() == null)
				return;
			App.runInUI(
				titleOf(imp),
				() -> new InfoDialog(imp).open());
		}

		private static String titleOf(Import imp) {
			return imp.isCanceled()
				? "Import canceled"
				: "Import finished";
		}

		private InfoDialog(Import imp) {
			super(UI.shell());
			this.imp = imp;
			this.log = imp.log();
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(titleOf(imp));
		}

		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 400, 300);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
			createButton(parent, IDialogConstants.DETAILS_ID,
				"Details...", false);
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId != IDialogConstants.DETAILS_ID) {
				super.buttonPressed(buttonId);
				return;
			}
			ImportLogView.open(log);
			okPressed();
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.formBody(mForm.getForm(), tk);
			UI.gridLayout(body, 1, 10, 25);

			long count = log.messages().stream()
				.filter(ImportLog.Message::hasDescriptor)
				.count();
			tk.createLabel(body, String.format("Handled %d data sets:", count))
				.setFont(UI.boldFont());

			var comp = tk.createComposite(body);
			UI.gridLayout(comp, 2, 5, 5);
			var states = new ImportLog.State[] {
				ImportLog.State.IMPORTED,
				ImportLog.State.UPDATED,
				ImportLog.State.SKIPPED,
				ImportLog.State.ERROR,
				ImportLog.State.WARNING
			};
			for (var state : states) {
				tk.createLabel(comp, headerOf(state));
				int c = log.countOf(state);
				tk.createLabel(comp, Integer.toString(c));
			}
		}

		private String headerOf(ImportLog.State state) {
			if (state == null)
				return "?";
			return switch (state) {
				case IMPORTED -> "Imported:";
				case UPDATED -> "Updated:";
				case SKIPPED -> "Skipped:";
				case ERROR -> "Errors:";
				case WARNING -> "Warnings:";
				case INFO -> "Other:";
			};
		}

	}

}
