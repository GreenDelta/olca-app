package org.openlca.app;

import org.eclipse.swt.graphics.RGB;

class ColorCalculator {

	private final RGB[] colors;
	private final int[] steps;

	public ColorCalculator(RGB[] colors, int[] steps) {
		if (steps.length != colors.length - 1)
			throw new IllegalArgumentException(
					"Must have exactly one more color than steps");
		int count = 0;
		for (int step : steps)
			count += step;
		if (count != 201)
			throw new IllegalArgumentException("Steps must sum up to 201");

		this.colors = colors;
		this.steps = steps;
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
