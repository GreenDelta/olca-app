package org.openlca.app.util;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Managed SWT colors: the colors are created on demand and disposed when the
 * application is closed.
 */
public class Colors {

	private static Logger log = LoggerFactory.getLogger(Colors.class);
	private static HashMap<RGB, Color> createdColors = new HashMap<>();

	private static Display display;

	static {
		display = PlatformUI.getWorkbench().getDisplay();
		PlatformUI.getWorkbench().addWorkbenchListener(new ShutDown());
	}

	public static Color errorColor() {
		RGB rgb = new RGB(255, 180, 180);
		return get(rgb);
	}

	public static Color get(RGB rgb) {
		Color color = createdColors.get(rgb);
		if (color == null || color.isDisposed()) {
			color = new Color(display, rgb);
			createdColors.put(rgb, color);
		}
		return color;
	}

	public static Color get(int r, int g, int b) {
		RGB rgb = new RGB(r, g, b);
		return get(rgb);
	}

	public static Color white() {
		return systemColor(SWT.COLOR_WHITE);
	}

	public static Color darkGray() {
		return systemColor(SWT.COLOR_DARK_GRAY);
	}

	public static Color linkBlue() {
		return get(25, 76, 127);
	}

	public static Color gray() {
		return get(128, 128, 128);
	}

	public static Color black() {
		return systemColor(SWT.COLOR_BLACK);
	}

	public static Color systemColor(int swtConstant) {
		return display.getSystemColor(swtConstant);
	}

	private static class ShutDown implements IWorkbenchListener {
		@Override
		public boolean preShutdown(IWorkbench workbench, boolean forced) {
			return true;
		}

		@Override
		public void postShutdown(IWorkbench workbench) {
			log.trace("dispose {} created colors", createdColors.size());
			for (Color color : createdColors.values()) {
				if (!color.isDisposed())
					color.dispose();
			}
		}
	}
}
