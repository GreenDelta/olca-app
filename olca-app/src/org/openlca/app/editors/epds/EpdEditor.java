package org.openlca.app.editors.epds;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;

public class EpdEditor extends ModelEditor<Epd> {

	public EpdEditor() {
		super(Epd.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new EpdPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to open EPD", e);
		}
	}

	private static class EpdPage extends ModelPage<Epd> {

		private final EpdEditor editor;

		EpdPage(EpdEditor editor) {
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
			referenceSection(body, tk);
			new EpdModulesSection(editor).render(body, tk);
			body.setFocus();
			form.reflow(true);
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
}
