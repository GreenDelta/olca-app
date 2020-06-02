package org.openlca.app.results.contributions;

import java.util.List;
import java.util.Stack;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.results.Contribution;

class ChartLegend {

	private ImageRegistry imageRegistry = new ImageRegistry();
	private Stack<Widget> createdLinks = new Stack<>();
	private Composite composite;
	ILabelProvider label = new BaseLabelProvider();

	ChartLegend(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		UI.gridData(composite, true, true);
		UI.gridLayout(composite, 1);
		composite.addDisposeListener((e) -> imageRegistry.dispose());
	}

	public void setData(List<Contribution<?>> data, double rest, String unit) {
		while (!createdLinks.isEmpty())
			createdLinks.pop().dispose();
		int colorIndex = 0;
		for (Contribution<?> item : data) {
			if (item.amount == 0d) {
				colorIndex++;
				continue;
			}
			String text = label.getText(item.item);
			element(getText(text, item.amount, unit), item.item, colorIndex++);
		}
		if (rest != 0d) {
			element(getText(M.Other, rest, unit), null, -1);
		}
		composite.layout(true);
	}

	private void element(String text, Object model, int colorIndex) {
		if (model instanceof CategorizedDescriptor || model instanceof CategorizedEntity) {
			ImageHyperlink link = new ImageHyperlink(composite, SWT.TOP);
			link.setText(text);
			link.setImage(getImage(colorIndex));
			Controls.onClick(link, (e) -> {
				if (model instanceof CategorizedDescriptor) {
					App.openEditor((CategorizedDescriptor) model);
				} else if (model instanceof CategorizedEntity) {
					App.openEditor((CategorizedEntity) model);
				}
			});
			createdLinks.push(link);
		} else {
			CLabel label = new CLabel(composite, SWT.TOP);
			label.setImage(getImage(colorIndex));
			label.setText(text);
			createdLinks.push(label);
		}
	}

	private String getText(String text, double amount, String unit) {
		String number = Numbers.format(amount, 3);
		if (unit != null)
			number += " " + unit;
		return number + ": " + text;
	}

	private Image getImage(int index) {
		String key = Integer.toString(index);
		Image image = imageRegistry.get(key);
		if (image != null)
			return image;
		image = new Image(Display.getCurrent(), 30, 15);
		GC gc = new GC(image);
		if (index != -1)
			gc.setBackground(Colors.getForChart(index));
		else
			gc.setBackground(Colors.gray());
		gc.fillRectangle(5, 5, 25, 5);
		gc.dispose();
		imageRegistry.put(key, image);
		return image;
	}

}
