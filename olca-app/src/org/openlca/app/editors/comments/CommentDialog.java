package org.openlca.app.editors.comments;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.UI;
import org.openlca.cloud.model.CommentDescriptor;
import org.openlca.cloud.model.Comments;

public class CommentDialog extends FormDialog {

	private final static DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
	private final String path;
	private final Comments comments;

	public CommentDialog(String path, Comments comments) {
		super(UI.shell());
		this.path = path;
		this.comments = comments;
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		form.getForm().setText("#Comments on " + path);
		Composite body = UI.formBody(form.getForm(), form.getToolkit());
		List<CommentDescriptor> comments = this.comments.get(path);
		int count = 0;
		for (CommentDescriptor comment : comments) {
			Composite wrapper = form.getToolkit().createComposite(body);
			UI.gridLayout(wrapper, 1, 5, 0);
			GridData wrapperData = UI.gridData(wrapper, true, false);
			if (comment.replyTo != 0) {
				wrapperData.horizontalIndent = 20;
			}
			Label title = UI.formLabel(wrapper, form.getToolkit(),
					comment.user + " wrote on " + formatter.format(comment.date));
			UI.gridData(title, true, false);
			Label text = UI.formLabel(wrapper, form.getToolkit(), comment.text);
			UI.gridData(text, true, false);
			count++;
			if (count != comments.size()) {
				UI.horizontalSeparator(wrapper);
			}
		}
	}

}