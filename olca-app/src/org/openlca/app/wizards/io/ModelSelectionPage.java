package org.openlca.app.wizards.io;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
import org.openlca.app.navigation.NavigationLabelProvider;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryDirElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.filters.ModelTypeFilter;
import org.openlca.app.preferences.Preferences;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.TreeCheckStateContentProvider;
import org.openlca.app.viewers.trees.CheckboxTreeViewers;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

class ModelSelectionPage extends WizardPage {

	private final ModelType[] types;
	private ModelContentProvider selectionProvider;
	private File exportDestination;
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
	static ModelSelectionPage forFile(String extension, ModelType... types) {
		var page = new ModelSelectionPage(types);
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

	public List<RootDescriptor> getSelectedModels() {
		return selectionProvider.getSelection()
			.stream()
			.filter(elem -> elem instanceof ModelElement)
			.map(elem -> ((ModelElement) elem).getContent())
			.toList();
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
		setPageComplete(
			exportDestination != null
				&& selectionProvider != null
				&& !selectionProvider.getSelection().isEmpty());
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
		createViewer(body);
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

	private void createViewer(Composite body) {
		var comp = createViewerComposite(body);
		selectionProvider = new ModelContentProvider();
		selectionProvider.setSelection(getInitialSelection());
		var viewer = CheckboxTreeViewers.create(comp, selectionProvider);
		viewer.addFilter(new LibraryFilter());
		viewer.addFilter(new ModelTypeFilter(types));
		viewer.setLabelProvider(NavigationLabelProvider.withoutRepositoryState());
		viewer.setComparator(new NavigationComparator());
		CheckboxTreeViewers.registerInputHandler(comp, viewer, getInput(), () -> {
			CheckboxTreeViewers.expandGrayed(viewer);
			checkCompletion();
		});
	}

	private INavigationElement<?> getInput() {
		return types != null && types.length == 1
			? Navigator.findElement(types[0])
			: Navigator.findElement(Database.getActiveConfiguration());
	}

	private Set<INavigationElement<?>> getInitialSelection() {
		// try to take the selection from the navigator
		var navigator = Navigator.getInstance();
		if (navigator == null)
			return new HashSet<>();
		return new HashSet<>(Navigator.collect(navigator.getAllSelected(),
			elem -> !(elem instanceof LibraryDirElement),
			elem -> elem instanceof ModelElement m && fitsType(m) ? elem : null));
	}

	private boolean fitsType(ModelElement element) {
		if (types == null || types.length == 0)
			return true;
		for (var type : types)
			if (element.getContent().type == type)
				return true;
		return false;
	}

	private class ModelContentProvider extends
		TreeCheckStateContentProvider<INavigationElement<?>> {

		@Override
		protected List<INavigationElement<?>> childrenOf(INavigationElement<?> element) {
			return element.getChildren();
		}

		@Override
		protected INavigationElement<?> parentOf(INavigationElement<?> element) {
			return element.getParent();
		}

		@Override
		protected boolean isLeaf(INavigationElement<?> element) {
			return element instanceof ModelElement;
		}

		@Override
		protected void onCheckStateChanged() {
			ModelSelectionPage.this.checkCompletion();
		}

	}

	private static class LibraryFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof LibraryDirElement)
				return false;
			if (element instanceof CategoryElement e)
				return e.hasNonLibraryContent();
			if (element instanceof ModelElement e)
				return !e.isFromLibrary();
			if (element instanceof INavigationElement<?> e) {
				for (var c : e.getChildren()) {
					if (select(viewer, element, c)) {
						return true;
					}
				}
				return false;
			}
			return true;
		}

	}

}
