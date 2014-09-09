package org.openlca.app.db;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.database.DatabaseContent;
import org.openlca.core.database.DbUtils;

class DatabaseWizardPage extends WizardPage {

	private Text nameText;
	private StackLayout stackLayout;
	private Button buttonLocal;
	private Button buttonRemote;
	private Composite localComposite;
	private Button[] contentRadios;
	private Composite remoteComposite;
	private Text hostText;
	private Text portText;
	private Text userText;
	private Text passwordText;
	private Composite stackComposite;

	public DatabaseWizardPage() {
		super("database-wizard-page", Messages.NewDatabase,
				ImageType.NEW_WIZ_DATABASE.getDescriptor());
		setDescription(Messages.CreateANewDatabase);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		UI.gridLayout(root, 1);
		Composite header = new Composite(root, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		setControl(header);
		UI.gridLayout(header, 2);
		nameText = UI.formText(header, Messages.DatabaseName);
		nameText.addModifyListener((e) -> validateInput());
		createTypeRadios(header);
		createStackComposite(root);
	}

	private void createTypeRadios(Composite headerComposite) {
		UI.formLabel(headerComposite, Messages.DatabaseType);
		Composite radioGroup = new Composite(headerComposite, SWT.NONE);
		radioGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		UI.gridLayout(radioGroup, 2, 10, 0);
		buttonLocal = new Button(radioGroup, SWT.RADIO);
		buttonLocal.setText(Messages.Local);
		buttonLocal.setSelection(true);
		Controls.onSelect(buttonLocal, (e) -> typeChanged());
		buttonRemote = new Button(radioGroup, SWT.RADIO);
		buttonRemote.setText(Messages.Remote);
		Controls.onSelect(buttonRemote, (e) -> typeChanged());
	}

	private void typeChanged() {
		if (buttonLocal.getSelection()) {
			stackLayout.topControl = localComposite;
			stackComposite.layout();
		} else if (buttonRemote.getSelection()) {
			stackLayout.topControl = remoteComposite;
			stackComposite.layout();
		}
		validateInput();
	}

	private void createStackComposite(Composite rootComposite) {
		Label separator = new Label(rootComposite, SWT.SEPARATOR
				| SWT.HORIZONTAL);
		UI.gridData(separator, true, true);
		stackComposite = new Composite(rootComposite, SWT.NONE);
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		stackLayout = new StackLayout();
		stackComposite.setLayout(stackLayout);
		createLocalComposite(stackComposite);
		createRemoteComposite(stackComposite);
	}

	private void createLocalComposite(Composite stackComposite) {
		localComposite = new Composite(stackComposite, SWT.NONE);
		stackLayout.topControl = localComposite;
		localComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		UI.gridLayout(localComposite, 2);
		UI.formLabel(localComposite, Messages.DatabaseContent);
		createContentRadios(localComposite);
	}

	private void createRemoteComposite(Composite stackComposite) {
		remoteComposite = new Composite(stackComposite, SWT.NONE);
		UI.gridLayout(remoteComposite, 2);
		hostText = UI.formText(remoteComposite, Messages.Host);
		hostText.addModifyListener((e) -> validateInput());
		portText = UI.formText(remoteComposite, Messages.Port);
		portText.addModifyListener((e) -> validateInput());
		userText = UI.formText(remoteComposite, Messages.User);
		userText.addModifyListener((e) -> validateInput());
		passwordText = UI.formText(remoteComposite, Messages.Password);
	}

	private void createContentRadios(Composite composite) {
		Composite radioGroup = new Composite(composite, SWT.NONE);
		radioGroup.setLayout(new RowLayout(SWT.VERTICAL));
		contentRadios = new Button[DatabaseContent.values().length];
		for (int i = 0; i < DatabaseContent.values().length; i++) {
			contentRadios[i] = new Button(radioGroup, SWT.RADIO);
			contentRadios[i].setText(contentLabel(DatabaseContent.values()[i]));
		}
		contentRadios[2].setSelection(true);
	}

	private String contentLabel(DatabaseContent content) {
		if (content == null)
			return null;
		switch (content) {
		case EMPTY:
			return Messages.EmptyDatabase;
		case UNITS:
			return Messages.UnitsAndFlowProperties;
		case ALL_REF_DATA:
			return Messages.CompleteReferenceData;
		default:
			return null;
		}
	}

	private void validateInput() {
		boolean valid = validateName(nameText.getText());
		if (!valid)
			return;
		if (buttonLocal.getSelection()) {
			setMessage(null);
			setPageComplete(true);
			return;
		}
		if (hostText.getText().isEmpty()) {
			error(Messages.PleaseSpecifyHost);
			return;
		}
		if (userText.getText().isEmpty()) {
			error(Messages.PleaseSpecifyUser);
			return;
		}
		valid = validatePort(portText.getText());
		if (!valid)
			return;
		setMessage(null);
		setPageComplete(true);
	}

	private boolean validatePort(String port) {
		if (port.isEmpty())
			error(Messages.PleaseSpecifyPortNumber);
		else
			try {
				Integer.parseInt(port);
				return true;
			} catch (NumberFormatException e) {
				error(Messages.PleaseSpecifyPortNumber);
			}
		return false;
	}

	private boolean validateName(String name) {
		if (name == null || name.length() < 4)
			error(Messages.NewDatabase_NameToShort);
		else if (!DbUtils.isValidName(name))
			error(Messages.NewDatabase_InvalidName);
		else if (Database.getConfigurations().nameExists(name))
			error(Messages.NewDatabase_AlreadyExists);
		else
			return true;
		return false;
	}

	private void error(String string) {
		setMessage(string, DialogPage.ERROR);
		setPageComplete(false);
	}

	IDatabaseConfiguration getPageData() {
		if (buttonLocal.getSelection()) {
			DerbyConfiguration derbyConfig = new DerbyConfiguration();
			derbyConfig.setName(getText(nameText));
			return derbyConfig;
		} else {
			MySQLConfiguration config = new MySQLConfiguration();
			config.setName(getText(nameText));
			config.setHost(getText(hostText));
			config.setPort(Integer.parseInt(getText(portText)));
			config.setPassword(getText(passwordText));
			config.setUser(getText(userText));
			return config;
		}
	}

	DatabaseContent getSelectedContent() {
		for (int i = 0; i < contentRadios.length; i++) {
			if (contentRadios[i].getSelection()) {
				return DatabaseContent.values()[i];
			}
		}
		return DatabaseContent.ALL_REF_DATA;
	}

	private String getText(Text text) {
		return text.getText().trim();
	}

}
