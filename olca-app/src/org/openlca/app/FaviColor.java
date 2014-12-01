package org.openlca.app;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.openlca.app.util.Colors;

/**
 * An index for a predefined color scale for chart colors and contribution
 * visualization.
 */
public class FaviColor {

	private static RGB[] chartColors = {
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

	private static final RGB[] COLORS = {
			new RGB(0, 150, 0),
			new RGB(0, 100, 180),
			new RGB(0, 150, 230),
			new RGB(255, 0, 0)
	};

	private static final int[] STEPS = { 80, 40, 81 };
	private static final ColorCalculator colorCalc = new ColorCalculator(
			COLORS, STEPS);

	private FaviColor() {
	}

	static void setChartColors(RGB[] chartColors) {
		FaviColor.chartColors = chartColors;
	}

	static RGB[] getChartColors() {
		return chartColors;
	}

	/**
	 * Returns the defined chart color for the given index. If the index is out
	 * of the range of the pre-defined colors, a random color is returned.
	 */
	public static Color getForChart(int idx) {
		return Colors.getColor(getRgbForChart(idx));
	}

	/**
	 * Returns the defined chart color for the given index. If the index is out
	 * of the range of the pre-defined colors, a random color is returned.
	 */
	public static RGB getRgbForChart(int idx) {
		if (idx < 0 || idx >= chartColors.length)
			return next(idx);
		RGB rgb = chartColors[idx];
		return rgb != null ? rgb : next(idx);
	}

	private static RGB next(int idx) {
		if (idx == 0)
			return new RGB(255, 255, 255);
		int blue = 255 / Math.abs(idx);
		int red = 255 - blue;
		int green = (blue + red) / 2;
		return new RGB(red, green, blue);
	}

	public static RGB getForContribution(double ratio) {
		return colorCalc.getColor((int) (ratio * 100));
	}

	/**
	 * Get the hex-code rrggbb for the given color.
	 */
	public static String toHex(RGB rgb) {
		if (rgb == null)
			return "000000";
		return String.format("%02x%02x%02x", rgb.red, rgb.green, rgb.blue);
	}

	/**
	 * Converts the hex-code to a RGB object.
	 */
	public static RGB fromHex(String hex) {
		if (hex == null)
			return new RGB(0, 0, 0);
		String s = hex.trim();
		if (s.length() != 6)
			throw new IllegalArgumentException(
					"Only format rrggbb allowed but was " + hex);
		return new RGB(Integer.parseInt(s.substring(0, 2), 16),
				Integer.parseInt(s.substring(2, 4), 16), Integer.parseInt(
						s.substring(4, 6), 16));
	}

}
