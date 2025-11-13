package org.openlca.app.editors.epds;

import java.util.Objects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdType;

class DocPage extends ModelPage<Epd> {

	private final EpdEditor editor;

	DocPage(EpdEditor editor) {
		super(editor, "EpdDocPage", M.Documentation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		timeSection(body, tk);
		technologySection(body, tk);
		publicationSection(body, tk);
		modellingSection(body, tk);
	}

	private void timeSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.TimeAndLocation, 3);
		UI.date(comp, tk, M.PublicationDate, getModel().validFrom, selected -> {
			if (Objects.equals(getModel().validFrom, selected))
				return;
			getModel().validFrom = selected;
			editor.setDirty();
		});
		UI.filler(comp, tk);
		UI.date(comp, tk, M.ValidUntil, getModel().validUntil, selected -> {
			if (Objects.equals(getModel().validUntil, selected))
				return;
			getModel().validUntil = selected;
			editor.setDirty();
		});
		UI.filler(comp, tk);
		modelLink(comp, M.Location, "location");
	}

	private void technologySection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Technology, 3);
		multiText(comp, M.ManufacturingDescription, "manufacturing");
		multiText(comp, M.ProductUsageDescription, "productUsage");
	}

	private void modellingSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.ModelingAndValidation, 3);

		// EPD type combo
		var typeCombo = UI.labeledCombo(comp, tk, M.EpdType);
		var types = EpdType.values();
		var items = new String[types.length + 1];
		items[0] = "";
		int selected = 0;
		for (int i = 0; i < types.length; i++) {
			var type = types[i];
			items[i + 1] = Labels.of(type);
			if (type == getModel().epdType) {
				selected = i + 1;
			}
		}
		typeCombo.setItems(items);
		typeCombo.select(selected);
		typeCombo.setEnabled(isEditable());

		Controls.onSelect(typeCombo, $ -> {
			int i = typeCombo.getSelectionIndex();
			getModel().epdType = i > 0 ? types[i - 1] : null;
			editor.setDirty();
		});
		UI.filler(comp, tk);

		modelLink(comp, M.Pcr, "pcr");
		modelLink(comp, M.DataGenerator, "dataGenerator");
		modelLink(comp, M.Verifier, "verifier");
		multiText(comp, M.UseAdvice, "useAdvice");
	}

	private void publicationSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.PublicationAndOwnership, 3);
		modelLink(comp, M.DeclarationOwnerManufacturer, "manufacturer");
		modelLink(comp, M.ProgramOperator, "programOperator");
		text(comp, M.RegistrationNumber, "registrationId");
		modelLink(comp, M.OriginalEpd, "originalEpd");
		UI.label(comp, tk, M.Urn);
		new UrnLink(editor).render(comp, tk);
	}

}
