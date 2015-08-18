package org.openlca.app.editors.social;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.SocialIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocialIndicatorEditor extends ModelEditor<SocialIndicator>
		implements IEditor {

	public static String ID = "editors.socialindicator";
	private Logger log = LoggerFactory.getLogger(getClass());

	public SocialIndicatorEditor() {
		super(SocialIndicator.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	private class Page extends ModelPage<SocialIndicator> {

		private SocialIndicatorEditor editor;

		Page() {
			super(SocialIndicatorEditor.this, "SocialIndicatorPage",
					Messages.GeneralInformation);
			editor = SocialIndicatorEditor.this;
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = UI.formHeader(managedForm, "#Social indicator: "
					+ getModel().getName());
			FormToolkit toolkit = managedForm.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			InfoSection infoSection = new InfoSection(getEditor());
			infoSection.render(body, toolkit);
			createAdditionalInfo(body, toolkit);
			body.setFocus();
			form.reflow(true);
		}

		private void createAdditionalInfo(Composite body, FormToolkit tk) {
			Composite comp = UI.formSection(body, tk,
					Messages.AdditionalInformation);
			Text ut = UI.formText(comp, tk, "Unit of measurement");
			if (getModel().unit != null)
				ut.setText(getModel().unit);
			ut.addModifyListener((e) -> {
				getModel().unit = ut.getText();
				editor.setDirty(true);
			});
			Text et = UI.formMultiText(comp, tk, "Evaluation scheme");
			if (getModel().evaluationScheme != null)
				et.setText(getModel().evaluationScheme);
			et.addModifyListener((e) -> {
				getModel().evaluationScheme = et.getText();
				editor.setDirty(true);
			});
		}

	}
}
