package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.Callback.Message;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.output.JsonExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ExportDataSets);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = ModelSelectionPage.forFile("zip", ModelType.categorized());
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		IDatabase db = Database.get();
		if (db == null)
			return true;
		List<BaseDescriptor> models = page.getSelectedModels();
		if (models == null || models.isEmpty())
			return true;
		File target = page.getExportDestination();
		if (target == null)
			return false;
		try {
			Export export = new Export(target, models, db);
			getContainer().run(true, true, export);
			return true;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to export data sets", e);
			return false;
		}
	}

	private class Export implements IRunnableWithProgress {

		private File zipFile;
		private List<BaseDescriptor> models;
		private IDatabase database;

		public Export(File zipFile, List<BaseDescriptor> models,
				IDatabase database) {
			this.zipFile = zipFile;
			this.models = models;
			this.database = database;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(M.Export, models.size());
			try (ZipStore store = ZipStore.open(zipFile)) {
				doExport(monitor, store);
				monitor.done();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}

		}

		private void doExport(IProgressMonitor monitor, ZipStore store) {
			JsonExport export = new JsonExport(database, store);
			export.setClientInfo("openLCA " + App.getVersion());
			for (BaseDescriptor model : models) {
				if (monitor.isCanceled())
					break;
				monitor.subTask(model.getName());
				ModelType type = model.getModelType();
				AbstractEntity o = Daos.base(database, type.getModelClass()).getForId(model.getId());
				if (o instanceof RootEntity)
					doExport(export, (RootEntity) o);
				monitor.worked(1);
			}
		}

		private void doExport(JsonExport export, RootEntity entity) {
			export.write(entity, (message, data) -> {
				if (message == null)
					return;
				switch (message.type) {
				case Message.INFO:
					log.trace("{}: {}", data, message.text);
					break;
				case Message.WARN:
					log.warn("{}: {}", data, message.text);
					break;
				case Message.ERROR:
					log.error("{}: {}", data, message.text);
					break;
				default:
					break;
				}
			});
		}
	}
}
