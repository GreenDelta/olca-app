package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ProcessTypeViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

class ProcessModelingPage extends ModelPage<Process> {

	private FormToolkit toolkit;
	private ProcessEditor editor;
	private ScrolledForm form;

	ProcessModelingPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage",
				M.ModelingAndValidation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
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
				M.ModelingAndValidation);
		getManagedForm().getToolkit().createLabel(composite,
				M.ProcessType);
		ProcessTypeViewer typeViewer = new ProcessTypeViewer(composite);
		getBinding().onModel(() -> getModel(), "processType", typeViewer);
		createMultiText(M.LCIMethod, "documentation.inventoryMethod",
				composite);
		createMultiText(M.ModelingConstants,
				"documentation.modelingConstants", composite);
		createMultiText(M.DataCompleteness,
				"documentation.completeness", composite);
		createMultiText(M.DataSelection, "documentation.dataSelection",
				composite);
		createMultiText(M.DataTreatment, "documentation.dataTreatment",
				composite);
	}

	private void createDataSourceSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				M.DataSourceInformation);
		createMultiText(M.SamplingProcedure, "documentation.sampling", composite);
		createMultiText(M.DataCollectionPeriod,
				"documentation.dataCollectionPeriod", composite);
	}

	private void createEvaluationSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				M.ProcessEvaluationAndValidation);
		createDropComponent(M.Reviewer, "documentation.reviewer",
				ModelType.ACTOR, composite);
		createMultiText(M.DataSetOtherEvaluation,
				"documentation.reviewDetails", composite);
	}

	private void createSourcesSection(Composite parent) {
		Section section = UI.section(parent, toolkit,
				M.Sources);
		Composite composite = toolkit.createComposite(section);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		section.setClient(composite);
		SourceViewer viewer = new SourceViewer(composite, Database.get(),
				editor, form);
		viewer.setInput(getModel());
		viewer.bindTo(section);
		editor.onSaved(() -> {
			viewer.setInput(getModel());
		});
	}
}
