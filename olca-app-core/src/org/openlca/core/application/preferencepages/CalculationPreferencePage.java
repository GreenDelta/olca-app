package org.openlca.core.application.preferencepages;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openlca.core.application.Messages;
import org.openlca.ui.UIFactory;

/**
 * Preference page for calculation properties
 * 
 * @author Sebastian Greve
 * 
 */
// TODO: calculation settings have changed
// we need a new preference page, matrix, sequential, seq-agg
// will follow in the next versions

public class CalculationPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * Combo viewer to select a calculator
	 */
	private ComboViewer comboViewer;

	/**
	 * Property for column 'Name'
	 */
	private final String PROPERTY_DISPLAY_NAME = Messages.CalculationPreferencePage_Name;

	/**
	 * Property for column 'Value'
	 */
	private final String PROPERTY_VALUE = Messages.CalculationPreferencePage_Value;

	/**
	 * The properties of the table viewers
	 */
	private final String[] PROPERTIES = new String[] { PROPERTY_DISPLAY_NAME,
			PROPERTY_VALUE };

	/**
	 * Displays the selection properties of a calculator
	 */
	private TableViewer selectionPropertiesViewer;

	/**
	 * Displays the string properties of a calculator
	 */
	private TableViewer stringPropertiesViewer;

	/**
	 * Initializes the listeners
	 */
	private void initListeners() {
		// listen on the calculation selection
		comboViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						stringPropertiesViewer.setInput(null);
						selectionPropertiesViewer.setInput(null);
						stringPropertiesViewer.getTable().setEnabled(true);
						selectionPropertiesViewer.getTable().setEnabled(true);
					}

				});
	}

	@Override
	protected Control createContents(final Composite parent) {

		// create body
		final Composite composite = UIFactory.createContainer(parent,
				UIFactory.createGridLayout(1));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// create calculation selection combo viewer
		final Composite comboComposite = UIFactory.createContainer(composite,
				UIFactory.createGridLayout(2, false, 5));
		comboComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		comboViewer = UIFactory.createComboViewerWithLabel(comboComposite,
				Messages.CalculationPreferencePage_SelectCalculator);
		comboViewer.setInput(new String[] { "Matrix calculation",
				"Sequential calculation" });
		comboViewer.getCombo().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, false));

		// create table viewer to display "string properties"
		stringPropertiesViewer = new TableViewer(composite, SWT.BORDER
				| SWT.SINGLE | SWT.FULL_SELECTION);
		stringPropertiesViewer.setContentProvider(new ArrayContentProvider());
		stringPropertiesViewer.setLabelProvider(new PropertiesLabelProvider());
		stringPropertiesViewer.setCellModifier(new PropertiesCellModifier());
		stringPropertiesViewer.setColumnProperties(PROPERTIES);
		stringPropertiesViewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		stringPropertiesViewer.getTable().setHeaderVisible(true);
		stringPropertiesViewer.getTable().setLinesVisible(true);
		stringPropertiesViewer.getTable().setEnabled(false);

		for (final String p : PROPERTIES) {
			final TableColumn c = new TableColumn(
					stringPropertiesViewer.getTable(), SWT.NONE);
			c.setText(p);
			c.setWidth(200);
		}

		final CellEditor[] editors = new CellEditor[2];
		editors[1] = new TextCellEditor(stringPropertiesViewer.getTable());
		stringPropertiesViewer.setCellEditors(editors);

		// create table viewer to display "selection properties"
		selectionPropertiesViewer = new TableViewer(composite, SWT.BORDER
				| SWT.SINGLE | SWT.FULL_SELECTION);
		selectionPropertiesViewer
				.setContentProvider(new ArrayContentProvider());
		selectionPropertiesViewer
				.setLabelProvider(new PropertiesLabelProvider());
		selectionPropertiesViewer.setCellModifier(new PropertiesCellModifier());
		selectionPropertiesViewer.setColumnProperties(PROPERTIES);
		selectionPropertiesViewer.getTable().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		selectionPropertiesViewer.getTable().setHeaderVisible(true);
		selectionPropertiesViewer.getTable().setLinesVisible(true);
		selectionPropertiesViewer.getTable().setEnabled(false);

		// for each property
		for (final String p : PROPERTIES) {
			// create column
			final TableColumn c = new TableColumn(
					selectionPropertiesViewer.getTable(), SWT.NONE);
			c.setText(p);
			c.setWidth(200);
		}

		final CellEditor[] editors2 = new CellEditor[2];
		editors2[1] = new ComboBoxCellEditor(
				selectionPropertiesViewer.getTable(), new String[0]);
		selectionPropertiesViewer.setCellEditors(editors2);

		initListeners();
		return composite;
	}

	@Override
	public void init(final IWorkbench workbench) {
		// nothing to initialize
	}

	/**
	 * A cell modifier for the properties table viewer
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class PropertiesCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			// only values can be edited
			return property.equals(PROPERTY_VALUE);
		}

		@Override
		public Object getValue(final Object element, final String property) {
			return null;
		}

		@Override
		public void modify(final Object element, final String property,
				final Object value) {
		}

	}

	/**
	 * Label provider for the properties table viewer
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class PropertiesLabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(final Object element, final int columnIndex) {
			return null;
		}

	}

}
