/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.processes;

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
import org.openlca.app.viewers.table.SourceViewer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

class ProcessModelingPage extends ModelPage<Process> {

	private FormToolkit toolkit;

	ProcessModelingPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage",
				Messages.ModelingAndValidationPageLabel);
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
				Messages.ModelingAndValidationPageLabel);

		getManagedForm().getToolkit().createLabel(composite,
				Messages.ProcessType);
		ProcessTypeViewer typeViewer = new ProcessTypeViewer(composite);
		getBinding().on(getModel(), "processType", typeViewer);
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
				Messages.DataSourceInfoSectionLabel);

		createMultiText(Messages.Sampling, "documentation.sampling", composite);
		createMultiText(Messages.DataCollectionPeriod,
				"documentation.dataCollectionPeriod", composite);
	}

	private void createEvaluationSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.EvaluationSectionLabel);

		createDropComponent(Messages.Reviewer, "documentation.reviewer",
				ModelType.ACTOR, composite);
		createMultiText(Messages.DatasetOtherEvaluation,
				"documentation.reviewDetails", composite);
	}

	private void createSourcesSection(Composite parent) {
		Section section = UI.section(parent, toolkit,
				Messages.SourcesInfoSectionLabel);
		Composite composite = toolkit.createComposite(section);
		UI.gridLayout(composite, 1);
		UI.gridData(composite, true, true);
		section.setClient(composite);

		SourceViewer sourceViewer = new SourceViewer(composite, Database.get());
		getBinding().on(getModel(), "documentation.sources", sourceViewer);
		sourceViewer.bindTo(section);
	}
}
