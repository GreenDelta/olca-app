/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.process;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.application.db.Database;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Actor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.DataBinding;
import org.openlca.ui.SelectObjectDialog;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.IModelDropHandler;
import org.openlca.ui.dnd.ISingleModelDrop;
import org.openlca.ui.dnd.TextDropComponent;
import org.openlca.ui.viewer.ISelectionChangedListener;
import org.openlca.ui.viewer.ProcessTypeViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FormPage to display and edit the modelling and validation information of a
 * process object
 * 
 */
public class ModelingAndValidationPage extends ModelEditorPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Process process;
	private ProcessDocumentation doc;
	private DataBinding dataBinding = new DataBinding();
	private ProcessTypeViewer processTypeViewer;
	private TextDropComponent reviewerDrop;
	private TableViewer sourceTableViewer;

	public ModelingAndValidationPage(ModelEditor editor) {
		super(editor, "ModelingAndValidationPage",
				Messages.Processes_ModelingAndValidationPageLabel);
		this.process = (Process) editor.getModelComponent();
		this.doc = process.getDocumentation();
	}

	@Override
	protected void createContents(Composite body, FormToolkit toolkit) {
		int heightHint = getManagedForm().getForm().computeSize(SWT.DEFAULT,
				SWT.DEFAULT).y / 3;
		Section section = UIFactory.createSection(body, toolkit,
				Messages.Processes_ModelingAndValidationPageLabel, true, false);

		Composite composite = UIFactory.createSectionComposite(section,
				toolkit, UIFactory.createGridLayout(2));

		UI.formLabel(composite, Messages.Processes_ProcessType);
		processTypeViewer = new ProcessTypeViewer(composite);

		Text text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_LCIMethod, true);
		dataBinding.onString(doc, "LCIMethod", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_ModelingConstants, true);
		dataBinding.onString(doc, "modelingConstants", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_DataCompleteness, true);
		dataBinding.onString(doc, "dataCompleteness", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_DataSelection, true);
		dataBinding.onString(doc, "dataSelection", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_DataTreatment, true);
		dataBinding.onString(doc, "dataTreatment", text);

		Section dataSourceInfoSection = UIFactory.createSection(body, toolkit,
				Messages.Processes_DataSourceInfoSectionLabel, true, false);

		Composite dataSourceInfoComposite = UIFactory.createSectionComposite(
				dataSourceInfoSection, toolkit, UIFactory.createGridLayout(2));

		text = UIFactory.createTextWithLabel(dataSourceInfoComposite, toolkit,
				Messages.Processes_Sampling, true);
		dataBinding.onString(doc, "sampling", text);

		text = UIFactory.createTextWithLabel(dataSourceInfoComposite, toolkit,
				Messages.Processes_DataCollectionPeriod, true);
		dataBinding.onString(doc, "dataCollectionPeriod", text);

		Section evaluationSection = UIFactory.createSection(body, toolkit,
				Messages.Processes_EvaluationSectionLabel, true, false);

		Composite evaluationComposite = UIFactory.createSectionComposite(
				evaluationSection, toolkit, UIFactory.createGridLayout(2));

		reviewerDrop = UIFactory.createDropComponent(evaluationComposite,
				Messages.Processes_Reviewer, toolkit, false, ModelType.ACTOR);
		reviewerDrop.setContent(Descriptors.toDescriptor(doc.getReviewer()));

		text = UIFactory.createTextWithLabel(evaluationComposite, toolkit,
				Messages.Processes_DatasetOtherEvaluation, true);
		dataBinding.onString(doc, "dataSetOtherEvaluation", text);

		Section sourcesSection = UI.section(body, toolkit,
				Messages.Processes_SourcesInfoSectionLabel);
		UI.gridData(sourcesSection, true, true);
		Composite sourcesComposite = UI.sectionClient(sourcesSection, toolkit);
		UI.gridLayout(sourcesComposite, 1);

		IModelDropHandler sourceDropHandler = new SourceDropHandler();

		sourceTableViewer = UIFactory.createTableViewer(sourcesComposite,
				ModelType.SOURCE, sourceDropHandler, toolkit, null);
		sourceTableViewer.setLabelProvider(new SourceLabelProvider());
		UI.gridData(sourceTableViewer.getTable(), true, true).heightHint = heightHint;
		bindSourcesActions(sourceTableViewer, sourcesSection);

		if (process != null) {
			if (doc.getSources() != null) {
				sourceTableViewer.setInput(doc.getSources());
			}
		}

		sourceTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (!selection.isEmpty()) {
					Source source = (Source) selection.getFirstElement();
					App.openEditor(source);
				}
			}
		});
	}

	private void bindSourcesActions(TableViewer viewer, Section section) {
		AddSourceAction add = new AddSourceAction();
		RemoveSourceAction remove = new RemoveSourceAction();
		UI.bindActions(viewer, add, remove);
		UI.bindActions(section, add, remove);
	}

	@Override
	protected String getFormTitle() {
		String title = Messages.Processes_FormText
				+ ": "
				+ (process != null ? process.getName() != null ? process
						.getName() : "" : "");
		return title;
	}

	@Override
	protected void initListeners() {
		processTypeViewer
				.addSelectionChangedListener(new ISelectionChangedListener<ProcessType>() {

					@Override
					public void selectionChanged(ProcessType selection) {
						process.setProcessType(processTypeViewer.getSelected());
					}

				});

		reviewerDrop.setHandler(new ISingleModelDrop() {
			@Override
			public void handle(BaseDescriptor descriptor) {
				if (descriptor == null)
					doc.setReviewer(null);
				else
					try {
						Actor actor = Database.load(descriptor);
						doc.setReviewer(actor);
					} catch (Exception e) {
						log.error("failed to load reviewer", e);
					}
			}
		});

	}

	private void checkAddSources(BaseDescriptor descriptor) {
		try {
			Source source = Database.load(descriptor);
			if (source != null && !doc.getSources().contains(source))
				doc.getSources().add(source);
		} catch (Exception e) {
			log.error("Reading source from database failed", e);
		}
	}

	private class AddSourceAction extends Action {

		public AddSourceAction() {
			setText(Messages.Processes_AddSourceText);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			SelectObjectDialog dialog = new SelectObjectDialog(UI.shell(),
					ModelType.SOURCE, true);
			int code = dialog.open();
			BaseDescriptor[] selection = dialog.getMultiSelection();
			if (code != Window.OK || selection == null)
				return;
			for (int i = 0; i < selection.length; i++) {
				BaseDescriptor descriptor = selection[i];
				checkAddSources(descriptor);
			}
			sourceTableViewer.setInput(doc.getSources());
		}
	}

	private class RemoveSourceAction extends DeleteWithQuestionAction {

		public RemoveSourceAction() {
			setId("ModelingAndValidationPage.RemoveSourceAction");
			setText(Messages.Processes_RemoveSourceText);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void delete() {
			StructuredSelection structuredSelection = (StructuredSelection) sourceTableViewer
					.getSelection();
			for (int i = 0; i < structuredSelection.toArray().length; i++) {
				Source source = (Source) structuredSelection.toArray()[i];
				doc.getSources().remove(source);
			}
			sourceTableViewer.setInput(doc.getSources());
		}

	}

	private class SourceDropHandler implements IModelDropHandler {

		@Override
		public void handleDrop(List<BaseDescriptor> droppedComponents) {
			if (droppedComponents == null)
				return;
			for (BaseDescriptor d : droppedComponents)
				checkAddSources(d);
			sourceTableViewer.setInput(doc.getSources());
		}

	}

	@Override
	protected void setData() {
		processTypeViewer.select(process.getProcessType());
	}

}
