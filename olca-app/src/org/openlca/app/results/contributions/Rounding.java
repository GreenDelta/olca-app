package org.openlca.app.results.contributions;

class Rounding {

	private final double original;
	private double value;
	private boolean negative;
	private int shift = 0;

	private Rounding(double value) {
		original = value;
		this.value = value;
	}

	private void round() {
		if (value == 0d)
			return;
		abs();
		shift();
		floor();
		up();
		reverseShift();
		reverseAbs();
	}

	private void abs() {
		if (value >= 0)
			return;
		value = Math.abs(value);
		negative = true;
	}

	private void shift() {
		if (value < 10)
			shiftLeft();
		else if (value >= 100)
			shiftRight();
	}

	private void shiftLeft() {
		while (value < 10) {
			value *= 10;
			shift++;
		}
	}

	private void shiftRight() {
		while (value >= 100) {
			value /= 10;
			shift--;
		}
	}

	private void floor() {
		value = Math.floor(value);
	}

	private void up() {
		if (Math.pow(10, -shift) * value == Math.abs(original))
			return;
		value += 1;
	}

	private void reverseShift() {
		value *= Math.pow(10, -shift);
	}

	private void reverseAbs() {
		if (!negative)
			return;
		value *= -1;
	}

	public static double apply(double value) {
		Rounding rounding = new Rounding(value);
		rounding.round();
		return rounding.value;
	}

}
