package org.openlca.app.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.app.viewers.combo.NormalizationWeightingSetViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Project;

public class ProjectSetupPage extends ModelPage<Project> {

	private FormToolkit toolkit;

	public ProjectSetupPage(ProjectEditor editor) {
		super(editor, "ProjectSetupPage", "Calculation setup");
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Project + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createSettingsSection(body);
		createParameterSecions(body);
	}

	private void createParameterSecions(Composite body) {

		// just a sample, TODO: create a parameter section for
		// each product system in the project
		for (int i = 1; i < 4; i++) {
			Section section = UI.section(body, toolkit,
					"Parameters for variant xyz" + i);
			UI.gridData(section, true, true);
			Composite client = UI.sectionClient(section, toolkit);
			new ProcessParameterSection(client);
			Actions.bind(section, new AddParamAction());
		}
	}

	private void createSettingsSection(Composite body) {
		Composite client = UI.formSection(body, toolkit, "Settings");
		UI.formLabel(client, toolkit, "Allocation method");
		new AllocationMethodViewer(client);
		UI.formLabel(client, toolkit, "LCIA Method");
		new ImpactMethodViewer(client);
		UI.formLabel(client, toolkit, "Normalisation and Weighting");
		new NormalizationWeightingSetViewer(client);
	}

	private class AddParamAction extends Action {

		public AddParamAction() {
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setText("Add parameter");
		}

		@Override
		public void run() {
			ObjectDialog.select(ModelType.PROCESS);
		}
	}

}
