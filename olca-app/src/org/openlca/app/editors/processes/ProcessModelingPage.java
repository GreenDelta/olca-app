package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ProcessTypeViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

class ProcessModelingPage extends ModelPage<Process> {

	private FormToolkit toolkit;
	private ProcessEditor editor;

	ProcessModelingPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage",
				Messages.ModelingAndValidation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createModelingSection(body);
		createDataSourceSection(body);
		createEvaluationSection(body);
		createSourcesSection(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createModelingSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.ModelingAndValidation);
		getManagedForm().getToolkit().createLabel(composite,
				Messages.ProcessType);
		ProcessTypeViewer typeViewer = new ProcessTypeViewer(composite);
		getBinding().onModel(() -> getModel(), "processType", typeViewer);
		createMultiText(Messages.LCIMethod, "documentation.inventoryMethod",
				composite);
		createMultiText(Messages.ModelingConstants,
				"documentation.modelingConstants", composite);
		createMultiText(Messages.DataCompleteness,
				"documentation.completeness", composite);
		createMultiText(Messages.DataSelection, "documentation.dataSelection",
				composite);
		createMultiText(Messages.DataTreatment, "documentation.dataTreatment",
				composite);
	}

	private void createDataSourceSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.DataSourceInformation);
		createMultiText(Messages.SamplingProcedure, "documentation.sampling", composite);
		createMultiText(Messages.DataCollectionPeriod,
				"documentation.dataCollectionPeriod", composite);
	}

	private void createEvaluationSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.ProcessEvaluationAndValidation);
		createDropComponent(Messages.Reviewer, "documentation.reviewer",
				ModelType.ACTOR, composite);
		createMultiText(Messages.DataSetOtherEvaluation,
				"documentation.reviewDetails", composite);
	}

	private void createSourcesSection(Composite parent) {
		Section section = UI.section(parent, toolkit,
				Messages.Sources);
		Composite composite = toolkit.createComposite(section);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		section.setClient(composite);
		SourceViewer viewer = new SourceViewer(composite, Database.get(),
				editor);
		viewer.setInput(getModel());
		viewer.bindTo(section);
		editor.onSaved(() -> viewer.setInput(getModel()));
	}
}
