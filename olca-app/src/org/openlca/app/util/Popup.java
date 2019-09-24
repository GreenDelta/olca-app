package org.openlca.app.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.opal.notifier.Notifier;
import org.eclipse.nebula.widgets.opal.notifier.NotifierColorsFactory.NotifierTheme;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;

public class Popup {

	private Popup() {
	}

	public static void info(String text) {
		info(M.Information, text);
	}

	public static void info(String title, String text) {
		show(Icon.INFO.get(), title, text);
	}

	public static void warning(String text) {
		warning(M.Warning, text);
	}

	public static void warning(String title, String text) {
		show(Icon.WARNING.get(), title, text);
	}

	public static void error(String text) {
		error(M.Error, text);
	}

	public static void error(String title, String text) {
		show(Icon.ERROR.get(), title, text);
	}

	private static void show(Image image, String title, String text) {
		UIJob job = new UIJob("Open popup") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				Display display = getDisplay();
				if (display == null || display.isDisposed())
					return Status.CANCEL_STATUS;
				Notifier.notify(
						image,
						title != null ? title : "?",
						text != null ? text : "?",
						NotifierTheme.YELLOW_THEME);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
