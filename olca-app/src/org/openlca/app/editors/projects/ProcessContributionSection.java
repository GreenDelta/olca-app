package org.openlca.app.editors.projects;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportProcess;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

class ProcessContributionSection {

	private TableViewer viewer;
	private ProjectEditor editor;

	ProcessContributionSection(ProjectEditor editor) {
		this.editor = editor;
	}

	void create(Composite body, FormToolkit toolkit) {
		Section section = UI.section(body, toolkit,
				Messages.ProcessContributions);
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Process, Messages.ReportName,
				Messages.Description };
		viewer = Tables.createViewer(composite, properties);
		viewer.setLabelProvider(new Label());
		Tables.bindColumnWidths(viewer, 0.3, 0.3, 0.4);
		bindModifySupport();
		bindActions(viewer, section);
		setInitialInput();
	}

	private void setInitialInput() {
		Report report = editor.getReport();
		if (report == null)
			return;
		report.getProcesses().sort((p1, p2) ->
				Strings.compare(p1.getReportName(), p2.getReportName()));
		viewer.setInput(report.getProcesses());
	}

	private void bindModifySupport() {
		ModifySupport<ReportProcess> support = new ModifySupport<>(viewer);
		support.bind(Messages.ReportName, ReportProcess::getReportName,
				(p, text) -> {
					p.setReportName(text);
					editor.setDirty(true);
				});
		support.bind(Messages.Description, ReportProcess::getReportDescription,
				(p, text) -> {
					p.setReportDescription(text);
					editor.setDirty(true);
				});
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = Actions.onAdd(this::onAdd);
		Action remove = Actions.onRemove(this::onRemove);
		Action copy = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove);
		Actions.bind(viewer, add, remove, copy);
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
				else {
					ReportProcess process = Viewers.getFirstSelected(viewer);
					if (process != null)
						App.openEditor(process.getDescriptor());
				}
			});
		Tables.onDeletePressed(viewer, (e) -> onRemove());
	}

	private void onAdd() {
		Report report = editor.getReport();
		if (report == null)
			return;
		BaseDescriptor d = ModelSelectionDialog.select(ModelType.PROCESS);
		if (!(d instanceof ProcessDescriptor))
			return;
		ProcessDescriptor descriptor = (ProcessDescriptor) d;
		ReportProcess process = new ReportProcess(descriptor);
		report.getProcesses().add(process);
		viewer.setInput(report.getProcesses());
		editor.setDirty(true);
	}

	private void onRemove() {
		Report report = editor.getReport();
		if (report == null)
			return;
		List<ReportProcess> selected = Viewers.getAllSelected(viewer);
		if (selected == null || selected.isEmpty())
			return;
		report.getProcesses().removeAll(selected);
		viewer.setInput(report.getProcesses());
		editor.setDirty(true);
	}

	private class Label extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col == 0)
				return ImageType.PROCESS_ICON.get();
			else
				return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			if (!(element instanceof ReportProcess))
				return null;
			ReportProcess process = (ReportProcess) element;
			switch (col) {
			case 0:
				return Labels.getDisplayName(process.getDescriptor());
			case 1:
				return process.getReportName();
			case 2:
				return process.getReportDescription();
			default:
				return null;
			}
		}
	}
}
