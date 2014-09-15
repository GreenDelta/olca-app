package org.openlca.app.editors.sources;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseFolder;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.Info;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

class SourceInfoPage extends ModelPage<Source> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ImageHyperlink fileLink;
	private ImageHyperlink deleteLink;

	SourceInfoPage(SourceEditor editor) {
		super(editor, "SourceInfoPage", Messages.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Source + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	protected void createAdditionalInfo(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.AdditionalInformation);
		createText(Messages.Doi, "doi", composite);
		createText(Messages.TextReference, "textReference", composite);
		Text text = UI.formText(composite, getManagedForm().getToolkit(),
				Messages.Year);
		getBinding().onShort(() -> getModel(), "year", text);
		createFileSection(composite);
	}

	private void createFileSection(Composite parent) {
		UI.formLabel(parent, Messages.File);
		Composite composite = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 10;
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.addMouseTrackListener(new DeleteFileVisibility());
		Button browseButton = toolkit.createButton(composite, Messages.Browse,
				SWT.NONE);
		Controls.onSelect(browseButton, (e) -> selectFile());
		createFileLink(composite);
		createDeleteLink(composite);
	}

	private void createDeleteLink(Composite composite) {
		deleteLink = toolkit.createImageHyperlink(composite, SWT.TOP);
		deleteLink.setImage(ImageType.DELETE_ICON_DISABLED.get());
		deleteLink.addMouseTrackListener(new DeleteFileVisibility());
		deleteLink.setVisible(false);
		deleteLink.setToolTipText(Messages.Delete);
		deleteLink.addHyperlinkListener(new DeleteFileListener());
	}

	private void createFileLink(Composite composite) {
		fileLink = toolkit.createImageHyperlink(composite, SWT.TOP);
		fileLink.setForeground(Colors.getLinkBlue());
		fileLink.addHyperlinkListener(new OpenFileListener());
		fileLink.addMouseTrackListener(new DeleteFileVisibility());
		String file = getModel().getExternalFile();
		if (file != null) {
			fileLink.setText(file);
			fileLink.setToolTipText(Messages.Open);
			fileLink.setImage(ImageType.forFile(file).get());
		}
	}

	private void selectFile() {
		File file = FileChooser.forImport("*.*");
		if (file == null)
			return;
		String fileName = file.getName();
		File dir = DatabaseFolder.getExternalDocLocation(Database.get());
		File dbFile = new File(dir, fileName);
		if (dbFile.exists()) {
			boolean doIt = Question
					.ask(Messages.OverwriteFile,
							Messages.SourceFileOverwriteFileQuestion);
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
			source.setExternalFile(dbFile.getName());
			getEditor().setDirty(true);
			updateFileLink();
		} catch (Exception e) {
			log.error("failed to copy file", e);
		}
	}

	private void updateFileLink() {
		String file = getModel().getExternalFile();
		if (file == null) {
			fileLink.setText("");
			fileLink.setToolTipText("");
			fileLink.setImage(null);
		} else {
			fileLink.setToolTipText(Messages.Open);
			fileLink.setText(file);
			fileLink.setImage(ImageType.forFile(file).get());
		}
		fileLink.getParent().getParent().layout();
	}

	private File getDatabaseFile() {
		String fileName = getModel().getExternalFile();
		if (fileName == null)
			return null;
		File dir = DatabaseFolder.getExternalDocLocation(Database.get());
		File file = new File(dir, fileName);
		return file;
	}

	private class OpenFileListener extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent evt) {
			File file = getDatabaseFile();
			if (file == null)
				return;
			if (file.exists()) {
				log.trace("open file {}", file);
				Desktop.browse(file.toURI().toString());
			} else {
				Info.showBox(Messages.FileDoesNotExist);
			}
		}
	}

	private class DeleteFileListener extends HyperlinkAdapter {
		@Override
		public void linkActivated(HyperlinkEvent e) {
			if (getModel().getExternalFile() == null)
				return;
			File file = getDatabaseFile();
			boolean doIt = Question.ask(Messages.DeleteFile,
					Messages.SourceFileDeleteQuestion);
			if (!doIt)
				return;
			try {
				if (file.exists())
					file.delete();
				getModel().setExternalFile(null);
				updateFileLink();
				getEditor().setDirty(true);
			} catch (Exception ex) {
				log.error("failed to delete file", ex);
			}
		}
	}

	private class DeleteFileVisibility extends MouseTrackAdapter {
		@Override
		public void mouseEnter(MouseEvent e) {
			if (e.widget == deleteLink)
				deleteLink.setImage(ImageType.DELETE_ICON.get());
			deleteLink.setVisible(getModel().getExternalFile() != null);
		}

		@Override
		public void mouseExit(MouseEvent e) {
			if (e.widget == deleteLink)
				deleteLink.setImage(ImageType.DELETE_ICON_DISABLED.get());
			deleteLink.setVisible(false);
		}
	}

}
