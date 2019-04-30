package org.openlca.app.tools.mapping;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;

class MappingPage extends FormPage {

	private final MappingTool tool;
	private TableViewer table;

	public MappingPage(MappingTool tool) {
		super(tool, "MappingPage", "Flow mapping");
		this.tool = tool;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, "Flow mapping");
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createInfoSection(tk, body);
		createTable(body, tk);
		form.reflow(true);
	}

	private void createInfoSection(FormToolkit tk, Composite body) {
		Composite comp = UI.formSection(body, tk, M.GeneralInformation);
		Text name = UI.formText(comp, tk, M.Name);
		Controls.set(name, this.tool.mapping.name);
		Text description = UI.formMultiText(comp, tk, M.Description);
		Controls.set(description, this.tool.mapping.description);

		Text sourceSys = UI.formText(comp, tk, "Source system");
		if (this.tool.mapping.source != null) {
			Controls.set(sourceSys, this.tool.mapping.source.name);
		}
		Text targetSys = UI.formText(comp, tk, "Target system");
		if (this.tool.mapping.target != null) {
			Controls.set(targetSys, this.tool.mapping.target.name);
		}
	}

	private void createTable(Composite body, FormToolkit tk) {
		Section section = UI.section(body, tk, "Flow mapping");
		UI.gridData(section, true, true);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		table = Tables.createViewer(
				comp,
				"Status",
				"Source flow",
				"Source category",
				"Source unit",
				"Target flow",
				"Target category",
				"Target unit",
				"Conversion factor",
				"Default provider");
		table.setLabelProvider(new TableLabel());
		double w = 1.0 / 9.0;
		Tables.bindColumnWidths(table, w, w, w, w, w, w, w, w, w);
		table.setInput(this.tool.mapping.entries);
	}
}