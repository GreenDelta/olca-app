package org.openlca.app.editors.systems;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileSelection;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.xls.systems.SystemExport;
import org.openlca.io.xls.systems.SystemExportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemExportDialog extends WizardDialog {

	private static class SystemExportWizard extends Wizard {

		private Logger log = LoggerFactory.getLogger(getClass());

		private class SystemExportWizardPage extends WizardPage {

			private AllocationMethodViewer allocationMethodViewer;
			private FileSelection directorySelection;
			private ImpactMethodViewer impactMethodViewer;

			protected SystemExportWizardPage() {
				super("SystemExportWizardPage");
				setImageDescriptor(ImageType.FILE_EXCEL_SMALL.getDescriptor());
				setTitle(Messages.ProductSystemExcelExport);
				setDescription(Messages.ProductSystemExcelExportMessage);
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
				Composite composite = new Composite(parent, SWT.NONE);
				UI.gridLayout(composite, 1);

				Group methodGroup = createGroup(Messages.Methods, composite, 1);
				UI.formLabel(methodGroup, Messages.AllocationMethod);
				allocationMethodViewer = new AllocationMethodViewer(
						methodGroup, AllocationMethod.values());
				UI.formLabel(methodGroup, Messages.ImpactAssessmentMethod);
				impactMethodViewer = new ImpactMethodViewer(methodGroup);
				impactMethodViewer.setInput(database);

				Group fileGroup = createGroup(Messages.ExportDirectory,
						composite, 1);
				directorySelection = new FileSelection(fileGroup);
				directorySelection.setSelectDirectory(true);
				directorySelection
						.addSelectionListener(new SelectionListener() {

							@Override
							public void widgetSelected(SelectionEvent e) {
								setPageComplete(directorySelection.getFile() != null);
							}

							@Override
							public void widgetDefaultSelected(SelectionEvent e) {
								setPageComplete(directorySelection.getFile() != null);
							}
						});
				setControl(composite);
			}

			private AllocationMethod getAllocationMethod() {
				return allocationMethodViewer.getSelected();
			}

			private BaseDescriptor getImpactMethod() {
				return impactMethodViewer.getSelected();
			}

			private File getDirectory() {
				return directorySelection.getFile();
			}

		}

		private IDatabase database;
		private ProductSystem productSystem;
		private SystemExportWizardPage page = new SystemExportWizardPage();

		private SystemExportWizard(ProductSystem productSystem,
				IDatabase database) {
			this.productSystem = productSystem;
			this.database = database;
			setNeedsProgressMonitor(true);
		}

		@Override
		public void addPages() {
			addPage(page);
		}

		@Override
		public boolean performFinish() {
			boolean errorOccured = false;
			final AllocationMethod allocation = page.getAllocationMethod();
			final BaseDescriptor impactMethod = page.getImpactMethod();
			final File directory = page.getDirectory();
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(Messages.Export,
								IProgressMonitor.UNKNOWN);
						SystemExportConfig conf = new SystemExportConfig(
								productSystem, database, App.getSolver()
										.getMatrixFactory());
						conf.setAllocationMethod(allocation);
						conf.setEntityCache(Cache.getEntityCache());
						conf.setImpactMethod(impactMethod);
						conf.setMatrixCache(Cache.getMatrixCache());
						conf.setOlcaVersion(App.getVersion());
						SystemExport export = new SystemExport(conf);
						try {
							export.exportTo(directory);
						} catch (IOException e) {
							throw new InvocationTargetException(e);
						} finally {
							monitor.done();
						}
					}
				});
			} catch (Exception e) {
				errorOccured = true;
				log.error("Error while exporting system", e);
			}
			return !errorOccured;
		}
	}

	public SystemExportDialog(ProductSystem productSystem, IDatabase database) {
		super(UI.shell(), new SystemExportWizard(productSystem, database));
	}
}
