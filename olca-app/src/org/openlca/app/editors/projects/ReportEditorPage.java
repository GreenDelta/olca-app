package org.openlca.app.editors.projects;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

class ReportEditorPage extends FormPage {

	private final ProjectEditor editor;

	private FormToolkit tk;
	private ReportSectionList sectionList;

	public ReportEditorPage(ProjectEditor editor) {
		super(editor, "ReportInfoPage", M.Report);
		this.editor = editor;
	}

	private Report report() {
		return editor.report();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, "Report");
		tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		createInfoSection(body);
		createProcessesSection(body);
		createAddButton(body);
		sectionList = new ReportSectionList(editor, body, form, tk);
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		var titleText = UI.formText(comp, tk, M.Title);
		if (report().title != null) {
			titleText.setText(report().title);
		}
		titleText.addModifyListener($ -> report().title = titleText.getText());
	}

	private void createProcessesSection(Composite body) {
		var section = UI.section(
			body, tk, "Selected processes (for contribution analyses)");
		section.setExpanded(report().processes.size() != 0);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(comp, M.Process);
		table.setLabelProvider(new ProcessLabel());
		Tables.bindColumnWidths(table, 1.0);

		// bind actions
		var add = Actions.onAdd(() -> {
			var selected = ModelSelector.multiSelect(ModelType.PROCESS);
			if (selected.isEmpty())
				return;
			var existing = report().processes.stream()
				.map(d -> d.refId)
				.collect(Collectors.toSet());
			var filtered = selected.stream()
				.filter(d -> d instanceof ProcessDescriptor
					&& !existing.contains(d.refId))
				.map(d -> (ProcessDescriptor) d)
				.collect(Collectors.toList());
			if (!filtered.isEmpty()) {
				report().processes.addAll(filtered);
				table.setInput(report().processes);
				editor.setDirty(true);
			}
		});

		var remove = Actions.onRemove(() -> {
			List<ProcessDescriptor> selected = Viewers.getAllSelected(table);
			report().processes.removeAll(selected);
			table.setInput(report().processes);
			editor.setDirty(true);
		});

		Actions.bind(section, add, remove);
		Actions.bind(table, add, remove);

		// sync and set initial input
		var current = report().processes;
		var db = Database.get();
		if (current.isEmpty() || db == null)
			return;
		var dao = new ProcessDao(db);
		var input = current.stream()
			.map(d -> dao.getDescriptorForRefId(d.refId))
			.sorted((d1, d2) -> Strings.compare(Labels.name(d1), Labels.name(d2)))
			.collect(Collectors.toList());
		table.setInput(input);
	}

	private void createAddButton(Composite body) {
		var comp = UI.formComposite(body, tk);
		UI.filler(comp);
		var addButton = tk.createButton(comp, M.AddSection, SWT.NONE);
		addButton.setImage(Icon.ADD.get());
		Controls.onSelect(addButton, e -> sectionList.addNew());
	}

	private static class ProcessLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ProcessDescriptor))
				return null;
			var process = (ProcessDescriptor) obj;
			return Images.get(process);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ProcessDescriptor))
				return null;
			var process = (ProcessDescriptor) obj;
			return Labels.name(process);
		}
	}

}
