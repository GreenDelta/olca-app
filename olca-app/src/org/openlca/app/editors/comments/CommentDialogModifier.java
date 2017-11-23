package org.openlca.app.editors.comments;

import java.util.function.Function;

import org.openlca.app.viewers.table.modify.DialogModifier;
import org.openlca.cloud.model.Comments;

public class CommentDialogModifier<T> extends DialogModifier<T> {

	private final Comments comments;
	private final Function<T, String> getPath;

	public CommentDialogModifier(Comments comments, Function<T, String> getPath) {
		this.comments = comments;
		this.getPath = getPath;
	}

	@Override
	protected void openDialog(T element) {
		String path = getPath.apply(element);
		if (path == null || comments == null || !comments.hasPath(path))
			return;
		new CommentDialog(path, comments).open();
	}

}
