package org.openlca.app;

import org.eclipse.swt.graphics.RGB;

/**
 * An index for a predefined color scale for chart colors and contribution
 * visualization.
 */
public class FaviColor {

	private static final ColorCalculator colorCalc = new ColorCalculator();

	private FaviColor() {
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

	private static class ColorCalculator {

		private static final RGB[] colors = {
				new RGB(0, 150, 0),
				new RGB(0, 100, 180),
				new RGB(0, 150, 230),
				new RGB(255, 0, 0)
		};

		private static final int[] steps = { 80, 40, 81 };

		private ColorCalculator() {
		}

		public RGB getColor(int percentage) {
			int perc = percentage;
			if (perc < -100)
				perc = -100;
			if (perc > 100)
				perc = 100;
			int value = perc + 100;
			int prev = 0;
			int index = 0;
			double steps = 0;
			for (int step : this.steps) {
				steps = step;
				if (value < step + prev) {
					break;
				} else {
					index++;
					prev += step;
				}
			}
			value = value - prev;

			RGB startColor = colors[index];
			RGB endColor = colors[index + 1];

			double diffRed = endColor.red - startColor.red;
			double diffGreen = endColor.green - startColor.green;
			double diffBlue = endColor.blue - startColor.blue;
			double step = value % steps;
			step /= steps;

			int red = (int) (startColor.red + step * diffRed);
			int green = (int) (startColor.green + step * diffGreen);
			int blue = (int) (startColor.blue + step * diffBlue);

			return new RGB(red, green, blue);
		}

	}

}
