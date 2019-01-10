package org.openlca.app.results.contributions;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.results.ContributionItem;

import javafx.embed.swt.FXCanvas;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

public class ContributionChart extends BarChart<String, Number> {

	private ChartLegend legend;

	public static ContributionChart create(Composite parent, FormToolkit toolkit) {
		// #see getGap for more info why this size specifically
		return create(parent, toolkit, 700, 300);
	}

	public static ContributionChart create(Composite parent, FormToolkit toolkit, double width, double height) {
		Composite container = UI.formComposite(parent, toolkit);
		UI.gridLayout(container, 2);
		UI.gridData(container, true, true);
		ContributionChart chart = new ContributionChart(width, height);
		FXCanvas canvas = new FXCanvas(container, SWT.NONE);
		Scene scene = new Scene(new Group(chart));
		String cssPath = ContributionChart.class.getPackage().getName().replace('.', '/');
		scene.getStylesheets().add("/" + cssPath + "/styles.css");
		canvas.setScene(scene);
		chart.legend = new ChartLegend(container);
		return chart;
	}

	public void setLabel(ILabelProvider label) {
		legend.label = label;
	}

	private ContributionChart(double width, double height) {
		super(new CategoryAxis(), new NumberAxis());
		setLegendVisible(false);
		getXAxis().setTickMarkVisible(false);
		setAnimated(false);
		setPrefSize(width, height);
	}

	public void setData(List<ContributionItem<?>> items, String unit) {
		getData().clear();
		Collections.sort(items, new Comparator(true));
		List<ContributionItem<?>> top = getTop(items);
		for (ContributionItem<?> item : top) {
			addData(getLabel(item), item.amount);
		}
		double others = 0;
		if (items.size() > 6) {
			for (int i = 5; i < items.size(); i++) {
				others += items.get(i).amount;
			}
			Data<String, Number> data = addData("Others", others);
			data.getNode().getStyleClass().add("others");
		}
		updateYAxis(top, others);
		int bars = items.size() > 5 ? 6 : items.size();
		setBarGap(getGap(bars));
		legend.setData(top, others, unit);
	}

	private List<ContributionItem<?>> getTop(List<ContributionItem<?>> items) {
		List<ContributionItem<?>> top = items.size() <= 6 ? items : items.subList(0, items.size() == 6 ? 6 : 5);
		Collections.sort(top, new Comparator(false));
		return top;
	}

	private double getGap(int bars) {
		// TODO maybe there is a better way to calculate this correctly, this
		// seems to work for now but not in all chart sizes, so be careful when
		// not using the standard size in 2-arg constructor
		switch (bars) {
		case 1:
			return 255;
		case 2:
			return 150;
		case 3:
			return 100;
		case 4:
			return 70;
		case 5:
			return 55;
		default:
			return 40;
		}
	}

	private Data<String, Number> addData(String label, double amount) {
		Series<String, Number> series = new Series<>();
		series.setName(label);
		Data<String, Number> data = new Data<>("", amount);
		series.getData().add(data);
		getData().add(series);
		Tooltip tooltip = new Tooltip();
		tooltip.setText(label + "\n" + data.getYValue().toString());
		Tooltip.install(data.getNode(), tooltip);
		return data;
	}

	private static String getLabel(ContributionItem<?> item) {
		if (item.item instanceof BaseDescriptor)
			return ((BaseDescriptor) item.item).name;
		return null;
	}

	private void updateYAxis(List<ContributionItem<?>> top, double others) {
		double min = others < 0 ? others : 0;
		double max = others > 0 ? others : 0;
		for (ContributionItem<?> item : top) {
			min = Math.min(min, item.amount);
			max = Math.max(max, item.amount);
		}
		min = Rounding.apply(min);
		max = Rounding.apply(max);
		double bound = Math.max(-min, max);
		NumberAxis yAxis = (NumberAxis) getYAxis();
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(min < 0 ? -bound : 0);
		yAxis.setUpperBound(bound);
		double tick = bound / (min < 0 ? 2 : 4);
		yAxis.setTickUnit(tick);
		yAxis.setTickLabelFormatter(new StringConverter<Number>() {

			@Override
			public String toString(Number arg0) {
				if (arg0 == null)
					return "0";
				return Numbers.format(arg0.doubleValue(), 1);
			}

			@Override
			public Number fromString(String arg0) {
				return Double.parseDouble(arg0);
			}
		});
	}

	private class Comparator implements java.util.Comparator<ContributionItem<?>> {

		private final boolean abs;

		private Comparator(boolean abs) {
			this.abs = abs;
		}

		@Override
		public int compare(ContributionItem<?> o1, ContributionItem<?> o2) {
			double a1 = o1.amount;
			double a2 = o2.amount;
			if (abs) {
				a1 = Math.abs(a1);
				a2 = Math.abs(a2);
			}
			if (a1 == a2)
				return 0;
			if (a1 == 0d)
				return 1;
			if (a2 == 0d)
				return -1;
			return -Double.compare(a1, a2);
		}

	}

}
