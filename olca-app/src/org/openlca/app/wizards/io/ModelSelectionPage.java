package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.Preferences;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.NavigationContentProvider;
import org.openlca.app.navigation.NavigationLabelProvider;
import org.openlca.app.navigation.NavigationSorter;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.filters.ModelTypeFilter;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

class ModelSelectionPage extends WizardPage {

	private ModelType[] types;
	private File exportDestination;
	private List<BaseDescriptor> selectedComponents = new ArrayList<>();
	private CheckboxTreeViewer viewer;

	public ModelSelectionPage(ModelType... types) {
		super(ModelSelectionPage.class.getCanonicalName());
		this.types = types;
		setPageComplete(false);
		createTexts();
	}

	public File getExportDestination() {
		return exportDestination;
	}

	public List<BaseDescriptor> getSelectedModels() {
		return selectedComponents;
	}

	private void createTexts() {
		// TODO: change labels to 'Select data sets etc.'
		String typeName = getTypeName();
		String title = Messages.bind(Messages.Select, typeName);
		setTitle(title);
		String descr = Messages.SelectObjectPage_Description;
		descr = Messages.bind(descr, typeName);
		setDescription(descr);
	}

	// TODO: this method can be removed if the labels are a bit more generic
	private String getTypeName() {
		if (types == null || types.length != 1)
			return "@data sets";
		ModelType type = types[0];
		switch (type) {
		case PROCESS:
			return Messages.Processes;
		case IMPACT_METHOD:
			return Messages.ImpactAssessmentMethods;
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

	@Override
	public void createControl(final Composite parent) {
		Composite body = UIFactory.createContainer(parent);
		GridLayout bodyLayout = new GridLayout(1, true);
		bodyLayout.marginHeight = 10;
		bodyLayout.marginWidth = 10;
		bodyLayout.verticalSpacing = 10;
		body.setLayout(bodyLayout);
		createChooseDirectoryComposite(body);
		Composite viewerComposite = createViewerComposite(body);
		createViewer(viewerComposite);
		setControl(body);
		checkCompletion();
	}

	private void createChooseDirectoryComposite(final Composite body) {
		Composite composite = new Composite(body, SWT.NONE);
		GridLayout layout = UI.gridLayout(composite, 3);
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		UI.gridData(composite, true, false);
		new Label(composite, SWT.NONE).setText(Messages.ToDirectory);
		Text text = createDirectoryText(composite);
		text.setEditable(false);
		text.setBackground(Colors.getWhite());
		Button button = new Button(composite, SWT.NONE);
		button.setText(Messages.Browse);
		Controls.onSelect(button, (e) -> selectDirectory(text));
	}

	private Text createDirectoryText(Composite composite) {
		Text text = new Text(composite, SWT.BORDER);
		String lastDir = Preferences.get(Preferences.LAST_EXPORT_FOLDER);
		if (lastDir != null && new File(lastDir).exists()) {
			text.setText(lastDir);
			exportDestination = new File(lastDir);
		} else {
			lastDir = null;
		}
		UI.gridData(text, true, false);
		return text;
	}

	private void selectDirectory(Text text) {
		DirectoryDialog dialog = new DirectoryDialog(UI.shell());
		String dir = Preferences.get(Preferences.LAST_EXPORT_FOLDER);
		if (dir != null && new File(dir).exists())
			dialog.setFilterPath(dir);
		String path = dialog.open();
		if (path != null) {
			text.setText(path);
			Preferences.set(Preferences.LAST_EXPORT_FOLDER, path);
			exportDestination = new File(path);
			checkCompletion();
		}
	}

	private Composite createViewerComposite(final Composite body) {
		Composite composite = new Composite(body, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return composite;
	}

	private void createViewer(Composite composite) {
		viewer = new CheckboxTreeViewer(composite, SWT.VIRTUAL
				| SWT.MULTI | SWT.BORDER);
		viewer.setUseHashlookup(true);
		viewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new NavigationContentProvider());
		viewer.setLabelProvider(new NavigationLabelProvider());
		viewer.setSorter(new NavigationSorter());
		viewer.addFilter(new ModelTypeFilter(types));
		viewer.addCheckStateListener(new ModelSelectionState(this, viewer));
		registerInputHandler(composite);
		ColumnViewerToolTipSupport.enableFor(viewer);
	}

	// We want to avoid a resizing of the import dialog when the user flips to
	// this page. Thus, we set the input of the tree viewer after receiving the
	// first paint event.
	private void registerInputHandler(Composite composite) {
		composite.addPaintListener(new PaintListener() {
			private boolean init = false;

			@Override
			public void paintControl(PaintEvent e) {
				if (init) {
					composite.removePaintListener(this);
					return;
				}
				init = true;
				setInitialInput();
			}
		});
	}

	private void setInitialInput() {
		if (types != null && types.length == 1)
			viewer.setInput(Navigator.findElement(types[0]));
		else {
			IDatabaseConfiguration config = Database.getActiveConfiguration();
			viewer.setInput(Navigator.findElement(config));
		}
	}

}
