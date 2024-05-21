package org.openlca.app.editors.comments;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.util.UI;

import com.google.gson.Gson;

public class CommentDialog extends FormDialog {

	private static final Gson gson = new Gson();
	private final String path;
	private final Comments comments;

	public CommentDialog(String path, Comments comments) {
		super(UI.shell());
		this.path = path;
		this.comments = comments;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var title = String.join(" - ", M.Comments, CommentLabels.get(path));
		var form = UI.header(mForm, title);
		var body = UI.body(form, mForm.getToolkit());
		body.setLayout(new FillLayout());
		var browser = new Browser(body, SWT.NONE);
		browser.setJavascriptEnabled(true);
		UI.onLoaded(browser, HtmlFolder.getUrl("comments.html"), () -> {
			for (var comment : comments.getForPath(path)) {
				browser.execute("add(" + gson.toJson(comment) + ");");
			}
		});
		form.reflow(true);
	}
}
