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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UIFactory;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NormalizationWeightingFactor;
import org.openlca.core.model.NormalizationWeightingSet;

/**
 * {@link ModelEditorPage} for displaying and editing the normalization and
 * weighting sets of an LCIA method
 * 
 * @author Sebastian Greve
 * 
 */
public class LCIANormalizationWeightingPage extends ModelEditorPage {

	private NormalizationWeightingSet actualSet;
	private TableViewer factorsViewer;

	private final String LCIA_CATEGORY = Messages.Common_ImpactCategory;
	private final ImpactMethod lciaMethod;

	/**
	 * Name property of the factors viewer
	 */
	private final String NAME = Messages.Common_NormalizationWeightingSet;

	/**
	 * Normalization factor property of the factors viewer
	 */
	private final String NORMALIZATION_FACTOR = Messages.Methods_NormalizationFactor;

	/**
	 * Unit property of the factors viewer
	 */
	private final String UNIT = Messages.Common_Unit;

	/**
	 * Weighting factor property of the factors viewer
	 */
	private final String WEIGHTING_FACTOR = Messages.Methods_WeightingFactor;

	/**
	 * The properties of the normalization and weighting set table
	 */
	private final String[] NW_PROPERTIES = new String[] { NAME, UNIT };

	/**
	 * The properties of the normalization and weighting set factor table
	 */
	private final String[] PROPERTIES = new String[] { LCIA_CATEGORY,
			NORMALIZATION_FACTOR, WEIGHTING_FACTOR };

	/**
	 * Action for removing a set
	 */
	private RemoveSetAction removeSetAction;

	/**
	 * Table viewer for displaying and editing the normalization and weighting
	 * sets of the LCIA method
	 */
	private TableViewer setViewer;

