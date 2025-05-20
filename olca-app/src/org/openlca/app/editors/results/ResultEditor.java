package org.openlca.app.editors.results;

import java.util.Comparator;
import java.util.Objects;

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
import org.openlca.core.model.FlowResult;
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
			var result = editor.getModel();
			result.flowResults.sort(new FlowResultComparator(result));

			FlowSection.forInputs(editor).render(sash, tk);
			FlowSection.forOutputs(editor).render(sash, tk);

			body.setFocus();
			form.reflow(true);
		}
	}

	private record FlowResultComparator(Result result)
			implements Comparator<FlowResult> {

		@Override
		public int compare(FlowResult ri, FlowResult rj) {

			// list the reference flow first
			if (Objects.equals(result.referenceFlow, ri))
				return -1;
			if (Objects.equals(result.referenceFlow, rj))
				return 1;

			// check flow types
			int ti = typeOf(ri);
			int tj = typeOf(rj);
			if (ti != tj)
				return ti - tj;

			// check the flow name
			var ni = Labels.name(ri.flow);
			var nj = Labels.name(rj.flow);
			var c =  Strings.compare(ni, nj);
			if (c != 0)
				return c;

			// check the category
			var pi = ri.flow != null && ri.flow.category != null
					? ri.flow.category.toPath()
					: null;
			var pj = rj.flow != null && rj.flow.category != null
					? rj.flow.category.toPath()
					: null;
			return Strings.compare(pi, pj);
		}

		private int typeOf(FlowResult r) {
			if (r.flow == null)
				return -1;
			return switch (r.flow.flowType) {
				case PRODUCT_FLOW -> 0;
				case WASTE_FLOW -> 1;
				case ELEMENTARY_FLOW -> 2;
				case null -> -1;
			};
		}
	}
}
