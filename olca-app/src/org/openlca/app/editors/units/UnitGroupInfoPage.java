package org.openlca.app.editors.units;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.util.Strings;

class UnitGroupInfoPage extends ModelPage<UnitGroup> {

	private FormToolkit toolkit;
	private UnitGroupEditor editor;

	UnitGroupInfoPage(UnitGroupEditor editor) {
		super(editor, "UnitGroupInfoPage", Messages.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.UnitGroup
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(infoSection, body);
		body.setFocus();
		form.reflow(true);
	}

	protected void createAdditionalInfo(InfoSection infoSection, Composite body) {
		createDropComponent(Messages.DefaultFlowProperty,
				"defaultFlowProperty", ModelType.FLOW_PROPERTY,
				infoSection.getContainer());
		Section section = UI.section(body, toolkit,
				Messages.Units);
		UI.gridData(section, true, true);
		Composite client = UI.sectionClient(section, toolkit);
		UnitViewer unitViewer = new UnitViewer(client, editor);
		unitViewer.bindTo(section);
		List<Unit> units = getModel().getUnits();
		units.sort((u1, u2) -> Strings.compare(u1.getName(), u2.getName()));
		unitViewer.setInput(units);
		editor.onSaved(() -> unitViewer.setInput(getModel().getUnits()));
	}
}
