package org.openlca.app.tools.params;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.IntStream;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.util.Strings;

public class ParameterAnalysisResultPage extends SimpleFormEditor {

	private ParamResult result;
	private TableComboViewer impactCombo;
	private Chart chart;

	static void open(ParamResult result) {
		if (result == null)
			return;
		var key = AppContext.put(result);
		var input = new SimpleEditorInput(key, "Parameter analysis results");
		Editors.open(input, "ParameterAnalysisResultPage");
	}

	@Override
	public void init(
			IEditorSite site, IEditorInput input
	) throws PartInitException {
		super.init(site, input);
		setTitleImage(Icon.ANALYSIS_RESULT.get());
		var inp = (SimpleEditorInput) input;
		result = AppContext.remove(inp.id, ParamResult.class);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		Page() {
			super(ParameterAnalysisResultPage.this, "ParamResultPage", "Results");
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, "Parameter analysis results");
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);
			setupSection(body, tk);
			tableSection(body, tk);
			chartSection(body, tk);
			form.reflow(true);
		}

		private void setupSection(Composite body, FormToolkit tk) {
			var comp = UI.formSection(body, tk, "Calculation setup");
			link(comp, tk, M.ProductSystem, result.system());
			link(comp, tk, M.ImpactAssessmentMethod, result.method());
			var allocation = UI.labeledText(comp, tk, M.AllocationMethod);
			allocation.setText(Labels.of(result.allocation()));
			allocation.setEditable(false);
			var iterations = UI.labeledText(comp, tk, "Number of iterations");
			iterations.setText(Integer.toString(result.count()));

			UI.filler(comp, tk);
			var excelBtn = UI.button(comp, tk, M.ExportToExcel);
			excelBtn.setImage(Images.get(FileType.EXCEL));
			Controls.onSelect(excelBtn, $ -> {
				var file = FileChooser.forSavingFile("Export results", "parameter analysis.xlsx");
				if (file == null)
					return;
				var export = new ParamResultExport(result, file);
				App.runWithProgress("Export results", export, () -> {
					if (export.error() != null) {
						ErrorReporter.on("Export failed", export.error());
					} else {
						Popup.info("Export done", "Wrote file " + file.getName());
					}
				});
			});
		}

		private void link(
				Composite comp, FormToolkit tk, String label, RootEntity e
		) {
			UI.label(comp, tk, label);
			var link = UI.imageHyperlink(comp, tk, SWT.TOP);
			link.setText(Labels.name(e));
			link.setImage(Images.get(e));
			Controls.onClick(link, $ -> App.open(e));
		}

		private void tableSection(Composite body, FormToolkit tk) {
			var comp = UI.formSection(body, tk, "Impact assessment results");
			UI.gridData(comp, true, true);

			var label = new Label();
			var header = new ArrayList<>(Collections.singleton(M.ImpactCategory));
			IntStream.rangeClosed(1, result.seq().count())
					.mapToObj(String::valueOf)
					.forEach(header::add);

			var table = Tables.createViewer(comp, header.toArray(String[]::new), label);

			var widths = new ArrayList<>(Collections.singleton(0.3));
			var count = result.seq().count();
			widths.addAll(Collections.nCopies(count, 0.1));
			var widthsDouble = widths.stream().mapToDouble(Double::valueOf).toArray();
			Tables.bindColumnWidths(table, widthsDouble);

			Viewers.sortByLabels(table, label, 0);
			table.setInput(result.results().keySet());
		}

		private void chartSection(Composite body, FormToolkit tk) {
			var comp = UI.formSection(body, tk,
					"Impact assessment results per category");
			UI.gridLayout(comp, 1);
			var top = tk.createComposite(comp);
			UI.fillHorizontal(top);
			UI.gridLayout(top, 2);
			UI.label(top, tk, M.ImpactCategory);
			var impacts = result.impacts();
			impactCombo = DescriptorCombo.of(top, tk, impacts);
			impactCombo.addSelectionChangedListener(e -> setChartData());

			chart = new Chart(comp, SWT.NONE);
			UI.fillHorizontal(chart).heightHint = 400;
			chart.setOrientation(SWT.HORIZONTAL);
			chart.getLegend().setVisible(false);

			// we set a white title just to fix the problem
			// that the y-axis is cut sometimes
			chart.getTitle().setText(".");
			chart.getTitle().setFont(UI.defaultFont());
			chart.getTitle().setForeground(Colors.background());

			// configure the x-axis with one category
			var x = chart.getAxisSet().getXAxis(0);
			x.getTitle().setText("Iterations");
			x.getTitle().setFont(UI.defaultFont());
			x.getTitle().setForeground(body.getForeground());
			x.getTick().setForeground(body.getForeground());
			x.getGrid().setStyle(LineStyle.NONE);

			// configure the y-axis
			var y = chart.getAxisSet().getYAxis(0);
			y.getTitle().setFont(UI.defaultFont());
			y.getTitle().setForeground(body.getForeground());
			y.getTick().setForeground(body.getForeground());
			y.getTick().setFormat(new DecimalFormat("0.0E0#",
					new DecimalFormatSymbols(Locale.US)));

			tk.adapt(chart);
			setChartData();
		}
	}

	private void setChartData() {
		// delete the old series
		Arrays.stream(chart.getSeriesSet().getSeries())
				.map(ISeries::getId)
				.forEach(id -> chart.getSeriesSet().deleteSeries(id));

		ImpactDescriptor d = Viewers.getFirstSelected(impactCombo);
		if (d == null)
			return;

		var data = result.seriesOf(d);
		var xSeries = IntStream.rangeClosed(1, data.length)
				.asDoubleStream()
				.toArray();
		var bars = (IBarSeries<?>) chart.getSeriesSet()
				.createSeries(SeriesType.BAR, "#data");
		bars.setXSeries(xSeries);
		bars.setYSeries(data);
		bars.setBarColor(Colors.get(178, 223, 219));

		var unit = Strings.isNotBlank(d.referenceUnit)
				? d.referenceUnit
				: "-";
		chart.getAxisSet().getYAxis(0)
				.getTitle()
				.setText(unit);

		var ticks = IntStream.rangeClosed(1, data.length)
				.mapToObj(String::valueOf)
				.toArray(String[]::new);
		chart.getAxisSet().getXAxis(0)
				.getTick()
				.setFormat(new ChoiceFormat(xSeries, ticks));

		chart.getAxisSet().adjustRange();
		chart.redraw();
	}

	private class Label extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (!(element instanceof ImpactDescriptor d))
				return null;
			if (col == 0) {
				return Images.get(d);
			}
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ImpactDescriptor i))
				return null;
			if (col == 0) {
				var unit = Strings.isNotBlank(i.referenceUnit)
						? " (" + i.referenceUnit + ")"
						: "";
				return i.name + unit;
			}
			var val = result.seriesOf(i)[col - 1];
			return Numbers.format(val);
		}
	}

}
