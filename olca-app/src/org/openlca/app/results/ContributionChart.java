package org.openlca.app.results;

import java.util.ArrayList;
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
import org.openlca.app.FaviColor;
import org.openlca.app.Messages;
import org.openlca.app.components.charts.ChartCanvas;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ProcessGrouping;

/**
 * A pie chart for showing a set of result contributions.
 */
public class ContributionChart {

	private ImageRegistry imageRegistry = new ImageRegistry();
	private Stack<ImageHyperlink> createdLinks = new Stack<>();
	private ChartCanvas chartCanvas;
	private Composite linkComposite;
	private String unit;

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

	public void setData(List<ContributionItem<?>> data, String unit) {
		this.unit = unit;
		setData(data);
	}

	public void setData(List<ContributionItem<?>> data) {
		if (data == null)
			return;
		while (!createdLinks.isEmpty())
			createdLinks.pop().dispose();
		UI.gridLayout(linkComposite, 1);
		boolean hasRest = hasRest(data);
		createChart(data, hasRest);
		createLinks(data);
		linkComposite.layout(true);
	}

	private boolean hasRest(List<ContributionItem<?>> data) {
		for (ContributionItem<?> item : data) {
			if (item.isRest())
				return true;
		}
		return false;
	}

	private void createChart(List<ContributionItem<?>> data, boolean withRest) {
		List<Double> vals = new ArrayList<>();
		for (ContributionItem<?> item : data)
			vals.add(item.getAmount());
		Chart chart = new ContributionChartCreator(vals).createChart(withRest);
		chartCanvas.setChart(chart);
		chartCanvas.redraw();
	}

	private void createLinks(List<ContributionItem<?>> data) {
		int colorIndex = 0;
		for (ContributionItem<?> item : data) {
			ImageHyperlink link = new ImageHyperlink(linkComposite, SWT.TOP);
			link.setText(getLinkText(item));
			if (item.isRest())
				link.setImage(getLinkImage(-1));
			else
				link.setImage(getLinkImage(colorIndex++));
			createdLinks.push(link);
		}
	}

	private String getLinkText(ContributionItem<?> item) {
		String number = Numbers.format(item.getAmount(), 3);
		if (unit != null)
			number += " " + unit;
		String text = "";
		Object content = item.getItem();
		// TODO: it would be better if a label provider could be set here
		if (content instanceof BaseDescriptor)
			text = Labels.getDisplayName((BaseDescriptor) content);
		else if (content instanceof RootEntity)
			text = Labels.getDisplayName((RootEntity) content);
		else if (content instanceof ProcessGrouping)
			text = ((ProcessGrouping) content).getName();
		else if (item.isRest())
			text = Messages.Other;
		return number + ": " + text;
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

}
