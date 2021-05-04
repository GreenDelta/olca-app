package org.openlca.app.editors.projects;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.editors.projects.reports.model.ReportIndicator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.field.StringModifier;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.util.Strings;

class IndicatorTable {

	private final ProjectEditor editor;
	private TableViewer viewer;

	public IndicatorTable(ProjectEditor editor) {
		this.editor = editor;
	}

	public void render(Composite parent) {
		viewer = Tables.createViewer(parent, M.ImpactCategory,
				M.Display, M.ReportName, M.Description);
		Tables.bindColumnWidths(viewer, 0.3, 0.1, 0.2, 0.4);
		UI.gridData(viewer.getTable(), true, false).heightHint = 150;
		viewer.setLabelProvider(new Label());
		ModifySupport<ReportIndicator> ms = new ModifySupport<>(viewer);
		ms.bind(M.Display, new DisplayModifier());
		ms.bind(M.ReportName, new StringModifier<>(editor, "reportName"));
		ms.bind(M.Description, new StringModifier<>(editor,
				"reportDescription"));
		if (editor.getReport() != null)
			viewer.setInput(editor.getReport().indicators);
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
	}

	public void methodChanged(ImpactMethodDescriptor method) {
		Report report = editor.getReport();
		if (report == null)
			return;
		report.indicators.clear();
		if (method == null) {
			viewer.setInput(null);
			return;
		}
		List<ReportIndicator> indicators = createReportIndicators(method);
		report.indicators.addAll(indicators);
		viewer.setInput(report.indicators);
	}

	private List<ReportIndicator> createReportIndicators(
			ImpactMethodDescriptor method) {
		var dao = new ImpactMethodDao(Database.get());
		var descriptors = dao
				.getCategoryDescriptors(method.id);
		List<ReportIndicator> indicators = new ArrayList<>();
		int id = 0;
		for (var descriptor : descriptors) {
			var reportIndicator = new ReportIndicator(id++);
			indicators.add(reportIndicator);
			reportIndicator.descriptor = descriptor;
			reportIndicator.reportName = descriptor.name;
			reportIndicator.displayed = true;
		}
		indicators.sort((r1, r2) -> Strings.compare(r1.descriptor.name,
				r2.descriptor.name));
		return indicators;
	}

	private static class Label extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 0)
				return Images.get(ModelType.IMPACT_CATEGORY);
			if (col != 1)
				return null;
			if (!(obj instanceof ReportIndicator))
				return null;
			var indicator = (ReportIndicator) obj;
			return Images.get(indicator.displayed);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ReportIndicator))
				return null;
			var indicator = (ReportIndicator) obj;
			return switch (col) {
				case 0 -> Labels.name(indicator.descriptor);
				case 2 -> indicator.reportName;
				case 3 -> indicator.reportDescription;
				default -> null;
			};
		}
	}

	private class DisplayModifier extends CheckBoxCellModifier<ReportIndicator> {
		@Override
		protected boolean isChecked(ReportIndicator indicator) {
			return indicator.displayed;
		}

		@Override
		protected void setChecked(ReportIndicator indicator, boolean value) {
			if (value == indicator.displayed)
				return;
			indicator.displayed = value;
			editor.setDirty(true);
		}
	}

}
