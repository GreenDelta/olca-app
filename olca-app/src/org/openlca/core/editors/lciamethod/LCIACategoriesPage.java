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

import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;

/**
 * Form page to display and edit the LCIA categories of an LCIA method
 * 
 * @author Sebastian Greve
 * 
 */
public class LCIACategoriesPage extends ModelEditorPage {

	/**
	 * String for the 'description' property of the lCIACategoriesTableViewer
	 */
	private String LCIACATEGORY_DESCRIPTION = Messages.Common_Description;

	/**
	 * String for the 'name' property of the lCIACategoriesTableViewer
	 */
	private String LCIACATEGORY_NAME = Messages.Common_Name;

	/**
	 * String for the 'reference unit' property of the lCIACategoriesTableViewer
	 */
	private String LCIACATEGORY_REFERENCEUNIT = Messages.Common_ReferenceUnit;

	/**
	 * Array of property strings for the lCIACategoriesTableViewer
	 */
	private String[] LCIACATEGORY_PROPERTIES = new String[] {
			LCIACATEGORY_NAME, LCIACATEGORY_DESCRIPTION,
			LCIACATEGORY_REFERENCEUNIT };

	/**
	 * A {@link TableViewer} widget for the lCIA categories-field of this lCIA
	 * method
	 */
	private TableViewer categoryViewer;

	/**
	 * the LCIA method object edited by this editor
	 */
	private LCIAMethod lciaMethod = null;

	/**
	 * Creates a new instance.
	 * 
	 * @param editor
	 *            the editor of this page
	 */
	public LCIACategoriesPage(ModelEditor editor) {
		super(editor, "LCIACategoriesPage", Messages.Common_ImpactCategories);
		this.lciaMethod = (LCIAMethod) editor.getModelComponent();
	}

	@Override
	protected void createContents(Composite body, FormToolkit toolkit) {
		int heightHint = getManagedForm().getForm().computeSize(SWT.DEFAULT,
				SWT.DEFAULT).y / 3;

		Section section = UI.section(body, toolkit,
				Messages.Common_ImpactCategories);
		UI.gridData(section, true, true);

		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);

		categoryViewer = UIFactory.createTableViewer(composite, null, null,
				toolkit, LCIACATEGORY_PROPERTIES, getDatabase());
		categoryViewer.setCellModifier(new LCIACategoryCellModifier());
		categoryViewer.setLabelProvider(new ImpactCategoryLabel());
		UI.gridData(categoryViewer.getTable(), true, true).heightHint = heightHint; // TODO?
		categoryViewer.getTable().getColumn(0).setWidth(150);

		bindActions(categoryViewer, section);

		// Create the cell editors
		CellEditor[] lciaCategoriesEditors = new CellEditor[3];
		for (int i = 0; i < 3; i++) {
			lciaCategoriesEditors[i] = new TextCellEditor(
					categoryViewer.getTable());
		}
		categoryViewer.setCellEditors(lciaCategoriesEditors);
	}

	private void bindActions(TableViewer viewer, Section section) {
		Action add = new AddLCIACategoryAction();
		Action remove = new RemoveLCIACategoryAction();
		UI.bindActions(viewer, add, remove);
		UI.bindActions(section, add, remove);
	}

	@Override
	protected String getFormTitle() {
		String title = Messages.Common_LCIAMethodTitle
				+ ": "
				+ (lciaMethod != null ? lciaMethod.getName() != null ? lciaMethod
						.getName() : ""
						: "");
		return title;
	}

	@Override
	protected void setData() {
		if (lciaMethod != null) {
			categoryViewer.setInput(lciaMethod.getLCIACategories());
		}
	}

	/**
	 * Adds a lCIA category object to this lCIA method
	 * 
	 * @see Action
	 */
	private class AddLCIACategoryAction extends Action {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.core.editors.lciamethod.LCIACategoriesPage.AddLCIACategoryAction";

		/**
		 * The text of the action
		 */
		public String TEXT = Messages.Methods_AddLCIACategoryText;

		/**
		 * Creates a new AddLCIACategoryAction and sets the ID, TEXT and
		 * ImageDescriptor
		 */
		public AddLCIACategoryAction() {
			setId(ID);
			setText(TEXT);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			// create LCIA category
			LCIACategory lciaCategory = new LCIACategory();
			lciaCategory.setId(UUID.randomUUID().toString());
			lciaCategory.setName(Messages.Common_ImpactCategory
					+ lciaMethod.getLCIACategories().length);
			lciaMethod.add(lciaCategory);

			// refresh table viewer
			categoryViewer.setInput(lciaMethod.getLCIACategories());
			categoryViewer.setSelection(new StructuredSelection(lciaCategory));
		}
	}

	/**
	 * A cell modifier for the lciaCategoryTableViewer
	 * 
	 * @see ICellModifier
	 */
	private class LCIACategoryCellModifier implements ICellModifier {

		@Override
		public boolean canModify(Object element, String property) {

			return true;
		}

		@Override
		public Object getValue(Object element, String property) {
			Object v = null;
			if (element instanceof LCIACategory) {
				LCIACategory lciaCategory = (LCIACategory) element;
				if (property.equals(LCIACATEGORY_NAME)) {
					// get name
					v = lciaCategory.getName();
				} else if (property.equals(LCIACATEGORY_DESCRIPTION)) {
					// get description
					v = lciaCategory.getDescription();
				} else if (property.equals(LCIACATEGORY_REFERENCEUNIT)) {
					// get reference unit
					v = lciaCategory.getReferenceUnit();
				}
			}

			return v != null ? v : "";
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}

			if (element instanceof LCIACategory) {
				LCIACategory lciaCategory = (LCIACategory) element;

				if (property.equals(LCIACATEGORY_NAME)) {
					// set name
					lciaCategory.setName(value.toString());
				} else if (property.equals(LCIACATEGORY_DESCRIPTION)) {
					// set description
					lciaCategory.setDescription(value.toString());
				} else if (property.equals(LCIACATEGORY_REFERENCEUNIT)) {
					// set reference unit
					lciaCategory.setReferenceUnit(value.toString());
				}
			}

			// update table viewer
			categoryViewer.setInput(lciaMethod.getLCIACategories());
			categoryViewer.refresh();

		}
	}

	/**
	 * Removes the selected lCIA category object from this lCIA method
	 * 
	 * @see Action
	 */
	private class RemoveLCIACategoryAction extends DeleteWithQuestionAction {

		public RemoveLCIACategoryAction() {
			setId("RemoveLCIACategoryAction");
			setText(Messages.Methods_RemoveLCIACategoryText);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		protected void delete() {
			StructuredSelection structuredSelection = (StructuredSelection) categoryViewer
					.getSelection();
			for (int i = 0; i < structuredSelection.toArray().length; i++) {
				LCIACategory lciaCategory = (LCIACategory) structuredSelection
						.toArray()[i];
				lciaMethod.remove(lciaCategory);
			}
			categoryViewer.setInput(lciaMethod.getLCIACategories());
		}

	}

}
