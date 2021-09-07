package org.openlca.app.editors.results;

import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ResultModel;

class ResultPage extends ModelPage<ResultModel> {

	ResultPage(ResultEditor editor) {
		super(editor, "ResultPage", "Result");
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		var infoSection = new InfoSection(getEditor());
		infoSection.render(body, tk);
		body.setFocus();
		form.reflow(true);
	}
}
