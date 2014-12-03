package org.openlca.app.editors.projects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportIndicator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.util.Strings;

class IndicatorTable {

	private ProjectEditor editor;
	private TableViewer viewer;

	public IndicatorTable(ProjectEditor editor) {
		this.editor = editor;
	}

	public void render(Composite parent) {
		viewer = Tables.createViewer(parent, Messages.ImpactCategory,
				Messages.Display, Messages.ReportName, Messages.Description);
		Tables.bindColumnWidths(viewer, 0.3, 0.1, 0.2, 0.4);
		UI.gridData(viewer.getTable(), true, false).heightHint = 150;
		Label label = new Label();
		viewer.setLabelProvider(label);
		ModifySupport<ReportIndicator> modifySupport = new ModifySupport<>(
				viewer);
		modifySupport.bind(Messages.Display, new DisplayModifier());
		modifySupport.bind(Messages.ReportName, new NameModifier());
		modifySupport.bind(Messages.Description, new DescriptionModifier());
		if (editor.getReport() != null)
			viewer.setInput(editor.getReport().getIndicators());
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
	}

	public void methodChanged(ImpactMethodDescriptor method) {
		Report report = editor.getReport();
		if (report == null)
			return;
		report.getIndicators().clear();
		if (method == null) {
			viewer.setInput(null);
			return;
		}
		List<ReportIndicator> indicators = createReportIndicators(method);
		report.getIndicators().addAll(indicators);
		viewer.setInput(report.getIndicators());
	}

	private List<ReportIndicator> createReportIndicators(
			ImpactMethodDescriptor method) {
		ImpactMethodDao dao = new ImpactMethodDao(Database.get());
		List<ImpactCategoryDescriptor> descriptors = dao
				.getCategoryDescriptors(method.getId());
		List<ReportIndicator> indicators = new ArrayList<>();
		int id = 0;
		for (ImpactCategoryDescriptor descriptor : descriptors) {
			ReportIndicator reportIndicator = new ReportIndicator(id++);
			indicators.add(reportIndicator);
			reportIndicator.setDescriptor(descriptor);
			reportIndicator.setReportName(descriptor.getName());
			reportIndicator.setDisplayed(true);
		}
		indicators.sort((r1, r2) -> Strings.compare(
				r1.getDescriptor().getName(), r2.getDescriptor().getName()));
		return indicators;
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 1)
				return null;
			if (!(element instanceof ReportIndicator))
				return null;
			ReportIndicator indicator = (ReportIndicator) element;
			return indicator.isDisplayed() ?
					ImageType.CHECK_TRUE.get() : ImageType.CHECK_FALSE.get();
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ReportIndicator))
				return null;
			ReportIndicator indicator = (ReportIndicator) element;
			switch (col) {
			case 0:
				return Labels.getDisplayName(indicator.getDescriptor());
			case 2:
				return indicator.getReportName();
			case 3:
				return indicator.getReportDescription();
			default:
				return null;
			}
		}
	}

	private class DisplayModifier extends CheckBoxCellModifier<ReportIndicator> {
		@Override
		protected boolean isChecked(ReportIndicator indicator) {
			return indicator.isDisplayed();
		}

		@Override
		protected void setChecked(ReportIndicator indicator, boolean value) {
			if (value == indicator.isDisplayed())
				return;
			indicator.setDisplayed(value);
			editor.setDirty(true);
		}
	}

	private class NameModifier extends TextCellModifier<ReportIndicator> {
		@Override
		protected String getText(ReportIndicator indicator) {
			return indicator.getReportName();
		}

		@Override
		protected void setText(ReportIndicator indicator, String text) {
			if (Objects.equals(indicator.getReportName(), text))
				return;
			indicator.setReportName(text);
			editor.setDirty(true);
		}
	}

	private class DescriptionModifier extends TextCellModifier<ReportIndicator> {
		@Override
		protected String getText(ReportIndicator indicator) {
			return indicator.getReportDescription();
		}

		@Override
		protected void setText(ReportIndicator indicator, String text) {
			if (Objects.equals(indicator.getReportDescription(), text))
				return;
			indicator.setReportDescription(text);
			editor.setDirty(true);
		}
	}
}
