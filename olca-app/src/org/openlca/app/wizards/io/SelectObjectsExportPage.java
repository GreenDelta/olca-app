/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.ApplicationProperties;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.NavigationContentProvider;
import org.openlca.app.navigation.NavigationLabelProvider;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * Wizard page for selecting objects for export
 */
public class SelectObjectsExportPage extends WizardPage {

	private Text errorText;
	private File exportDestination;
	private String fileName = "openLCA";
	private List<BaseDescriptor> selectedComponents = new ArrayList<>();
	private boolean selectFileName;
	private boolean withSelection;
	private ModelType type;
	private CheckboxTreeViewer viewer;
	private String fileExtension = ".csv";

	public static SelectObjectsExportPage withoutSelection(ModelType type) {
		SelectObjectsExportPage page = new SelectObjectsExportPage();
		page.withSelection = false;
		page.type = type;
		return page;
	}

	public static SelectObjectsExportPage withSelection(ModelType type) {
		SelectObjectsExportPage page = new SelectObjectsExportPage();
		page.withSelection = true;
		page.type = type;
		return page;
	}

	private SelectObjectsExportPage() {
		super(SelectObjectsExportPage.class.getCanonicalName());
		setPageComplete(false);
	}

	public void setSelectFileName(boolean selectFileName) {
		this.selectFileName = selectFileName;
	}

	public void setSubDirectory(String directory) {
		createTexts(directory);
	}

	private void createTexts(String subDirectory) {
		String typeName = getTypeName(type);
		String title = null;
		String descr = null;
		if (!withSelection) {
			title = Messages.SelectDirectoryPage_Title;
			descr = Messages.SelectDirectoryPage_Description;
		} else {
			title = Messages.bind(Messages.SelectObjectPage_Title, typeName);
			descr = Messages.SelectObjectPage_Description;
		}
		setTitle(title);
		descr = Messages.bind(descr, typeName);
		if (subDirectory != null)
			descr += ". "
					+ NLS.bind(Messages.DirectoryWillBeCreated, subDirectory);
		setDescription(descr);
	}

	private String getTypeName(ModelType type) {
		switch (type) {
		case PROCESS:
			return Messages.Processes;
		case IMPACT_METHOD:
			return Messages.ImpactMethods;
		case FLOW:
			return Messages.Flows;
		case FLOW_PROPERTY:
			return Messages.FlowProperties;
		case UNIT_GROUP:
			return Messages.UnitGroups;
		case ACTOR:
			return Messages.Actors;
		case SOURCE:
			return Messages.Sources;
		case PRODUCT_SYSTEM:
			return Messages.ProductSystems;
		default:
			return "unknown";
		}
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getFileName() {
		return fileName;
	}

	public File getExportDestinationFile() {
		if (!selectFileName) {
			throw new IllegalStateException("selectFileName needs to be true");
		}
		return new File(getExportDestination(), getFileName()
				+ getFileExtension());
	}

	void checkCompletion() {
		if (!withSelection) {
			setPageComplete(exportDestination != null);
		} else {
			setPageComplete(exportDestination != null
					&& selectedComponents.size() > 0);
		}
		if (isPageComplete()) {
			setPageComplete(!selectFileName || fileName != null);
		}
	}

	private void checkFileName() {
		if (selectFileName) {
			if (exportDestination != null && fileName != null) {
				final File file = new File(exportDestination.getAbsolutePath()
						+ "/" + fileName + getFileExtension());
				if (file.exists())
					errorText.setText(Messages.AlreadyExistsWarning);
				else
					errorText.setText("");
			} else {
				errorText.setText("");
			}
		}
	}

	private void createChooseDirectoryComposite(final Composite body) {
		// create composite
		final Composite chooseDirectoryComposite = new Composite(body, SWT.NONE);
		final GridLayout dirLayout = new GridLayout(3, false);
		dirLayout.marginLeft = 0;
		dirLayout.marginRight = 0;
		dirLayout.marginBottom = 0;
		dirLayout.marginTop = 0;
		dirLayout.marginHeight = 0;
		dirLayout.marginWidth = 0;
		chooseDirectoryComposite.setLayout(dirLayout);
		chooseDirectoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, false));

		new Label(chooseDirectoryComposite, SWT.NONE)
				.setText(Messages.ChooseDirectoryLabel);

