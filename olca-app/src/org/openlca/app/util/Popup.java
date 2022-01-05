package org.openlca.app.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.opal.notifier.Notifier;
import org.eclipse.nebula.widgets.opal.notifier.NotifierColorsFactory.NotifierTheme;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;

public class Popup {

	private static final int INFO = 0;
	private static final int WARNING = 1;
	private static final int ERROR = 2;

	private Popup() {
	}

	public static void info(String text) {
		info(M.Information, text);
	}

	public static void info(String title, String text) {
		show(INFO, title, text);
	}

	public static void warning(String text) {
		warning(M.Warning, text);
	}

	public static void warning(String title, String text) {
		show(WARNING, title, text);
	}

	public static void error(String text) {
		error(M.Error, text);
	}

	public static void error(String title, String text) {
		show(ERROR, title, text);
	}

	private static void show(int state, String title, String text) {


		UIJob job = new UIJob("Open popup") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				var display = getDisplay();
				if (display == null || display.isDisposed())
					return Status.CANCEL_STATUS;

				var theme = state == INFO
					? NotifierTheme.BLUE_THEME
					: NotifierTheme.YELLOW_THEME;

				var image = state == INFO
					? Icon.INFO.get()
					: null;
				
				Notifier.notify(
					image,
					title != null ? title : M.Information,
					text != null ? text : "No message",
					theme);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

}
