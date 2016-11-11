package org.openlca.app.results.contributions;

import java.util.List;
import java.util.Stack;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.FaviColor;
import org.openlca.app.M;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.core.results.ContributionItem;

class ChartLegend {

	private ImageRegistry imageRegistry = new ImageRegistry();
	private Stack<ImageHyperlink> createdLinks = new Stack<>();
	private Composite composite;
	ILabelProvider label = new BaseLabelProvider();

	ChartLegend(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		UI.gridData(composite, true, true);
		UI.gridLayout(composite, 1);
		composite.addDisposeListener((e) -> imageRegistry.dispose());
	}

	public void setData(List<ContributionItem<?>> data, double rest, String unit) {
		while (!createdLinks.isEmpty())
			createdLinks.pop().dispose();
		int colorIndex = 0;
		for (ContributionItem<?> item : data) {
			if (item.amount == 0d) {
				colorIndex++;
				continue;
			}
			String text = label.getText(item.item);
			link(getLinkText(text, item.amount, unit), colorIndex++);
		}
		if (rest != 0d) {
			link(getLinkText(M.Other, rest, unit), -1);
		}
		composite.layout(true);
	}

	private void link(String text, int colorIndex) {
		ImageHyperlink link = new ImageHyperlink(composite, SWT.TOP);
		link.setText(text);
		link.setImage(getLinkImage(colorIndex));
		createdLinks.push(link);
	}

	private String getLinkText(String text, double amount, String unit) {
		String number = Numbers.format(amount, 3);
		if (unit != null)
			number += " " + unit;
		return number + ": " + text;
	}

	private Image getLinkImage(int index) {
		String key = Integer.toString(index);
		Image image = imageRegistry.get(key);
		if (image != null)
			return image;
		image = new Image(Display.getCurrent(), 30, 15);
		GC gc = new GC(image);
		if (index != -1)
			gc.setBackground(Colors.get(FaviColor.getRgbForChart(index)));
		else
			gc.setBackground(Colors.gray());
		gc.fillRectangle(5, 5, 25, 5);
		gc.dispose();
		imageRegistry.put(key, image);
		return image;
	}

}
