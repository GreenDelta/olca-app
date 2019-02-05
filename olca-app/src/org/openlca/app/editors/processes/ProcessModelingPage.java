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
		Composite composite = UI.formSection(parent, toolkit, M.ModelingAndValidation, 3);
		getManagedForm().getToolkit().createLabel(composite, M.ProcessType);
		ProcessTypeViewer typeViewer = new ProcessTypeViewer(composite);
		getBinding().onModel(() -> getModel(), "processType", typeViewer);
		new CommentControl(composite, getToolkit(), "processType", getComments());
		multiText(composite, M.LCIMethod, "documentation.inventoryMethod", 40);
		multiText(composite, M.ModelingConstants, "documentation.modelingConstants", 40);
		multiText(composite, M.DataCompleteness, "documentation.completeness", 40);
		multiText(composite, M.DataSelection, "documentation.dataSelection", 40);
		multiText(composite, M.DataTreatment, "documentation.dataTreatment", 40);
	}

	private void createDataSourceSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.DataSourceInformation, 3);
		multiText(composite, M.SamplingProcedure, "documentation.sampling", 40);
		multiText(composite, M.DataCollectionPeriod, "documentation.dataCollectionPeriod", 40);
	}

	private void createEvaluationSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.ProcessEvaluationAndValidation, 3);
		dropComponent(composite, M.Reviewer, "documentation.reviewer");
		multiText(composite, M.DataSetOtherEvaluation, "documentation.reviewDetails", 40);
	}

	private void createSourcesSection(Composite parent) {
		Section section = UI.section(parent, toolkit, M.Sources);
		Composite comp = toolkit.createComposite(section);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, true);
		section.setClient(comp);
		SourceViewer viewer = new SourceViewer(editor, comp, Database.get());
		viewer.setInput(getModel());
		CommentAction.bindTo(section, viewer, "documentation.sources", editor.getComments());
		editor.onSaved(() -> viewer.setInput(getModel()));
	}
}
