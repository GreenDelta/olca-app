package org.openlca.app.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.app.Messages;
import org.openlca.app.results.simulation.StatisticsCanvas;
import org.openlca.core.math.NumberGenerator;

public class UncertaintyShell {

	public static void show(final NumberGenerator fun) {

		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setText(Messages.TestDistribution);
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
					if (!display.isDisposed())
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

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();

	}
}
