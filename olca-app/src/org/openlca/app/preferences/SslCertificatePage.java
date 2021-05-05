package org.openlca.app.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Actions;
import org.openlca.app.util.DateFormatter;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.cloud.util.Ssl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslCertificatePage extends PreferencePage implements IWorkbenchPreferencePage {

	private final static Logger log = LoggerFactory.getLogger(SslCertificatePage.class);
	private File certificateDir;
	private CertificateFactory certFactory;
	private TableViewer viewer;

	@Override
	public void init(IWorkbench workbench) {
		certificateDir = new File(Workspace.getDir(), "ssl-certificates");
		try {
			certFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			log.error("Error initializing X.509 certificate factory", e);
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		UI.gridLayout(parent, 1);
		UI.gridData(parent, true, true);
		viewer = Tables.createViewer(parent, new String[] { "Issuer", "Validity" }, new LabelProvider());
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(getInput());
		viewer.addDoubleClickListener(e -> openCertficateDialog(e.getSelection()));
		Tables.bindColumnWidths(viewer, 0.8, 0.2);
		Tables.onDeletePressed(viewer, e -> onRemove());
		Action onAdd = Actions.onAdd(this::onAdd);
		Action onRemove = Actions.onRemove(this::onRemove);
		Actions.bind(viewer, onAdd, onRemove);
		return parent;
	}

	private Input[] getInput() {
		List<Input> inputs = new ArrayList<>();
		if (certificateDir.exists()) {
			for (File file : certificateDir.listFiles()) {
				inputs.add(new Input(file, readCertificate(file)));
			}
		}
		return inputs.toArray(new Input[inputs.size()]);
	}

	private X509Certificate readCertificate(File file) {
		try (FileInputStream is = new FileInputStream(file)) {
			return (X509Certificate) certFactory.generateCertificate(is);
		} catch (Exception e) {
			log.warn("Could not generate certificate from file " + file.getAbsolutePath(), e);
			return null;
		}
	}

	private void openCertficateDialog(ISelection selection) {
		Input[] inputs = getSelection(selection);
		if (inputs == null || inputs.length == 0)
			return;
		new SslCertificateDialog(inputs[0].cert).open();
	}

	private Input[] getSelection(ISelection sel) {
		if (!(sel instanceof StructuredSelection))
			return new Input[0];
		StructuredSelection selection = (StructuredSelection) sel;
		Input[] inputs = new Input[selection.size()];
		int i = 0;
		for (Object input : selection) {
			inputs[i++] = (Input) input;
		}
		return inputs;
	}

	private void onAdd() {
		String filename = new FileDialog(UI.shell()).open();
		if (filename == null)
			return;
		File from = new File(filename);
		String name = from.getName().substring(0, from.getName().lastIndexOf("."));
		if (!certificateDir.exists()) {
			certificateDir.mkdirs();
		}
		File to = new File(certificateDir, name + ".pem");
		try (FileInputStream in = new FileInputStream(to)) {
			Files.copy(from.toPath(), to.toPath());
			Ssl.addCertificate(name, in);
			viewer.setInput(getInput());
		} catch (IOException e) {
			log.error("Error copying certificate", e);
		}
	}

	private void onRemove() {
		Input[] selection = getSelection(viewer.getSelection());
		if (selection == null || selection.length == 0)
			return;
		for (Input input : selection) {
			input.file.delete();
			Ssl.removeCertificate(input.file.getName().substring(0, input.file.getName().lastIndexOf(".")));
		}
		viewer.setInput(getInput());
	}

	private class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			X509Certificate cert = ((Input) element).cert;
			switch (columnIndex) {
			case 0:
				return cert.getSubjectX500Principal().getName();
			case 1:
				String validity = "";
				if (cert.getNotBefore() != null) {
					validity = DateFormatter.formatShort(cert.getNotBefore()) + " ";
				}
				validity += "-";
				if (cert.getNotAfter() != null) {
					validity += " " + DateFormatter.formatShort(cert.getNotAfter());
				}
				return validity;
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

	}

	private class Input {

		private final File file;
		private final X509Certificate cert;

		private Input(File file, X509Certificate cert) {
			this.file = file;
			this.cert = cert;
		}

	}

}
