package org.openlca.app.util;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.preferences.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Managed SWT colors: the colors are created on demand and disposed when the
 * application is closed.
 */
public class Colors {

	private static final Display display;

	private static final HashMap<RGBA, Color> createdColors = new HashMap<>();

	static {
		display = PlatformUI.getWorkbench().getDisplay();
		display.disposeExec(() -> {
			for (Color c : createdColors.values()) {
				if (c != null && !c.isDisposed()) {
					c.dispose();
				}
			}
		});
	}

	private static final RGB[] chartColors = {
			new RGB(229, 48, 57),
			new RGB(41, 111, 196),
			new RGB(255, 201, 35),
			new RGB(82, 168, 77),
			new RGB(132, 76, 173),
			new RGB(127, 183, 229),
			new RGB(255, 137, 0),
			new RGB(128, 0, 128),
			new RGB(135, 76, 63),
			new RGB(252, 255, 100),
			new RGB(0, 177, 241),
			new RGB(112, 187, 40),
			new RGB(18, 89, 133),
			new RGB(226, 0, 115),
			new RGB(255, 255, 85),
			new RGB(218, 0, 24),
			new RGB(0, 111, 154),
			new RGB(255, 153, 0)
	};

	private static final RGB[] contributionColors = {
			new RGB(0, 150, 0),
			new RGB(0, 100, 180),
			new RGB(0, 150, 230),
			new RGB(255, 0, 0)
	};

	/**
	 * Returns the defined chart color for the given index. If the index is out of
	 * the range of the pre-defined colors, a random color is returned.
	 */
	public static Color getForChart(int idx) {
		if (idx >= 0 && idx < chartColors.length) {
			RGB rgb = chartColors[idx];
			return get(rgb);
		}
		int blue = 255 / Math.abs(idx);
		int red = 255 - blue;
		int green = (blue + red) / 2;
		return get(red, green, blue, 255);
	}

	public static Color errorColor() {
		RGB rgb = new RGB(255, 235, 238);
		return get(rgb);
	}

	public static Color get(int r, int g, int b) {
		return get(new RGBA(r, g, b, 255));
	}

	public static Color get(RGB rgb) {
		RGBA rgba = new RGBA(rgb.red, rgb.green, rgb.blue, 255);
		return get(rgba);
	}

	public static Color get(int r, int g, int b, int a) {
		return get(new RGBA(r, g, b, a));
	}

	public static Color get(RGBA rgba) {
		Color color = createdColors.get(rgba);
		if (color == null || color.isDisposed()) {
			color = new Color(display, rgba);
			createdColors.put(rgba, color);
		}
		return color;
	}

	public static Color fromHex(String hex) {
		if (hex == null)
			return white();
		String s = hex.trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}

		// support 3 character CSS colors
	    if (s.length() == 3) {
	      s = String.valueOf(s.charAt(0))
	          + s.charAt(0)
	          + s.charAt(1)
	          + s.charAt(1)
	          + s.charAt(2)
	          + s.charAt(2);
	    }

		if (s.length() < 6)
			return white();

		String rh = s.substring(0, 2);
		String gh = s.substring(2, 4);
		String bh = s.substring(4, 6);
		String ah = (s.length() > 7)
				? s.substring(6, 8)
				: null;

		try {
			int r = Integer.parseInt(rh, 16);
			int g = Integer.parseInt(gh, 16);
			int b = Integer.parseInt(bh, 16);
			int a = ah != null
					? Integer.parseInt(ah, 16)
					: 255;
			return get(r, g, b, a);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(Colors.class);
			log.error("failed to parse hex color " + hex, e);
			return black();
		}
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

	public static Color darkBackground() {
		// Also defined in /olca-app/css/e4_dark_fixes.css
		return get(47, 47, 47);
	}

	public static Color gray() {
		return get(128, 128, 128);
	}

	public static Color black() {
		return systemColor(SWT.COLOR_BLACK);
	}

	public static Color red() {
		return systemColor(SWT.COLOR_RED);
	}

	public static Color systemColor(int swtConstant) {
		return display.getSystemColor(swtConstant);
	}

	public static Color tagBackground() {
		return fromHex("#e8eaf6");
	}

	public static Color background() {
		return Theme.isDark() ? Colors.darkBackground() : Colors.white();
	}

	/**
	 * Gets the contribution color for the given ratio which must be in a range of
	 * [-1, 1].
	 */
	public static Color getForContribution(double ratio) {
		int perc = (int) (ratio * 100);
		if (perc < -100) {
			perc = -100;
		}
		if (perc > 100) {
			perc = 100;
		}
		int value = perc + 100;

		int prev = 0;
		int index = 0;
		int[] conSteps = { 80, 40, 81 };
		double steps = 0;
		for (int step : conSteps) {
			steps = step;
			if (value < step + prev) {
				break;
			} else {
				index++;
				prev += step;
			}
		}
		value = value - prev;

		RGB startColor = contributionColors[index];
		RGB endColor = contributionColors[index + 1];

		double diffRed = endColor.red - startColor.red;
		double diffGreen = endColor.green - startColor.green;
		double diffBlue = endColor.blue - startColor.blue;
		double step = value % steps;
		step /= steps;

		int red = (int) (startColor.red + step * diffRed);
		int green = (int) (startColor.green + step * diffGreen);
		int blue = (int) (startColor.blue + step * diffBlue);

		return get(red, green, blue);
	}
}
