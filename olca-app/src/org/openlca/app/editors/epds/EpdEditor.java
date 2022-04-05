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
			var form = UI.formHeader(this);
			var tk = mForm.getToolkit();
			var body = UI.formBody(form, tk);

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
			var comp = UI.formSection(body, tk, "References", 3);
			modelLink(comp, "Manufacturer", "manufacturer");
			modelLink(comp, "Program operator", "programOperator");
			modelLink(comp, "PCR", "pcr");
			modelLink(comp, "Verifier", "verifier");

			UI.formLabel(comp, tk, "URN");
			new UrnLink(editor).render(comp, tk);
		}
	}
}
