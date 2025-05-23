package org.openlca.app.collaboration.browse.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.collaboration.browse.ServerNavigator;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.ServerElement;
import org.openlca.app.collaboration.util.CredentialStore;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

class CreateRepositoryAction extends Action implements IServerNavigationAction {

	private ServerElement elem;

	CreateRepositoryAction() {
		setText(M.CreateRepository);
		setImageDescriptor(Icon.REPOSITORY.descriptor(Overlay.NEW));
	}

	@Override
	public void run() {
		var groups = WebRequests.execute(elem.getClient()::listWritableGroups);
		if (groups == null)
			return;
		if (groups.isEmpty()) {
			MsgBox.info(M.NotAllowedToCreateRepositories);
			return;
		}
		var dialog = new CreateRepositoryDialog(groups);
		if (dialog.open() != CreateRepositoryDialog.OK)
			return;
		if (!WebRequests.execute(() -> elem.getClient().createRepository(dialog.repositoryId())))
			return;
		ServerNavigator.refresh();
	}

	@Override
	public boolean accept(List<IServerNavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof ServerElement serverElem))
			return false;
		this.elem = serverElem;
		return true;
	}

	private class CreateRepositoryDialog extends FormDialog {

		private final List<String> groups;
		private String group;
		private String name;

		private CreateRepositoryDialog(List<String> groups) {
			super(UI.shell());
			setBlockOnOpen(true);
			this.groups = groups;
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			var toolkit = form.getToolkit();
			var formBody = UI.header(form, toolkit,
					M.CreateRepository, M.SelectGroupAndEnterRepositoryName);
			var container = UI.composite(formBody, toolkit);
			UI.gridLayout(container, 2);
			UI.gridData(container, true, false);
			var groupCombo = UI.labeledCombo(container, toolkit, M.Group);
			groupCombo.setItems(groups.toArray(new String[groups.size()]));
			groupCombo.addModifyListener(e -> {
				group = groups.get(groupCombo.getSelectionIndex());
				updateButtons();
			});
			var initialGroup = getInitialGroup();
			if (initialGroup != null) {
				groupCombo.select(initialGroup);
			}
			var nameText = UI.labeledText(container, toolkit, M.Name, SWT.NONE);
			nameText.addModifyListener(e -> {
				name = nameText.getText();
				updateButtons();
			});
			UI.label(container);
			UI.label(container, toolkit, M.RepositoryNameHint);
			form.getForm().reflow(true);
			nameText.setFocus();
		}

		private Integer getInitialGroup() {
			var username = CredentialStore.getUsername(elem.getClient().url);
			if (Strings.nullOrEmpty(username))
				return null;
			for (var i = 0; i < groups.size(); i++) 
				if (username.equals(groups.get(i)))
					return i;
			return null;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
			var ok = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			ok.setEnabled(false);
			setButtonLayoutData(ok);
		}

		private void updateButtons() {
			var button = getButton(IDialogConstants.OK_ID);
			if (button == null)
				return;
			button.setEnabled(isComplete());
		}

		private boolean isComplete() {
			if (Strings.nullOrEmpty(group))
				return false;
			if (Strings.nullOrEmpty(name) || name.length() < 4 || !name.matches("^[a-zA-Z0-9_]+$"))
				return false;
			return true;
		}

		private String repositoryId() {
			return group + "/" + name;
		}

	}

}