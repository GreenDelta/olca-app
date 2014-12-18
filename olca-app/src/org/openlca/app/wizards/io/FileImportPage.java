package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.Preferences;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;

/**
 * Wizard page for file import: the user can select files from directories.
 */
public class FileImportPage extends WizardPage {

	private TreeViewer directoryViewer;
	private HashSet<String> fileExtensions = new HashSet<>();
	private File[] selectedFiles;
	private TableViewer fileViewer;
	private boolean multiSelection;

	private File lastDir;
	private Text directoryText;

	public FileImportPage(String[] fileExtensions, boolean multi) {
		super("FileImportPage");
		setTitle(Messages.SelectImportFiles);
		setDescription(Messages.FileImportPage_Description);
		if (fileExtensions != null) {
			for (String extension : fileExtensions)
				this.fileExtensions.add(extension.toLowerCase());
		}
		setPageComplete(false);
		this.multiSelection = multi;
		lastDir = getLastDir();
	}

	private File getLastDir() {
		String lastDirPath = Preferences.get(Preferences.LAST_IMPORT_FOLDER);
		if (lastDirPath == null)
			return null;
		File f = new File(lastDirPath);
		if (f.exists() && f.isDirectory())
			return f;
		return null;
	}

	@Override
	public void createControl(final Composite parent) {
		// create body
		final Composite body = new Composite(parent, SWT.NONE);
		final GridLayout bodyLayout = new GridLayout(1, true);
		bodyLayout.marginHeight = 10;
		bodyLayout.marginWidth = 10;
		bodyLayout.verticalSpacing = 10;
		body.setLayout(bodyLayout);

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
				.setText(Messages.FromDirectory);

		createDirectoryText(chooseDirectoryComposite);

		// create button to open directory dialog
		final Button chooseDirectoryButton = new Button(
				chooseDirectoryComposite, SWT.NONE);
		chooseDirectoryButton.setText(Messages.ChooseDirectory);
		chooseDirectoryButton.addSelectionListener(new DirectorySelection());

		new Label(body, SWT.SEPARATOR | SWT.HORIZONTAL)
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// create composite
		final Composite chooseFileComposite = new Composite(body, SWT.NONE);
		final GridLayout fileLayout = new GridLayout(2, true);
		fileLayout.marginLeft = 0;
		fileLayout.marginRight = 0;
		fileLayout.marginBottom = 0;
		fileLayout.marginTop = 0;
		fileLayout.marginHeight = 0;
		fileLayout.marginWidth = 0;
		fileLayout.horizontalSpacing = 10;
		chooseFileComposite.setLayout(fileLayout);
		chooseFileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, true));

		// create tree viewer for selecting a sub directory
		directoryViewer = new TreeViewer(chooseFileComposite, SWT.BORDER
				| SWT.SINGLE);
		final GridData gddv = new GridData(SWT.FILL, SWT.FILL, true, true);
		directoryViewer.getTree().setLayoutData(gddv);
		directoryViewer.setContentProvider(new DirectoryContentProvider());
		directoryViewer.setLabelProvider(new FileLabelProvider());
		directoryViewer.addSelectionChangedListener((e) -> {
			if (!e.getSelection().isEmpty()) {
				IStructuredSelection selection = (IStructuredSelection) e
						.getSelection();
				fileViewer.setInput(selection.getFirstElement());
			}
		});

		// create table viewer to select a file from a selected sub directory
		fileViewer = new TableViewer(chooseFileComposite, SWT.BORDER
				| SWT.FULL_SELECTION
				| (multiSelection ? SWT.MULTI : SWT.SINGLE));
		final GridData gdfv = new GridData(SWT.FILL, SWT.FILL, true, true);
		fileViewer.getTable().setLayoutData(gdfv);
		fileViewer.setContentProvider(new FileContentProvider());
		fileViewer.setLabelProvider(new FileLabelProvider());

		fileViewer.addSelectionChangedListener((event) -> {
			ISelection selection = event.getSelection();
			if (!(selection instanceof IStructuredSelection)
					|| selection.isEmpty()) {
				setPageComplete(false);
				return;
			}
			Object[] files = ((IStructuredSelection) selection).toArray();
			selectedFiles = new File[files.length];
			for (int i = 0; i < files.length; i++) {
				selectedFiles[i] = (File) files[i];
			}
			setPageComplete(true);
		});

		setViewerInput();
		setControl(body);
	}

	private void setViewerInput() {
		// we need to set the input when the components are already painted to
		// avoid SWT sizing problems
		fileViewer.getTable().addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (lastDir != null) {
					fileViewer.setInput(lastDir);
					directoryViewer.setInput(lastDir);
				}
				fileViewer.getTable().removePaintListener(this);
			}
		});
	}

	private void createDirectoryText(Composite composite) {
		directoryText = new Text(composite, SWT.BORDER);
		if (lastDir != null)
			directoryText.setText(lastDir.getAbsolutePath());
		directoryText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		directoryText.setEditable(false);
		directoryText.setBackground(Colors.getColor(255, 255, 255));
	}

	/** Get the selected files from the page. */
	public File[] getFiles() {
		return selectedFiles;
	}

	private class DirectoryContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getChildren(final Object parentElement) {
			final ArrayList<File> elements = new ArrayList<>();
			if (parentElement instanceof File) {
				final File file = (File) parentElement;
				for (final File child : file.listFiles()) {
					if (child.isDirectory()) {
						elements.add(child);
					}
				}
			}
			return elements.toArray(new File[elements.size()]);
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			final ArrayList<File> elements = new ArrayList<>();
			if (inputElement instanceof File) {
				final File file = (File) inputElement;
				for (final File child : file.listFiles()) {
					if (child.isDirectory()) {
						elements.add(child);
					}
				}
			}
			return elements.toArray(new File[elements.size()]);
		}

		@Override
		public Object getParent(final Object element) {
			File parent = null;
			if (element instanceof File) {
				final File file = (File) element;
				if (file.getParentFile() != null
						&& file.getParentFile().isDirectory()) {
					parent = file;
				}
			}
			return parent;
		}

		@Override
		public boolean hasChildren(final Object element) {
			boolean hasChildren = false;
			if (element instanceof File) {
				final File file = (File) element;
				int i = 0;
				final File[] files = file.listFiles();
				if (files != null) {
					while (!hasChildren && i < files.length) {
						if (files[i].isDirectory()) {
							hasChildren = true;
						} else {
							i++;
						}
					}
				}
			}
			return hasChildren;
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
		}

	}

	private class FileContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			final ArrayList<File> elements = new ArrayList<>();
			if (inputElement instanceof File) {
				final File file = (File) inputElement;
				if (file.isDirectory()) {
					final File[] files = file.listFiles();
					if (files != null) {
						for (final File child : files) {
							if (child.isFile()) {
								String extension = child.getName();
								while (extension.contains(".")) {
									extension = extension.substring(extension
											.indexOf('.') + 1);
								}
								extension = extension.toLowerCase();
								if (fileExtensions.size() == 0
										|| fileExtensions.contains(extension)) {
									elements.add(child);
								}
							}
						}
					}
				}
			}
			return elements.toArray(new File[elements.size()]);
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
		}

	}

	private class DirectorySelection implements SelectionListener {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(final SelectionEvent e) {
			DirectoryDialog dialog = new DirectoryDialog(UI.shell());
			if (lastDir != null)
				dialog.setFilterPath(lastDir.getAbsolutePath());
			String directoryPath = dialog.open();
			if (directoryPath != null) {
				lastDir = new File(directoryPath);
				directoryText.setText(directoryPath);
				Preferences.set(Preferences.LAST_IMPORT_FOLDER, directoryPath);
				directoryViewer.setInput(lastDir);
				fileViewer.setInput(lastDir);
			}
		}
	}

}
