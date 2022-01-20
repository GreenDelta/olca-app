package org.openlca.app.editors.comments;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.model.Comment;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.util.UI;

import com.google.gson.Gson;

public class CommentDialog extends FormDialog {

	private final String path;
	private final Comments comments;

	public CommentDialog(String path, Comments comments) {
		super(UI.shell());
		this.path = path;
		this.comments = comments;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		ScrolledForm form = UI.formHeader(mForm,
				M.Comments + CommentLabels.get(path));
		Composite body = UI.formBody(form, mForm.getToolkit());
		body.setLayout(new FillLayout());
		Browser browser = new Browser(body, SWT.NONE);
		browser.setJavascriptEnabled(true);
		UI.bindFunction(browser, "getLabel", (args) -> {
			if (args == null || args.length == 0)
				return "";
			Object path = args[0];
			if (path == null)
				return "";
			return CommentLabels.get(path.toString());
		});

		UI.onLoaded(browser, HtmlFolder.getUrl("comments.html"), () -> {
			Gson gson = new Gson();
			for (Comment comment : comments.getForPath(path)) {
				browser.execute("add(" + gson.toJson(comment) + ", true);");
			}
		});
		form.reflow(true);
	}
}