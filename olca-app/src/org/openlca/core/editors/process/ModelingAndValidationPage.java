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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Actor;
import org.openlca.core.model.ModelingAndValidation;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.Source;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.DataBinding;
import org.openlca.ui.IContentChangedListener;
import org.openlca.ui.SelectObjectDialog;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.IModelDropHandler;
import org.openlca.ui.dnd.TextDropComponent;
import org.openlca.ui.viewer.ISelectionChangedListener;
import org.openlca.ui.viewer.ProcessTypeViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FormPage to display and edit the modeling and validation information of a
 * process object
 * 
 * @author Sebastian Greve
 * 
 */
public class ModelingAndValidationPage extends ModelEditorPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Process process;
	private ModelingAndValidation modelingAndValidation;
	private DataBinding dataBinding = new DataBinding();
	private ProcessTypeViewer processTypeViewer;
	private TextDropComponent reviewerDropComponent;
	private TableViewer sourceTableViewer;

	private OpenEditorAction openAction;

	public ModelingAndValidationPage(ModelEditor editor,
			ModelingAndValidation modelingAndValidation) {
		super(editor, "ModelingAndValidationPage",
				Messages.Processes_ModelingAndValidationPageLabel);
		this.process = (Process) editor.getModelComponent();
		this.modelingAndValidation = modelingAndValidation;
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
		dataBinding.onString(modelingAndValidation, "LCIMethod", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_ModelingConstants, true);
		dataBinding.onString(modelingAndValidation, "modelingConstants", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_DataCompleteness, true);
		dataBinding.onString(modelingAndValidation, "dataCompleteness", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_DataSelection, true);
		dataBinding.onString(modelingAndValidation, "dataSelection", text);

		text = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_DataTreatment, true);
		dataBinding.onString(modelingAndValidation, "dataTreatment", text);

		Section dataSourceInfoSection = UIFactory.createSection(body, toolkit,
				Messages.Processes_DataSourceInfoSectionLabel, true, false);

		Composite dataSourceInfoComposite = UIFactory.createSectionComposite(
				dataSourceInfoSection, toolkit, UIFactory.createGridLayout(2));

		text = UIFactory.createTextWithLabel(dataSourceInfoComposite, toolkit,
				Messages.Processes_Sampling, true);
		dataBinding.onString(modelingAndValidation, "sampling", text);

		text = UIFactory.createTextWithLabel(dataSourceInfoComposite, toolkit,
				Messages.Processes_DataCollectionPeriod, true);
		dataBinding.onString(modelingAndValidation, "dataCollectionPeriod",
				text);

		Section evaluationSection = UIFactory.createSection(body, toolkit,
				Messages.Processes_EvaluationSectionLabel, true, false);

		Composite evaluationComposite = UIFactory.createSectionComposite(
				evaluationSection, toolkit, UIFactory.createGridLayout(2));

		reviewerDropComponent = createDropComponent(evaluationComposite,
				toolkit, Messages.Processes_Reviewer,
				modelingAndValidation.getReviewer(), Actor.class, false);

		text = UIFactory.createTextWithLabel(evaluationComposite, toolkit,
				Messages.Processes_DatasetOtherEvaluation, true);
		dataBinding.onString(modelingAndValidation, "dataSetOtherEvaluation",
				text);

		Section sourcesSection = UI.section(body, toolkit,
				Messages.Processes_SourcesInfoSectionLabel);
		UI.gridData(sourcesSection, true, true);
		Composite sourcesComposite = UI.sectionClient(sourcesSection, toolkit);
		UI.gridLayout(sourcesComposite, 1);

		IModelDropHandler sourceDropHandler = new SourceDropHandler();

		sourceTableViewer = UIFactory.createTableViewer(sourcesComposite,
				Source.class, sourceDropHandler, toolkit, null, getDatabase());
		sourceTableViewer.setLabelProvider(new SourceLabelProvider());
		UI.gridData(sourceTableViewer.getTable(), true, true).heightHint = heightHint;
		bindSourcesActions(sourceTableViewer, sourcesSection);

		if (process != null) {
			if (modelingAndValidation.getSources() != null) {
				sourceTableViewer.setInput(modelingAndValidation.getSources());
			}
		}

		openAction = new OpenEditorAction();
		sourceTableViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (!selection.isEmpty()) {
					Source source = (Source) selection.getFirstElement();
					openAction.setModelComponent(getDatabase(), source);
					openAction.run();
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

		reviewerDropComponent
				.addContentChangedListener(new IContentChangedListener() {

					@Override
					public void contentChanged(Control source, Object content) {
						if (content != null) {
							if (modelingAndValidation != null) {
								try {
									Actor reviewer = getDatabase()
											.select(Actor.class,
													((IModelComponent) content)
															.getId());
									modelingAndValidation.setReviewer(reviewer);
								} catch (Exception e) {
									log.error(
											"Reading actor from database failed",
											e);
								}
							}
						} else {
							modelingAndValidation.setReviewer(null);
						}
					}

				});
	}

	private class AddSourceAction extends Action {

		private final String ID = "org.openlca.core.editors.process.ModelingAndValidationPage.AddSourceAction";

		public AddSourceAction() {
			setId(ID);
			setText(Messages.Processes_AddSourceText);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			// get navigation root
			NavigationRoot root = null;
			Navigator navigator = (Navigator) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(Navigator.ID);
			if (navigator != null) {
				root = navigator.getRoot();
			}
			// create select object dialog
			SelectObjectDialog dialog = new SelectObjectDialog(UI.shell(),
					root, true, getDatabase(), Source.class);
			dialog.open();
			int code = dialog.getReturnCode();

			if (code == Window.OK && dialog.getMultiSelection() != null) {
				// add sources
				Source[] sources = new Source[dialog.getMultiSelection().length];
				// for each selected source
				for (int i = 0; i < dialog.getMultiSelection().length; i++) {
					// load source
					try {
						Source source = getDatabase().select(Source.class,
								dialog.getMultiSelection()[i].getId());
						// add source
						modelingAndValidation.add(source);
						sources[i] = source;
					} catch (Exception e) {
						log.error("Reading source from database failed", e);
					}
				}

				// refresh viewer
				sourceTableViewer.setInput(modelingAndValidation.getSources());
				sourceTableViewer
						.setSelection(new StructuredSelection(sources));
			}
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
				modelingAndValidation.remove(source);
			}
			sourceTableViewer.setInput(modelingAndValidation.getSources());
		}

	}

	private class SourceDropHandler implements IModelDropHandler {

		@Override
		public void handleDrop(IModelComponent[] droppedComponents) {
			Source[] sources = new Source[droppedComponents.length];
			for (int i = 0; i < droppedComponents.length; i++) {
				try {
					Source source = getDatabase().select(Source.class,
							droppedComponents[i].getId());
					modelingAndValidation.add(source);
					sources[i] = source;
				} catch (Exception e) {
					log.error("Reading source from database failed", e);
				}
			}
			sourceTableViewer.setInput(modelingAndValidation.getSources());
			sourceTableViewer.setSelection(new StructuredSelection(sources));
		}

	}

	@Override
	protected void setData() {
		processTypeViewer.select(process.getProcessType());
	}

}
