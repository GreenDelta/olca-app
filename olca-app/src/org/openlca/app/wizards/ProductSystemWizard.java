package org.openlca.app.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.App;
import org.openlca.app.FeatureFlag;
import org.openlca.app.Messages;
import org.openlca.app.components.ProgressAdapter;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IProductSystemBuilder;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemWizard extends AbstractWizard<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ProductSystemWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		ProductSystemWizardPage page = (ProductSystemWizardPage) getPage();
		ProductSystem system = page.createModel();
		if (system == null) {
			log.error("Unexpected error: no page or product system");
			return false;
		}
		try {
			createDao().insert(system);
			boolean autoComplete = page.addSupplyChain();
			if (!autoComplete)
				return true;
			boolean preferSystems = page.useSystemProcesses();
			Runner runner = new Runner(system, preferSystems);
			if (FeatureFlag.PRODUCT_SYSTEM_CUTOFF.isEnabled())
				runner.setCutoff(page.getCutoff());
			getContainer().run(true, true, runner);
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
		private IDatabase db;
		private Double cutoff;

		public Runner(ProductSystem system, boolean preferSystemProcesses) {
			this.system = system;
			this.preferSystemProcesses = preferSystemProcesses;
			this.db = Database.get();
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

	@Override
	protected String getTitle() {
		return Messages.Systems_WizardTitle;
	}

	@Override
	protected BaseDao<ProductSystem> createDao() {
		return Database.createDao(ProductSystem.class);
	}

	@Override
	protected AbstractWizardPage<ProductSystem> createPage() {
		return new ProductSystemWizardPage();
	}
}
