package org.openlca.app.editors.lcia_methods;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactCategoryEditor extends ModelEditor<ImpactCategory> {

	public ImpactCategoryEditor() {
		super(ImpactCategory.class);
	}

	@Override
	protected void addPages() {
		// TODO LCIA factors, parameters, regionalization
		try {
			addPage(new InfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to init pages", e);
		}
	}

	static class InfoPage extends ModelPage<ImpactCategory> {

		InfoPage(ImpactCategoryEditor editor) {
			super(editor, "ImpactCategoryInfo", M.GeneralInformation);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(this);
			FormToolkit toolkit = mform.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			InfoSection infoSection = new InfoSection(getEditor());
			infoSection.render(body, toolkit);
			body.setFocus();
			form.reflow(true);
		}
	}

}
