package org.openlca.app.editors.epds;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Controls;
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
		var comp = UI.formSection(body, tk, "#Time and location", 3);
		UI.label(comp, tk, "#Publication date");
		dateBox(comp, getModel().validFrom, d -> getModel().validFrom = d);
		UI.filler(comp, tk);

		UI.label(comp, tk, "#Valid until");
		dateBox(comp, getModel().validUntil, d -> getModel().validUntil = d);
		UI.filler(comp, tk);

		modelLink(comp, M.Location, "location");
	}

	private void dateBox(Composite comp, Date date, Consumer<Date> fn) {
		var box = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		if (date != null) {
			var cal = new GregorianCalendar();
			cal.setTime(date);
			box.setDate(
					cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH),
					cal.get(Calendar.DAY_OF_MONTH)
			);
		}
		box.addSelectionListener(Controls.onSelect($ -> {
			var next = new GregorianCalendar(
					box.getYear(), box.getMonth(), box.getDay()).getTime();
			fn.accept(next);
			editor.setDirty();
		}));
	}

	private void technologySection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Technology, 3);
		multiText(comp, "#Manufacturing description", "manufacturing");
		multiText(comp, "#Product usage description", "productUsage");
	}

	private void modellingSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "#Modelling and validation", 3);

		// EPD type combo
		var typeCombo = UI.labeledCombo(comp, tk, "#EPD Type");
		var types = EpdType.values();
		var items = new String[types.length + 1];
		items[0] = "";
		int selected = 0;
		for (int i = 0; i < types.length; i++) {
			var type = types[i];
			items[i + 1] = labelOf(type);
			if (type == getModel().epdType) {
				selected = i + 1;
			}
		}
		typeCombo.setItems(items);
		typeCombo.select(selected);

		Controls.onSelect(typeCombo, $ -> {
			int i = typeCombo.getSelectionIndex();
			getModel().epdType = i > 0 ? types[i-1] : null;
			editor.setDirty();
		});
		UI.filler(comp, tk);

		modelLink(comp, "PCR", "pcr");
		modelLink(comp, M.DataGenerator, "dataGenerator");
		modelLink(comp, M.Verifier, "verifier");
		multiText(comp, M.UseAdvice, "useAdvice");
	}

	private void publicationSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "#Publication and ownership", 3);
		modelLink(comp, "#Declaration owner / Manufacturer", "manufacturer");
		modelLink(comp, M.ProgramOperator, "programOperator");
		text(comp, "#Registration number", "registrationId");
		modelLink(comp, "#Original EPD", "originalEpd");
		UI.label(comp, tk, "URN");
		new UrnLink(editor).render(comp, tk);
	}

	private String labelOf(EpdType type) {
		if (type == null)
			return "";
		return switch (type) {
			case AVERAGE_DATASET -> "#Average dataset";
			case GENERIC_DATASET -> "#Generic dataset";
			case REPRESENTATIVE_DATASET -> "#Representative dataset";
			case SPECIFIC_DATASET -> "#Specific dataset";
			case TEMPLATE_DATASET -> "#Template dataset";
		};
	}

}
