package org.openlca.app.wizards;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.core.matrix.LinkingConfig;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

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
		LinkingConfig config = page.getLinkingConfig();
		system.cutoff = config.cutoff;
		addCreationInfo(system, page);
		try {
			createDao().insert(system);
			boolean autoComplete = page.addSupplyChain();
			if (!autoComplete) {
				App.openEditor(system);
				return true;
			}
			Runner runner = new Runner(system, config);
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

	private void addCreationInfo(ProductSystem system, ProductSystemWizardPage page) {
		if (system == null)
			return;
		String text = system.getDescription();
		if (Strings.isNullOrEmpty(text))
			text = "";
		else
			text += "\n\n~~~~~~\n\n";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		text += "First created: " + format.format(new Date()) + "\n";
		text += "Linking approach during creation: " + getLinkingInfo(page);
		system.setDescription(text);
	}

	private String getLinkingInfo(ProductSystemWizardPage page) {
		LinkingConfig config = page.getLinkingConfig();
		if (!page.addSupplyChain() || config == null)
			return M.None;
		String suffix = "; " + M.PreferredProcessType + ": ";
		if (config.preferredType == ProcessType.UNIT_PROCESS) {
			suffix += M.UnitProcess;
		} else {
			suffix += M.SystemProcess;
		}
		if (config.cutoff != null) {
			suffix += "; cutoff = " + config.cutoff.toString();
		}
		if (config.providerLinking == null)
			return suffix;
		switch (config.providerLinking) {
		case IGNORE:
			return M.IgnoreDefaultProviders + suffix;
		case ONLY:
			return M.OnlyLinkDefaultProviders + suffix;
		case PREFER:
			return M.PreferDefaultProviders + suffix;
		default:
			return "???" + suffix;
		}
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.PRODUCT_SYSTEM;
	}

	private class Runner implements IRunnableWithProgress {

		private ProductSystem system;
		private final LinkingConfig config;

		public Runner(ProductSystem system, LinkingConfig config) {
			this.system = system;
			this.config = config;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(M.CreatingProductSystem,
						IProgressMonitor.UNKNOWN);
				ProductSystemBuilder builder = new ProductSystemBuilder(
						Cache.getMatrixCache(), config);
				builder.autoComplete(system);
				system = builder.saveUpdates(system);
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
