package org.openlca.app.editors.results;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Result;
import org.openlca.util.Strings;

public class ResultEditor extends ModelEditor<Result> {

	public ResultEditor() {
		super(Result.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to open result", e);
		}
	}

	private static class Page extends ModelPage<Result> {

		private final ResultEditor editor;

		Page(ResultEditor editor) {
			super(editor, "ResultPage", M.Result);
			this.editor = editor;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.header(this);
			var tk = mform.getToolkit();
			var body = UI.body(form, tk);

			var infoSection = new InfoSection(editor).render(body, tk);
			var comp = infoSection.composite();
			modelLink(comp, M.ProductSystem, "productSystem");
			modelLink(comp, M.LciaMethod, "impactMethod");

			var sash = new SashForm(body, SWT.VERTICAL);
			UI.gridLayout(sash, 1);
			UI.gridData(sash, true, false);
			tk.adapt(sash);

			new ImpactSection(editor).render(sash, tk);
			editor.getModel().flowResults.sort((ri, rj) -> {
				var ni = Labels.name(ri.flow);
				var nj = Labels.name(rj.flow);
				return Strings.compare(ni, nj);
			});
			FlowSection.forInputs(editor).render(sash, tk);
			FlowSection.forOutputs(editor).render(sash, tk);

			body.setFocus();
			form.reflow(true);
		}
	}
}
