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

class DocPage extends ModelPage<Epd> {

	private final EpdEditor editor;

	DocPage(EpdEditor editor) {
		super(editor, "EpdDocPage", M.Documentation);
		this.editor = editor;
	}

	@Override
	protected  void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		timeSection(body, tk);
		locationSection(body, tk);
		technologySection(body, tk);
		modellingSection(body, tk);
		publicationSection(body, tk);
	}

	private void timeSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Time);
		UI.label(comp, tk, "#Publication date");
		dateBox(comp, getModel().validFrom, d -> getModel().validFrom = d);
		UI.label(comp, tk, "#Valid until");
		dateBox(comp, getModel().validUntil, d -> getModel().validUntil = d);

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

	private void locationSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Location);

	}

	private void technologySection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Technology);

	}

	private void modellingSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "#Modelling and validation");

	}

	private void publicationSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "#Publication and ownership");

	}
}
