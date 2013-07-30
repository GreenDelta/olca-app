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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.SelectObjectDialog;
import org.openlca.app.UI;
import org.openlca.app.UIFactory;
import org.openlca.app.Viewers;
import org.openlca.app.db.Database;
import org.openlca.app.dnd.IModelDropHandler;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form page to edit the flow properties of this flow object
 */
public class FlowPropertiesPage extends ModelEditorPage {

	private Flow flow;
	private TableViewer propertyViewer;

	public FlowPropertiesPage(ModelEditor editor) {
		super(editor, "FlowPropertiesPage",
				Messages.Flows_FlowPropertiesPageLabel);
		this.flow = (Flow) editor.getModelComponent();
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {

		final int heightHint = getManagedForm().getForm().computeSize(
				SWT.DEFAULT, SWT.DEFAULT).y / 3;

		Section section = UIFactory.createSection(body, toolkit,
				Messages.Flows_FlowPropertiesPageLabel, true, true);
		final Composite composite = UIFactory.createSectionComposite(section,
				toolkit, UIFactory.createGridLayout(1, true, 0));

		// create table viewer for displaying and editing flow properties
		propertyViewer = UIFactory.createTableViewer(composite,
				ModelType.FLOW_PROPERTY, new FlowPropertyDropHandler(),
				toolkit, FlowPropertyColumn.LABELS);
		propertyViewer.setCellModifier(new FlowPropertyFactorCellModifier());
		FlowPropertyLabel labelProvider = new FlowPropertyLabel(flow,
				propertyViewer.getTable());
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
		FlowPropertyAddAction add = new FlowPropertyAddAction();
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
		propertyViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (!selection.isEmpty()) {
					FlowProperty flowProperty = ((FlowPropertyFactor) selection
							.getFirstElement()).getFlowProperty();
					App.openEditor(flowProperty);
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

	private FlowPropertyFactor findOrCreateFactor(BaseDescriptor descriptor) {
		try {
			FlowProperty prop = Database.load(descriptor);
			FlowPropertyFactor factor = flow.getFactor(prop);
			if (factor != null)
				return factor;
			factor = new FlowPropertyFactor();
			factor.setConversionFactor(1.0);
			factor.setFlowProperty(prop);
			return factor;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to create flow property factor");
			return null;
		}
	}

	private void refreshViewer(List<FlowPropertyFactor> selection) {
		if (selection == null)
			return;
		if (propertyViewer != null) {
			propertyViewer.setInput(flow.getFlowPropertyFactors());
			propertyViewer.setSelection(new StructuredSelection(selection));
		}
	}

	private class FlowPropertyDropHandler implements IModelDropHandler {

		@Override
		public void handleDrop(List<BaseDescriptor> descriptors) {
			if (descriptors == null || descriptors.isEmpty())
				return;
			List<FlowPropertyFactor> factors = new ArrayList<>();
			for (BaseDescriptor descriptor : descriptors) {
				if (descriptor.getModelType() != ModelType.FLOW_PROPERTY)
					continue;
				FlowPropertyFactor factor = findOrCreateFactor(descriptor);
				if (factor != null)
					factors.add(factor);
			}
			propertyViewer.setInput(flow.getFlowPropertyFactors());
			propertyViewer.setSelection(new StructuredSelection(factors));
		}

	}

	class FlowPropertyAddAction extends Action {

		public FlowPropertyAddAction() {
			setId("FlowPropertyAddAction");
			setText(Messages.Flows_AddFlowPropertyFactorText);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			SelectObjectDialog dialog = new SelectObjectDialog(UI.shell(),
					ModelType.FLOW_PROPERTY, false);
			int code = dialog.open();
			BaseDescriptor descriptor = dialog.getSelection();
			if (code != Window.OK || descriptor == null
					|| descriptor.getModelType() != ModelType.FLOW_PROPERTY)
				return;
			FlowPropertyFactor factor = findOrCreateFactor(descriptor);
			refreshViewer(Arrays.asList(factor));
		}
	}

	private class FlowPropertyFactorCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			boolean canModifiy = false;
			if (property.equals(Messages.Flows_ConversionFactor)
					|| property.equals(Messages.Flows_IsReference)) {
				canModifiy = true;
			}
			if (property.equals(Messages.Flows_ConversionFactor)) {
				if (element instanceof FlowPropertyFactor) {
					if (flow.getReferenceFlowProperty().equals(
							((FlowPropertyFactor) element).getFlowProperty())) {
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
				FlowPropertyFactor flowPropertyFactor = (FlowPropertyFactor) element;
				if (property.equals(Messages.Flows_ConversionFactor)) {
					v = Double.toString(flowPropertyFactor
							.getConversionFactor());
				} else if (property.equals(Messages.Flows_IsReference)) {
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
					Double factor = flowPropertyFactor.getConversionFactor();
					try {
						factor = Double.parseDouble(value.toString());
					} catch (final NumberFormatException e) {
					}
					flowPropertyFactor.setConversionFactor(factor);
				} else if (property.equals(Messages.Flows_IsReference)) {
					flow.setReferenceFlowProperty(flowPropertyFactor
							.getFlowProperty());
					double factor = flowPropertyFactor.getConversionFactor();
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
			propertyViewer.setInput(flow.getFlowPropertyFactors());
		}
	}

	private class RemoveFlowPropertyFactorAction extends
			DeleteWithQuestionAction {

		public RemoveFlowPropertyFactorAction() {
			setText(Messages.Flows_RemoveFlowPropertyFactorText);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void delete() {
			List<FlowPropertyFactor> selection = Viewers
					.getAllSelected(propertyViewer);
			// TODO: do not delete reference flow property and used factors !!!
			flow.getFlowPropertyFactors().removeAll(selection);
			propertyViewer.setInput(flow.getFlowPropertyFactors());
		}

	}

}
