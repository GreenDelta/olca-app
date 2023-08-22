package org.openlca.app.wizards.io;

import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.simapro.csv.output.SimaProExport;

public class SimaProProcessExportWizard
		extends Wizard implements IExportWizard {

	private ModelSelectionPage modelPage;
	private ConfigPage configPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Export processes to a SimaPro CSV file");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		modelPage = ModelSelectionPage.forFile("csv", ModelType.PROCESS);
		addPage(modelPage);
		configPage = new ConfigPage();
		addPage(configPage);
	}

	@Override
	public boolean performFinish() {

		// convert selection to process descriptors
		var models = modelPage.getSelectedModels()
				.stream()
				.filter(m -> m instanceof ProcessDescriptor)
				.map(m -> (ProcessDescriptor) m)
				.collect(Collectors.toList());
		if (models.isEmpty())
			return true;

		// write CSV file
		try {
			var file = modelPage.getExportDestination();
			getContainer().run(true, true, monitor -> {
				monitor.beginTask(
						M.ExportingProcesses, IProgressMonitor.UNKNOWN);
				SimaProExport.of(Database.get(), models)
						.withTopCategoryAsType(configPage.withTopCategoryToType)
						.withFlowMap(configPage.flowMap)
						.withProcessSuffixes(configPage.withProcessSuffix)
						.withLocationSuffixes(configPage.withLocationSuffix)
						.withTypeSuffixes(configPage.withTypeSuffix)
						.writeTo(file);
				monitor.done();
			});
			Popup.info(
					"Export done",
					"Wrote " + models.size() + " data sets to " + file.getName());
			return true;
		} catch (Exception e) {
			ErrorReporter.on("SimaPro CSV export failed", e);
			return false;
		}
	}

	private static class ConfigPage extends WizardPage {

		private FlowMap flowMap;
		private boolean withTopCategoryToType = true;
		private boolean withProcessSuffix = true;
		private boolean withLocationSuffix = true;
		private boolean withTypeSuffix = true;

		ConfigPage() {
			super("ConfigPage");
			setTitle("Export configuration");
			setPageComplete(true);
		}

		@Override
		public void createControl(Composite parent) {
			var body = UI.composite(parent);
			setControl(body);
			UI.gridLayout(body, 1);

			var generalGroup = new Group(body, SWT.NONE);
			generalGroup.setText("General export settings");
			UI.gridLayout(generalGroup, 1);
			UI.fillHorizontal(generalGroup);

			var categoryCheck = UI.checkbox(
					generalGroup, "Use matching top categories as process types");
			categoryCheck.setSelection(withTopCategoryToType);
			Controls.onSelect(categoryCheck,
					$ -> withTopCategoryToType = categoryCheck.getSelection());

			var mappingComp = UI.composite(generalGroup);
			UI.fillHorizontal(mappingComp);
			UI.gridLayout(mappingComp, 3);
			MappingSelector.on(flowMap -> this.flowMap = flowMap)
					.render(mappingComp);

			// product names
			var productGroup = new Group(body, SWT.NONE);
			productGroup.setText("Exported product names");
			UI.fillHorizontal(productGroup);
			UI.gridLayout(productGroup, 1);

			var example = UI.label(
					productGroup, "Example: product | process {GLO}, U");
			Runnable updateExample = () -> {
				var text = "Example: product";
				if (withProcessSuffix) {
					text += " | process";
				}
				if (withLocationSuffix) {
					text += " {GLO}";
				}
				if (withTypeSuffix) {
					text += ", U";
				}
				example.setText(text);
			};
			updateExample.run();

			var processCheck = UI.checkbox(productGroup, "Append process names");
			processCheck.setSelection(withProcessSuffix);
			Controls.onSelect(processCheck, $ -> {
				withProcessSuffix = processCheck.getSelection();
				updateExample.run();
			});

			var locationCheck = UI.checkbox(productGroup, "Append location codes");
			locationCheck.setSelection(withLocationSuffix);
			Controls.onSelect(locationCheck, $ -> {
				withLocationSuffix = locationCheck.getSelection();
				updateExample.run();
			});

			var typeCheck = UI.checkbox(productGroup,
					"Append process types (U: unit process, S: system/LCI result)");
			typeCheck.setSelection(withTypeSuffix);
			Controls.onSelect(typeCheck, $ -> {
				withTypeSuffix = typeCheck.getSelection();
				updateExample.run();
			});
		}
	}
}