	/**
	 * Creates a new instance
	 * 
	 * @param editor
	 *            The parent model editor containing the page
	 */
	public LCIANormalizationWeightingPage(final ModelEditor editor) {
		super(editor, "LCIANormalizationWeightingPage",
				Messages.Methods_NormalizationWeightingPageLabel);
		lciaMethod = (ImpactMethod) editor.getModelComponent();
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		// create section
		final Section section = UIFactory.createSection(body, toolkit,
				Messages.Methods_NormalizationWeightingSets, true, true);
		// create tool bar
		final ToolBarManager toolBar = new ToolBarManager();
		final AddSetAction addSetAction = new AddSetAction();
		toolBar.add(addSetAction);
		removeSetAction = new RemoveSetAction();
		toolBar.add(removeSetAction);
		section.setTextClient(toolBar.createControl(section));
		final MenuManager menu = new MenuManager();
		menu.add(addSetAction);
		menu.add(removeSetAction);

		// create composite
		final Composite composite = UIFactory.createSectionComposite(section,
				toolkit, UIFactory.createGridLayout(1));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// create sash form
		final SashForm sashForm = new SashForm(composite, SWT.NONE);
		final GridData sashGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashGD.widthHint = 400;
		sashForm.setLayoutData(sashGD);
		sashForm.setLayout(new GridLayout(2, false));

		// create table viewer for selecting and editing a normalization and
		// weighting set
		setViewer = new TableViewer(sashForm, SWT.FULL_SELECTION | SWT.BORDER
				| SWT.SINGLE);
		final GridData sgd = new GridData(SWT.FILL, SWT.FILL, false, true);
		setViewer.getTable().setLayoutData(sgd);
		setViewer.getTable().setHeaderVisible(true);
		setViewer.getTable().setLinesVisible(true);
		setViewer.setContentProvider(new ArrayContentProvider());
		setViewer.setLabelProvider(new NwSetLabelProvider());
		setViewer.setCellModifier(new SetCellModifier());
		final CellEditor[] cellEditors = new CellEditor[2];
		cellEditors[0] = new TextCellEditor(setViewer.getTable());
		cellEditors[1] = new TextCellEditor(setViewer.getTable());
		setViewer.setCellEditors(cellEditors);

		// for each table column property
		for (final String property : NW_PROPERTIES) {
			// create a new table column
			final TableColumn c = new TableColumn(setViewer.getTable(),
					SWT.NONE);
			c.setText(property);
			c.pack();
		}

		setViewer.setColumnProperties(NW_PROPERTIES);
		setViewer.getTable().setMenu(
				menu.createContextMenu(setViewer.getTable()));

		// create a table viewer for displaying and editing the normalization
		// and weigthing factors of a selected set
		factorsViewer = new TableViewer(sashForm, SWT.FULL_SELECTION
				| SWT.BORDER | SWT.SINGLE);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 300;
		factorsViewer.getTable().setLayoutData(gd);
		factorsViewer.setContentProvider(new ArrayContentProvider());
		factorsViewer.setLabelProvider(new NwSetFactorLabel(lciaMethod));
		factorsViewer.setCellModifier(new FactorCellModifier());
		final CellEditor[] cellEditors2 = new CellEditor[3];
		cellEditors2[1] = new TextCellEditor(factorsViewer.getTable());
		cellEditors2[2] = new TextCellEditor(factorsViewer.getTable());
		factorsViewer.setCellEditors(cellEditors2);
		factorsViewer.setColumnProperties(PROPERTIES);
		factorsViewer.getTable().setLinesVisible(true);
		factorsViewer.getTable().setHeaderVisible(true);

		// for each table column property
		for (final String p : PROPERTIES) {
			// create a new table column
			final TableColumn c1 = new TableColumn(factorsViewer.getTable(),
					SWT.NONE);
			c1.setText(p);
		}

		// for each table column
		for (final TableColumn c1 : factorsViewer.getTable().getColumns()) {
			// pack column
			c1.pack();
		}
		factorsViewer.getTable().getColumn(0).setWidth(150);
		sashForm.setWeights(new int[] { 25, 75 });
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
		setViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				actualSet = (NormalizationWeightingSet) selection
						.getFirstElement();
				if (actualSet != null) {
					factorsViewer.setInput(actualSet
							.getNormalizationWeightingFactors());
					removeSetAction.setEnabled(true);
				} else {
					removeSetAction.setEnabled(false);
				}
			}
		});
	}

	@Override
	protected void setData() {
		setViewer.setInput(lciaMethod.getNormalizationWeightingSets());
		if (lciaMethod.getNormalizationWeightingSets().size() > 0) {
			actualSet = lciaMethod.getNormalizationWeightingSets().get(0);
			setViewer.setSelection(new StructuredSelection(actualSet));
			factorsViewer
					.setInput(actualSet.getNormalizationWeightingFactors());
			removeSetAction.setEnabled(true);
		} else {
			removeSetAction.setEnabled(false);
		}
	}

	@Override
	public void setActive(final boolean active) {
		super.setActive(active);
		if (active && actualSet != null) {
			setViewer.setSelection(new StructuredSelection(actualSet));
		}
	}

	/**
	 * Action for adding a new normalization and weighting set to the LCIA
	 * method
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class AddSetAction extends Action {

		/**
		 * Creates a new instance
		 */
		public AddSetAction() {
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public String getText() {
			return Messages.Methods_AddNormalizationWeightingSet;
		}

		@Override
		public void run() {
			NormalizationWeightingSet set = new NormalizationWeightingSet();
			set.setReferenceSystem(Messages.Common_NormalizationWeightingSet
					+ (lciaMethod.getNormalizationWeightingSets().size() + 1));
			set.setUnit("points");
			lciaMethod.getNormalizationWeightingSets().add(set);
			actualSet = set;

			// refresh the viewer
			setViewer.setInput(lciaMethod.getNormalizationWeightingSets());
			setViewer.setSelection(new StructuredSelection(actualSet));
		}
	}

	/**
	 * Cell modifier for the factor table
	 */
	private class FactorCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			return !property.equals(LCIA_CATEGORY);
		}

		@Override
		public Object getValue(final Object element, final String property) {
			final NormalizationWeightingFactor factor = (NormalizationWeightingFactor) element;
			Object o = null;
			if (property.equals(NORMALIZATION_FACTOR)) {
				// get normalization factor
				if (factor.getNormalizationFactor() != null) {
					o = Double.toString(factor.getNormalizationFactor());
				} else {
					o = "-";
				}
			} else if (property.equals(WEIGHTING_FACTOR)) {
				// get weighting factor
				if (factor.getWeightingFactor() != null) {
					o = Double.toString(factor.getWeightingFactor());
				} else {
					o = "-";
				}
			}
			return o;
		}

		@Override
		public void modify(final Object element, final String property,
				final Object value) {
			final TableItem item = (TableItem) element;
			final NormalizationWeightingFactor factor = (NormalizationWeightingFactor) item
					.getData();
			if (property.equals(NORMALIZATION_FACTOR)) {
				// set normalization factor
				if (value.toString().equals("")) {
					factor.setNormalizationFactor(null);
				}
				try {
					factor.setNormalizationFactor(Double.parseDouble(value
							.toString()));
				} catch (final NumberFormatException e) {
				}
			} else if (property.equals(WEIGHTING_FACTOR)) {
				// set weighting factor
				if (value.toString().equals("")) {
					factor.setWeightingFactor(null);
				}
				try {
					factor.setWeightingFactor(Double.parseDouble(value
							.toString()));
				} catch (final NumberFormatException e) {
				}
			}
			factorsViewer.refresh();
		}
	}

	/**
	 * Action for removing a normalization and weighting set from the LCIA
	 * method
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class RemoveSetAction extends Action {

		/**
		 * Creates a new instance
		 */
		public RemoveSetAction() {
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public String getText() {
			return Messages.Methods_RemoveNormalizationWeightingSet;
		}

		@Override
		public void run() {
			if (actualSet != null) {
				lciaMethod.getNormalizationWeightingSets().remove(actualSet);
				setViewer.setInput(lciaMethod.getNormalizationWeightingSets());
				setViewer.refresh();
				if (lciaMethod.getNormalizationWeightingSets().size() > 0) {
					actualSet = lciaMethod.getNormalizationWeightingSets().get(
							0);
					setViewer.setSelection(new StructuredSelection(actualSet));
				} else {
					setEnabled(false);
					actualSet = null;
					factorsViewer.setInput(new NormalizationWeightingFactor[0]);
					factorsViewer.refresh();
				}
			}
		}

	}

	/**
	 * Cell modifier for the normalization and weighting set table
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class SetCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			return true;
		}

		@Override
		public Object getValue(final Object element, final String property) {
			String text = null;
			final NormalizationWeightingSet set = (NormalizationWeightingSet) element;
			if (property.equals(NAME)) {
				// get reference system
				text = set.getReferenceSystem();
			} else if (property.equals(UNIT)) {
				// get unit
				text = set.getUnit();
			}
			return text;
		}

		@Override
		public void modify(final Object element, final String property,
				final Object value) {
			final TableItem item = (TableItem) element;
			final NormalizationWeightingSet set = (NormalizationWeightingSet) item
					.getData();
			if (property.equals(NAME)) {
				// set reference system
				set.setReferenceSystem(value.toString());
			} else if (property.equals(UNIT)) {
				// set unit
				set.setUnit(value.toString());
			}
			// refresh viewer
			setViewer.refresh();
		}

	}

}
