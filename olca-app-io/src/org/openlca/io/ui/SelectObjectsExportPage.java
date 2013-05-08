/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.navigation.NavigationContentProvider;
import org.openlca.core.application.navigation.NavigationLabelProvider;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.ui.Colors;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;

/**
 * Wizard page for selecting objects for export
 */
public class SelectObjectsExportPage extends WizardPage {

	public final static int METHOD = 1;
	public final static int FLOW = 2;
	public final static int FLOW_PROPERTY = 3;
	public final static int UNIT_GROUP = 4;
	public final static int PROCESS = 0;
	public final static int ACTOR = 5;
	public final static int SOURCE = 6;
	public final static int PRODUCT_SYSTEM = 7;

	private Text errorText;
	private File exportDestination;
	private String fileName = "openLCA";
	private List<ObjectWrapper> selectedComponents = new ArrayList<>();
	private boolean selectFileName;
	private boolean singleExport;
	private int type;
	private CheckboxTreeViewer viewer;
	private String fileExtension = ".csv";

	public SelectObjectsExportPage(boolean singleExport, int type,
			boolean selectFileName, String subDirectory) {
		super("Ecospold01ExportPage");
		setPageComplete(false);
		this.selectFileName = selectFileName;
		this.singleExport = singleExport;
		this.type = type;
		createTexts(subDirectory);
	}

	private void createTexts(String subDirectory) {
		String typeName = getTypeName(type);
		String title = null;
		String descr = null;
		if (singleExport) {
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

	private String getTypeName(int type) {
		switch (type) {
		case PROCESS:
			return Phrases.Processes;
		case METHOD:
			return Phrases.LCIAMethods;
		case FLOW:
			return Phrases.Flows;
		case FLOW_PROPERTY:
			return Phrases.FlowProperties;
		case UNIT_GROUP:
			return Phrases.UnitGroups;
		case ACTOR:
			return Phrases.Actors;
		case SOURCE:
			return Phrases.Sources;
		case PRODUCT_SYSTEM:
			return Phrases.ProductSystems;
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
		if (singleExport) {
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

		if (!singleExport) {
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
		NavigationRoot root = Navigator.getNavigationRoot();
		viewer = new CheckboxTreeViewer(processComposite, SWT.VIRTUAL
				| SWT.MULTI | SWT.BORDER);
		viewer.setUseHashlookup(true);
		viewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new NavigationContentProvider());
		viewer.setLabelProvider(new NavigationLabelProvider());
		viewer.addCheckStateListener(new SelectObjectCheckState(this, viewer));
		viewer.setFilters(new ViewerFilter[] { new CategoryViewerFilter(
				getTypeClass()) });
		if (root != null) {
			viewer.setInput(root);
		}
		ColumnViewerToolTipSupport.enableFor(viewer);
	}

	private Class<?> getTypeClass() {
		switch (type) {
		case PROCESS:
			return Process.class;
		case METHOD:
			return LCIAMethod.class;
		case FLOW:
			return Flow.class;
		case FLOW_PROPERTY:
			return FlowProperty.class;
		case UNIT_GROUP:
			return UnitGroup.class;
		case ACTOR:
			return Actor.class;
		case SOURCE:
			return Source.class;
		case PRODUCT_SYSTEM:
			return ProductSystem.class;
		default:
			return null;
		}
	}

	/**
	 * Getter of the export destination
	 * 
	 * @return The directory or file to export the selected components
	 */
	public File getExportDestination() {
		return new File(exportDestination.getAbsolutePath());
	}

	/**
	 * Returns a live list of the selected model components.
	 */
	public List<ObjectWrapper> getSelectedModelComponents() {
		return selectedComponents;
	}

}
