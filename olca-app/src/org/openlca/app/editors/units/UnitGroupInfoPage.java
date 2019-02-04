package org.openlca.app.editors.units;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.util.UI;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.util.Strings;

class UnitGroupInfoPage extends ModelPage<UnitGroup> {

	private FormToolkit toolkit;
	private UnitGroupEditor editor;
	private ScrolledForm form;

	UnitGroupInfoPage(UnitGroupEditor editor) {
		super(editor, "UnitGroupInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(infoSection, body);
		body.setFocus();
		form.reflow(true);
	}

	protected void createAdditionalInfo(InfoSection infoSection, Composite body) {
		dropComponent(infoSection.getContainer(), M.DefaultFlowProperty, "defaultFlowProperty");
		Section section = UI.section(body, toolkit, M.Units);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		UnitViewer unitViewer = new UnitViewer(client, editor);
		CommentAction.bindTo(section, unitViewer, "units", getComments());
		List<Unit> units = getModel().units;
		units.sort((u1, u2) -> Strings.compare(u1.name, u2.name));
		unitViewer.setInput(units);
		editor.onSaved(() -> unitViewer.setInput(getModel().units));
	}
}
