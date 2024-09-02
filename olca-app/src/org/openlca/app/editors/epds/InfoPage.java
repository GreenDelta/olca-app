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
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;

class InfoPage extends ModelPage<Epd> {

	private final EpdEditor editor;

	InfoPage(EpdEditor editor) {
		super(editor, "EpdInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		var info = new InfoSection(editor).render(body, tk);
		UI.filler(info.composite());
		new UploadButton(editor).render(info.composite(), tk);

		new EpdProductSection(editor).render(body, tk);
		timeSection(body, tk);

		referenceSection(body, tk);
		new EpdModulesSection(editor).render(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void timeSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Time);
		UI.label(comp, tk, "#Publication date");
		var startBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		setDate(startBox, getModel().validFrom);
		onDateChanged(startBox, date -> getModel().validFrom = date);

		UI.label(comp, tk, "#Valid until");
		var endBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		setDate(endBox, getModel().validUntil);
		onDateChanged(endBox, date -> getModel().validUntil = date);
	}

	private void setDate(DateTime widget, Date date) {
		if (date == null)
			return;
		var cal = new GregorianCalendar();
		cal.setTime(date);
		widget.setDate(
				cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH)
		);
	}

	private void onDateChanged(DateTime widget, Consumer<Date> fn) {
		widget.addSelectionListener(Controls.onSelect($ -> {
			var date = new GregorianCalendar(
					widget.getYear(), widget.getMonth(), widget.getDay()).getTime();
			fn.accept(date);
			editor.setDirty();
		}));
	}

	private void referenceSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.References, 3);
		modelLink(comp, M.Manufacturer, "manufacturer");
		modelLink(comp, M.ProgramOperator, "programOperator");
		modelLink(comp, "PCR", "pcr");
		modelLink(comp, M.Verifier, "verifier");

		UI.label(comp, tk, "URN");
		new UrnLink(editor).render(comp, tk);
	}
}
