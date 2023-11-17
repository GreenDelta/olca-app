package org.openlca.app.editors.lcia.geo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.geo.Shape;
import org.openlca.geo.calc.FeatureValidation;
import org.openlca.geo.calc.FeatureValidation.Stats;
import org.openlca.geo.lcia.GeoFactorSetup;

class Validation {

	private Validation() {
	}

	static void run(GeoFactorSetup setup) {
		if (setup == null
				|| setup.features == null
				|| setup.features.isEmpty()) {
			MsgBox.error("Invalid setup",
					"Could not find any geometry in the setup.");
			return;
		}

		var validation = FeatureValidation.of(setup.features);
		var job = new UIJob("Validate geometries") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				monitor.beginTask("Validate geometries", validation.count());
				validation.onValidated((count) -> {
					monitor.worked(count);
					if (monitor.isCanceled()) {
						validation.cancel();
					}
				});
				validation.run();
				// TODO: return CANCEL when validation was cancelled
				StatsDialog.open(validation.stats());
				return Status.OK_STATUS;
			}
		};
		PlatformUI.getWorkbench()
				.getProgressService()
				.showInDialog(UI.shell(), job);
		job.schedule();
	}

	private static class StatsDialog extends FormDialog {

		private final Stats stats;

		private StatsDialog(Stats stats) {
			super(UI.shell());
			this.stats = stats;
		}

		static void open(Stats stats) {
			if (stats == null)
				return;
			new StatsDialog(stats).open();
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText("Validation results");
		}

		@Override
		protected Point getInitialSize() {
			return new Point(600, 400);
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			var tk = form.getToolkit();
			var body = UI.dialogBody(form.getForm(), tk);
			var table = Tables.createViewer(body,
					"Geometry type", "Valid features", "Invalid features");
			table.setLabelProvider(new StatsLabel(stats));
			table.setInput(Shape.values());
			Tables.bindColumnWidths(table, 0.4, 0.3, 0.3);
		}

		private static class StatsLabel extends LabelProvider
				implements ITableLabelProvider {

			private final Stats stats;

			private StatsLabel(Stats stats) {
				this.stats = stats;
			}

			@Override
			public Image getColumnImage(Object obj, int col) {
				return null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof Shape shape))
					return null;
				return switch (col) {
					case 0 -> shape.toString();
					case 1 -> Integer.toString(stats.validCountOf(shape));
					case 2 -> Integer.toString(stats.invalidCountOf(shape));
					default -> null;
				};
			}
		}
	}
}
