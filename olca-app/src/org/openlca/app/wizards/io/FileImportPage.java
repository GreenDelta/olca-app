package org.openlca.app.wizards.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.preferences.Preferences;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.core.database.MappingFileDao;
import org.openlca.io.maps.FlowMap;
import org.openlca.util.Strings;

/**
 * Wizard page for file import: the user can select files from directories.
 */
public class FileImportPage extends WizardPage {

	boolean withMultiSelection;
	boolean withMappingFile;
	FlowMap flowMap;

	private TreeViewer directoryViewer;
	private final String[] extensions;
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

	public FileImportPage(File selected) {
		this(FilenameUtils.getExtension(selected.getName()));
		selectedFiles = List.of(selected);
		folder = selected.getParentFile();
		setPageComplete(true);
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
			File folder = Selections.firstOf(e);
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
			selectedFiles = Selections.allOf(e);
			setPageComplete(!selectedFiles.isEmpty());
		});

		mappingRow(body);
		setInitialInput();
		setControl(body);
	}

	private void mappingRow(Composite body) {
		if (!withMappingFile)
			return;
		var db = Database.get();
		if (db == null)
			return;

		var comp = new Composite(body, SWT.NONE);
		UI.gridLayout(comp, 3, 5, 0);
		UI.gridData(comp, true, false);
		new Label(comp, SWT.NONE).setText("Flow mapping");

		// initialize the combo box
		var combo = new Combo(comp, SWT.READ_ONLY);
		UI.gridData(combo, true, false);
		var dbFiles = new MappingFileDao(db)
				.getNames()
				.stream()
				.sorted()
				.collect(Collectors.toList());
		var items = new String[dbFiles.size() + 1];
		items[0] = "";
		for (int i = 0; i < dbFiles.size(); i++) {
			items[i + 1] = dbFiles.get(i);
		}
		combo.setItems(items);
		combo.select(0);

		// handle a combo selection event
		IntConsumer onSelect = idx -> {
			try {
				// no mapping
				var mapping = combo.getItem(idx);
				if (Strings.nullOrEmpty(mapping)) {
					flowMap = null;
					return;
				}

				// db mapping
				var dbMap = new MappingFileDao(db)
						.getForName(mapping);
				if (dbMap != null) {
					flowMap = FlowMap.of(dbMap);
					return;
				}

				// file mapping
				var file = new File(mapping);
				if (file.exists()) {
					flowMap = FlowMap.fromCsv(file);
				}
			} catch (Exception e) {
				ErrorReporter.on("Failed to open mapping", e);
			}
		};
		Controls.onSelect(combo, _e -> {
			var i = combo.getSelectionIndex();
			onSelect.accept(i);
		});

		// add the file button
		var fileBtn = new Button(comp, SWT.NONE);
		fileBtn.setText("From file");
		Controls.onSelect(fileBtn, e -> {
			var file = FileChooser.open("*.csv");
			if (file == null) 
				return;
			var oldItems = combo.getItems();
			var nextItems = Arrays.copyOf(oldItems, oldItems.length + 1);
			var idx = nextItems.length - 1;
			nextItems[idx] = file.getAbsolutePath();
			combo.setItems(nextItems);
			combo.select(idx); // does not fire an event
			onSelect.accept(idx);
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
					if (selectedFiles != null) {
						fileViewer.setSelection(
								new StructuredSelection(selectedFiles));
					}
				}
				fileViewer.getTable().removePaintListener(this);
			}
		});
	}

	/** Get the selected files from the page. */
	public File[] getFiles() {
		return selectedFiles == null
				? new File[0]
				: selectedFiles.toArray(new File[0]);
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

	private static class DirectoryContentProvider implements ITreeContentProvider {

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

	private static class FileLabel extends LabelProvider {

		@Override
		public Image getImage(Object obj) {
			if (!(obj instanceof File))
				return null;
			var file = (File) obj;
			return file.isDirectory()
					? Images.platformImage(ISharedImages.IMG_OBJ_FOLDER)
					: Images.get(FileType.of(file));
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
