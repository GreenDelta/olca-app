package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
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
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.util.Strings;

class DbImportPage extends WizardPage {

	private final ImportConfig config;

	private Button browseButton;
	private ComboViewer existingViewer;
	private Text fileText;

	DbImportPage() {
		super("DbImportPage");
		config = new ImportConfig();
		config.mode = config.EXISTING_MODE;
		setTitle(M.DatabaseImport);
		setDescription(M.DatabaseImportDescription);
		setPageComplete(false);
	}

	DbImportPage(File file) {
		this();
		if (file != null) {
			config.mode = config.FILE_MODE;
			config.file = file;
			setPageComplete(true);
		}
	}

	ImportConfig getConfig() {
		return config;
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = UI.composite(parent);
		UI.gridLayout(body, 1);
		createExistingSection(body);
		createFileSection(body);
		setSelection(config.mode);
		setControl(body);
	}

	private void createExistingSection(Composite body) {
		var check = new Button(body, SWT.RADIO);
		check.setText("Existing database");
		check.setSelection(config.mode == config.EXISTING_MODE);
		Controls.onSelect(check, e -> setSelection(config.EXISTING_MODE));
		var comp = UI.composite(body);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, false);

		existingViewer = new ComboViewer(comp);
		UI.gridData(existingViewer.getControl(), true, false);
		existingViewer.setLabelProvider(new DbLabel());
		existingViewer.setContentProvider(ArrayContentProvider.getInstance());
		existingViewer.addSelectionChangedListener(e -> {
			DatabaseConfig db = Viewers.getFirstSelected(existingViewer);
			config.databaseConfiguration = db;
			setPageComplete(db != null);
		});
		existingViewer.setInput(existing());
	}

	/**
	 * Returns the existing databases of the openLCA workspace that could be
	 * imported into the currently active database.
	 */
	private List<DatabaseConfig> existing() {
		var configs = new ArrayList<DatabaseConfig>();
		Database.getConfigurations()
				.getAll()
				.stream()
				.filter(c -> c != null && !Database.isActive(c))
				.forEach(configs::add);
		configs.sort((c1, c2) -> Strings.compare(c1.name(), c2.name()));
		return configs;
	}

	private void createFileSection(Composite body) {
		var check = new Button(body, SWT.RADIO);
		check.setText("From exported zolca-File");
		check.setSelection(config.mode == config.FILE_MODE);
		Controls.onSelect(check, e -> setSelection(config.FILE_MODE));
		var comp = UI.composite(body);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, false);
		fileText = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
		fileText.setBackground(Colors.white());
		UI.gridData(fileText, true, false);
		if (config.file != null) {
			fileText.setText(config.file.getAbsolutePath());
		}

		browseButton = new Button(comp, SWT.NONE);
		browseButton.setText("Browse");
		Controls.onSelect(browseButton, e -> {
			var zolca = FileChooser.open("*.zolca");
			if (zolca == null)
				return;
			fileText.setText(zolca.getAbsolutePath());
			config.file = zolca;
			setPageComplete(true);
		});
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
		DatabaseConfig databaseConfiguration;
		int mode;
	}

	private static class DbLabel extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (!(element instanceof DatabaseConfig config))
				return null;
			return config.name();
		}
	}
}
