package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.openlca.app.M;
import org.openlca.app.Preferences;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;

/**
 * Wizard page for file import: the user can select files from directories.
 */
public class FileImportPage extends WizardPage {

	boolean withMultiSelection;
	boolean withMappingFile;
	File mappingFile;

	private TreeViewer directoryViewer;
	private String[] extensions;
	private List<File> selectedFiles;
	private TableViewer fileViewer;

	private File folder;
	private Text folderText;

	public FileImportPage(String... extensions) {
		super("FileImportPage");
		setTitle(M.SelectImportFiles);
		setDescription(M.FileImportPage_Description);
		this.extensions = extensions;
		setPageComplete(false);
		folder = getLastDir();
	}

	private File getLastDir() {
		String path = Preferences.get(
				Preferences.LAST_IMPORT_FOLDER);
		if (path == null)
			return null;
		File f = new File(path);
		if (f.exists() && f.isDirectory())
			return f;
		return null;
	}

	@Override
	public void createControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1, 10, 10);

		Composite folderComp = new Composite(body, SWT.NONE);
		UI.gridLayout(folderComp, 3, 5, 0);
		UI.gridData(folderComp, true, false);
		new Label(folderComp, SWT.NONE).setText(M.FromDirectory);
		folderText = new Text(folderComp, SWT.BORDER);
		if (folder != null)
			folderText.setText(folder.getAbsolutePath());
		UI.gridData(folderText, true, false);
		folderText.setEditable(false);
		folderText.setBackground(Colors.white());

		// create button to open directory dialog
		Button browseButton = new Button(
				folderComp, SWT.NONE);
		browseButton.setText(M.Browse);
		Controls.onSelect(browseButton, e -> selectFolder());

		Composite fileComp = new Composite(body, SWT.NONE);
		UI.gridLayout(fileComp, 2, 10, 0);
		UI.gridData(fileComp, true, true);

		// create tree viewer for selecting a sub directory
		directoryViewer = new TreeViewer(fileComp,
				SWT.BORDER | SWT.SINGLE);
		UI.gridData(directoryViewer.getTree(), true, true);
		directoryViewer.setContentProvider(new DirectoryContentProvider());
		directoryViewer.setLabelProvider(new FileLabel());
		directoryViewer.addSelectionChangedListener(e -> {
			File folder = Viewers.getFirst(e.getStructuredSelection());
			fileViewer.setInput(getFiles(folder, extensions));
		});

		// create table viewer to select a file from a selected sub directory
		fileViewer = new TableViewer(fileComp, SWT.BORDER
				| SWT.FULL_SELECTION
				| (withMultiSelection ? SWT.MULTI : SWT.SINGLE));
		UI.gridData(fileViewer.getTable(), true, true);
		fileViewer.setContentProvider(ArrayContentProvider.getInstance());
		fileViewer.setLabelProvider(new FileLabel());
		fileViewer.addSelectionChangedListener(e -> {
			selectedFiles = Viewers.getAll(e.getStructuredSelection());
			setPageComplete(!selectedFiles.isEmpty());
		});

		mappingFileRow(body);
		setInitialInput();
		setControl(body);
	}

	private void mappingFileRow(Composite body) {
		if (!withMappingFile)
			return;
		Composite comp = new Composite(body, SWT.NONE);
		UI.gridLayout(comp, 3, 5, 0);
		UI.gridData(comp, true, false);
		new Label(comp, SWT.NONE).setText("Mapping file");
		Text text = new Text(comp, SWT.BORDER);
		UI.gridData(text, true, false);
		text.setEditable(false);
		text.setBackground(Colors.white());
		Button button = new Button(comp, SWT.NONE);
		button.setText(M.Browse);
		Controls.onSelect(button, e -> {
			mappingFile = FileChooser.open("*.csv");
			if (mappingFile == null) {
				text.setText("");
			} else {
				text.setText(mappingFile.getAbsolutePath());
			}
		});
	}

	private void setInitialInput() {
		// we need to set the input when the components are already painted to
		// avoid SWT sizing problems
		fileViewer.getTable().addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (folder != null) {
					directoryViewer.setInput(folder);
					fileViewer.setInput(getFiles(folder, extensions));
				}
				fileViewer.getTable().removePaintListener(this);
			}
		});
	}

	/** Get the selected files from the page. */
	public File[] getFiles() {
		return selectedFiles == null
				? new File[0]
				: selectedFiles.toArray(
						new File[selectedFiles.size()]);
	}

	private void selectFolder() {
		DirectoryDialog dialog = new DirectoryDialog(UI.shell());
		if (folder != null)
			dialog.setFilterPath(folder.getAbsolutePath());
		String path = dialog.open();
		if (path != null) {
			folder = new File(path);
			folderText.setText(path);
			Preferences.set(Preferences.LAST_IMPORT_FOLDER, path);
			directoryViewer.setInput(folder);
			fileViewer.setInput(getFiles(folder, extensions));
		}
	}

	private static List<File> getFiles(File folder, String... extensions) {
		if (folder == null || !folder.isDirectory())
			return Collections.emptyList();
		Set<String> exts = new HashSet<>();
		if (extensions != null) {
			for (String ext : extensions) {
				if (ext == null)
					continue;
				exts.add(ext.toLowerCase());
			}
		}
		List<File> childs = new ArrayList<>();
		for (File file : listFiles(folder)) {
			if (!file.isFile())
				continue;
			if (exts.isEmpty()) {
				childs.add(file);
				continue;
			}
			String name = file.getName().toLowerCase();
			for (String ext : exts) {
				if (name.endsWith(ext)) {
					childs.add(file);
					break;
				}
			}
		}
		return childs;
	}

	/**
	 * Returns the content of the given folder. If the given file is not a directory
	 * or has no content an empty array is returned.
	 */
	private static File[] listFiles(File dir) {
		if (dir == null || !dir.isDirectory())
			return new File[0];
		// it seems that we had some issues on Windows
		// where isDirectory returned true but
		// listFiles returned null; so we try to check
		// these cases here
		File[] files = dir.listFiles();
		if (files == null)
			return new File[0];
		return files;
	}

	private class DirectoryContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof File))
				return null;
			File dir = (File) parent;
			ArrayList<File> childs = new ArrayList<>();
			for (File f : listFiles(dir)) {
				if (f.isDirectory()) {
					childs.add(f);
				}
			}
			return childs.toArray();
		}

		@Override
		public Object[] getElements(Object obj) {
			if (!(obj instanceof File))
				return null;
			File dir = (File) obj;
			ArrayList<File> dirs = new ArrayList<>();
			for (File f : listFiles(dir)) {
				if (f.isDirectory()) {
					dirs.add(f);
				}
			}
			return dirs.toArray();
		}

		@Override
		public Object getParent(Object obj) {
			if (!(obj instanceof File))
				return null;
			File file = (File) obj;
			return file.getParentFile();
		}

		@Override
		public boolean hasChildren(Object obj) {
			if (!(obj instanceof File))
				return false;
			File dir = (File) obj;
			for (File f : listFiles(dir)) {
				if (f.isDirectory())
					return true;
			}
			return false;
		}

		@Override
		public void inputChanged(
				Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private class FileLabel extends LabelProvider {

		@Override
		public Image getImage(Object obj) {
			if (!(obj instanceof File))
				return null;
			File file = (File) obj;
			if (file.isDirectory())
				return Images.platformImage(
						ISharedImages.IMG_OBJ_FOLDER);
			return Images.get(FileType.of(file));
		}

		@Override
		public String getText(Object obj) {
			if (!(obj instanceof File))
				return null;
			File file = (File) obj;
			return file.getAbsoluteFile().getName();
		}
	}
}
