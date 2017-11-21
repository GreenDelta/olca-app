package org.openlca.app.editors.comments;

import javafx.scene.web.WebEngine;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.UI;
import org.openlca.cloud.model.CommentDescriptor;
import org.openlca.core.model.CategorizedEntity;

import com.google.gson.Gson;

public class CommentsPage<T extends CategorizedEntity> extends ModelPage<T> implements WebPage {

	public CommentsPage(ModelEditor<T> editor) {
		super(editor, "CommentsPage", "#Comments");
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		ScrolledForm form = UI.formHeader(this);
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
		String refId = getEditor().getModel().getRefId();
		for (CommentDescriptor comment : getComments().getForRefId(refId)) {
			webkit.executeScript("add(" + gson.toJson(comment) + ", false);");
		}
	}

	public class Js {

		public String getLabel(String path) {
			return CommentLabels.get(path);
		}

	}

}