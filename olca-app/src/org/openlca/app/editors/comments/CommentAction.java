package org.openlca.app.editors.comments;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.cloud.model.Comments;

public class CommentAction extends Action {

	private final String path;
	private final Comments comments;

	public static void bindTo(Section section, AbstractTableViewer<?> viewer, String path, Comments comments) {
		if (!Database.isConnected() || !comments.has(path)) {
			viewer.bindTo(section);
			return;
		}
		viewer.bindTo(section, new CommentAction(path, comments));
	}

	public CommentAction(String path, Comments comments) {
		this.path = path;
		this.comments = comments;
		setText("#Comment");
		setImageDescriptor(Icon.SHOW_COMMENTS.descriptor());
	}

	@Override
	public void run() {
		new CommentDialog(path, comments).open();
	}

}
