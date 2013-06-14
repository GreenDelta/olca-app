/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.flow;

import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.application.wizards.DeleteWizard;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.IDropHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form page to edit the flow properties of this flow object
 */
public class FlowPropertiesPage extends ModelEditorPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Flow flow;
	private TableViewer propertyViewer;

	private OpenEditorAction openEditorAction = null;
	private Section section;

	public FlowPropertiesPage(ModelEditor editor) {
		super(editor, "FlowPropertiesPage",
				Messages.Flows_FlowPropertiesPageLabel);
		this.flow = (Flow) editor.getModelComponent();
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		openEditorAction = new OpenEditorAction();

		final int heightHint = getManagedForm().getForm().computeSize(
				SWT.DEFAULT, SWT.DEFAULT).y / 3;

		section = UIFactory.createSection(body, toolkit,
				Messages.Flows_FlowPropertiesPageLabel, true, true);
		final Composite composite = UIFactory.createSectionComposite(section,
				toolkit, UIFactory.createGridLayout(1, true, 0));

		// create table viewer for displaying and editing flow properties
		propertyViewer = UIFactory.createTableViewer(composite,
				FlowProperty.class, new FlowPropertyDropHandler(), toolkit,
				FlowPropertyColumn.LABELS, getDatabase());
		propertyViewer.setCellModifier(new FlowPropertyFactorCellModifier());
		FlowPropertyLabel labelProvider = new FlowPropertyLabel(flow,
				getDatabase(), propertyViewer.getTable());
		propertyViewer.setLabelProvider(labelProvider);

		bindActions(section, propertyViewer);

		final GridData flowPropertiesGridData = new GridData(SWT.FILL,
				SWT.FILL, true, true);
		flowPropertiesGridData.heightHint = heightHint;
		propertyViewer.getTable().setLayoutData(flowPropertiesGridData);
		propertyViewer.getTable().getColumn(0).setWidth(150);

		// Create the cell editors
		final CellEditor[] flowPropertiesEditors = new CellEditor[4];
		flowPropertiesEditors[1] = new TextCellEditor(propertyViewer.getTable());
		flowPropertiesEditors[3] = new CheckboxCellEditor(
				propertyViewer.getTable());

		propertyViewer.setCellEditors(flowPropertiesEditors);
	}

	private void bindActions(Section section, TableViewer viewer) {
		FlowPropertyAddAction add = new FlowPropertyAddAction(flow,
				getDatabase());
		add.setViewer(viewer);
		RemoveFlowPropertyFactorAction remove = new RemoveFlowPropertyFactorAction();
		UI.bindActions(viewer, add, remove);
		UI.bindActions(section, add, remove);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Common_Flow
				+ ": "
				+ (flow != null ? flow.getName() != null ? flow.getName() : ""
						: "");
		return title;
	}

	@Override
	protected void initListeners() {
		section.addExpansionListener(new IExpansionListener() {

			@Override
			public void expansionStateChanged(final ExpansionEvent e) {

			}

			@Override
			public void expansionStateChanging(final ExpansionEvent e) {
				((GridData) section.getLayoutData()).grabExcessVerticalSpace = e
						.getState();
			}
		});

		propertyViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (!selection.isEmpty()) {
					final FlowProperty flowProperty = ((FlowPropertyFactor) selection
							.getFirstElement()).getFlowProperty();
					openEditorAction.setModelComponent(getDatabase(),
							flowProperty);
					openEditorAction.run();
				}
			}

		});

		propertyViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						final IStructuredSelection selection = (IStructuredSelection) event
								.getSelection();
						boolean isValid = true;
						int i = 0;
						while (isValid && i < selection.toArray().length) {
							if (flow.getReferenceFlowProperty()
									.getId()
									.equals(((FlowPropertyFactor) selection
											.toArray()[i]).getFlowProperty()
											.getId())) {
								isValid = false;
							} else {
								i++;
							}
						}
					}

				});
	}

	@Override
	protected void setData() {
		if (flow != null) {
			if (flow.getFlowPropertyFactors() != null) {
				propertyViewer.setInput(flow.getFlowPropertyFactors());
			}
		}
	}

	/**
	 * Implementation of {@link IDropHandler} for flow properties
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class FlowPropertyDropHandler implements IDropHandler {

		@Override
		public void handleDrop(final IModelComponent[] droppedComponents) {
			final FlowPropertyFactor[] factors = new FlowPropertyFactor[droppedComponents.length];
			// for each dropped component
			for (int i = 0; i < droppedComponents.length; i++) {
				// load flow property
				try {
					final FlowProperty flowProperty = getDatabase().select(
							FlowProperty.class, droppedComponents[i].getId());
					// create new flow property factor
					factors[i] = new FlowPropertyFactor(UUID.randomUUID()
							.toString(), flowProperty, 1);
					boolean contains = false;
					int j = 0;

					// check if already contained
					while (!contains
							&& j < flow.getFlowPropertyFactors().length) {
						if (flow.getFlowPropertyFactors()[j].getFlowProperty()
								.equals(flowProperty)) {
							contains = true;
						} else {
							j++;
						}
					}

					// if not contained
					if (!contains) {
						// add to flow information
						flow.add(factors[i]);
					}
				} catch (final Exception e) {
					log.error("Load flow property failed", e);
				}

			}

			// refresh table viewer
			propertyViewer.setInput(flow.getFlowPropertyFactors());
			propertyViewer.setSelection(new StructuredSelection(factors));
		}
	}

	/**
	 * A cell modifier for the flowPropertiesTableViewer
	 * 
	 * @see ICellModifier
	 */
	private class FlowPropertyFactorCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			boolean canModifiy = false;
			// if conversion factor or reference column
			if (property.equals(Messages.Flows_ConversionFactor)
					|| property.equals(Messages.Flows_IsReference)) {
				canModifiy = true;
			}
			// if conversion factor
			if (property.equals(Messages.Flows_ConversionFactor)) {
				// if flow property factor
				if (element instanceof FlowPropertyFactor) {
					// if is reference flow property
					if (flow.getReferenceFlowProperty().equals(
							((FlowPropertyFactor) element).getFlowProperty())) {
						// cannot modify
						canModifiy = false;
					}
				}
			}
			return canModifiy;
		}

		@Override
		public Object getValue(final Object element, final String property) {
			Object v = null;
			if (element instanceof FlowPropertyFactor) {
				final FlowPropertyFactor flowPropertyFactor = (FlowPropertyFactor) element;

				if (property.equals(Messages.Flows_ConversionFactor)) {
					// get conversion factor
					v = Double.toString(flowPropertyFactor
							.getConversionFactor());
				} else if (property.equals(Messages.Flows_IsReference)) {
					// get "is reference"
					v = flowPropertyFactor.getFlowProperty().equals(
							flow.getReferenceFlowProperty());
				}
			}
			return v != null ? v : "";
		}

		@Override
		public void modify(Object element, final String property,
				final Object value) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}

			if (element instanceof FlowPropertyFactor) {
				final FlowPropertyFactor flowPropertyFactor = (FlowPropertyFactor) element;
				if (property.equals(Messages.Flows_ConversionFactor)) {
					// set conversion factor
					Double factor = flowPropertyFactor.getConversionFactor();
					try {
						factor = Double.parseDouble(value.toString());
					} catch (final NumberFormatException e) {
					}
					flowPropertyFactor.setConversionFactor(factor);
				} else if (property.equals(Messages.Flows_IsReference)) {
					// set reference flow property
					flow.setReferenceFlowProperty(flowPropertyFactor
							.getFlowProperty());
					final double factor = flowPropertyFactor
							.getConversionFactor();
					// for each flow property factor of the flow information
					for (final FlowPropertyFactor fpFactor : flow
							.getFlowPropertyFactors()) {
						// convert factor to new reference
						fpFactor.setConversionFactor((double) Math
								.round(1000000000
										* fpFactor.getConversionFactor()
										/ factor) / 1000000000);
					}
				}
			}
			// refresh table viewer
			propertyViewer.setInput(flow.getFlowPropertyFactors());
			propertyViewer.refresh();

		}
	}

	/**
	 * Removes the selected flow property factor object from this flow
	 * 
	 * @see Action
	 */
	private class RemoveFlowPropertyFactorAction extends
			DeleteWithQuestionAction {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.core.editors.flow.FlowInfoPage.RemoveFlowPropertyFactorAction";

		/**
		 * The text of the action
		 */
		public String TEXT = Messages.Flows_RemoveFlowPropertyFactorText;

		/**
		 * Creates a new RemoveFlowPropertyFactorAction and sets the ID, TEXT
		 * and ImageDescriptor
		 */
		public RemoveFlowPropertyFactorAction() {
			setId(ID);
			setText(TEXT);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void delete() {
			// TODO: assert not reference flow property !!

			final StructuredSelection structuredSelection = (StructuredSelection) propertyViewer
					.getSelection();
			// for each selected flow property factor
			for (int i = 0; i < structuredSelection.toArray().length; i++) {
				// cast
				final FlowPropertyFactor flowPropertyFactor = (FlowPropertyFactor) structuredSelection
						.toArray()[i];
				// create deletewizard
				final DeleteWizard wizard = new DeleteWizard(getDatabase(),
						new FlowPropertyFactorReferenceSearcher(),
						flowPropertyFactor);
				boolean canDelete = true;
				// if references detected
				if (wizard.hasProblems()) {
					// show reference problems
					canDelete = new WizardDialog(UI.shell(), wizard).open() == Window.OK;
				}
				// if can delete
				if (canDelete) {
					// remove flow property factor
					flow.remove(flowPropertyFactor);
					// update table viewer
					propertyViewer.setInput(flow.getFlowPropertyFactors());
				}
			}
		}

	}

}
