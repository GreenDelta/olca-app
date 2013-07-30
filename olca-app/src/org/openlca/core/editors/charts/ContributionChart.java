package org.openlca.core.editors.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import org.eclipse.birt.chart.model.Chart;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.Colors;
import org.openlca.app.UI;
import org.openlca.core.application.FaviColor;
import org.openlca.core.application.Numbers;
import org.openlca.core.editors.ContributionItem;

/**
 * A pie chart for showing a set of result contributions.
 */
public class ContributionChart {

	private ImageRegistry imageRegistry = new ImageRegistry();
	private Stack<ImageHyperlink> createdLinks = new Stack<>();
	private ChartCanvas chartCanvas;
	private Composite linkComposite;

	public ContributionChart(Composite parent, FormToolkit toolkit) {
		parent.addDisposeListener(new Dispose());
		initContent(parent, toolkit);
	}

	private void initContent(Composite parent, FormToolkit toolkit) {
		parent.setLayout(new FillLayout());
		Composite composite = toolkit.createComposite(parent);
		UI.gridLayout(composite, 2);
		chartCanvas = new ChartCanvas(composite, SWT.NONE);
		GridData gridData = UI.gridData(chartCanvas, false, false);
		gridData.heightHint = 250;
		gridData.widthHint = 300;
		linkComposite = toolkit.createComposite(composite);
		UI.gridData(linkComposite, true, true);
		UI.gridLayout(linkComposite, 1).verticalSpacing = 0;
	}

	public void setData(List<ContributionItem> data) {
		if (data == null)
			return;
		Collections.sort(data, new Sorter());
		while (!createdLinks.isEmpty())
			createdLinks.pop().dispose();
		UI.gridLayout(linkComposite, 1);
		boolean hasRest = hasRest(data);
		createChart(data, hasRest);
		createLinks(data);
		linkComposite.layout(true);
	}

	private boolean hasRest(List<ContributionItem> data) {
		for (ContributionItem item : data) {
			if (item.isRest())
				return true;
		}
		return false;
	}

	private void createChart(List<ContributionItem> data, boolean withRest) {
		List<Double> vals = new ArrayList<>();
		for (ContributionItem item : data)
			vals.add(item.getAmount());
		Chart chart = new ContributionChartCreator(vals).createChart(withRest);
		chartCanvas.setChart(chart);
		chartCanvas.redraw();
	}

	private void createLinks(List<ContributionItem> data) {
		int colorIndex = 0;
		for (ContributionItem item : data) {
			ImageHyperlink link = new ImageHyperlink(linkComposite, SWT.TOP);
			link.setText(getLinkText(item));
			if (item.isRest())
				link.setImage(getLinkImage(-1));
			else
				link.setImage(getLinkImage(colorIndex++));
			createdLinks.push(link);
		}
	}

	private String getLinkText(ContributionItem item) {
		String number = Numbers.format(item.getAmount(), 3);
		if (item.getUnit() != null)
			number += " " + item.getUnit();
		return number + ": " + item.getLabel();
	}

	private Image getLinkImage(int index) {
		String key = Integer.toString(index);
		Image image = imageRegistry.get(key);
		if (image == null) {
			image = new Image(Display.getCurrent(), 30, 15);
			GC gc = new GC(image);
			if (index != -1)
				gc.setBackground(Colors.getColor(FaviColor
						.getRgbForChart(index)));
			else
				gc.setBackground(Colors.getGray());
			gc.fillRectangle(5, 5, 25, 5);
			gc.dispose();
			imageRegistry.put(key, image);
		}
		return image;
	}

	private class Dispose implements DisposeListener {
		@Override
		public void widgetDisposed(DisposeEvent e) {
			imageRegistry.dispose();
		}
	}

	private class Sorter implements Comparator<ContributionItem> {
		@Override
		public int compare(ContributionItem o1, ContributionItem o2) {
			if (o1 == null || o2 == null)
				return 0;
			if (o1.isRest())
				return 1;
			if (o2.isRest())
				return -1;
			return -Double.compare(o1.getAmount(), o2.getAmount());
		}
	}
}
