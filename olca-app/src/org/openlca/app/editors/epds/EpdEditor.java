package org.openlca.app.editors.epds;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Epd;
import org.openlca.core.model.Source;

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
			new InfoSection(editor).render(body, tk);
			new EpdProductSection(editor).render(body, tk);
			referenceSection(body, tk);
			new EpdModulesSection(editor).render(body, tk);
			body.setFocus();
			form.reflow(true);
		}

		private void referenceSection(Composite body, FormToolkit tk) {
			var refComp = UI.formSection(body, tk, "References", 2);
			ModelLink.of(Actor.class)
				.renderOn(refComp, tk, "Manufacturer")
				.setModel(epd().manufacturer)
				.onChange(actor -> {
					epd().manufacturer = actor;
					editor.setDirty();
				});

			ModelLink.of(Actor.class)
				.renderOn(refComp, tk, "Program operator")
				.setModel(epd().programOperator)
				.onChange(actor -> {
					epd().programOperator = actor;
					editor.setDirty();
				});

			ModelLink.of(Source.class)
				.renderOn(refComp, tk, "PCR")
				.setModel(epd().pcr)
				.onChange(source -> {
					epd().pcr = source;
					editor.setDirty();
				});

			ModelLink.of(Actor.class)
				.renderOn(refComp, tk, "Verifier")
				.setModel(epd().verifier)
				.onChange(actor -> {
					epd().verifier = actor;
					editor.setDirty();
				});
		}

		private Epd epd() {
			return getModel();
		}
	}
}
