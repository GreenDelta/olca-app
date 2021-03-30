package org.openlca.app.results.comparison;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.results.ContributionResult;

/**
 * Overall information page of the analysis editor.
 */
public class ComparisonPage extends FormPage {

	private final ResultEditor<?> editor;

	public ComparisonPage(ResultEditor<?> editor) {
		super(editor, "ComparisonPage", "ComparisonPage");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ContributionResult result = editor.result;
		ScrolledForm form = UI.formHeader(mform, Labels.name(editor.setup.productSystem), Images.get(editor.result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		body.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				var client = body.getClientArea();
				Rectangle rect = new Rectangle(client.x + 0, client.x + 0, 200, 200);

				e.gc.drawRectangle(rect);

			}
		});

		form.reflow(true);
	}
}
