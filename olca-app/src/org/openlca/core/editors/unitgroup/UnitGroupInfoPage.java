/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.unitgroup;

import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.IContentChangedListener;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.TextDropComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form page for displaying and editing a unit group object
 * 
 * @author Sebastian Greve
 * 
 */
public class UnitGroupInfoPage extends ModelEditorInfoPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private TextDropComponent defaultFlowPropertyDropComponent;
	private IMessageManager messageManager;

	private String UNIT_CONVERSION_FACTOR = Messages.Units_ConversionFactor;
	private String UNIT_DESCRIPTION = Messages.Common_Description;
	private String UNIT_FORMULA = Messages.Units_Formula;
	private String UNIT_IS_REFERENCE = Messages.Units_IsReference;
	private String UNIT_NAME = Messages.Common_Name;
	private String UNIT_SYNONYMS = Messages.Units_Synonyms;

	private String[] UNIT_PROPERTIES = new String[] { UNIT_NAME,
			UNIT_DESCRIPTION, UNIT_SYNONYMS, UNIT_CONVERSION_FACTOR,
			UNIT_FORMULA, UNIT_IS_REFERENCE };

	private UnitGroup unitGroup = null;
	private Section unitsSection;
	private TableViewer unitsTableViewer;

	public UnitGroupInfoPage(ModelEditor editor) {
		super(editor, "UnitGroupInfoPage", Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		this.unitGroup = (UnitGroup) editor.getModelComponent();
	}

	@Override
	protected void createContents(Composite body, FormToolkit toolkit) {
		super.createContents(body, toolkit);
		messageManager = getForm().getMessageManager();

		int heightHint = getForm().computeSize(SWT.DEFAULT, SWT.DEFAULT).y / 3;

		// create flow property drop component
		defaultFlowPropertyDropComponent = createDropComponent(
				getMainComposite(), toolkit,
				Messages.Units_DefaultFlowProperty,
				unitGroup.getDefaultFlowProperty(), FlowProperty.class, false);

		// create unit section
		unitsSection = UIFactory.createSection(body, toolkit,
				Messages.Units_UnitGroupInfoSectionLabel, true, true);
		Composite unitsComposite = UIFactory.createSectionComposite(
				unitsSection, toolkit, UIFactory.createGridLayout(1, true, 0));

		// create table viewer for displaying and editing units
		unitsTableViewer = UIFactory.createTableViewer(unitsComposite, null,
				null, toolkit, UNIT_PROPERTIES, getDatabase());
		unitsTableViewer.setCellModifier(new UnitCellModifier());
		unitsTableViewer.setLabelProvider(new UnitLabelProvider(unitGroup,
				unitsTableViewer.getTable()));

		bindActions(unitsSection, unitsTableViewer);

		GridData unitsGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		unitsGridData.heightHint = heightHint;
		unitsTableViewer.getTable().setLayoutData(unitsGridData);
		unitsTableViewer.getTable().getColumn(0).setWidth(75);

		// Create the cell editors
		CellEditor[] unitsEditors = new CellEditor[6];
		for (int i = 0; i < 5; i++) {
			unitsEditors[i] = new TextCellEditor(unitsTableViewer.getTable());
		}
		unitsEditors[5] = new CheckboxCellEditor(unitsTableViewer.getTable());
		unitsTableViewer.setCellEditors(unitsEditors);
	}

	private void bindActions(Section section, TableViewer viewer) {
		Action add = new AddUnitAction();
		Action remove = new RemoveUnitAction();
		UI.bindActions(section, add, remove);
		UI.bindActions(viewer, add, remove);
	}

	@Override
	protected String getFormTitle() {
		String title = Messages.Units_FormText
				+ ": "
				+ (unitGroup != null ? unitGroup.getName() != null ? unitGroup
						.getName() : "" : "");
		return title;
	}

	@Override
	protected void initListeners() {
		super.initListeners();
		defaultFlowPropertyDropComponent
				.addContentChangedListener(new IContentChangedListener() {

					@Override
					public void contentChanged(Control source, Object content) {
						if (content != null) {
							// load flow property
							FlowProperty defaultFlowProperty;
							try {
								defaultFlowProperty = getDatabase().select(
										FlowProperty.class,
										((IModelComponent) content).getId());
								unitGroup
										.setDefaultFlowProperty(defaultFlowProperty);
							} catch (Exception e) {
								log.error("Loading flow property failed", e);
							}

						} else {
							unitGroup.setDefaultFlowProperty(null);
						}
					}

				});

		unitsTableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						IStructuredSelection selection = (IStructuredSelection) event
								.getSelection();
						boolean isValid = true;
						int i = 0;
						while (isValid && i < selection.toArray().length) {
							if (unitGroup.getReferenceUnit().equals(
									selection.toArray()[i])) {
								isValid = false;
							} else {
								i++;
							}
						}
					}

				});

		unitsSection.addExpansionListener(new IExpansionListener() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {

			}

			@Override
			public void expansionStateChanging(ExpansionEvent e) {
				((GridData) unitsSection.getLayoutData()).grabExcessVerticalSpace = e
						.getState();
			}
		});

	}

	@Override
	protected void setData() {
		super.setData();
		if (unitGroup != null) {
			if (unitGroup.getUnits() != null) {
				unitsTableViewer.setInput(unitGroup.getUnits());
			}
		}
	}

	/**
	 * Adds a unit object to the unit group
	 * 
	 * @see Action
	 */
	private class AddUnitAction extends Action {

		public AddUnitAction() {
			setId("UnitGroupInfoPage.AddUnitAction");
			setText(Messages.Units_AddUnitText);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());

		}

		@Override
		public void run() {
			// create unit
			Unit unit = new Unit();
			String name = Messages.Units_Unit;
			int i = 1;
			// check if exists, if so set new name
			while (unitGroup.getUnit(name) != null) {
				name = Messages.Units_Unit + (unitGroup.getUnits().size() + i);
			}
			unit.setName(name);
			unit.setId(UUID.randomUUID().toString());
			unitGroup.getUnits().add(unit);
			if (unitGroup.getUnits().size() == 1) {
				unitGroup.setReferenceUnit(unit);
			}

			// refresh table viewer
			unitsTableViewer.setInput(unitGroup.getUnits());
			unitsTableViewer.setSelection(new StructuredSelection(unit));
		}
	}

	/**
	 * Removes the selected unit object from the unit group
	 * 
	 * @see Action
	 */
	private class RemoveUnitAction extends DeleteWithQuestionAction {

		public RemoveUnitAction() {
			setId("UnitGroupInfoPage.RemoveUnitAction");
			setText(Messages.Units_RemoveUnitText);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void delete() {
			StructuredSelection structuredSelection = (StructuredSelection) unitsTableViewer
					.getSelection();
			for (int i = 0; i < structuredSelection.toArray().length; i++) {
				Unit unit = (Unit) structuredSelection.toArray()[i];
				// TODO: check that unit is not used
				messageManager.removeMessage(unit.toString());
				unitGroup.getUnits().remove(unit);
			}
			unitsTableViewer.setInput(unitGroup.getUnits());
		}

	}

	/**
	 * A cell modifier for the unitsTableViewer
	 * 
	 * @see ICellModifier
	 */
	private class UnitCellModifier implements ICellModifier {

		@Override
		public boolean canModify(Object element, String property) {

			boolean canModify = false;
			if (!property.equals(UNIT_FORMULA)) {
				// formulas cannot be modified
				canModify = true;
			}
			if (property.equals(UNIT_CONVERSION_FACTOR)
					&& unitGroup.getReferenceUnit().equals(element)) {
				// reference unit's conversion factor is always 1
				canModify = false;
			}
			return canModify;
		}

		@Override
		public Object getValue(Object element, String property) {
			Object v = null;

			if (element instanceof Unit) {
				Unit unit = (Unit) element;
				if (property.equals(UNIT_NAME)) {
					// get name
					v = unit.getName();
				} else if (property.equals(UNIT_DESCRIPTION)) {
					// get description
					v = unit.getDescription();
				} else if (property.equals(UNIT_SYNONYMS)) {
					// get synonyms
					v = unit.getSynonyms();
				} else if (property.equals(UNIT_CONVERSION_FACTOR)) {
					// get conversion factor
					v = Double.toString(unit.getConversionFactor());
				} else if (property.equals(UNIT_IS_REFERENCE)) {
					// get reference
					v = unit.equals(unitGroup.getReferenceUnit());
				}
			}

			return v != null ? v : "";
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}

			if (element instanceof Unit) {
				Unit unit = (Unit) element;
				if (property.equals(UNIT_NAME)) {
					// set name
					if (!unit.getName().equals(value.toString())) {
						if (unitGroup.getUnit(value.toString()) == null) {
							unit.setName(value.toString());
						} else {
							// unit exists already in the unit group
							MessageDialog.openError(UI.shell(),
									Messages.Units_UnitExists, NLS.bind(
											Messages.Units_UnitExistsMessage,
											new String[] { value.toString(),
													unitGroup.getName() }));
						}
					}
				} else if (property.equals(UNIT_DESCRIPTION)) {
					unit.setDescription(value.toString());
				} else if (property.equals(UNIT_SYNONYMS)) {
					unit.setSynonyms(value.toString());
				} else if (property.equals(UNIT_CONVERSION_FACTOR)) {
					Double factor = unit.getConversionFactor();
					try {
						factor = Double.parseDouble(value.toString());
					} catch (NumberFormatException e) {
					}
					unit.setConversionFactor(factor);
				} else if (property.equals(UNIT_IS_REFERENCE)) {
					unitGroup.setReferenceUnit(unit);
					double factor = unit.getConversionFactor();
					for (Unit u : unitGroup.getUnits()) {
						u.setConversionFactor((double) Math.round(1000000000
								* u.getConversionFactor() / factor) / 1000000000);
					}
				}
			}
			unitsTableViewer.setInput(unitGroup.getUnits());
			unitsTableViewer.refresh();
		}
	}
}
