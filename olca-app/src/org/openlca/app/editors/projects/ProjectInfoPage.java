package org.openlca.app.editors.projects;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.Project;

class ProjectInfoPage extends ModelPage<Project> {

	private FormToolkit toolkit;
	private ScrolledForm form;

	public ProjectInfoPage(ProjectEditor editor) {
		super(editor, "ProjectInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createGoalAndScopeSection(body);
		createTimeInfoSection(body);
		form.reflow(true);
	}

	private void createGoalAndScopeSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.GoalAndScope, 3);
		multiText(composite, M.Goal, "goal");
		multiText(composite, M.FunctionalUnit, "functionalUnit");
	}

	private void createTimeInfoSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, M.TimeAndAuthor, 3);
		readOnly(composite, M.CreationDate, "creationDate");
		readOnly(composite, M.LastModificationDate, "lastModificationDate");
		dropComponent(composite, M.Author, "author");
	}

}
