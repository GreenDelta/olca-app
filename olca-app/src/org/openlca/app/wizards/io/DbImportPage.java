package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseList;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.util.Strings;

class DbImportPage extends WizardPage {

	private final ImportConfig config;

	private Button browseButton;
	private ComboViewer existingViewer;
	private Text fileText;

	public DbImportPage() {
		super("DbImportPage");
		config = new ImportConfig();
		config.mode = config.EXISTING_MODE;
		setTitle(M.DatabaseImport);
		setDescription(M.DatabaseImportDescription);
		setPageComplete(false);
	}

	public ImportConfig getConfig() {
		return config;
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1);
		createExistingSection(body);
		createFileSection(body);
		setSelection(config.EXISTING_MODE);
		setControl(body);
	}

	private void createExistingSection(Composite body) {
		Button existingCheck = new Button(body, SWT.RADIO);
		existingCheck.setText("Existing database");
		existingCheck.setSelection(true);
		Controls.onSelect(existingCheck, (e) -> {
			setSelection(config.EXISTING_MODE);
		});
		Composite composite = new Composite(body, SWT.NONE);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, false);
		existingViewer = new ComboViewer(composite);
		UI.gridData(existingViewer.getControl(), true, false);
		existingViewer.setLabelProvider(new DbLabel());
		existingViewer.setContentProvider(ArrayContentProvider.getInstance());
		existingViewer.addSelectionChangedListener(e -> selectDatabase());
		fillExistingViewer();
	}

	private void fillExistingViewer() {
		DatabaseList dbList = Database.getConfigurations();
		List<IDatabaseConfiguration> configs = new ArrayList<>();
		for (IDatabaseConfiguration config : dbList.getLocalDatabases()) {
			if (config != null && !Database.isActive(config)) {
				configs.add(config);
			}
		}
		for (IDatabaseConfiguration config : dbList.getRemoteDatabases()) {
			if (config != null && !Database.isActive(config)) {
				configs.add(config);
			}
		}
		Collections.sort(configs,
				(c1, c2) -> Strings.compare(c1.getName(), c2.getName()));
		existingViewer.setInput(configs);
	}

	private void selectDatabase() {
		IDatabaseConfiguration db = Viewers.getFirstSelected(existingViewer);
		config.databaseConfiguration = db;
		setPageComplete(db != null);
	}

	private void createFileSection(Composite body) {
		Button fileCheck = new Button(body, SWT.RADIO);
		fileCheck.setText("From exported zolca-File");
		Controls.onSelect(fileCheck, (e) -> setSelection(config.FILE_MODE));
		Composite composite = UI.formComposite(body);
		UI.gridData(composite, true, false);
		fileText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
		fileText.setBackground(Colors.white());
		UI.gridData(fileText, true, false);
		browseButton = new Button(composite, SWT.NONE);
		browseButton.setText("Browse");
		Controls.onSelect(browseButton, (e) -> selectFile());
	}

	private void selectFile() {
		File zolcaFile = FileChooser.forImport("*.zolca");
		if (zolcaFile == null)
			return;
		fileText.setText(zolcaFile.getAbsolutePath());
		config.file = zolcaFile;
		setPageComplete(true);
	}

	private void setSelection(int mode) {
		config.mode = mode;
		if (mode == config.EXISTING_MODE) {
			existingViewer.getCombo().setEnabled(true);
			fileText.setEnabled(false);
			browseButton.setEnabled(false);
			setPageComplete(config.databaseConfiguration != null);
		} else {
			existingViewer.getCombo().setEnabled(false);
			fileText.setEnabled(true);
			browseButton.setEnabled(true);
			setPageComplete(config.file != null);
		}
	}

	static class ImportConfig {

		final int EXISTING_MODE = 0;
		final int FILE_MODE = 1;

		File file;
		IDatabaseConfiguration databaseConfiguration;
		int mode;
	}

	private class DbLabel extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (!(element instanceof IDatabaseConfiguration))
				return null;
			IDatabaseConfiguration config = (IDatabaseConfiguration) element;
			return config.getName();
		}
	}
}
