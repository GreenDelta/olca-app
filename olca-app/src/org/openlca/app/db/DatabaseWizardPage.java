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
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.database.DbUtils;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;
import org.openlca.core.database.config.MySqlConfig;

public class DatabaseWizardPage extends WizardPage {

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
		super("database-wizard-page", M.NewDatabase,
				Icon.DATABASE_WIZARD.descriptor());
		setDescription(M.CreateANewDatabase);
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
		nameText = UI.formText(header, M.DatabaseName);
		nameText.addModifyListener((e) -> validateInput());
		createTypeRadios(header);
		createStackComposite(root);
	}

	private void createTypeRadios(Composite headerComposite) {
		UI.formLabel(headerComposite, M.DatabaseType);
		Composite radioGroup = new Composite(headerComposite, SWT.NONE);
		radioGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		UI.gridLayout(radioGroup, 2, 10, 0);
		buttonLocal = new Button(radioGroup, SWT.RADIO);
		buttonLocal.setText(M.Local);
		buttonLocal.setSelection(true);
		Controls.onSelect(buttonLocal, (e) -> typeChanged());
		buttonRemote = new Button(radioGroup, SWT.RADIO);
		buttonRemote.setText(M.Remote);
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
		UI.formLabel(localComposite, M.DatabaseContent);
		createContentRadios(localComposite);
	}

	private void createRemoteComposite(Composite stackComposite) {
		remoteComposite = new Composite(stackComposite, SWT.NONE);
		UI.gridLayout(remoteComposite, 2);
		hostText = UI.formText(remoteComposite, M.Host);
		hostText.addModifyListener((e) -> validateInput());
		portText = UI.formText(remoteComposite, M.Port);
		portText.addModifyListener((e) -> validateInput());
		userText = UI.formText(remoteComposite, M.User);
		userText.addModifyListener((e) -> validateInput());
		passwordText = UI.formText(remoteComposite, M.Password);
	}

	private void createContentRadios(Composite composite) {
		Composite radioGroup = new Composite(composite, SWT.NONE);
		radioGroup.setLayout(new RowLayout(SWT.VERTICAL));
		contentRadios = new Button[DbTemplate.values().length];
		for (int i = 0; i < DbTemplate.values().length; i++) {
			contentRadios[i] = new Button(radioGroup, SWT.RADIO);
			contentRadios[i].setText(contentLabel(DbTemplate.values()[i]));
		}
		contentRadios[2].setSelection(true);
	}

	private String contentLabel(DbTemplate content) {
		if (content == null)
			return null;
		switch (content) {
			case EMPTY:
				return M.EmptyDatabase;
			case UNITS:
				return M.UnitsAndFlowProperties;
			case FLOWS:
				return M.CompleteReferenceData;
			default:
				return null;
		}
	}

	private void validateInput() {
		boolean valid = _validateName(nameText.getText());
		if (!valid)
			return;
		if (buttonLocal.getSelection()) {
			setMessage(null);
			setPageComplete(true);
			return;
		}
		if (hostText.getText().isEmpty()) {
			error(M.PleaseSpecifyHost);
			return;
		}
		if (userText.getText().isEmpty()) {
			error(M.PleaseSpecifyUser);
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
			error(M.PleaseSpecifyPortNumber);
		else
			try {
				Integer.parseInt(port);
				return true;
			} catch (NumberFormatException e) {
				error(M.PleaseSpecifyPortNumber);
			}
		return false;
	}

	private boolean _validateName(String name) {
		var error = validateName(name);
		if (error == null)
			return true;
		error(error);
		return false;
	}

	public static String validateName(String name) {
		if (name == null || name.length() < 4)
			return M.NewDatabase_NameToShort;
		if (!DbUtils.isValidName(name))
			return M.NewDatabase_InvalidName;
		if (Database.getConfigurations().nameExists(name))
			return M.NewDatabase_AlreadyExists;
		return null;
	}

	private void error(String string) {
		setMessage(string, DialogPage.ERROR);
		setPageComplete(false);
	}

	DatabaseConfig getPageData() {
		if (buttonLocal.getSelection()) {
			DerbyConfig derbyConfig = new DerbyConfig();
			derbyConfig.name(getText(nameText));
			return derbyConfig;
		} else {
			MySqlConfig config = new MySqlConfig();
			config.name(getText(nameText));
			config.host(getText(hostText));
			config.port(Integer.parseInt(getText(portText)));
			config.password(getText(passwordText));
			config.user(getText(userText));
			return config;
		}
	}

	DbTemplate getSelectedContent() {
		for (int i = 0; i < contentRadios.length; i++) {
			if (contentRadios[i].getSelection()) {
				return DbTemplate.values()[i];
			}
		}
		return DbTemplate.FLOWS;
	}

	private String getText(Text text) {
		return text.getText().trim();
	}

}
