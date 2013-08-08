package org.openlca.core.editors;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.openlca.core.application.FaviColor;
import org.openlca.ui.Colors;

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
	 * Get an contribution image for table label providers. Returns null if the
	 * contribution is lower than -1 or greater than 1.
	 * 
	 * @param contribution
	 *            the contribution value between -1 and 1
	 */
	public Image getForTable(double contribution) {
		if (contribution < -1 || contribution > 1)
			return null;
		int contributionInt = (int) (50d * contribution);
		String key = Integer.toString(contributionInt);
		Image image = imageRegistry.get(key);
		if (image == null) {
			image = new Image(display, 60, 15);
			GC gc = new GC(image);
			RGB color = FaviColor.getForContribution(contribution);
			gc.setBackground(Colors.getColor(color));
			int width = Math.abs(contributionInt);
			gc.fillRectangle(5, 5, width, 5);
			gc.dispose();
			imageRegistry.put(key, image);
		}
		return image;

	}

}
