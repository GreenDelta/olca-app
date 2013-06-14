package org.openlca.core.editors.productsystem;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.application.App;
import org.openlca.core.application.FeatureFlag;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.ModelWizard;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IProductSystemBuilder;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.ProgressAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemWizard extends ModelWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private String title = Messages.Systems_WizardTitle;

	public ProductSystemWizard() {
		super(Messages.Systems_WizardTitle, new ProductSystemWizardPage());
		setNeedsProgressMonitor(true);
	}

	private ProductSystemWizardPage getPage() {
		IWizardPage[] pages = getPages();
		if (pages == null)
			return null;
		for (IWizardPage page : pages) {
			if (page instanceof ProductSystemWizardPage)
				return (ProductSystemWizardPage) page;
		}
		return null;
	}

	private ProductSystem getProductSystem() {
		ProductSystemWizardPage page = getPage();
		if (page == null)
			return null;
		Object[] data = page.getData();
		if (data == null)
			return null;
		for (Object datum : data)
			if (datum instanceof ProductSystem)
				return (ProductSystem) datum;
		return null;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(title);
		setDefaultPageImageDescriptor(ImageType.NEW_WIZARD.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		ProductSystemWizardPage page = getPage();
		ProductSystem system = getProductSystem();
		if (page == null || system == null) {
			log.error("Unexpected error: no page or product system");
			return false;
		}
		try {
			database.insert(system);
			boolean autoComplete = page.addSupplyChain();
			if (!autoComplete)
				return true;
			boolean preferSystems = page.useSystemProcesses();
			Runner runner = new Runner(database, system, preferSystems);
			if (FeatureFlag.PRODUCT_SYSTEM_CUTOFF.isEnabled())
				runner.setCutoff(page.getCutoff());
			getContainer().run(true, true, runner);
			App.openEditor(system, database);
			return true;
		} catch (Exception e) {
			log.error("Failed to create model", e);
			return false;
		}
	}

	private class Runner implements IRunnableWithProgress {

		private ProductSystem system;
		private boolean preferSystemProcesses;
		private IDatabase db;
		private Double cutoff;

		public Runner(IDatabase db, ProductSystem system,
				boolean preferSystemProcesses) {
			this.system = system;
			this.preferSystemProcesses = preferSystemProcesses;
			this.db = db;
		}

		public void setCutoff(double cutoff) {
			this.cutoff = cutoff;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(Messages.Systems_CreatingProductSystem,
						IProgressMonitor.UNKNOWN);
				ProgressAdapter progress = new ProgressAdapter(monitor);
				IProductSystemBuilder builder = null;
				if (cutoff == null)
					builder = IProductSystemBuilder.Factory.create(db,
							progress, preferSystemProcesses);
				else
					builder = IProductSystemBuilder.Factory.create(db,
							progress, preferSystemProcesses, cutoff);
				builder.autoComplete(system);
				monitor.done();
			} catch (Exception e) {
				log.error("Failed to auto-complete product system", e);
			}
		}
	}
}
