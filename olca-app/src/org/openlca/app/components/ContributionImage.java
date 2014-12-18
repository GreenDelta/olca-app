package org.openlca.app.components;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.FaviColor;
import org.openlca.app.util.Colors;

/**
 * Generates contribution images for UI items. As this class manages an image
 * registry you have to call dispose in order to free native resources.
 */
public class ContributionImage {

	private ImageRegistry imageRegistry = new ImageRegistry();
	private Display display;

	public ContributionImage(Display display) {
		this.display = display;
	}

	public void dispose() {
		imageRegistry.dispose();
	}

	/**
	 * Get an contribution image for table label providers. Returns the maximum
	 * image (for +1 or -1) if the contribution is lower than -1 or greater than
	 * 1.
	 * 
	 * @param contribution
	 *            the contribution value between -1 and 1
	 */
	public Image getForTable(double contribution) {
		double c = contribution;
		if (c < -1)
			c = -1;
		if (c > 1)
			c = 1;
		int contributionInt = (int) (50d * c);
		String key = Integer.toString(contributionInt);
		Image image = imageRegistry.get(key);
		if (image == null) {
			image = new Image(display, 60, 15);
			GC gc = new GC(image);
			RGB color = FaviColor.getForContribution(c);
			gc.setBackground(Colors.getColor(color));
			int width = Math.abs(contributionInt);
			gc.fillRectangle(5, 5, width, 5);
			gc.dispose();
			imageRegistry.put(key, image);
		}
		return image;

	}

}
