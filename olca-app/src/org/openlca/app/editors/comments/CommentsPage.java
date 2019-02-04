package org.openlca.app.editors.comments;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.cloud.model.Comment;
import org.openlca.cloud.model.Comments;
import org.openlca.cloud.util.Datasets;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.util.Strings;

import com.google.gson.Gson;

import javafx.scene.web.WebEngine;

public class CommentsPage extends FormPage implements WebPage {

	private final CategorizedEntity model;
	private final List<Comment> comments;

	public CommentsPage(FormEditor editor, List<Comment> comments) {
		super(editor, "CommentsPage", M.Comments);
		this.model = null;
		Comments.sort(comments);
		this.comments = comments;
	}

	public CommentsPage(FormEditor editor, Comments comments, CategorizedEntity model) {
		super(editor, "CommentsPage", M.Comments);
		this.model = model;
		this.comments = comments.getForRefId(model.refId);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		String title = getTitle();
		Image image = null;
		if (model != null) {
			title += ": " + model.name;
			image = Images.get(model);
		}
		ScrolledForm form = UI.formHeader(mForm, title, image);
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
		for (Comment comment : comments) {
			String fullPath = getFullPath(comment);
			fullPath = fullPath != null ? "'" + fullPath + "'" : fullPath;
			webkit.executeScript("add(" + gson.toJson(comment) + ", false, " + fullPath + ");");
		}
	}

	private String getFullPath(Comment comment) {
		if (model != null) // not needed
			return null;
		CategorizedDescriptor descriptor = getDescriptor(comment.type, comment.refId);
		Category category = getCategory(descriptor);
		List<String> categories = Datasets.getCategories(category);
		if (categories == null || categories.size() == 0)
			return descriptor.name;
		return Strings.join(categories, '/') + "/" + descriptor.name;
	}

	private CategorizedDescriptor getDescriptor(ModelType type, String refId) {
		return Daos.categorized(Database.get(), type).getDescriptorForRefId(refId);
	}

	private Category getCategory(CategorizedDescriptor descriptor) {
		if (descriptor.category == null)
			return null;
		return new CategoryDao(Database.get()).getForId(descriptor.category);
	}

	public class Js {

		public String getLabel(String path) {
			return CommentLabels.get(path);
		}

		public void openModel(String type, String refId) {
			App.openEditor(getDescriptor(ModelType.valueOf(type), refId));
		}

	}

}