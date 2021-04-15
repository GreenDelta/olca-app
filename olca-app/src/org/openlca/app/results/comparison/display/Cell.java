package org.openlca.app.results.comparison.display;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.util.Pair;

public class Cell {

	private RGB rgb;
	private Point startingLinksPoint;
	private Point endingLinkPoint;
	private boolean isDrawable;
	static Config config;
	private Result result;
	private double minAmount;
	static ColorCellCriteria criteria;
	private boolean isCutoff;
	private Contributions contributions;
	private boolean isDisplayed;
	private Rectangle rectCell;
	private String tooltip;
	static IDatabase db;

	public void setData(Point startingLinksPoint, Point endingLinkPoint, Rectangle rectCell, boolean isCutoff) {
		this.startingLinksPoint = startingLinksPoint;
		this.endingLinkPoint = endingLinkPoint;
		this.rectCell = rectCell;
		this.isCutoff = isCutoff;
		isDisplayed = true;
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

	public Cell(Contribution<CategorizedDescriptor> contributionsList, double minAmount, Contributions c) {
		this.minAmount = minAmount;
		contributions = c;
		result = new Result(contributionsList);
		isDrawable = true;
		isCutoff = false;
		rgb = computeRGB();
		isDisplayed = true;
	}

	private void setTooltip() {
		var contribution = result.getContribution();
		var locationId = ((ProcessDescriptor) contribution.item).location;
		var locationName = new LocationDao(db).getDescriptor(locationId).code;
		var processName = contribution.item.name + " - " + locationName;

		tooltip = "Process name : " + processName + "\n" + "Amount : " + contribution.amount + " "
				+ StringUtils.defaultIfEmpty(contribution.unit, "");
	}

	public String getTooltip() {
		if (tooltip != null)
			return tooltip;
		setTooltip();
		return tooltip;
	}

	public Result getResult() {
		return result;
	}

	public double getTargetValue() {
		return result.getValue();
	}

	public double getNormalizedValue() {
		return result.getValue() + Math.abs(minAmount);
	}

	public double getAmount() {
		return result.getAmount();
	}

	public double getNormalizedAmount() {
		return result.getAmount() + Math.abs(minAmount);
	}

	public RGB computeRGB() {
		double percentage = 0;
		long value = 0;
		long min = 0, max = 0;
		Pair<Long, Long> pair = null;
		switch (criteria) {
		case LOCATION:
			value = ((ProcessDescriptor) result.getContribution().item).location;
			pair = contributions.getMinMaxLocation();
			min = pair.first;
			max = pair.second;
			break;
		case CATEGORY:
			value = result.getContribution().item.category;
			pair = contributions.getMinMaxCategory();
			min = pair.first;
			max = pair.second;
			break;
		default:
			value = result.getContribution().item.id;
			pair = contributions.getMinMaxProcessId();
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
		} else if (percentage == -1 || result.getContribution().amount == 0.0) {
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
		return isDrawable && !isCutoff && isDisplayed;
	}

	public void setIsDisplayed(boolean isDisplayed) {
		this.isDisplayed = isDisplayed;
	}

	public boolean isDisplayed() {
		return isDisplayed;
	}

	public boolean contains(Point p) {
		return rectCell.contains(p);
	}

	public String toString() {
		return rgb + " / " + result + " / [ " + rectCell.x + "; " + (rectCell.x + rectCell.width) + " ]";
	}

}
