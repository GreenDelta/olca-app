package org.openlca.app.editors.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ResultModel;

class ResultPage extends ModelPage<ResultModel> {

	private final ResultEditor editor;

	ResultPage(ResultEditor editor) {
		super(editor, "ResultPage", "Result");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		new InfoSection(editor)
			.render(body, tk)
			.section()
			.setExpanded(false);
		new SetupSection(editor).render(body, tk);

		var sash = new SashForm(body, SWT.VERTICAL);
		UI.gridLayout(sash, 1);
		UI.gridData(sash, true, false);
		tk.adapt(sash);
		new ImpactSection(editor).render(sash, tk);
		FlowSection.forInputs(editor).render(sash, tk);
		FlowSection.forOutputs(editor).render(sash, tk);

		body.setFocus();
		form.reflow(true);
	}
}
