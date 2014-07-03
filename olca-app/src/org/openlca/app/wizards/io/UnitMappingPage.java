package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.Tables;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.io.UnitMapping;
import org.openlca.io.UnitMappingEntry;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page for mapping unit names to flow properties (e.g. for the import of
 * EcoSpold data sets)
 */
public abstract class UnitMappingPage extends WizardPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private IDatabase database = Database.get();

	private final String CONVERSION_FACTOR = Messages.ConversionFactor;
	private final String FLOW_PROPERTY = Messages.FlowProperty;
	private final String REFERENCE_UNIT = Messages.ReferenceUnit;
	private final String UNIT = Messages.Unit;
	private final String FORMULA = Messages.Formula;

	private final String[] PROPERTIES = new String[] { UNIT, FLOW_PROPERTY,
			REFERENCE_UNIT, CONVERSION_FACTOR, FORMULA };

	private List<FlowProperty> flowProperties = new ArrayList<>();
	private ComboBoxCellEditor flowPropertyCellEditor;

	/**
	 * To check if something has changed (used by getControl())
	 */
	private File[] lastFiles = new File[0];

	private TableViewer tableViewer;
	private List<UnitMappingEntry> mappings = new ArrayList<>();

	public UnitMappingPage() {
		super("UnitMappingPage");
		setTitle(Messages.AssignUnits);
		setDescription(Messages.UnitMappingPage_Description);
		setPageComplete(false);
	}

	private void checkCompletion() {
		boolean complete = true;
		for (UnitMappingEntry entry : mappings) {
			FlowProperty prop = entry.getFlowProperty();
			Double factor = entry.getFactor();
			if (prop == null || factor == null) {
				complete = false;
				break;
			}
		}
		setPageComplete(complete);
	}

	private void update() {
		final File[] files = getFiles();
		if (!filesChanged(files))
			return;
		try {
			lastFiles = files;
			final List<String> unitNames = new ArrayList<>();
			getWizard().getContainer().run(true, false,
					new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							monitor.beginTask(
									Messages.SearchingForUnits,
									IProgressMonitor.UNKNOWN);
							String[] names = checkFiles(files);
							for (String s : names)
								unitNames.add(s);
							monitor.done();
						}
					});
			mapAndSetUnitInput(unitNames);
		} catch (final Exception e) {
			log.error("Update failed", e);
		}
	}

	private boolean filesChanged(File[] newFiles) {
		if (newFiles.length != lastFiles.length)
			return true;
		else {
			for (int i = 0; i < newFiles.length; i++) {
				if (!Objects.equals(newFiles[i], lastFiles[i]))
					return true;
			}
			return false;
		}
	}

	private void mapAndSetUnitInput(List<String> units) {
		UnitMapping defaultMapping = UnitMapping.createDefault(database);
		mappings.clear();
		for (String unitName : units) {
			UnitMappingEntry entry = defaultMapping.getEntry(unitName);
			if (entry == null) {
				entry = new UnitMappingEntry();
				entry.setUnitName(unitName);
			}
			mappings.add(entry);
		}
		tableViewer.setInput(mappings);
		checkCompletion();
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
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout(1, true));
		tableViewer = Tables.createViewer(body, PROPERTIES);
		tableViewer.setLabelProvider(new LabelProvider());
		tableViewer.setCellModifier(new CellModifier());
		Tables.bindColumnWidths(tableViewer, 0.1, 0.2, 0.2, 0.2, 0.3);
		createCellEditors();
		setControl(body);
	}

	private void createCellEditors() {
		String[] flowPropertyNames = new String[flowProperties.size()];
		for (int i = 0; i < flowProperties.size(); i++)
			flowPropertyNames[i] = flowProperties.get(i).getName();
		flowPropertyCellEditor = new ComboBoxCellEditor(tableViewer.getTable(),
				flowPropertyNames, SWT.READ_ONLY);
		final CellEditor[] editors = new CellEditor[] {
				new TextCellEditor(tableViewer.getTable()),
				flowPropertyCellEditor,
				new TextCellEditor(tableViewer.getTable()),
				new TextCellEditor(tableViewer.getTable()),
				new TextCellEditor(tableViewer.getTable()) };
		tableViewer.setCellEditors(editors);
	}

	public List<UnitMappingEntry> getUnitMappings() {
		return mappings;
	}

	@Override
	public void setVisible(final boolean visible) {
		if (!visible)
			setPageComplete(false);
		else {
			try {
				flowProperties = database.createDao(FlowProperty.class)
						.getAll();
				// flow properties are sorted for the combo cell editor
				Collections.sort(flowProperties,
						new Comparator<FlowProperty>() {
							@Override
							public int compare(FlowProperty o1, FlowProperty o2) {
								return Strings.compare(o1.getName(),
										o2.getName());
							}
						});
				update();
				checkCompletion();
			} catch (Exception e) {
				log.error("failed to build unit mappings", e);
			}
		}
		super.setVisible(visible);
	}

	private class CellModifier implements ICellModifier {

		@Override
		public boolean canModify(Object element, String property) {
			if (element instanceof Item)
				element = ((Item) element).getData();
			if (!(element instanceof UnitMappingEntry))
				return false;
			UnitMappingEntry entry = (UnitMappingEntry) element;
			if (property.equals(FLOW_PROPERTY))
				return true;
			if (property.equals(CONVERSION_FACTOR)) {
				UnitGroup group = entry.getUnitGroup();
				if (group == null)
					return false;
				Unit unit = group.getUnit(entry.getUnitName());
				return unit == null;
			}
			return false;
		}

		@Override
		public Object getValue(Object element, String property) {
			if (element instanceof Item)
				element = ((Item) element).getData();
			if (!(element instanceof UnitMappingEntry))
				return null;
			UnitMappingEntry entry = (UnitMappingEntry) element;
			if (property.equals(CONVERSION_FACTOR))
				return Double.toString(entry.getFactor());
			if (property.equals(FLOW_PROPERTY)) {
				String[] candidates = getFlowPropertyCandidates(entry
						.getUnitName());
				flowPropertyCellEditor.setItems(candidates);
				if (entry.getFlowProperty() == null)
					return new Integer(-1);
				else
					return getIndex(entry.getFlowProperty().getName(),
							candidates);
			}
			return null;
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (value == null)
				return;
			if (element instanceof Item)
				element = ((Item) element).getData();
			if (!(element instanceof UnitMappingEntry))
				return;
			UnitMappingEntry entry = (UnitMappingEntry) element;
			if (property.equals(FLOW_PROPERTY)) {
				int val = Integer.parseInt(value.toString());
				String[] candidates = getFlowPropertyCandidates(entry
						.getUnitName());
				updateEntry(entry, val, candidates);
			} else if (property.equals(CONVERSION_FACTOR)) {
				try {
					entry.setFactor(Double.parseDouble(value.toString()));
				} catch (Exception e) {
					// do nothing
				}
			}
			checkCompletion();
			tableViewer.refresh();
		}

		private String[] getFlowPropertyCandidates(String unitName) {
			List<String> flowPropertyNames = new ArrayList<>();
			for (FlowProperty flowProperty : flowProperties) {
				if (flowProperty.getUnitGroup().getUnit(unitName) != null)
					flowPropertyNames.add(flowProperty.getName());
			}
			if (flowPropertyNames.size() == 0) {
				// unit does not exist in database
				for (FlowProperty flowProperty : flowProperties)
					flowPropertyNames.add(flowProperty.getName());
			}
			return flowPropertyNames.toArray(new String[flowPropertyNames
					.size()]);
		}

		private int getIndex(String name, String[] candidates) {
			for (int i = 0; i < candidates.length; i++)
				if (Objects.equals(name, candidates[i]))
					return i;
			return -1;
		}

		private void updateEntry(UnitMappingEntry entry, int index,
				String[] candidates) {
			if (index < 0 || index > (candidates.length - 1))
				return;
			String candidate = candidates[index];
			FlowProperty prop = null;
			for (FlowProperty flowProperty : flowProperties) {
				if (Objects.equals(flowProperty.getName(), candidate)) {
					prop = flowProperty;
					break;
				}
			}
			if (prop == null)
				return;
			updateEntry(entry, prop);
		}

		private void updateEntry(UnitMappingEntry entry, FlowProperty prop) {
			entry.setFlowProperty(prop);
			UnitGroup unitGroup = prop.getUnitGroup();
			entry.setUnitGroup(unitGroup);
			Unit unit = unitGroup.getUnit(entry.getUnitName());
			entry.setUnit(unit);
			if (unit != null)
				entry.setFactor(unit.getConversionFactor());
			else
				entry.setFactor(1d);
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
			if (!(element instanceof UnitMappingEntry))
				return null;
			UnitMappingEntry row = (UnitMappingEntry) element;
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
				if (unitGroup.getReferenceUnit() == null)
					return null;
				return unitGroup.getReferenceUnit().getName();
			case 3:
				if (row.getFactor() == null)
					return null;
				return Double.toString(row.getFactor());
			case 4:
				return getFormula(row);
			default:
				return null;
			}
		}

		private String getFormula(UnitMappingEntry row) {
			if (row == null || row.getFactor() == null
					|| row.getUnitGroup() == null)
				return "";
			return "1.0 " + row.getUnitName() + " = "
					+ row.getFactor().toString() + " "
					+ row.getUnitGroup().getReferenceUnit().getName();
		}
	}

}
