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
import org.openlca.ilcd.io.ZipStore;
import org.openlca.io.ilcd.output.Export;

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
				ModelType.IMPACT_CATEGORY,
				ModelType.EPD,
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
							.withExtensions("*.zip")
							.withLabel(M.UseAZipFileTemplate)
							.withDialogTitle("Select a valid ILCD package as export template")
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
		var target = prepareTarget();
		if (target == null)
			return false;
		var descriptors = page.getSelectedModels();
		try (var zip = new ZipStore(target)) {
			var export = new Export(Database.get(), zip)
					.withLang(IoPreference.getIlcdLanguage());
			getContainer().run(true, true,
					monitor -> runExport(monitor, export, descriptors));
			return true;
		} catch (Exception e) {
			ErrorReporter.on("ILCD export failed", e);
			return false;
		}
	}

	private void runExport(
			IProgressMonitor monitor, Export export, List<RootDescriptor> descriptors
	) throws InvocationTargetException {

		int n = descriptors.size();
		monitor.beginTask(M.Export, n == 1 ? IProgressMonitor.UNKNOWN : n);
		int worked = 0;

		for (var d : descriptors) {
			if (monitor.isCanceled())
				break;
			monitor.setTaskName(d.name);
			try {
				var e = Daos.root(Database.get(), d.type).getForId(d.id);
				export.write(e);
				if (n > 1) {
					monitor.worked(++worked);
				}
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		}
	}

	private File prepareTarget() {
		var target = page.getExportDestination();
		if (target == null)
			return null;

		if (template != null && template.equals(target)) {
			MsgBox.error(M.SameFileForExportTargetAndTemplate,
					M.SameFileForExportTargetAndTemplateErr);
			return null;
		}

		if (target.exists()) {
			try {
				Files.delete(target.toPath());
			} catch (Exception e) {
				MsgBox.error(M.CouldNotDeleteOldFile, M.CouldNotDeleteOldFileErr);
				return null;
			}
		}

		if (template != null && template.exists()) {
			try {
				Files.copy(template.toPath(), target.toPath());
			} catch (Exception e) {
				MsgBox.error(M.FailedToCopyTemplateFile, M.FailedToCopyTemplateFileErr);
				return null;
			}
		}
		return target;
	}
}
