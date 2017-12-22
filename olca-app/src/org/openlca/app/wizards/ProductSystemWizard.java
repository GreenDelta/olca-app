package org.openlca.app.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.model.ModelType;
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
		if (system == null)
			return false;
		system.setCategory(getCategory());
		if (page.cutoff != null)
			system.cutoff = page.cutoff;
		try {
			createDao().insert(system);
			boolean autoComplete = page.addSupplyChain();
			if (!autoComplete) {
				App.openEditor(system);
				return true;
			}
			boolean preferSystems = page.useSystemProcesses();
			boolean linkProvidedOnly = page.linkProvidedOnly();
			Runner runner = new Runner(system, preferSystems, linkProvidedOnly);
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

	@Override
	protected ModelType getModelType() {
		return ModelType.PRODUCT_SYSTEM;
	}

	private class Runner implements IRunnableWithProgress {

		private ProductSystem system;
		private boolean preferSystemProcesses;
		private boolean linkProvidedOnly;
		private MatrixCache cache;

		public Runner(ProductSystem system, boolean preferSystemProcesses, boolean linkProvidedOnly) {
			this.system = system;
			this.preferSystemProcesses = preferSystemProcesses;
			this.cache = Cache.getMatrixCache();
			this.linkProvidedOnly = linkProvidedOnly;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(M.CreatingProductSystem,
						IProgressMonitor.UNKNOWN);
				ProductSystemBuilder builder = new ProductSystemBuilder(cache);
				builder.setPreferSystemProcesses(preferSystemProcesses);
				builder.setLinkProvidedOnly(linkProvidedOnly);
				if (system.cutoff != null)
					builder.setCutoff(system.cutoff);
				system = builder.autoComplete(system);
				monitor.done();
			} catch (Exception e) {
				log.error("Failed to auto-complete product system", e);
			}
		}
	}

	@Override
	protected String getTitle() {
		return M.NewProductSystem;
	}

	@Override
	protected AbstractWizardPage<ProductSystem> createPage() {
		ProductSystemWizardPage page = new ProductSystemWizardPage();
		page.setProcess(process);
		return page;
	}
}
