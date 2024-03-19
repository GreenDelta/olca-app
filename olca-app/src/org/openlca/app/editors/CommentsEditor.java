package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Repository;
import org.openlca.app.editors.comments.CommentsPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.collaboration.model.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommentsEditor extends SimpleFormEditor {

	private static final String TYPE = "CommentsEditor";
	private static final Logger log = LoggerFactory.getLogger(CommentsEditor.class);

	public static void open() {
		Editors.open(new SimpleEditorInput(TYPE, M.Comments), TYPE);
	}

	public static void close() {
		for (IEditorReference ref : Editors.getReferences()) {
			try {
				if (!(ref.getEditorInput() instanceof SimpleEditorInput))
					continue;
				SimpleEditorInput input = (SimpleEditorInput) ref.getEditorInput();
				if (!TYPE.equals(input.id))
					continue;
				Editors.close(ref);
			} catch (PartInitException e) {
				log.error("Error closing editor " + ref.getId());
			}
		}

	}

	@Override
	protected FormPage getPage() {
		setTitleImage(Icon.COMMENTS_VIEW.get());
		List<Comment> comments = new ArrayList<>();
		if (Repository.CURRENT.isCollaborationServer()) {
			comments = WebRequests.execute(
					() -> Repository.CURRENT.server.getComments(Repository.CURRENT.id),
					new ArrayList<>());
		}
		return new CommentsPage(this, comments);
	}

}
