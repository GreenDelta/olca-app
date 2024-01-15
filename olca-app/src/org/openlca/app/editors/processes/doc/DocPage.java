package org.openlca.app.editors.processes.doc;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ProcessTypeViewer;
import org.openlca.core.model.Process;

public class DocPage extends ModelPage<Process> {

	private final ProcessEditor editor;

	public DocPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", "Documentation");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		createInventorySection(body, tk);
		createDataSourceSection(body, tk);
		createReviewSection(body, tk);
		new CompletenessTable(editor).render(body, tk);
		createSourcesSection(body, tk);
		createAdminInfoSection(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void createInventorySection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, M.LCIMethod, 3);
		UI.label(comp, getToolkit(), M.ProcessType);
		var typeCombo = new ProcessTypeViewer(comp);
		getBinding().onModel(this::getModel, "processType", typeCombo);
		typeCombo.setEnabled(isEditable());
		new CommentControl(comp, getToolkit(), "processType", getComments());
		multiText(comp, M.LCIMethod, "documentation.inventoryMethod", 40);
		multiText(comp, M.ModelingConstants, "documentation.modelingConstants", 40);
	}

	private void createDataSourceSection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, M.DataSourceInformation, 3);
		multiText(comp, M.DataCompleteness, "documentation.dataCompleteness", 40);
		multiText(comp, M.DataSelection, "documentation.dataSelection", 40);
		multiText(comp, M.DataTreatment, "documentation.dataTreatment", 40);
		multiText(comp, M.SamplingProcedure, "documentation.samplingProcedure", 40);
		multiText(comp, M.DataCollectionPeriod, "documentation.dataCollectionPeriod", 40);
		multiText(comp, "Use advice", "documentation.useAdvice", 40);
	}

	private void createReviewSection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, "Review", 3);
		modelLink(comp, M.Reviewer, "documentation.reviewer");
		modelLink(comp, "Review report", "documentation.reviewReport");
		multiText(comp, "Review details", "documentation.reviewDetails", 40);
	}

	private void createSourcesSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, M.Sources);
		var comp = UI.composite(section, tk);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, true);
		section.setClient(comp);
		var viewer = new SourcesTable(editor, comp, Database.get());
		viewer.setInput(getModel());
		CommentAction.bindTo(section, viewer, "documentation.sources", editor.getComments());
		editor.onSaved(() -> viewer.setInput(getModel()));
	}

	private void createAdminInfoSection(Composite parent, FormToolkit tk) {
		var comp = UI.formSection(parent, tk, M.AdministrativeInformation, 3);
		multiText(comp, M.Project, "documentation.project", 40);
		multiText(comp, M.IntendedApplication, "documentation.intendedApplication", 40);
		modelLink(comp, M.DataSetOwner, "documentation.dataOwner");
		modelLink(comp, M.DataGenerator, "documentation.dataGenerator");
		modelLink(comp, M.DataDocumentor, "documentation.dataDocumentor");
		modelLink(comp, M.Publication, "documentation.publication");
		readOnly(comp, M.CreationDate, "documentation.creationDate");
		checkBox(comp, M.Copyright, "documentation.copyright");
		multiText(comp, M.AccessAndUseRestrictions, "documentation.accessRestrictions", 40);
	}

}
