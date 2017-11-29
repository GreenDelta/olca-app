package org.openlca.app.editors.comments;

import javafx.scene.web.WebEngine;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.UI;
import org.openlca.cloud.model.Comment;
import org.openlca.cloud.model.Comments;

import com.google.gson.Gson;

public class CommentDialog extends FormDialog implements WebPage {

	private final String path;
	private final Comments comments;

	public CommentDialog(String path, Comments comments) {
		super(UI.shell());
		this.path = path;
		this.comments = comments;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		ScrolledForm form = UI.formHeader(mForm, "#Comments: " + CommentLabels.get(path));
		Composite body = UI.formBody(form, mForm.getToolkit());
		body.setLayout(new FillLayout());
		UI.createWebView(body, this);
		form.reflow(true);
	}

	@Override
	public String getUrl() {
		return HtmlView.COMMENTS.getUrl();
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		UI.bindVar(webkit, "java", new Js());
		Gson gson = new Gson();
		for (Comment comment : comments.getForPath(path)) {
			webkit.executeScript("add(" + gson.toJson(comment) + ", true);");
		}
	}

	public class Js {

		public String getLabel(String path) {
			return CommentLabels.get(path);
		}

	}

}