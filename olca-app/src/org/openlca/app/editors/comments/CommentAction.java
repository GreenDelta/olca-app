package org.openlca.app.editors.comments;

import org.openlca.app.M;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.cloud.model.Comments;

public class CommentAction extends Action {

	private final String path;
	private final Comments comments;

	public static void bindTo(Section section, AbstractTableViewer<?> viewer, String path, Comments comments) {
		if (!App.isCommentingEnabled() || comments == null || !comments.hasPath(path)) {
			viewer.bindTo(section);
			return;
		}
		viewer.bindTo(section, new CommentAction(path, comments));
	}

	public static void bindTo(TableViewer viewer, String path, Comments comments, Action... other) {
		List<Action> actions = new ArrayList<>(Arrays.asList(other));
		if (App.isCommentingEnabled() && comments != null && comments.hasPath(path)) {
			actions.add(new CommentAction(path, comments));
		}
		if (actions.isEmpty())
			return;
		Actions.bind(viewer, actions.toArray(new Action[actions.size()]));
	}

	public static void bindTo(Section section, String path, Comments comments, Action... other) {
		List<Action> actions = new ArrayList<>(Arrays.asList(other));
		if (App.isCommentingEnabled() && comments != null && comments.hasPath(path)) {
			actions.add(new CommentAction(path, comments));
		}
		if (actions.isEmpty())
			return;
		Actions.bind(section, actions.toArray(new Action[actions.size()]));
	}

	public CommentAction(String path, Comments comments) {
		this.path = path;
		this.comments = comments;
		setText(M.ShowComments);
		setImageDescriptor(Icon.SHOW_COMMENTS.descriptor());
	}

	@Override
	public void run() {
		new CommentDialog(path, comments).open();
	}

}
