package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentsPage;
import org.openlca.app.util.DefaultInput;
import org.openlca.app.util.Editors;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.CommentDescriptor;
import org.openlca.cloud.util.WebRequests.WebRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommentsEditor extends SimpleFormEditor {

	private static final String TYPE = "CommentsEditor";
	private static final Logger log = LoggerFactory.getLogger(CommentsEditor.class);
	
	public static void open() {
		Editors.open(new DefaultInput(TYPE, TYPE, "#Comments"), TYPE);
	}

	public static void close() {
		for (IEditorReference ref : Editors.getReferences()) {
			try {
				if (!(ref.getEditorInput() instanceof DefaultInput))
					continue;
				DefaultInput input = (DefaultInput) ref.getEditorInput();
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
		List<CommentDescriptor> comments = new ArrayList<>();
		try {
			comments = client.getAllComments();
		} catch (WebRequestException e) {
			log.error("Error loading comments" , e);
		}
		return new CommentsPage(this, comments);
	}

}
