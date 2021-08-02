package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.NavigationComparator;
import org.openlca.app.navigation.NavigationContentProvider;
import org.openlca.app.navigation.NavigationLabelProvider;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.filters.ModelTypeFilter;
import org.openlca.app.preferences.Preferences;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;

class ModelSelectionPage extends WizardPage {

	private final ModelType[] types;
	private final List<Descriptor> selectedModels = new ArrayList<>();

	private File exportDestination;
	private CheckboxTreeViewer viewer;
	private boolean targetIsDir;
	private String fileExtension;

	static ModelSelectionPage forDirectory(ModelType... types) {
		ModelSelectionPage page = new ModelSelectionPage(types);
		page.targetIsDir = true;
		return page;
	}

	/**
	 * For the file extension please use only the extension, e.g. zip instead of
	 * *.zip
	 */
	static ModelSelectionPage forFile(String extension,
																		ModelType... types) {
		ModelSelectionPage page = new ModelSelectionPage(types);
		page.targetIsDir = false;
		page.fileExtension = extension;
		return page;
	}

	private ModelSelectionPage(ModelType... types) {
		super(ModelSelectionPage.class.getCanonicalName());
		this.types = types;
		setPageComplete(false);
		createTexts();
	}

	public File getExportDestination() {
		return exportDestination;
	}

	public List<Descriptor> getSelectedModels() {
		return selectedModels;
	}

	private void createTexts() {
		var typeName = types == null || types.length != 1
			? M.DataSets
			: Labels.plural(types[0]);
		setTitle(M.bind(M.Select, typeName));
		var descr = M.bind(M.SelectObjectPage_Description, typeName);
		setDescription(descr);
	}


	void checkCompletion() {
		setPageComplete(exportDestination != null && selectedModels.size() > 0);
	}

	@Override
	public void createControl(final Composite parent) {
		Composite body = new Composite(parent, SWT.NULL);
		GridLayout bodyLayout = new GridLayout(1, true);
		bodyLayout.marginHeight = 10;
		bodyLayout.marginWidth = 10;
		bodyLayout.verticalSpacing = 10;
		body.setLayout(bodyLayout);
		createChooseTargetComposite(body);
		Composite viewerComposite = createViewerComposite(body);
		createViewer(viewerComposite);
		setControl(body);
		checkCompletion();
	}

	private void createChooseTargetComposite(final Composite body) {
		Composite composite = new Composite(body, SWT.NONE);
		GridLayout layout = UI.gridLayout(composite, 3);
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		UI.gridData(composite, true, false);
		String label = targetIsDir ? M.ToDirectory : M.ToFile;
		new Label(composite, SWT.NONE).setText(label);
		Text text = createTargetText(composite);
		text.setEditable(false);
		text.setBackground(Colors.white());
		Button button = new Button(composite, SWT.NONE);
		button.setText(M.Browse);
		Controls.onSelect(button, (e) -> selectTarget(text));
	}

	private Text createTargetText(Composite composite) {
		Text text = new Text(composite, SWT.BORDER);
		UI.gridData(text, true, false);
		String lastDir = Preferences.get(Preferences.LAST_EXPORT_FOLDER);
		if (lastDir == null || !new File(lastDir).exists())
			return text;
		String path = lastDir;
		if (!targetIsDir)
			path += File.separator + defaultName();
		text.setText(path);
		exportDestination = new File(path);
		return text;
	}

	private String defaultName() {
		return Database.get().getName() + "." + fileExtension;
	}

	private void selectTarget(Text text) {
		exportDestination = targetIsDir
			? FileChooser.selectFolder()
			: FileChooser.forSavingFile(M.Export, defaultName());
		if (exportDestination == null)
			return;
		String path = exportDestination.getAbsolutePath();
		text.setText(path);
		if (!targetIsDir)
			path = exportDestination.getParentFile().getAbsolutePath();
		Preferences.set(Preferences.LAST_EXPORT_FOLDER, path);
		checkCompletion();
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
		viewer = new CheckboxTreeViewer(composite, SWT.VIRTUAL | SWT.MULTI
			| SWT.BORDER);
		viewer.setUseHashlookup(true);
		viewer.getTree().setLayoutData(
			new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new NavigationContentProvider());
		viewer.setLabelProvider(new NavigationLabelProvider(false));
		viewer.setComparator(new NavigationComparator());
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
		var input = types != null && types.length == 1
			? Navigator.findElement(types[0])
			: Navigator.findElement(Database.getActiveConfiguration());
		if (input == null)
			return;
		viewer.setInput(input);

		// try to take the selection from the navigator
		var navigator = Navigator.getInstance();
		if (navigator == null)
			return;
		var filters = viewer.getFilters();
		var selection = navigator.getAllSelected()
			.stream()
			.filter(elem -> {
				for (var filter : filters) {
					if (!filter.select(viewer, elem.getParent(), elem))
						return false;
				}
				return true;
			})
			.toArray(INavigationElement<?>[]::new);
		if (selection.length == 0)
			return;

		// check the selected elements
		viewer.setCheckedElements(selection);
		var checkState = new ModelSelectionState(this, viewer);
		for (var e : selection) {
			if (e instanceof ModelElement) {
				checkState.updateSelection((ModelElement) e, true);
			}
			checkState.updateChildren(e, true);
			checkState.updateParent(e);
		}

		// expand the selection
		var expanded = new HashSet<INavigationElement<?>>();
		for (var elem : selection) {
			expanded.add(elem);
			var parent = elem.getParent();
			while (parent != null) {
				expanded.add(parent);
				parent = parent.getParent();
			}
		}
		viewer.setExpandedElements(expanded.toArray());

		checkCompletion();
	}
}
