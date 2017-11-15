package org.openlca.app.editors.comments;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.cloud.model.Comments;

public class CommentControl {

	private String path;
	private Comments comments;

	public CommentControl(Composite parent, FormToolkit toolkit, String path, Comments comments) {
		this.path = path;
		this.comments = comments;
		initControl(parent, toolkit);
	}

	private void initControl(Composite parent, FormToolkit toolkit) {
		if (!Database.isConnected() || !comments.has(path)) {
			UI.filler(parent, toolkit);
			return;
		}
		ImageHyperlink control = new ImageHyperlink(parent, SWT.NONE);
		UI.gridData(control, false, false).verticalAlignment = SWT.TOP;
		Controls.onClick(control, (e) -> {
			new CommentDialog(path, comments).open();
		});
		control.setImage(Icon.SHOW_COMMENTS.get());
		control.setToolTipText("#Comment");
	}


}
