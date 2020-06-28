package org.openlca.app.editors.systems;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileSelection;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationCombo;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.xls.systems.SystemExport;
import org.openlca.io.xls.systems.SystemExportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemExportDialog extends WizardDialog {

	private static class SystemExportWizard extends Wizard {

		private Logger log = LoggerFactory.getLogger(getClass());

		private class SystemExportWizardPage extends WizardPage {

			private AllocationCombo allocationCombo;
			private FileSelection fileChooser;
			private ImpactMethodViewer impactCombo;

			protected SystemExportWizardPage() {
				super("SystemExportWizardPage");
				setImageDescriptor(Images.descriptor(FileType.EXCEL));
				setTitle(M.ProductSystemExcelExport);
				setDescription(M.ProductSystemExcelExportMessage);
				setPageComplete(false);
			}

			private Group createGroup(String label, Composite parent, int cols) {
				Group group = new Group(parent, SWT.NONE);
				UI.gridLayout(group, cols);
				UI.gridData(group, true, false);
				group.setText(label);
				return group;
			}

			@Override
			public void createControl(Composite parent) {
				Composite comp = new Composite(parent, SWT.NONE);
				UI.gridLayout(comp, 1);

				Group mgroup = createGroup(M.Methods, comp, 1);
				UI.formLabel(mgroup, M.AllocationMethod);
				allocationCombo = new AllocationCombo(
						mgroup, AllocationMethod.values());
				UI.formLabel(mgroup, M.ImpactAssessmentMethod);
				impactCombo = new ImpactMethodViewer(mgroup);
				impactCombo.setInput(db);

				Group fgroup = createGroup(
						M.ExportDirectory, comp, 1);
				fileChooser = new FileSelection(fgroup);
				fileChooser.setSelectDirectory(true);
				fileChooser.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						setPageComplete(fileChooser.getFile() != null);
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						setPageComplete(fileChooser.getFile() != null);
					}
				});
				setControl(comp);
			}
		}

		private IDatabase db;
		private ProductSystem system;
		private SystemExportWizardPage page = new SystemExportWizardPage();

		private SystemExportWizard(ProductSystem productSystem,
				IDatabase database) {
			this.system = productSystem;
			this.db = database;
			setNeedsProgressMonitor(true);
		}

		@Override
		public void addPages() {
			addPage(page);
		}

		@Override
		public boolean performFinish() {
			try {
				SystemExportConfig conf = new SystemExportConfig(
						system, db);
				conf.allocationMethod = page.allocationCombo.getSelected();
				conf.impactMethod = page.impactCombo.getSelected();
				conf.olcaVersion = App.getVersion();
				getContainer().run(true, true, monitor -> {
					monitor.beginTask(M.Export,
							IProgressMonitor.UNKNOWN);
					SystemExport export = new SystemExport(conf);
					try {
						export.exportTo(page.fileChooser.getFile());
					} catch (IOException e) {
						throw new InvocationTargetException(e);
					}
				});
				return true;
			} catch (Exception e) {
				log.error("Error while exporting system", e);
				return false;
			}
		}
	}

	public SystemExportDialog(ProductSystem system, IDatabase db) {
		super(UI.shell(), new SystemExportWizard(system, db));
	}
}
