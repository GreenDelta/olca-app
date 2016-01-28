package org.openlca.app.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.M;
import org.openlca.app.results.simulation.StatisticsCanvas;
import org.openlca.core.math.NumberGenerator;

public class UncertaintyShell {

	public static void show(final NumberGenerator fun) {

		final Display display = Display.getCurrent();
		final Shell shell = new Shell(display, SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
		shell.setText(M.TestDistribution);
		shell.setLayout(new FillLayout());
		final StatisticsCanvas canvas = new StatisticsCanvas(shell);
		shell.pack();
		shell.setSize(620, 400);
		shell.open();

		new Thread() {
			public void run() {
				final List<Double> values = new ArrayList<>();
				for (int i = 0; i < 1000; i++) {
					values.add(fun.next());
					if (!shell.isDisposed())
						display.syncExec(new Runnable() {
							public void run() {
								if (shell.isDisposed())
									return;
								canvas.setValues((values));
							}
						});
				}
				if (!display.isDisposed())
					display.wake();
			}
		}.start();
	}
}
