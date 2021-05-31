package org.openlca.app.components;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.util.Colors;

/**
 * Generates contribution images for UI items. As this class manages an image
 * registry you have to call dispose in order to free native resources.
 */
public class ContributionImage {

	private final ImageRegistry imageRegistry = new ImageRegistry();
	private final Display display;

	private Color color;
	private double barWidth = 50;
	private int width = 60;

	public ContributionImage() {
		this.display = Display.getCurrent();
	}

	public ContributionImage withColor(Color color) {
		this.color = color;
		return this;
	}

	public ContributionImage withFullWidth(int width) {
		this.width = width;
		this.barWidth = 0.8 * width;
		return this;
	}

	public void dispose() {
		imageRegistry.dispose();
	}

	/**
	 * Get an contribution image for table label providers. Returns the maximum
	 * image (for +1 or -1) if the contribution is lower than -1 or greater than 1.
	 *
	 * @param contribution the contribution value between -1 and 1
	 */
	public Image get(double contribution) {
		return get(contribution, this.color);
	}

	/**
	 * Same as {@link #get(double)} but the returned image will have the given
	 * color.
	 */
	public Image get(double contribution, Color color) {
		var selectedColor = orContributionColor(color, contribution);
		double share = Math.abs(fix(contribution));
		int filledWidth = (int) (barWidth * share);
		var key = selectedColor.toString() + filledWidth;
		var image = imageRegistry.get(key);
		if (image != null)
			return image;
		image = new Image(display, width, 15);
		GC gc = new GC(image);
		gc.setBackground(selectedColor);
		gc.fillRectangle((int) ((width - barWidth) / 2), 5, filledWidth, 5);
		gc.dispose();
		imageRegistry.put(key, image);
		return image;
	}

	private Color orContributionColor(Color color, double contribution) {
		if (color != null)
			return color;
		var share = fix(contribution);
		return Colors.getForContribution(share);
	}

	private double fix(double contribution) {
		if (contribution < -1)
			return -1;
		if (contribution > 1)
			return 1;
		return contribution;
	}
}
