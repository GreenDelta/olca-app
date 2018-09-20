package org.openlca.app.editors;

import org.openlca.app.M;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentsPage;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.Comment;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommentsEditor extends SimpleFormEditor {

	private static final String TYPE = "CommentsEditor";
	private static final Logger log = LoggerFactory.getLogger(CommentsEditor.class);
	
	public static void open() {
		Editors.open(new SimpleEditorInput(TYPE, TYPE, M.Comments), TYPE);
	}

	public static void close() {
		for (IEditorReference ref : Editors.getReferences()) {
			try {
				if (!(ref.getEditorInput() instanceof SimpleEditorInput))
					continue;
				SimpleEditorInput input = (SimpleEditorInput) ref.getEditorInput();
				if (!TYPE.equals(input.type))
					continue;
				Editors.close(ref);
			} catch (PartInitException e) {
				log.error("Error closing editor " + ref.getId());
			}
		}

	}

	@Override
	protected FormPage getPage() {
		RepositoryClient client = Database.getRepositoryClient();
		List<Comment> comments = new ArrayList<>();
		try {
			comments = client.getAllComments();
		} catch (WebRequestException e) {
			log.error("Error loading comments" , e);
		}
		return new CommentsPage(this, comments);
	}

}
