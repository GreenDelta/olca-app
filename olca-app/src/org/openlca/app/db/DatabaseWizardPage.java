package org.openlca.app.db;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
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
		setDescription(Messages.NewDatabase_Description);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite rootComposite = new Composite(parent, SWT.NONE);
		UI.gridLayout(rootComposite, 1);

		Composite headerComposite = new Composite(rootComposite, SWT.NONE);
		headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		setControl(headerComposite);
		UI.gridLayout(headerComposite, 2);
		nameText = UI.formText(headerComposite, Messages.NewDatabase_Name);
		nameText.addModifyListener(new TextListener());

		UI.formLabel(headerComposite, "Database type");
		Composite radioGroup = new Composite(headerComposite, SWT.NONE);
		radioGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		UI.gridLayout(radioGroup, 2, 10, 0);

		buttonLocal = new Button(radioGroup, SWT.RADIO);
		buttonLocal.setText("Local");
		buttonLocal.setSelection(true);
		buttonLocal.addSelectionListener(new RadioGroupListener());
		buttonRemote = new Button(radioGroup, SWT.RADIO);
		buttonRemote.setText("Remote");
		buttonRemote.addSelectionListener(new RadioGroupListener());

		createStackComposite(rootComposite);
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
		UI.formLabel(localComposite, Messages.NewDatabase_RefData);
		createContentRadios(localComposite);
	}

	private void createRemoteComposite(Composite stackComposite) {
		remoteComposite = new Composite(stackComposite, SWT.NONE);
		UI.gridLayout(remoteComposite, 2);
		hostText = UI.formText(remoteComposite, "Host");
		hostText.addModifyListener(new TextListener());
		portText = UI.formText(remoteComposite, "Port");
		portText.addModifyListener(new TextListener());
		userText = UI.formText(remoteComposite, "User");
		userText.addModifyListener(new TextListener());
		passwordText = UI.formText(remoteComposite, "Password");
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
			return Messages.UnitsAndFlowProps;
		case ALL_REF_DATA:
			return Messages.CompleteRefData;
		default:
			return null;
		}
	}

	private void validateInput() {
		boolean valid = validateName(nameText.getText());
		if (valid)
			if (buttonRemote.getSelection())
				if (hostText.getText().isEmpty()) {
					error("Please specify a host");
					valid = false;
				} else if (userText.getText().isEmpty()) {
					error("Please specify a user");
					valid = false;
				} else
					valid = validatePort(portText.getText());
		if (valid) {
			setMessage(null);
			setPageComplete(true);
		}
	}

	private boolean validatePort(String port) {
		if (port.isEmpty())
			error("Please specify a port number");
		else
			try {
				Integer.parseInt(port);
				return true;
			} catch (NumberFormatException e) {
				error("Please specify a valid port number");
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

	private class RadioGroupListener implements SelectionListener {

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (buttonLocal.getSelection()) {
				stackLayout.topControl = localComposite;
				stackComposite.layout();
			} else if (buttonRemote.getSelection()) {
				stackLayout.topControl = remoteComposite;
				stackComposite.layout();
			}
			validateInput();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	private class TextListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			validateInput();
		}
	}

}
