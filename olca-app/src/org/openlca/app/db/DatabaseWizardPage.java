package org.openlca.app.db;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.DerbyConfig;

public class DatabaseWizardPage extends WizardPage {

	private Text nameText;
	private Button[] contentRadios;

	public DatabaseWizardPage() {
		super("database-wizard-page", M.NewDatabase,
				Icon.DATABASE_WIZARD.descriptor());
		setDescription(M.CreateANewDatabase);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = UI.composite(parent);
		setControl(body);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		UI.gridLayout(body, 2);
		createNameText(body);
		createDatabaseContent(body);
	}

	private void createNameText(Composite comp) {
		var label = new Label(comp, SWT.NONE);
		label.setText(M.DatabaseName);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		nameText = new Text(comp, SWT.BORDER);
		UI.fillHorizontal(nameText);
		nameText.addModifyListener((e) -> validateInput());
	}

	private void createDatabaseContent(Composite comp) {
		var label = new Label(comp, SWT.NONE);
		label.setText(M.DatabaseContent);
		var gd = UI.gridData(label, false, false);
		gd.verticalAlignment = SWT.TOP;
		gd.verticalIndent = 2;
		createContentRadios(comp);
	}

	private void createContentRadios(Composite composite) {
		Composite radioGroup = UI.composite(composite);
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
		var err = Database.validateNewName(nameText.getText());
		if (err != null) {
			error(err);
		} else {
			setMessage(null);
			setPageComplete(true);
		}
	}

	private void error(String string) {
		setMessage(string, DialogPage.ERROR);
		setPageComplete(false);
	}

	DatabaseConfig getPageData() {
		var derbyConfig = new DerbyConfig();
		derbyConfig.name(getText(nameText));
		return derbyConfig;
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
