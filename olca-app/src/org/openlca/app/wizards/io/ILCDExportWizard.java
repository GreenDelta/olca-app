package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.preferences.IoPreference;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.Daos;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.ilcd.ILCDExport;
import org.openlca.io.ilcd.output.ExportConfig;

public class ILCDExportWizard extends Wizard implements IExportWizard {

	private ModelSelectionPage page;
	private File template;

	public ILCDExportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		ModelType[] types = {
				ModelType.PRODUCT_SYSTEM,
				ModelType.IMPACT_METHOD,
				ModelType.PROCESS,
				ModelType.FLOW,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.ACTOR,
				ModelType.SOURCE};
		page = ModelSelectionPage.forFile("zip", types)
				.withExtension(parent -> {
					var comp = UI.composite(parent);
					UI.fillHorizontal(comp);
					UI.gridLayout(comp, 3);
					FileSelector.on((file) -> template = file)
							.withExtensions("*zip")
							.withTitle("Use a zip file template:")
							.render(comp);
				});
		addPage(page);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(M.ExportILCD);
	}

	@Override
	public boolean performFinish() {
		var config = createConfig();
		if (config == null)
			return false;
		var descriptors = page.getSelectedModels();
		try {
			getContainer().run(true, true,
					monitor -> runExport(monitor, config, descriptors));
			return true;
		} catch (Exception e) {
			ErrorReporter.on("ILCD export failed", e);
			return false;
		}
	}

	private void runExport(
			IProgressMonitor monitor,
			ExportConfig config,
			List<RootDescriptor> descriptors) throws InvocationTargetException {

		int n = descriptors.size();
		monitor.beginTask(M.Export, n == 1 ? IProgressMonitor.UNKNOWN : n);
		int worked = 0;

		var export = new ILCDExport(config);
		for (var d : descriptors) {
			if (monitor.isCanceled())
				break;
			monitor.setTaskName(d.name);
			try {
				var e = Daos.root(config.db, d.type).getForId(d.id);
				export.export(e);
				if (n > 1) {
					monitor.worked(++worked);
				}
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		}
		export.close();
	}

	private ExportConfig createConfig() {
		var target = page.getExportDestination();
		if (target == null)
			return null;

		if (template != null && template.equals(target)) {
			MsgBox.error("Same file for export target and template",
					"You selected the same file as export target and template.");
			return null;
		}

		if (target.exists()) {
			try {
				Files.delete(target.toPath());
			} catch (Exception e) {
				MsgBox.error("Could not delete old file",
						"The file you selected as export file exists and could not" +
								" be deleted. It is maybe opened by another program or" +
								" you do not have write access to it.");
				return null;
			}
		}

		if (template != null && template.exists()) {
			try {
				Files.copy(template.toPath(), target.toPath());
			} catch (Exception e) {
				MsgBox.error("Failed to copy template file",
						"Failed to copy the template file to the target location.");
				return null;
			}
		}

		var config = new ExportConfig(Database.get(), target);
		config.lang = IoPreference.getIlcdLanguage();
		return config;
	}
}
