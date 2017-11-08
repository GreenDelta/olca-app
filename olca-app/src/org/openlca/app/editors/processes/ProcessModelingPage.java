package org.openlca.app.editors.processes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ProcessTypeViewer;
import org.openlca.core.model.Process;

class ProcessModelingPage extends ModelPage<Process> {

	private FormToolkit toolkit;
	private ProcessEditor editor;
	private ScrolledForm form;

	ProcessModelingPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.ModelingAndValidation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm);
		updateFormTitle();
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createModelingSection(body);
		createDataSourceSection(body);
		createEvaluationSection(body);
		createSourcesSection(body);
		body.setFocus();
		form.reflow(true);
	}

	@Override
	protected void updateFormTitle() {
		if (form == null)
			return;
		form.setText(M.Process + ": " + getModel().getName());
	}

	private void createModelingSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.ModelingAndValidation, 3);
		getManagedForm().getToolkit().createLabel(composite, M.ProcessType);
		ProcessTypeViewer typeViewer = new ProcessTypeViewer(composite);
		getBinding().onModel(() -> getModel(), "processType", typeViewer);
		new CommentControl(composite, getToolkit(), "processType", getComments());
		multiText(composite, M.LCIMethod, "documentation.inventoryMethod");
		multiText(composite, M.ModelingConstants, "documentation.modelingConstants");
		multiText(composite, M.DataCompleteness, "documentation.completeness");
		multiText(composite, M.DataSelection, "documentation.dataSelection");
		multiText(composite, M.DataTreatment, "documentation.dataTreatment");
	}

	private void createDataSourceSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.DataSourceInformation, 3);
		multiText(composite, M.SamplingProcedure, "documentation.sampling");
		multiText(composite, M.DataCollectionPeriod, "documentation.dataCollectionPeriod");
	}

	private void createEvaluationSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.ProcessEvaluationAndValidation, 3);
		dropComponent(composite, M.Reviewer, "documentation.reviewer");
		multiText(composite, M.DataSetOtherEvaluation, "documentation.reviewDetails");
	}

	private void createSourcesSection(Composite parent) {
		Section section = UI.section(parent, toolkit, M.Sources);
		Composite composite = toolkit.createComposite(section);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		section.setClient(composite);
		SourceViewer viewer = new SourceViewer(composite, Database.get(), editor, form);
		viewer.setInput(getModel());
		CommentAction.bindTo(section, viewer, "sources", editor.getComments());
		editor.onSaved(() -> viewer.setInput(getModel()));
	}
}
