/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.lciamethod;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
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
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.SelectObjectDialog;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.IModelDropHandler;
import org.openlca.ui.viewer.ISelectionChangedListener;
import org.openlca.ui.viewer.LCIACategoryViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FormPage to display and edit the lcia factors of an lcia category
 * 
 * @author Sebastian Greve
 * 
 */
public class LCIAFactorsPage extends ModelEditorPage implements
		PropertyChangeListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private LCIACategoryViewer categoryViewer;
	private TableViewer factorViewer;

	private ImpactMethod lciaMethod;
	private OpenEditorAction openAction = null;

	public LCIAFactorsPage(final ModelEditor editor) {
		super(editor, "LCIAFactorsPage", Messages.Common_LCIAFactors);
		this.lciaMethod = (ImpactMethod) editor.getModelComponent();
	}

	/**
	 * Add new LCIA factors for each flow id
	 * 
	 * @param flowIds
	 *            The id's of the flows to add a factor for
	 */
	private void addFactors(final String[] flowIds) {
		final ImpactFactor[] factors = new ImpactFactor[flowIds.length];
		int i = 0;
		// for each flow id
		for (final String flowId : flowIds) {
			Flow flow = null;
			UnitGroup unitGroup = null;
			// load flow information, flow and unit group
			try {
				flow = getDatabase().select(Flow.class, flowId);
				unitGroup = getDatabase().select(UnitGroup.class,
						flow.getReferenceFlowProperty().getUnitGroupId());
			} catch (final Exception e) {
				log.error(
						"Loading flow, flow information and unit group from database failed",
						e);
			}
			if (unitGroup != null) {
				// create LCIA factor
				final ImpactFactor factor = new ImpactFactor();
				factor.setFlow(flow);
				factor.setFlowPropertyFactor(flow.getFlowPropertyFactor(flow
						.getReferenceFlowProperty().getId()));
				factor.setUnit(unitGroup.getReferenceUnit());

				// check if factor already exists
				boolean contains = false;
				int j = 0;
				while (!contains
						&& j < categoryViewer.getSelected().getLCIAFactors().length) {
					if (categoryViewer.getSelected().getLCIAFactors()[j]
							.getFlow().equals(factor.getFlow())) {
						contains = true;
					} else {
						j++;
					}
				}

				// if not already exists
				if (!contains) {
					// add factor
					categoryViewer.getSelected().add(factor);
				}
				factors[i] = factor;
				i++;
			}
		}

		// refresh table viewer
		factorViewer.setInput(categoryViewer.getSelected().getLCIAFactors());
		factorViewer.setSelection(new StructuredSelection(factors));

	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		final int heightHint = getForm().computeSize(SWT.DEFAULT, SWT.DEFAULT).y / 3;

		// create section
		final Section categorySection = UIFactory.createSection(body, toolkit,
				Messages.Methods_SelectLCIACategory, true, false);
		final Composite composite = UIFactory.createSectionComposite(
				categorySection, toolkit,
				UIFactory.createGridLayout(2, false, 5));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// create combo viewer for selecting an LCIA category
		UI.formLabel(composite, Messages.Common_ImpactCategory);
		categoryViewer = new LCIACategoryViewer(composite);

		// create LCIA factor section
		Section factorSection = UIFactory.createSection(body, toolkit,
				Messages.Common_LCIAFactors + " ("
						+ Messages.Processes_FormulaViewMode + ")", true, true);
		final Composite factorInfoComposite = UIFactory.createSectionComposite(
				factorSection, toolkit, UIFactory.createGridLayout(1));

		// create table viewer to display and edit LCIA factors
		factorViewer = UIFactory.createTableViewer(factorInfoComposite,
				Flow.class, new FlowDropHandler(), toolkit,
				FactorTable.COLUMN_PROPERTIES, getDatabase());
		factorViewer.setCellModifier(new FactorCellModifier(factorViewer,
				getDatabase()));
		factorViewer.setLabelProvider(new ImpactFactorLabel(getDatabase()));
		final GridData lciaFactorsGridData = new GridData(SWT.FILL, SWT.FILL,
				true, true);
		lciaFactorsGridData.heightHint = heightHint;
		lciaFactorsGridData.widthHint = 300;
		factorViewer.getTable().setLayoutData(lciaFactorsGridData);

		bindActions(factorViewer, factorSection);

		// for each table column
		for (final TableColumn c : factorViewer.getTable().getColumns()) {
			// if column is flow column
			if (c.getText().equals(FactorTable.FLOW)) {
				// set width to 150
				c.setWidth(150);
				break;
			}
		}

		openAction = new OpenEditorAction();

		// Create the cell editors
		final CellEditor[] lciaFactorsEditors = new CellEditor[6];
		lciaFactorsEditors[2] = new ComboBoxCellEditor(factorViewer.getTable(),
				new String[0], SWT.READ_ONLY);
		lciaFactorsEditors[3] = new ComboBoxCellEditor(factorViewer.getTable(),
				new String[0], SWT.READ_ONLY);
		lciaFactorsEditors[4] = new TextCellEditor(factorViewer.getTable());
		lciaFactorsEditors[5] = new UncertaintyCellEditor(
				factorViewer.getTable());
		factorViewer.setCellEditors(lciaFactorsEditors);

		// set sorter
		factorViewer.setSorter(new ViewerSorter() {

			@Override
			public int compare(final Viewer viewer, final Object e1,
					final Object e2) {
				int compare = 0;
				if (e1 instanceof ImpactFactor && e2 instanceof ImpactFactor) {
					// compare flow name
					final ImpactFactor f1 = (ImpactFactor) e1;
					final ImpactFactor f2 = (ImpactFactor) e2;
					compare = f1.getFlow().getName().toLowerCase()
							.compareTo(f2.getFlow().getName().toLowerCase());
				}
				return compare;
			}

		});

		UI.bindColumnWidths(factorViewer.getTable(), FactorTable.COLUMN_WIDTHS);

	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = new AddLCIAFactorAction();
		Action remove = new RemoveLCIAFactorAction();
		UI.bindActions(section, add, remove);
		UI.bindActions(factorViewer, add, remove);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Common_LCIAMethodTitle
				+ ": "
				+ (lciaMethod != null ? lciaMethod.getName() != null ? lciaMethod
						.getName() : ""
						: "");
		return title;
	}

	@Override
	protected void initListeners() {
		factorViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.getFirstElement() != null) {
					final Flow flow = ((ImpactFactor) selection
							.getFirstElement()).getFlow();
					openAction.setModelComponent(getDatabase(), flow);
					openAction.run();
				}
			}
		});

		categoryViewer
				.addSelectionChangedListener(new ISelectionChangedListener<LCIACategory>() {

					@Override
					public void selectionChanged(LCIACategory selection) {
						if (selection != null) {
							if (factorViewer != null) {
								factorViewer.setInput(selection
										.getLCIAFactors());
							}
						}
					}

				});

	}

	@Override
	protected void setData() {
		categoryViewer.setInput(lciaMethod);
		if (lciaMethod.getLCIACategories().length > 0) {
			final LCIACategory category = lciaMethod.getLCIACategories()[0];
			categoryViewer.select(category);
			factorViewer.setInput(category.getLCIAFactors());
		}
	}

	@Override
	public void dispose() {
		if (lciaMethod != null)
			lciaMethod.removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		if (propName.equals("lciaCategories") || propName.equals("name")) {
			if (categoryViewer != null) {
				LCIACategory selected = categoryViewer.getSelected();
				categoryViewer.setInput(lciaMethod);
				if (selected != null)
					categoryViewer.select(selected);
				else if (lciaMethod.getLCIACategories().length > 0)
					categoryViewer.select(lciaMethod.getLCIACategories()[0]);
			}
		} else if (propName.equals("value")
				|| propName.startsWith("uncertaintyParameter")) {
			if (factorViewer != null) {
				factorViewer.refresh();
			}
		}
	}

	/**
	 * Adds an lcia factor object to the selected lcia category
	 * 
	 * @see Action
	 */
	private class AddLCIAFactorAction extends Action {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.core.editors.lciamethod.LCIAFactorsPage.AddLCIACategoryAction";

		/**
		 * The text of the action
		 */
		public final String TEXT = Messages.Methods_AddLCIAFactorText;

		/**
		 * Creates a new instance
		 */
		public AddLCIAFactorAction() {
			setId(ID);
			setText(TEXT);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			// get the navigation root
			NavigationRoot root = null;
			final Navigator navigator = (Navigator) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(Navigator.ID);
			if (navigator != null) {
				root = navigator.getRoot();
			}

			// create select object dialog
			final SelectObjectDialog dialog = new SelectObjectDialog(
					UI.shell(), root, true, getDatabase(), Flow.class);
			dialog.open();
			final int code = dialog.getReturnCode();
			if (code == Window.OK && dialog.getMultiSelection() != null) {
				// if selection is not empty
				final String[] ids = new String[dialog.getMultiSelection().length];
				int i = 0;
				// for each selected flow
				for (final IModelComponent component : dialog
						.getMultiSelection()) {
					ids[i] = component.getId();
					i++;
				}
				// add factors
				addFactors(ids);
			}
		}

	}

	/**
	 * Implementation of {@link IModelDropHandler} for flows
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class FlowDropHandler implements IModelDropHandler {

		@Override
		public void handleDrop(final IModelComponent[] droppedComponents) {
			if (categoryViewer.getSelected() != null) {
				final String[] ids = new String[droppedComponents.length];
				int i = 0;
				for (final IModelComponent component : droppedComponents) {
					ids[i] = component.getId();
					i++;
				}
				addFactors(ids);
			}
		}
	}

	/**
	 * Removes the selected lCIA factor object from this lCIA category
	 * 
	 * @see Action
	 */
	private class RemoveLCIAFactorAction extends DeleteWithQuestionAction {

		public RemoveLCIAFactorAction() {
			setId("LCIAFactorsPage.RemoveLCIACategoryAction");
			setText(Messages.Methods_RemoveLCIAFactorText);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		protected void delete() {
			final StructuredSelection structuredSelection = (StructuredSelection) factorViewer
					.getSelection();
			final LCIAFactor lCIAFactor = (ImpactFactor) structuredSelection
					.getFirstElement();
			categoryViewer.getSelected().remove(lCIAFactor);
			factorViewer.setInput(categoryViewer.getSelected()
					.getImpactFactors());
		}

	}

}
