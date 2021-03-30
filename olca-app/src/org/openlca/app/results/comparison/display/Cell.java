package org.openlca.app.results.comparison.display;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.util.Pair;

public class Cell {

	private int startPixel;
	private int endPixel;
	private RGB rgb;
	private Point startingLinksPoint;
	private Point endingLinkPoint;
	private boolean isDrawable;
	static Config config;
	private List<Result> result;
	private double minAmount;
	static ColorCellCriteria criteria;
	private boolean isCutoff;
	private Contributions product;

	public void setData(Point startingLinksPoint, Point endingLinkPoint, int startX, int endx, boolean isCutoff) {
		this.startingLinksPoint = startingLinksPoint;
		this.endingLinkPoint = endingLinkPoint;
		startPixel = startX;
		endPixel = endx;
		this.isCutoff = isCutoff;
	}

	public Point getStartingLinkPoint() {
		return startingLinksPoint;
	}

	public void setStartingLinkPoint(Point startingLinksPoint) {
		this.startingLinksPoint = startingLinksPoint;
	}

	public Point getEndingLinkPoint() {
		return endingLinkPoint;
	}

	public void setEndingLinkPoint(Point endingLinkPoint) {
		this.endingLinkPoint = endingLinkPoint;
	}

	public Cell(List<Contribution<CategorizedDescriptor>> contributions, double minAmount, Contributions p) {
		this.minAmount = minAmount;
		product = p;
		result = contributions.stream().map(c -> new Result(c)).collect(Collectors.toList());
		isDrawable = true;
		isCutoff = false;
		rgb = computeRGB();
	}

	public List<Result> getResult() {
		return result;
	}

	public double getTargetValue() {
		return result.stream().mapToDouble(r -> r.getValue()).sum();
	}

	public double getNormalizedValue() {
		return result.stream().mapToDouble(r -> r.getValue() + Math.abs(minAmount)).sum();
	}

	public double getAmount() {
		return result.stream().mapToDouble(r -> r.getAmount()).sum();
	}

	public double getNormalizedAmount() {
		return result.stream().mapToDouble(r -> r.getAmount() + Math.abs(minAmount)).sum();
	}

	public RGB computeRGB() {
		double percentage = 0;
		long value = 0;
		long min = 0, max = 0;
		Pair<Long, Long> pair = null;
		switch (criteria) {
		case LOCATION:
			value = ((ProcessDescriptor) result.get(0).getContribution().item).location;
			pair = product.getMinMaxLocation();
			min = pair.first;
			max = pair.second;
			break;
		case CATEGORY:
			value = result.get(0).getContribution().item.category;
			pair = product.getMinMaxCategory();
			min = pair.first;
			max = pair.second;
			break;
		default:
			value = result.get(0).getContribution().item.id;
			pair = product.getMinMaxProcessId();
			min = pair.first;
			max = pair.second;
			break;
		}
		try {
			percentage = (((value - min) * 100) / (max - min)) / 100.0;
		} catch (Exception e) {
			percentage = -1;
		}
		if (percentage > 100.0) { // It happens because of uncertainty of division
			percentage = 100.0;
		} else if (percentage == -1 || result.get(0).getContribution().amount == 0.0) {
			isDrawable = false;
			return new RGB(192, 192, 192); // Grey color for unfocused values (0 or null)
		}
		if (config.useGradientColor) {
			java.awt.Color tmpColor = GradientColorHelper.numberToColorPercentage(percentage);
			rgb = new RGB(tmpColor.getRed(), tmpColor.getGreen(), tmpColor.getBlue());
		} else {
			rgb = ColorPaletteHelper.getColor(percentage);
		}
		return rgb;
	}

	public void resetDefaultRGB() {
		rgb = computeRGB();
	}

	public RGB getRgb() {
		return rgb;
	}

	public void setRgb(RGB rgb) {
		this.rgb = rgb;
	}

	public boolean isLinkDrawable() {
		return isDrawable && !isCutoff;
	}

	public int getStartPixel() {
		return startPixel;
	}

	public void setStartPixel(int startIndex) {
		this.startPixel = startIndex;
	}

	public int getEndPixel() {
		return endPixel;
	}

	public void setEndPixel(int startIndex) {
		this.endPixel = startIndex;
	}

	public String toString() {
		var results = result.stream().map(r -> Double.toString(r.getValue())).collect(Collectors.toList());
		return rgb + " / " + String.join(", ", results) + " / [ " + startPixel + "; " + endPixel + " ]";
	}

}
