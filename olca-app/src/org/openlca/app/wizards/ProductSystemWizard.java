package org.openlca.app.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IProductSystemBuilder;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemWizard extends AbstractWizard<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Process process;

	public ProductSystemWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Optionally sets the reference process for the product system that should
	 * be created.
	 */
	public void setProcess(Process process) {
		this.process = process;
	}

	@Override
	public boolean performFinish() {
		ProductSystemWizardPage page = (ProductSystemWizardPage) getPage();
		ProductSystem system = page.createModel();
		if (system == null) {
			log.error("Unexpected error: no page or product system");
			return false;
		}
		system.setCategory(getCategory());
		try {
			createDao().insert(system);
			boolean autoComplete = page.addSupplyChain();
			if (!autoComplete) {
				App.openEditor(system);
				return true;
			}
			boolean preferSystems = page.useSystemProcesses();
			Runner runner = new Runner(system, preferSystems);
			getContainer().run(true, true, runner);
			system = runner.system;
			Cache.registerNew(Descriptors.toDescriptor(system));
			App.openEditor(system);
			return true;
		} catch (Exception e) {
			log.error("Failed to create model", e);
			return false;
		}
	}

	private class Runner implements IRunnableWithProgress {

		private ProductSystem system;
		private boolean preferSystemProcesses;
		private MatrixCache cache;
		private Double cutoff;

		public Runner(ProductSystem system, boolean preferSystemProcesses) {
			this.system = system;
			this.preferSystemProcesses = preferSystemProcesses;
			this.cache = Cache.getMatrixCache();
		}

		public void setCutoff(double cutoff) {
			this.cutoff = cutoff;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(Messages.CreatingProductSystem,
						IProgressMonitor.UNKNOWN);
				IProductSystemBuilder builder = null;
				if (cutoff == null)
					builder = IProductSystemBuilder.Factory.create(cache,
							preferSystemProcesses);
				else
					builder = IProductSystemBuilder.Factory.create(cache,
							preferSystemProcesses, cutoff);
				system = builder.autoComplete(system);
				monitor.done();
			} catch (Exception e) {
				log.error("Failed to auto-complete product system", e);
			}
		}
	}

	@Override
	protected String getTitle() {
		return Messages.NewProductSystem;
	}

	@Override
	protected BaseDao<ProductSystem> createDao() {
		return Database.createDao(ProductSystem.class);
	}

	@Override
	protected AbstractWizardPage<ProductSystem> createPage() {
		ProductSystemWizardPage page = new ProductSystemWizardPage();
		page.setProcess(process);
		return page;
	}
}
