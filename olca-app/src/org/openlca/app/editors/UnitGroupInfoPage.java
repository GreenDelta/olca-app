package org.openlca.app.editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.UnitViewer;
import org.openlca.core.editors.InfoSection;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitGroupInfoPage extends ModelPage<UnitGroup> {

	private Logger log = LoggerFactory.getLogger(getClass());

	private IMessageManager messageManager;
	private FormToolkit toolkit;

	public UnitGroupInfoPage(UnitGroupEditor editor) {
		super(editor, "UnitGroupInfoPage", Messages.Common_GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Units_FormText
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getModel(), getBinding());
		infoSection.render(body, toolkit);
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	protected void createAdditionalInfo(Composite body) {
		createDropComponent(Messages.Units_DefaultFlowProperty,
				"defaultFlowProperty", ModelType.FLOW_PROPERTY, body);

		Section section = UI.section(body, toolkit,
				Messages.Units_UnitGroupInfoSectionLabel);
		Composite client = UI.sectionClient(section, toolkit);

		UnitViewer unitViewer = new UnitViewer(client);
		getBinding().onList(getModel(), "units", unitViewer);
		unitViewer.bindTo(section);
	}

	// @Override
	// protected void createContents(Composite body, FormToolkit toolkit) {
	// super.createContents(body, toolkit);
	// messageManager = getForm().getMessageManager();
	//
	// int heightHint = getForm().computeSize(SWT.DEFAULT, SWT.DEFAULT).y / 3;
	// createPropertyText(toolkit);
	//
	// // create unit section
	// unitsSection = UIFactory.createSection(body, toolkit,
	// Messages.Units_UnitGroupInfoSectionLabel, true, true);
	// Composite unitsComposite = UIFactory.createSectionComposite(
	// unitsSection, toolkit, UIFactory.createGridLayout(1, true, 0));
	//
	// // create table viewer for displaying and editing units
	// unitsTableViewer = UIFactory.createTableViewer(unitsComposite, null,
	// null, toolkit, UNIT_PROPERTIES);
	// unitsTableViewer.setCellModifier(new UnitCellModifier());
	// unitsTableViewer.setLabelProvider(new UnitLabelProvider(unitGroup,
	// unitsTableViewer.getTable()));
	//
	// bindActions(unitsSection, unitsTableViewer);
	//
	// GridData unitsGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
	// unitsGridData.heightHint = heightHint;
	// unitsTableViewer.getTable().setLayoutData(unitsGridData);
	// unitsTableViewer.getTable().getColumn(0).setWidth(75);
	//
	// // Create the cell editors
	// CellEditor[] unitsEditors = new CellEditor[6];
	// for (int i = 0; i < 5; i++) {
	// unitsEditors[i] = new TextCellEditor(unitsTableViewer.getTable());
	// }
	// unitsEditors[5] = new CheckboxCellEditor(unitsTableViewer.getTable());
	// unitsTableViewer.setCellEditors(unitsEditors);
	// }

	// private void bindActions(Section section, TableViewer viewer) {
	// Action add = new AddUnitAction();
	// Action remove = new RemoveUnitAction();
	// UI.bindActions(section, add, remove);
	// UI.bindActions(viewer, add, remove);
	// }

	// @Override
	// protected void initListeners() {
	// super.initListeners();
	//
	// unitsTableViewer
	// .addSelectionChangedListener(new ISelectionChangedListener() {
	//
	// @Override
	// public void selectionChanged(SelectionChangedEvent event) {
	// IStructuredSelection selection = (IStructuredSelection) event
	// .getSelection();
	// boolean isValid = true;
	// int i = 0;
	// while (isValid && i < selection.toArray().length) {
	// if (getModel().getReferenceUnit().equals(
	// selection.toArray()[i])) {
	// isValid = false;
	// } else {
	// i++;
	// }
	// }
	// }
	//
	// });
	//
	// unitsSection.addExpansionListener(new IExpansionListener() {
	//
	// @Override
	// public void expansionStateChanged(ExpansionEvent e) {
	//
	// }
	//
	// @Override
	// public void expansionStateChanging(ExpansionEvent e) {
	// ((GridData) unitsSection.getLayoutData()).grabExcessVerticalSpace = e
	// .getState();
	// }
	// });
	//
	// }
	//
	// @Override
	// protected void setData() {
	// super.setData();
	// if (unitGroup != null) {
	// if (unitGroup.getUnits() != null) {
	// unitsTableViewer.setInput(unitGroup.getUnits());
	// }
	// }
	// }
	//
	// /**
	// * Adds a unit object to the unit group
	// *
	// * @see Action
	// */
	// private class AddUnitAction extends Action {
	//
	// public AddUnitAction() {
	// setId("UnitGroupInfoPage.AddUnitAction");
	// setText(Messages.Units_AddUnitText);
	// setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
	// setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
	// .getDescriptor());
	//
	// }
	//
	// @Override
	// public void run() {
	// Unit unit = new Unit();
	// String name = Messages.Units_Unit;
	// int i = 1;
	// while (unitGroup.getUnit(name) != null) {
	// name = Messages.Units_Unit + (unitGroup.getUnits().size() + i);
	// }
	// unit.setName(name);
	// unit.setRefId(UUID.randomUUID().toString());
	// unitGroup.getUnits().add(unit);
	// if (unitGroup.getUnits().size() == 1) {
	// unitGroup.setReferenceUnit(unit);
	// }
	// unitsTableViewer.setInput(unitGroup.getUnits());
	// unitsTableViewer.setSelection(new StructuredSelection(unit));
	// }
	// }
	//
	// /**
	// * Removes the selected unit object from the unit group
	// *
	// * @see Action
	// */
	// private class RemoveUnitAction extends DeleteWithQuestionAction {
	//
	// public RemoveUnitAction() {
	// setId("UnitGroupInfoPage.RemoveUnitAction");
	// setText(Messages.Units_RemoveUnitText);
	// setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
	// setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
	// .getDescriptor());
	// }
	//
	// @Override
	// public void delete() {
	// StructuredSelection structuredSelection = (StructuredSelection)
	// unitsTableViewer
	// .getSelection();
	// for (int i = 0; i < structuredSelection.toArray().length; i++) {
	// Unit unit = (Unit) structuredSelection.toArray()[i];
	// // TODO: check that unit is not used
	// messageManager.removeMessage(unit.toString());
	// unitGroup.getUnits().remove(unit);
	// }
	// unitsTableViewer.setInput(unitGroup.getUnits());
	// }
	//
	// }
	//
	// /**
	// * A cell modifier for the unitsTableViewer
	// *
	// * @see ICellModifier
	// */
	// private class UnitCellModifier implements ICellModifier {
	//
	// @Override
	// public boolean canModify(Object element, String property) {
	//
	// boolean canModify = false;
	// if (!property.equals(UNIT_FORMULA)) {
	// // formulas cannot be modified
	// canModify = true;
	// }
	// if (property.equals(UNIT_CONVERSION_FACTOR)
	// && unitGroup.getReferenceUnit().equals(element)) {
	// // reference unit's conversion factor is always 1
	// canModify = false;
	// }
	// return canModify;
	// }
	//
	// @Override
	// public Object getValue(Object element, String property) {
	// Object v = null;
	//
	// if (element instanceof Unit) {
	// Unit unit = (Unit) element;
	// if (property.equals(UNIT_NAME)) {
	// // get name
	// v = unit.getName();
	// } else if (property.equals(UNIT_DESCRIPTION)) {
	// // get description
	// v = unit.getDescription();
	// } else if (property.equals(UNIT_SYNONYMS)) {
	// // get synonyms
	// v = unit.getSynonyms();
	// } else if (property.equals(UNIT_CONVERSION_FACTOR)) {
	// // get conversion factor
	// v = Double.toString(unit.getConversionFactor());
	// } else if (property.equals(UNIT_IS_REFERENCE)) {
	// // get reference
	// v = unit.equals(unitGroup.getReferenceUnit());
	// }
	// }
	//
	// return v != null ? v : "";
	// }
	//
	// @Override
	// public void modify(Object element, String property, Object value) {
	// if (element instanceof Item) {
	// element = ((Item) element).getData();
	// }
	//
	// if (element instanceof Unit) {
	// Unit unit = (Unit) element;
	// if (property.equals(UNIT_NAME)) {
	// // set name
	// if (!unit.getName().equals(value.toString())) {
	// if (unitGroup.getUnit(value.toString()) == null) {
	// unit.setName(value.toString());
	// } else {
	// // unit exists already in the unit group
	// MessageDialog.openError(UI.shell(),
	// Messages.Units_UnitExists, NLS.bind(
	// Messages.Units_UnitExistsMessage,
	// new String[] { value.toString(),
	// unitGroup.getName() }));
	// }
	// }
	// } else if (property.equals(UNIT_DESCRIPTION)) {
	// unit.setDescription(value.toString());
	// } else if (property.equals(UNIT_SYNONYMS)) {
	// unit.setSynonyms(value.toString());
	// } else if (property.equals(UNIT_CONVERSION_FACTOR)) {
	// Double factor = unit.getConversionFactor();
	// try {
	// factor = Double.parseDouble(value.toString());
	// } catch (NumberFormatException e) {
	// }
	// unit.setConversionFactor(factor);
	// } else if (property.equals(UNIT_IS_REFERENCE)) {
	// unitGroup.setReferenceUnit(unit);
	// double factor = unit.getConversionFactor();
	// for (Unit u : unitGroup.getUnits()) {
	// u.setConversionFactor((double) Math.round(1000000000
	// * u.getConversionFactor() / factor) / 1000000000);
	// }
	// }
	// }
	// unitsTableViewer.setInput(unitGroup.getUnits());
	// unitsTableViewer.refresh();
	// }
	// }
}