		// create text for selecting a category
		final Text directoryText = new Text(chooseDirectoryComposite,
				SWT.BORDER);
		String lastDirectory = ApplicationProperties.PROP_EXPORT_DIRECTORY
				.getValue();
		if (lastDirectory != null && new File(lastDirectory).exists()) {
			directoryText.setText(lastDirectory);
			exportDestination = new File(lastDirectory);
		} else {
			lastDirectory = null;
		}
		directoryText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		directoryText.setEditable(false);
		directoryText.setBackground(Colors.getWhite());

		// create button to open directory dialog
		final Button chooseDirectoryButton = new Button(
				chooseDirectoryComposite, SWT.NONE);
		chooseDirectoryButton.setText(Messages.ChooseDirectoryButton);
		chooseDirectoryButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no default action
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final DirectoryDialog dialog = new DirectoryDialog(UI.shell());
				final String dir = ApplicationProperties.PROP_EXPORT_DIRECTORY
						.getValue();
				dialog.setFilterPath(dir != null && new File(dir).exists() ? dir
						: "");
				final String directoryPath = dialog.open();
				if (directoryPath != null) {
					directoryText.setText(directoryPath);
					ApplicationProperties.PROP_EXPORT_DIRECTORY
							.setValue(directoryPath);
					exportDestination = new File(directoryPath);
					checkCompletion();
				}
				checkFileName();
			}

		});
	}

	private void createChooseFileNameComposite(final Composite parent) {
		final Composite chooseFileNameComposite = new Composite(parent,
				SWT.NONE);
		final GridLayout fileLayout = new GridLayout(3, false);
		fileLayout.marginLeft = 0;
		fileLayout.marginRight = 0;
		fileLayout.marginBottom = 0;
		fileLayout.marginTop = 0;
		fileLayout.marginHeight = 0;
		fileLayout.marginWidth = 0;
		chooseFileNameComposite.setLayout(fileLayout);
		chooseFileNameComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, false));

		new Label(chooseFileNameComposite, SWT.NONE)
				.setText(Messages.ChooseFileNameLabel);

		final Text fileNameText = new Text(chooseFileNameComposite, SWT.BORDER);
		fileNameText.setText("openLCA");
		fileNameText
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		new Label(chooseFileNameComposite, SWT.NONE)
				.setText(getFileExtension());

		new Label(chooseFileNameComposite, SWT.NONE);
		errorText = new Text(chooseFileNameComposite, SWT.NONE);
		errorText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		errorText.setBackground(chooseFileNameComposite.getBackground());
		new Label(chooseFileNameComposite, SWT.NONE);

		fileNameText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				if (fileNameText.getText().length() > 0) {
					fileName = fileNameText.getText();
				} else {
					fileName = null;
				}
				checkFileName();
				checkCompletion();
			}
		});
	}

	@Override
	public void createControl(final Composite parent) {
		final Composite body = UIFactory.createContainer(parent);
		final GridLayout bodyLayout = new GridLayout(1, true);
		bodyLayout.marginHeight = 10;
		bodyLayout.marginWidth = 10;
		bodyLayout.verticalSpacing = 10;
		body.setLayout(bodyLayout);

		createChooseDirectoryComposite(body);

		if (selectFileName) {
			createChooseFileNameComposite(body);
		}

		if (withSelection) {
			final Composite processComposite = new Composite(body, SWT.NONE);
			final GridLayout processLayout = new GridLayout(2, false);
			processLayout.marginLeft = 0;
			processLayout.marginRight = 0;
			processLayout.marginBottom = 0;
			processLayout.marginTop = 0;
			processLayout.marginHeight = 0;
			processLayout.marginWidth = 0;
			processComposite.setLayout(processLayout);
			processComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
					true, true));

			createViewer(processComposite);
		}

		setControl(body);
		checkCompletion();
	}

	private void createViewer(Composite processComposite) {
		viewer = new CheckboxTreeViewer(processComposite, SWT.VIRTUAL
				| SWT.MULTI | SWT.BORDER);
		viewer.setUseHashlookup(true);
		viewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new NavigationContentProvider());
		viewer.setLabelProvider(new NavigationLabelProvider());
		viewer.addCheckStateListener(new SelectObjectCheckState(this, viewer));
		if (type != null)
			viewer.setInput(Navigator.findElement(type));
		else
			viewer.setInput(Navigator.findElement(Database
					.getActiveConfiguration()));

		ColumnViewerToolTipSupport.enableFor(viewer);
	}

	public File getExportDestination() {
		return new File(exportDestination.getAbsolutePath());
	}

	public List<BaseDescriptor> getSelectedModelComponents() {
		return selectedComponents;
	}

}
