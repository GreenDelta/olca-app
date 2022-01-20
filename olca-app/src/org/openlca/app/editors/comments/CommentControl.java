package org.openlca.app.editors.comments;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

public class CommentControl {

	private String path;
	private Comments comments;

	public CommentControl(Composite parent, FormToolkit tk, String path, Comments comments) {
		this.path = path;
		this.comments = comments;
		initControl(parent, tk);
	}

	private void initControl(Composite parent, FormToolkit tk) {
		if (!App.isCommentingEnabled() || comments == null || !comments.hasPath(path)) {
			UI.filler(parent, tk);
			return;
		}
		ImageHyperlink control = new ImageHyperlink(parent, SWT.NONE);
		UI.gridData(control, false, false).verticalAlignment = SWT.TOP;
		Controls.onClick(control, (e) -> {
			new CommentDialog(path, comments).open();
		});
		control.setImage(Icon.COMMENT.get());
		control.setToolTipText(M.Comment);
	}

}
