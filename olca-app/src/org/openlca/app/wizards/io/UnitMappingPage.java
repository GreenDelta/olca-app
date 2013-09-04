package org.openlca.app.wizards.io;

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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.Numbers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.io.UnitMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page for mapping units to flow properties (e.g. while importing eco spold)
 */
public abstract class UnitMappingPage extends WizardPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final String CONVERSION_FACTOR = Messages.ConversionFactor;
	private final String FLOW_PROPERTY = Messages.FlowProperty;
	private final List<FlowProperty> flowProperties = new ArrayList<>();
	private ComboBoxCellEditor flowPropertyCellEditor;

	/**
	 * To check if something has changed (used by getControl())
	 */
	private File[] lastFiles = new File[0];

	private final String REFERENCE_UNIT = Messages.ReferenceUnit;
	private final String UNIT = Messages.Unit;
	private final String UNIT_GROUP = Messages.UnitGroup;
	private final String[] PROPERTIES = new String[] { UNIT, FLOW_PROPERTY,
			UNIT_GROUP, REFERENCE_UNIT, CONVERSION_FACTOR };

	private TableViewer tableViewer;
	private final Map<Long, UnitGroup> unitGroups = new HashMap<>();
	private final UnitMapping unitMapping = new UnitMapping();

	public UnitMappingPage() {
		super("UnitMappingPage");
		setTitle(Messages.UnitMappingPage_Title);
		setDescription(Messages.UnitMappingPage_Description);
		setPageComplete(false);
	}

	private void checkCompletion() {
		boolean complete = true;
		String[] units = unitMapping.getUnits();
		for (int i = 0; i < units.length; i++) {
			FlowProperty prop = unitMapping.getFlowProperty(units[i]);
			Double factor = unitMapping.getConversionFactor(units[i]);
			if (prop == null || factor == null) {
				complete = false;
				break;
			}
		}
		setPageComplete(complete);
	}

	private void mapAndSetUnitInput(List<String> units) {
		List<TableRow> inputs = new ArrayList<>();
		for (String unitName : units) {
			TableRow input = new TableRow(unitName);
			inputs.add(input);
			for (FlowProperty flowProperty : flowProperties) {
				Unit unit = unitGroups.get(flowProperty.getUnitGroup().getId())
						.getUnit(unitName);
				if (unit != null) {
					input.setFlowProperty(flowProperty);
					input.setConversionFactor(unit.getConversionFactor());
					if (unitGroups.get(flowProperty.getUnitGroup().getId())
							.getDefaultFlowProperty() != null
							&& flowProperty.getId() == unitGroups
									.get(flowProperty.getUnitGroup().getId())
									.getDefaultFlowProperty().getId()) {
						break;
					}
				}
			}

			UnitGroup unitGroup = null;
			if (input.getFlowProperty() != null) {
				unitGroup = unitGroups.get(input.getFlowProperty()
						.getUnitGroup().getId());
			}
			unitMapping.put(unitName, input.getFlowProperty(), unitGroup,
					input.getConversionFactor());
		}
		tableViewer.setInput(inputs);
		checkCompletion();
	}

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
				mapAndSetUnitInput(unitNames);
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

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
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

		tableViewer.setInput(new TableRow[0]);

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
	 */
	private class CellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			final TableRow input = (TableRow) element;
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
			final TableRow input = (TableRow) element;
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
			final TableRow input = (TableRow) item.getData();
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

	private class TableRow {

		private double conversionFactor = 1;
		private FlowProperty flowProperty;
		private String unitName;

		protected TableRow(String unitName) {
			this.unitName = unitName;
		}

		public double getConversionFactor() {
			return conversionFactor;
		}

		public FlowProperty getFlowProperty() {
			return flowProperty;
		}

		public String getUnitName() {
			return unitName;
		}

		public void setConversionFactor(final double conversionFactor) {
			this.conversionFactor = conversionFactor;
			unitMapping.set(unitName, conversionFactor);
		}

		public void setFlowProperty(final FlowProperty flowProperty) {
			this.flowProperty = flowProperty;
			unitMapping.set(unitName, flowProperty,
					unitGroups.get(flowProperty.getUnitGroup().getId()));
		}

	}

	private class LabelProvider extends
			org.eclipse.jface.viewers.BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int column) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int column) {
			if (!(element instanceof TableRow))
				return null;
			TableRow row = (TableRow) element;
			if (column == 0)
				return row.getUnitName(); // no flow-prop check
			if (row.getFlowProperty() == null)
				return null;
			FlowProperty prop = row.getFlowProperty();
			UnitGroup unitGroup = prop.getUnitGroup();
			switch (column) {
			case 1:
				return prop.getName();
			case 2:
				return unitGroup.getName();
			case 3:
				if (unitGroup.getReferenceUnit() == null)
					return null;
				return unitGroup.getReferenceUnit().getName();
			case 4:
				return Numbers.format(row.getConversionFactor());
			default:
				return null;
			}
		}
	}

}
