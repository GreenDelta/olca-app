package org.openlca.app.db;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;

class DatabaseWizardPage extends WizardPage {

	private final String folder;

	private Text nameText;
	private Text folderText;
	private Button[] contentRadios;

	DatabaseWizardPage(String folder) {
		super("database-wizard-page", M.NewDatabase,
				Icon.DATABASE_WIZARD.descriptor());
		this.folder = folder;
		setDescription(M.CreateANewDatabase);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		var body = UI.composite(parent);
		setControl(body);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		UI.gridLayout(body, 2);
		nameText = createText(body, M.DatabaseName);
		nameText.addModifyListener(_ -> validateInput());
		folderText = createText(body, M.Folder);
		if (Strings.isNotBlank(folder)) {
			folderText.setText(folder);
		}
		createDatabaseContent(body);
	}

	private Text createText(Composite comp, String label) {
		var l = new Label(comp, SWT.NONE);
		l.setText(label);
		var gd = UI.gridData(l, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		var text = new Text(comp, SWT.BORDER);
		UI.stretchX(text);
		return text;
	}

	private void createDatabaseContent(Composite comp) {
		var label = new Label(comp, SWT.NONE);
		label.setText(M.DatabaseContent);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		createContentRadios(comp);
	}

	private void createContentRadios(Composite comp) {
		var radioGroup = UI.composite(comp);
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
		return switch (content) {
			case EMPTY -> M.EmptyDatabase;
			case UNITS -> M.UnitsAndFlowProperties;
			case FLOWS -> M.CompleteReferenceData;
		};
	}

	private void validateInput() {
		if (nameText == null)
			return;
		// check the write access first: the name validation reports an invalid
		// name when the databases folder cannot be created
		var err = checkWriteAccess();
		if (err == null) {
			err = Database.validateNewName(nameText.getText());
		}
		if (err != null) {
			error(err);
		} else {
			setMessage(null);
			setPageComplete(true);
		}
	}

	/// Checks that we can create a database folder in the databases folder of
	/// the workspace. Returns an error message if this is not the case. Note
	/// that the operating system can still deny the access, e.g. because of
	/// privacy settings, so errors of the database creation are still handled.
	private String checkWriteAccess() {
		File dir = null;
		try {
			dir = Workspace.dbDir();
			return Files.isWritable(dir.toPath())
				? null
				: NLS.bind(M.NoWriteAccessToFolder, dir.getPath());
		} catch (Exception e) {
			// `dbDir` tries to create the folder if it does not exist
			var path = dir != null
				? dir.getPath()
				: new File(Workspace.root(), "databases").getPath();
			return NLS.bind(M.NoWriteAccessToFolder, path);
		}
	}

	private void error(String string) {
		setMessage(string, DialogPage.ERROR);
		setPageComplete(false);
	}

	DatabaseConfig getPageData() {
		var config = new DerbyConfig();
		config.name(getText(nameText));
		var path = folderText.getText();
		if (Strings.isNotBlank(path)) {
			// normalize the path
			var parts = DatabaseDirElement.split(path);
			config.setCategory(String.join("/", parts));
		}
		return config;
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
