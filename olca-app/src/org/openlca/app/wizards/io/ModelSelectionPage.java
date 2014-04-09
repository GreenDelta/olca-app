package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
import org.openlca.app.navigation.NavigationSorter;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

class ModelSelectionPage extends WizardPage {

	private final ModelType type;

	private File exportDestination;
	private List<BaseDescriptor> selectedComponents = new ArrayList<>();
	private CheckboxTreeViewer viewer;

	public ModelSelectionPage(ModelType type) {
		super(ModelSelectionPage.class.getCanonicalName());
		this.type = type;
		setPageComplete(false);
		createTexts();
	}

	private void createTexts() {
		String typeName = getTypeName(type);
		String title = Messages.bind(Messages.SelectObjectPage_Title, typeName);
		setTitle(title);
		String descr = Messages.SelectObjectPage_Description;
		descr = Messages.bind(descr, typeName);
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

	void checkCompletion() {
		setPageComplete(exportDestination != null
				&& selectedComponents.size() > 0);
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
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(UI.shell());
				String dir = ApplicationProperties.PROP_EXPORT_DIRECTORY
						.getValue();
				dialog.setFilterPath(dir != null && new File(dir).exists() ? dir
						: "");
				String directoryPath = dialog.open();
				if (directoryPath != null) {
					directoryText.setText(directoryPath);
					ApplicationProperties.PROP_EXPORT_DIRECTORY
							.setValue(directoryPath);
					exportDestination = new File(directoryPath);
					checkCompletion();
				}
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

		final Composite processComposite = new Composite(body, SWT.NONE);
		final GridLayout processLayout = new GridLayout(2, false);
		processLayout.marginLeft = 0;
		processLayout.marginRight = 0;
		processLayout.marginBottom = 0;
		processLayout.marginTop = 0;
		processLayout.marginHeight = 0;
		processLayout.marginWidth = 0;
		processComposite.setLayout(processLayout);
		processComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));

		createViewer(processComposite);

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
		viewer.setSorter(new NavigationSorter());
		viewer.addCheckStateListener(new ModelSelectionState(this, viewer));
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

	public List<BaseDescriptor> getSelectedModels() {
		return selectedComponents;
	}

}
