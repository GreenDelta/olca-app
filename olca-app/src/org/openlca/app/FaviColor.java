package org.openlca.app;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.openlca.app.util.Colors;

/**
 * An index for a predefined color scale for chart colors and contribution
 * visualization.
 */
public class FaviColor {

	private static RGB[] chartColors = { new RGB(229, 48, 57),
			new RGB(41, 111, 196), new RGB(255, 201, 35), new RGB(82, 168, 77),
			new RGB(132, 76, 173), new RGB(127, 183, 229),
			new RGB(255, 137, 0), new RGB(53, 155, 88), new RGB(49, 148, 68),
			new RGB(252, 255, 100) };

	private static RGB[] contributionColors = { new RGB(53, 155, 88),
			new RGB(87, 197, 124), new RGB(152, 220, 175),
			new RGB(183, 231, 199), new RGB(214, 242, 223),
			new RGB(218, 218, 235), new RGB(188, 189, 220),
			new RGB(158, 154, 200), new RGB(117, 107, 177),
			new RGB(162, 107, 177) };

	private FaviColor() {
	}

	static void setChartColors(RGB[] chartColors) {
		FaviColor.chartColors = chartColors;
	}

	static RGB[] getChartColors() {
		return chartColors;
	}

	static RGB[] getContributionColors() {
		return contributionColors;
	}

	static void setContributionColors(RGB[] contributionColors) {
		FaviColor.contributionColors = contributionColors;
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

	public static RGB getContributionColor(int idx) {
		if (idx < 0)
			return contributionColors[0];
		if (idx > (contributionColors.length - 1))
			return contributionColors[contributionColors.length - 1];
		return contributionColors[idx];
	}

	public static RGB getForContribution(double ratio) {
		int idx = contributionIndex(ratio);
		return contributionColors[idx];
	}

	private static int contributionIndex(double val) {
		int i = (int) ((val + 1) / 0.2);
		if (i < 0)
			return 0;
		if (i > 9)
			return 9;
		return i;
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
