package org.openlca.app.editors.comments;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.model.Comment;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.Daos;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.jsonld.Json;
import org.openlca.util.Categories;
import org.openlca.util.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class CommentsPage extends FormPage {

	private static final Gson gson = new Gson();
	private final RootEntity model;
	private final List<Comment> comments;

	public CommentsPage(FormEditor editor, List<Comment> comments) {
		super(editor, "CommentsPage", M.Comments);
		this.model = null;
		Comments.sort(comments);
		this.comments = comments;
	}

	public CommentsPage(FormEditor editor, Comments comments, RootEntity model) {
		super(editor, "CommentsPage", M.Comments);
		this.model = model;
		this.comments = comments.getForRefId(model.refId);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var title = getTitle();
		Image image = null;
		if (model != null) {
			title += ": " + model.name;
			image = Images.get(model);
		}
		var form = UI.header(mForm, title, image);
		var body = UI.body(form, mForm.getToolkit());
		body.setLayout(new FillLayout());
		var browser = new Browser(body, SWT.NONE);
		browser.setJavascriptEnabled(true);

		UI.bindFunction(browser, "openModel", (args) -> {
			if (args == null || args.length < 2)
				return null;
			var type = args[0];
			var refId = args[1];
			if (type == null || refId == null)
				return null;
			App.open(getDescriptor(
					ModelType.valueOf(type.toString()),
					refId.toString()));
			return null;
		});

		UI.onLoaded(browser, HtmlFolder.getUrl("comments.html"), () -> {
			for (Comment comment : comments) {
				var json = gson.fromJson(gson.toJson(comment), JsonObject.class);
				Json.put(json, "label", CommentLabels.get(comment.type(), comment.path()));
				Json.put(json, "fullPath", getFullPath(comment));
				System.out.println(json);
				browser.execute("add(" + gson.toJson(json) + ");");
			}
		});

		form.reflow(true);
	}

	private String getFullPath(Comment comment) {
		if (model != null) // not needed
			return null;
		var descriptor = getDescriptor(comment.type(), comment.refId());
		var category = getCategory(descriptor);
		var categories = Categories.path(category);
		if (categories == null || categories.size() == 0)
			return descriptor.name;
		return Strings.join(categories, '/') + "/" + descriptor.name;
	}

	private RootDescriptor getDescriptor(ModelType type, String refId) {
		return Daos.root(Database.get(), type).getDescriptorForRefId(refId);
	}

	private Category getCategory(RootDescriptor descriptor) {
		if (descriptor.category == null)
			return null;
		return new CategoryDao(Database.get()).getForId(descriptor.category);
	}

	public class Js {

		public String getLabel(String path) {
			return CommentLabels.get(path);
		}

		public void openModel(String type, String refId) {
			App.open(getDescriptor(ModelType.valueOf(type), refId));
		}

	}

}
