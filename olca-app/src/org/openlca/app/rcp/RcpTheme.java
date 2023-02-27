package org.openlca.app.rcp;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;

public class RcpTheme {

	public static void setTheme(boolean isDarkTheme) {
		Display display = Display.getCurrent();

		if (display == null)
			throw new NullPointerException(
					"Display must be already created before you call RcpTheme.setTheme()");

		display.setData("org.eclipse.swt.internal.win32.useDarkModeExplorerTheme", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.menuBarForegroundColor",
				isDarkTheme ? new Color(display, 0xD0, 0xD0, 0xD0) : null);
		display.setData("org.eclipse.swt.internal.win32.menuBarBackgroundColor",
				isDarkTheme ? new Color(display, 0x30, 0x30, 0x30) : null);
		display.setData("org.eclipse.swt.internal.win32.menuBarBorderColor",
				isDarkTheme ? new Color(display, 0x50, 0x50, 0x50) : null);
		display.setData("org.eclipse.swt.internal.win32.Canvas.use_WS_BORDER", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.List.use_WS_BORDER", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.Table.use_WS_BORDER", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.Text.use_WS_BORDER", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.Tree.use_WS_BORDER", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.Table.headerLineColor",
				isDarkTheme ? new Color(display, 0x50, 0x50, 0x50) : null);
		display.setData("org.eclipse.swt.internal.win32.Label.disabledForegroundColor",
				isDarkTheme ? new Color(display, 0x80, 0x80, 0x80) : null);
		display.setData("org.eclipse.swt.internal.win32.Combo.useDarkTheme", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.ProgressBar.useColors", isDarkTheme);
		display.setData("org.eclipse.swt.internal.win32.useShellTitleColoring", isDarkTheme);
	}

}
