package org.openlca.app.db;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.database.DatabaseContent;

class DatabaseWizardPage extends WizardPage {

	private Text nameText;
	private Button[] contentRadios;
	private DatabaseContent[] contentTypes;

	public DatabaseWizardPage() {
		super("database-wizard-page", Messages.NewDatabase,
				ImageType.NEW_WIZ_DATABASE.getDescriptor());
		setDescription(Messages.NewDatabase_Description);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		UI.gridLayout(composite, 2);
		nameText = UI.formText(composite, Messages.NewDatabase_Name);
		nameText.addModifyListener(new TextListener());
		UI.formLabel(composite, Messages.NewDatabase_RefData);
		createContentRadios(composite);
	}

	private void createContentRadios(Composite composite) {
		Composite radioGroup = new Composite(composite, SWT.NONE);
		radioGroup.setLayout(new RowLayout(SWT.VERTICAL));
		String[] labels = { Messages.EmptyDatabase,
				Messages.UnitsAndFlowProps, Messages.CompleteRefData };
		contentTypes = new DatabaseContent[] { DatabaseContent.EMPTY,
				DatabaseContent.UNITS, DatabaseContent.ALL_REF_DATA };
		contentRadios = new Button[3];
		for (int i = 0; i < 3; i++) {
			contentRadios[i] = new Button(radioGroup, SWT.RADIO);
			contentRadios[i].setText(labels[i]);
		}
		contentRadios[2].setSelection(true);
	}

	private class TextListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			String text = nameText.getText();
			validateName(text.toLowerCase());
		}
	}

	private void validateName(String text) {
		if (text == null) {
			error(Messages.NewDatabase_NameToShort);
			return;
		}
		String name = text.trim();
		if (name == null || name.length() < 4)
			error(Messages.NewDatabase_NameToShort);
		else if (name.equals("test") || name.equals("mysql"))
			error(name + " " + Messages.NewDatabase_ReservedName);
		else if (!isIdentifier(name))
			error(Messages.NewDatabase_InvalidName);
		else if (exists(name))
			error(Messages.NewDatabase_AlreadyExists);
		else {
			setMessage(null);
			setPageComplete(true);
		}
	}

	private void error(String string) {
		this.setMessage(string, DialogPage.ERROR);
		setPageComplete(false);

	}

	private boolean isIdentifier(String s) {
		if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0)))
			return false;
		for (int i = 1; i < s.length(); i++)
			if (!Character.isJavaIdentifierPart(s.charAt(i)))
				return false;
		return true;
	}

	private boolean exists(String name) {
		if (name == null)
			return false;
		for (IDatabaseConfiguration config : Database.getConfigurations()
				.getLocalDatabases()) {
			if (name.equalsIgnoreCase(config.getName()))
				return true;
		}
		return false;
	}

	PageData getPageData() {
		PageData data = new PageData();
		for (int i = 0; i < contentRadios.length; i++) {
			if (contentRadios[i].getSelection()) {
				data.contentType = contentTypes[i];
				break;
			}
		}
		data.databaseName = nameText.getText().trim();
		return data;
	}

	class PageData {
		DatabaseContent contentType = DatabaseContent.EMPTY;
		String databaseName;
	}

}
