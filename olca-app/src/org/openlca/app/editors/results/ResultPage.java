package org.openlca.app.editors.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.components.ModelLink;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Result;

class ResultPage extends ModelPage<Result> {

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

		// info section
		var infoSection = new InfoSection(editor).render(body, tk);
		var comp = infoSection.composite();

		// product system
		ModelLink.of(ProductSystem.class)
			.renderOn(comp, tk, M.ProductSystem)
			.setModel(getModel().productSystem)
			.onChange(system -> {
				editor.getModel().productSystem = system;
				editor.setDirty();
			});
		UI.filler(comp, tk);

		// LCIA method
		ModelLink.of(ImpactMethod.class)
			.renderOn(comp, tk, "LCIA method")
			.setModel(getModel().impactMethod)
			.onChange(method -> {
				var result = editor.getModel();
				result.impactMethod = method;
				editor.setDirty();
			});
		UI.filler(comp, tk);

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
