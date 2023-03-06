package org.openlca.app.editors.sources;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.FileType;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.FileStore;
import org.openlca.core.model.Source;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

class SourceInfoPage extends ModelPage<Source> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit tk;
	private ImageHyperlink fileLink;
	private ImageHyperlink deleteLink;
	private ImageView image;

	SourceInfoPage(SourceEditor editor) {
		super(editor, "SourceInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		new InfoSection(getEditor()).render(body, tk);
		additionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	protected void additionalInfo(Composite body) {
		Composite comp = UI.formSection(body, tk, M.AdditionalInformation, 3);
		UI.gridLayout(comp, 4);
		text(comp, M.URL, "url");
		Button urlButton = tk.createButton(comp, M.Open, SWT.NONE);
		urlButton.setImage(Icon.MAP.get());
		Controls.onSelect(urlButton, e -> {
			String url = getModel().url;
			if (Strings.nullOrEmpty(url))
				return;
			Desktop.browse(url);
		});
		text(comp, M.TextReference, "textReference");
		UI.formFiller(comp, tk);
		shortText(comp, M.Year, "year");
		UI.formFiller(comp, tk);
		fileSection(comp);

		UI.formFiller(comp, tk);
		UI.formFiller(comp, tk);
		image = new ImageView(comp, this::getDatabaseFile);
	}

	private void fileSection(Composite parent) {
		UI.formLabel(parent, M.File);
		Composite comp = tk.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 10;
		layout.numColumns = 3;
		comp.setLayout(layout);

		var browseButton = UI.formButton(comp, tk, M.Browse);
		var editable = getEditor().isEditable();
		browseButton.setEnabled(editable);
		Controls.onSelect(browseButton, e -> selectFile());
		fileLink(comp);
		if (editable) {
			deleteLink(comp);
			comp.addMouseTrackListener(new DeleteFileVisibility());
		}
		new CommentControl(parent, tk, "externalFile", getComments());
	}

	private void deleteLink(Composite comp) {
		deleteLink = tk.createImageHyperlink(comp, SWT.TOP);
		deleteLink.setImage(Icon.DELETE_DISABLED.get());
		deleteLink.addMouseTrackListener(new DeleteFileVisibility());
		deleteLink.setVisible(false);
		deleteLink.setToolTipText(M.Delete);
		Controls.onClick(deleteLink, this::deleteFile);
	}

	private void fileLink(Composite comp) {
		fileLink = UI.formImageHyperlink(comp, tk, SWT.TOP);
		fileLink.setForeground(Colors.linkBlue());
		Controls.onClick(fileLink, this::openFile);
		fileLink.addMouseTrackListener(new DeleteFileVisibility());
		String file = getModel().externalFile;
		if (file != null) {
			fileLink.setText(file);
			fileLink.setToolTipText(M.Open);
			fileLink.setImage(Images.get(FileType.forName(file)));
		}
	}

	private void selectFile() {
		File file = FileChooser.open("*.*");
		if (file == null)
			return;
		String fileName = file.getName();
		File dir = DatabaseDir.getDir(getModel());
		File dbFile = new File(dir, fileName);
		if (dbFile.exists()) {
			boolean doIt = Question
					.ask(M.OverwriteFile,
							M.SourceFileOverwriteFileQuestion);
			if (!doIt)
				return;
		}
		copyAndSetFile(file, dbFile);
	}

	private void copyAndSetFile(File file, File dbFile) {
		try {
			log.trace("copy file {} to database folder", file);
			File dir = dbFile.getParentFile();
			if (!dir.exists())
				dir.mkdirs();
			Files.copy(file, dbFile);
			Source source = getModel();
			source.externalFile = dbFile.getName();
			getEditor().setDirty(true);
			updateFileLink();
		} catch (Exception e) {
			log.error("failed to copy file", e);
		}
	}

	private void updateFileLink() {
		String file = getModel().externalFile;
		if (file == null) {
			fileLink.setText("");
			fileLink.setToolTipText("");
			fileLink.setImage(null);
		} else {
			fileLink.setToolTipText(M.Open);
			fileLink.setText(file);
			fileLink.setImage(Images.get(FileType.forName(file)));
		}
		fileLink.getParent().getParent().layout();
		image.update();
	}

	private File getDatabaseFile() {
		String fileName = getModel().externalFile;
		if (fileName == null)
			return null;
		File dir = new FileStore(Database.get()).getFolder(getModel());
		return new File(dir, fileName);
	}

	private void openFile(HyperlinkEvent evt) {
		File file = getDatabaseFile();
		if (file == null)
			return;
		if (file.exists()) {
			log.trace("open file {}", file);
			Desktop.browse(file.toURI().toString());
		} else {
			MsgBox.info(M.FileDoesNotExist);
		}
	}

	private void deleteFile(HyperlinkEvent e) {
		if (getModel().externalFile == null)
			return;
		File file = getDatabaseFile();
		if (file == null)
			return;
		boolean doIt = Question.ask(M.DeleteFile,
				M.SourceFileDeleteQuestion);
		if (!doIt)
			return;
		try {
			if (file.exists())
				file.delete();
			getModel().externalFile = null;
			updateFileLink();
			getEditor().setDirty(true);
		} catch (Exception ex) {
			log.error("failed to delete file", ex);
		}
	}

	private class DeleteFileVisibility extends MouseTrackAdapter {
		@Override
		public void mouseEnter(MouseEvent e) {
			if (e.widget == deleteLink)
				deleteLink.setImage(Icon.DELETE.get());
			deleteLink.setVisible(getModel().externalFile != null);
		}

		@Override
		public void mouseExit(MouseEvent e) {
			if (e.widget == deleteLink)
				deleteLink.setImage(Icon.DELETE_DISABLED.get());
			deleteLink.setVisible(false);
		}
	}

}
