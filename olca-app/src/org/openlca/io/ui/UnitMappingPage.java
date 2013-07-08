/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.io.UnitMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page for mapping units to flow properties (e.g. while importing eco spold)
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class UnitMappingPage extends WizardPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final String REFERENCE_UNIT = Phrases.ReferenceUnit;
	private final String CONVERSION_FACTOR = Phrases.ConversionFactor;
	private final String FLOW_PROPERTY = Phrases.FlowProperty;
	private final String UNIT = Phrases.Unit;
	private final String UNIT_GROUP = Phrases.UnitGroup;
	private final String[] PROPERTIES = new String[] { UNIT, FLOW_PROPERTY,
			UNIT_GROUP, REFERENCE_UNIT, CONVERSION_FACTOR };

	private final List<FlowProperty> flowProperties = new ArrayList<>();
	private ComboBoxCellEditor flowPropertyCellEditor;
	private File[] lastFiles = new File[0];

	private TableViewer tableViewer;

	private final Map<String, UnitGroup> unitGroups = new HashMap<>();
	private final UnitMapping unitMapping = new UnitMapping();

	public UnitMappingPage() {
		super("UnitMappingPage");
		setTitle(Messages.UnitMappingPage_Title);
		setDescription(Messages.UnitMappingPage_Description);
		setPageComplete(false);
	}

	private void checkCompletion() {
		boolean isCompleted = true;
		final String[] unitNames = unitMapping.getUnits();
		int i = 0;
		while (isCompleted && i < unitNames.length) {
			if (unitMapping.getFlowProperty(unitNames[i]) == null
					|| unitMapping.getConversionFactor(unitNames[i]) == null) {
				isCompleted = false;
			} else {
				i++;
			}
		}
		setPageComplete(isCompleted);
	}

	private void setUnits(String[] unitNames) {
		final Input[] input = new Input[unitNames.length];
		for (int i = 0; i < unitNames.length; i++) {
			input[i] = new Input(unitNames[i]);
			for (FlowProperty flowProperty : flowProperties) {
				Unit unit = unitGroups.get(flowProperty.getUnitGroup().getId())
						.getUnit(unitNames[i]);
				if (unit != null) {
					input[i].setFlowProperty(flowProperty);
					input[i].setConversionFactor(unit.getConversionFactor());
					if (unitGroups.get(flowProperty.getUnitGroup().getId())
							.getDefaultFlowProperty() != null
							&& flowProperty.getId().equals(
									unitGroups
											.get(flowProperty.getUnitGroup()
													.getId())
											.getDefaultFlowProperty().getId())) {
						break;
					}
				}
			}

			UnitGroup unitGroup = null;
			if (input[i].getFlowProperty() != null) {
				unitGroup = unitGroups.get(input[i].getFlowProperty()
						.getUnitGroup().getId());
			}
			unitMapping.put(unitNames[i], input[i].getFlowProperty(),
					unitGroup, input[i].getConversionFactor());
		}
		tableViewer.setInput(input);
		checkCompletion();
	}

	/**
	 * Updates the unit mapping if new files were selected
	 */
	private void update() {
		try {
			final File[] files = getFiles();
			boolean check = false;
			if (files.length != lastFiles.length) {
				check = true;
			} else {
				int i = 0;
				while (!check && i < files.length) {
					if (!files[i].getAbsolutePath().equals(
							lastFiles[i].getAbsolutePath())) {
						check = true;
					} else {
						i++;
					}
				}
			}
			if (check) {
				lastFiles = files;
				final List<String> unitNames = new ArrayList<>();
				getWizard().getContainer().run(true, false,
						new IRunnableWithProgress() {

							@Override
							public void run(final IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								monitor.beginTask(
										Messages.UnitMappingPage_CheckingUnits,
										IProgressMonitor.UNKNOWN);
								final String[] names = checkFiles(getFiles());
								for (final String s : names) {
									unitNames.add(s);
								}
								monitor.done();
							}

						});
				setUnits(unitNames.toArray(new String[unitNames.size()]));
			}
		} catch (final Exception e) {
			log.error("Update failed", e);
		}
	}

	/**
	 * Gets a list of files and searches for unit names. Returns the found unit
	 * names as an array
	 * 
	 * @param files
	 *            - the files to be checked
	 * @return String[] - the names of the units, found in the files
	 */
	protected abstract String[] checkFiles(File[] files);

	/**
	 * Get the files selected previously. (Normally the wizard should provide
	 * the page with the files)
	 * 
	 * @return File[] - a list of files
	 */
	protected abstract File[] getFiles();

	@Override
	public void createControl(final Composite parent) {
		final Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout(1, true));

		tableViewer = new TableViewer(body, SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);

		for (final String p : PROPERTIES) {
			final TableColumn c = new TableColumn(tableViewer.getTable(),
					SWT.NULL);
			c.setText(p);
		}
		for (final TableColumn c : tableViewer.getTable().getColumns()) {
			c.pack();
		}

		tableViewer.setContentProvider(new ContentProvider());
		tableViewer.setLabelProvider(new LabelProvider());
		tableViewer.setColumnProperties(PROPERTIES);
		tableViewer.setCellModifier(new CellModifier());

		final String[] flowPropertyNames = new String[flowProperties.size()];
		for (int i = 0; i < flowProperties.size(); i++) {
			flowPropertyNames[i] = flowProperties.get(i).getName();
		}
		flowPropertyCellEditor = new ComboBoxCellEditor(tableViewer.getTable(),
				flowPropertyNames, SWT.READ_ONLY);

		final CellEditor[] editors = new CellEditor[] {
				new TextCellEditor(tableViewer.getTable()),
				flowPropertyCellEditor,
				new TextCellEditor(tableViewer.getTable()),
				new TextCellEditor(tableViewer.getTable()),
				new TextCellEditor(tableViewer.getTable()) };
		tableViewer.setCellEditors(editors);

		tableViewer.setInput(new Input[0]);

		setControl(body);
	}

	/**
	 * Getter of the unitMapping-field
	 * 
	 * @return the unit mapping
	 */
	public UnitMapping getUnitMapping() {
		return unitMapping;
	}

	@Override
	public void setVisible(final boolean visible) {
		if (!visible) {
			setPageComplete(false);
		} else {
			final IDatabase database = Database.get();
			try {
				List<FlowProperty> flowProperties = database.createDao(
						FlowProperty.class).getAll();
				List<UnitGroup> unitGroups = database
						.createDao(UnitGroup.class).getAll();
				this.flowProperties.clear();
				for (final FlowProperty flowProperty : flowProperties) {
					this.flowProperties.add(flowProperty);
				}
				Collections.sort(this.flowProperties,
						new Comparator<FlowProperty>() {

							@Override
							public int compare(final FlowProperty o1,
									final FlowProperty o2) {
								return o1.getName().toLowerCase()
										.compareTo(o2.getName().toLowerCase());
							}

						});
				for (final UnitGroup unitGroup : unitGroups) {
					this.unitGroups.put(unitGroup.getId(), unitGroup);
				}
				update();
				checkCompletion();
			} catch (final Exception e) {

			}
		}
		super.setVisible(visible);
	}

	/**
	 * Cell modifier for modifing the unit mapping
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class CellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			final Input input = (Input) element;
			return !property.equals(UNIT)
					&& !property.equals(UNIT_GROUP)
					&& !property.equals(REFERENCE_UNIT)
					&& !((input.getFlowProperty() == null || unitGroups.get(
							input.getFlowProperty().getUnitGroup().getId())
							.getUnit(input.getUnitName()) != null) && property
							.equals(CONVERSION_FACTOR));
		}

		@Override
		public Object getValue(final Object element, final String property) {
			Object value = null;
			final Input input = (Input) element;
			if (property.equals(FLOW_PROPERTY)) {
				final List<String> flowPropertyNames = new ArrayList<>();
				for (final FlowProperty flowProperty : flowProperties) {
					if (unitGroups.get(flowProperty.getUnitGroup().getId())
							.getUnit(input.getUnitName()) != null) {
						flowPropertyNames.add(flowProperty.getName());
					}
				}
				if (flowPropertyNames.size() == 0) {
					for (final FlowProperty flowProperty : flowProperties) {
						flowPropertyNames.add(flowProperty.getName());
					}
				}
				flowPropertyCellEditor.setItems(flowPropertyNames
						.toArray(new String[flowPropertyNames.size()]));
				if (input.getFlowProperty() == null) {
					value = new Integer(-1);
				} else {
					value = new Integer(flowPropertyNames.indexOf(input
							.getFlowProperty().getName()));
				}
			} else if (property.equals(CONVERSION_FACTOR)) {
				value = Double.toString(input.getConversionFactor());
			}
			return value;
		}

		@Override
		public void modify(final Object element, final String property,
				final Object value) {
			final TableItem item = (TableItem) element;
			final Input input = (Input) item.getData();
			if (property.equals(FLOW_PROPERTY)) {
				final List<String> flowPropertyNames = new ArrayList<>();
				for (final FlowProperty flowProperty : flowProperties) {
					if (unitGroups.get(flowProperty.getUnitGroup().getId())
							.getUnit(input.getUnitName()) != null) {
						flowPropertyNames.add(flowProperty.getName());
					}
				}
				if (flowPropertyNames.size() == 0) {
					for (final FlowProperty flowProperty : flowProperties) {
						flowPropertyNames.add(flowProperty.getName());
					}
				}
				if (value != null) {
					final int val = Integer.parseInt(value.toString());
					if (val < flowPropertyNames.size() && val >= 0) {
						for (final FlowProperty flowProperty : flowProperties) {
							if (flowProperty.getName().equals(
									flowPropertyNames.get(val))) {
								input.setFlowProperty(flowProperty);
								break;
							}
						}
						final Unit unit = unitGroups.get(
								input.getFlowProperty().getUnitGroup().getId())
								.getUnit(input.getUnitName());
						if (unit != null) {
							input.setConversionFactor(unit
									.getConversionFactor());
						} else {
							input.setConversionFactor(1);
						}
					}
				}
			} else if (property.equals(CONVERSION_FACTOR)) {
				try {
					input.setConversionFactor(Double.parseDouble(value
							.toString()));
				} catch (final Exception e) {
					// do nothing
				}
			}
			checkCompletion();
			tableViewer.refresh();
		}
	}

	/**
	 * Content provider for the unit mapping table
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class ContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
			// nothing to dispose
		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return (Input[]) inputElement;
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
			// nothing to do
		}

	}

	/**
	 * Input element of the unit mapping table
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class Input {

		/**
		 * The conversion factor of the unit
		 */
		private double conversionFactor = 1;

		/**
		 * The flow property of the unit
		 */
		private FlowProperty flowProperty;

		/**
		 * The units name
		 */
		private final String unitName;

		/**
		 * Creates a new instance
		 * 
		 * @param unitName
		 *            The units name
		 */
		protected Input(final String unitName) {
			this.unitName = unitName;
		}

		/**
		 * Getter of the conversion factor
		 * 
		 * @return The conversion factor of the unit
		 */
		public double getConversionFactor() {
			return conversionFactor;
		}

		/**
		 * Getter of the flow property
		 * 
		 * @return The flow property of the unit
		 */
		public FlowProperty getFlowProperty() {
			return flowProperty;
		}

		/**
		 * Getter of the unit name
		 * 
		 * @return The units name
		 */
		public String getUnitName() {
			return unitName;
		}

		/**
		 * Setter of the conversion factor
		 * 
		 * @param conversionFactor
		 *            The new conversion factor
		 */
		public void setConversionFactor(final double conversionFactor) {
			this.conversionFactor = conversionFactor;
			unitMapping.set(unitName, conversionFactor);
		}

		/**
		 * Setter of the flow property
		 * 
		 * @param flowProperty
		 *            The new flow property
		 */
		public void setFlowProperty(final FlowProperty flowProperty) {
			this.flowProperty = flowProperty;
			unitMapping.set(unitName, flowProperty,
					unitGroups.get(flowProperty.getUnitGroup().getId()));
		}

	}

	/**
	 * Label provider for the unit mapping table
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class LabelProvider extends
			org.eclipse.jface.viewers.BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(final Object element, final int columnIndex) {
			String text = "";
			final Input input = (Input) element;
			if (columnIndex == 0) {
				text = input.getUnitName();
			} else if (columnIndex == 1) {
				if (input.getFlowProperty() != null) {
					text = input.getFlowProperty().getName();
				}
			} else if (columnIndex == 2) {
				if (input.getFlowProperty() != null) {
					final UnitGroup unitGroup = unitGroups.get(input
							.getFlowProperty().getUnitGroup().getId());
					text = unitGroup.getName();
				}
			} else if (columnIndex == 3) {
				if (input.getFlowProperty() != null) {
					final UnitGroup unitGroup = unitGroups.get(input
							.getFlowProperty().getUnitGroup().getId());
					text = unitGroup.getReferenceUnit().getName();
				}
			} else if (columnIndex == 4) {
				if (input.getFlowProperty() != null) {
					text = Double.toString(input.getConversionFactor());
				}
			}
			return text;
		}
	}

}
