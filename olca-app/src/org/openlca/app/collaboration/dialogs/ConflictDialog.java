package org.openlca.app.collaboration.dialogs;

import java.util.Collections;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.navigation.actions.ConflictResolutions;
import org.openlca.app.collaboration.viewers.diff.ConflictViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.util.UI;
import org.openlca.git.actions.ConflictResolver.ConflictResolution;
import org.openlca.git.actions.ConflictResolver.GitContext;

public class ConflictDialog extends FormDialog {

	private static final int OVERWRITE = 2;
	private static final int KEEP = 3;
	private final Repository repo;
	private final ConflictResolutions resolutions;
	private final GitContext context;
	private final DiffNode rootNode;
	private ConflictViewer viewer;

	public ConflictDialog(Repository repo, ConflictResolutions resolutions, GitContext context, DiffNode rootNode) {
		super(UI.shell());
		this.repo = repo;
		this.resolutions = resolutions;
		this.context = context;
		this.rootNode = rootNode;
		setBlockOnOpen(true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1000, 800);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = context == GitContext.LOCAL
				? UI.header(mform, M.ResolveLocalConflicts)
				: UI.header(mform, M.ResolveWorkspaceConflicts);
		var toolkit = mform.getToolkit();
		var body = UI.body(form, toolkit);
		viewer = new ConflictViewer(body, repo, resolutions, context);
		viewer.setOnMerge(() -> getButton(OK).setEnabled(!viewer.hasConflicts()));
		form.reflow(true);
		viewer.setInput(Collections.singletonList(rootNode));
	}

	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		if (id == OK)
			button.setEnabled(!viewer.hasConflicts());
		return button;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (context == GitContext.LOCAL) {
			createButton(parent, OVERWRITE, M.OverwriteLocalChanges, false);
		} else {
			createButton(parent, OVERWRITE, M.OverwriteWorkspaceChanges, false);
		}
		createButton(parent, KEEP, M.OverwriteRemoteChanges, false);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OVERWRITE) {
			solveAll(ConflictResolution.overwrite(context));
		} else if (buttonId == KEEP) {
			solveAll(ConflictResolution.keep(context));
		} else {
			super.buttonPressed(buttonId);
		}
	}

	private void solveAll(ConflictResolution resolution) {
		viewer.solveAll(resolution);
		viewer.refresh();
		getButton(OK).setEnabled(true);
		getButton(OVERWRITE).setEnabled(false);
		getButton(KEEP).setEnabled(false);
	}

}
