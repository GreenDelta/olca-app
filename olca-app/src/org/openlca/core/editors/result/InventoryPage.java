/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.result;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Numbers;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Expression;
import org.openlca.core.model.Flow;
import org.openlca.core.model.UncertaintyDistributionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FormPage for displaying the LCI results of a product system
 * 
 * @author Sebastian Greve
 * 
 */
public class InventoryPage extends ModelEditorPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final String AMOUNT = Messages.Amount;
	private final String CATEGORY = Messages.Category;
	private final String FLOW = Messages.Flow;
	private final String FLOWPROPERTY = Messages.FlowProperty;

	private int iDirection = 1;

	/**
	 * Property of the selected column of the input viewer
	 */
	private String iLastColumnSelect = FLOW;

	/**
	 * The inputs
	 */
	private final Exchange[] inputs;

	/**
	 * Table viewer widget for displaying the lci inputs
	 */
	private TableViewer inputTableViewer;

	/**
	 * The name of the product system
	 */
	private final String name;

	/**
	 * Sort direction of the input viewer
	 */
	private int oDirection = 1;

	/**
	 * Property of the selected column of the input viewer
	 */
	private String oLastColumnSelect = FLOW;

	/**
	 * Action for opening the flow of a selected lci result
	 */
	private OpenEditorAction openAction;

	/**
	 * The outputs
	 */
	private final Exchange[] outputs;

	/**
	 * Table viewer widget for displaying the lci outputs
	 */
	private TableViewer outputTableViewer;

	/**
	 * Property for the input/output table viewer
	 */
	private final String SD = Messages.Systems_SD;

	/**
	 * Property for the input/output table viewer
	 */
	private final String UNIT = Messages.Unit;

	/**
	 * Properties for the input/output table viewer
	 */
	private final String[] PROPERTIES = new String[] { FLOW, CATEGORY,
			FLOWPROPERTY, AMOUNT, UNIT, SD };

	/**
	 * Creates a new instance.
	 * 
	 * @param editor
	 *            the editor of this page
	 * @param name
	 *            The name of the product system
	 * @param inventory
	 *            The inventory of the product system to display
	 */
	public InventoryPage(ModelEditor editor, String name,
			List<Exchange> inventory) {
		super(editor, "InventoryPage", Messages.LCI);
		this.name = name;
		final List<Exchange> inputs = new ArrayList<>();
		final List<Exchange> outputs = new ArrayList<>();

		// for each exchange of the inventory
		for (final Exchange exchange : inventory) {
			if (exchange.isInput()) {
				inputs.add(exchange);
			} else {
				outputs.add(exchange);
			}
		}

		this.inputs = inputs.toArray(new Exchange[inputs.size()]);
		this.outputs = outputs.toArray(new Exchange[outputs.size()]);
	}

	/**
	 * Getter of the category path of the flow of the given exchange
	 * 
	 * @param exchange
	 *            The exchange to get the category path of the flow for
	 * @return The category path of the flow of the given exchange
	 */
	private String getCategoryString(final Exchange exchange) {
		String text = "";
		if (exchange.getFlow() != null) {
			try {
				// load category
				final Category category = getDatabase().select(Category.class,
						exchange.getFlow().getCategoryId());
				text += category.getFullPath();
			} catch (final Exception e) {
				log.error("Reading category from db failed", e);
			}
		}
		return text;
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		// create tool bar
		final IToolBarManager toolBar = getForm().getToolBarManager();
		toolBar.removeAll();
		openAction = new OpenEditorAction();

		// create sash form
		final SashForm sashForm = new SashForm(body, SWT.NONE);
		final GridData sashGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashForm.setLayoutData(sashGD);
		sashForm.setLayout(new GridLayout(2, true));
		toolkit.adapt(sashForm);

		// create input composite
		final Composite inputComposite = toolkit.createComposite(sashForm);
		inputComposite.setLayout(new GridLayout());
		inputComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));
		toolkit.createLabel(inputComposite, Messages.Inputs);

		// create table viewer for displaying and editing inputs
		inputTableViewer = new TableViewer(inputComposite, SWT.FULL_SELECTION
				| SWT.BORDER);
		inputTableViewer.setLabelProvider(new ResultLabelProvider());
		inputTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		inputTableViewer.setColumnProperties(PROPERTIES);
		final Table inputTable = inputTableViewer.getTable();
		inputTable.setLinesVisible(true);
		inputTable.setHeaderVisible(true);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		inputTable.setLayoutData(gd);

		// for each column property
		for (final String property : PROPERTIES) {
			// create a table column
			final TableColumn c = new TableColumn(inputTable, SWT.NULL);
			c.setText(property);
		}

		// for each table column
		for (final TableColumn c : inputTable.getColumns()) {
			if (c.getText().equals(FLOW)) {
				// flow column should be 150 px width
				c.setWidth(150);
				c.getParent().setSortColumn(c);
				c.getParent().setSortDirection(SWT.DOWN);
			} else {
				c.pack();
			}
			c.setMoveable(true);
			c.setResizable(true);
		}
		inputTableViewer.setSorter(new ExchangeViewerSorter(FLOW, 1));

		// create composite for outputs
		final Composite outputComposite = toolkit.createComposite(sashForm);
		outputComposite.setLayout(new GridLayout());
		outputComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true));

		// create table viewer for displaying and editing outputs
		toolkit.createLabel(outputComposite, Messages.Outputs);
		outputTableViewer = new TableViewer(outputComposite, SWT.FULL_SELECTION
				| SWT.BORDER);
		outputTableViewer.setLabelProvider(new ResultLabelProvider());
		outputTableViewer
				.setContentProvider(ArrayContentProvider.getInstance());
		outputTableViewer.setColumnProperties(PROPERTIES);
		final Table outputTable = outputTableViewer.getTable();
		final GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		outputTable.setLayoutData(gd2);
		outputTable.setLinesVisible(true);
		outputTable.setHeaderVisible(true);

		// for each column property
		for (final String property : PROPERTIES) {
			// create a table column
			final TableColumn c = new TableColumn(outputTable, SWT.NULL);
			c.setText(property);
		}

		// for each table column
		for (final TableColumn c : outputTable.getColumns()) {
			if (c.getText().equals(FLOW)) {
				c.setWidth(150);
				c.getParent().setSortColumn(c);
				c.getParent().setSortDirection(SWT.DOWN);
			} else {
				c.pack();
			}
		}
		outputTableViewer.setSorter(new ExchangeViewerSorter(FLOW, 1));

	}

	@Override
	protected String getFormTitle() {
		return NLS.bind(Messages.LCIOf, name);
	}

	@Override
	protected void initListeners() {
		for (final TableColumn c : inputTableViewer.getTable().getColumns()) {
			c.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					// no action on default selection
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (c.getText().equals(iLastColumnSelect)) {
						iDirection = iDirection * -1;
					} else {
						iDirection = 1;
					}
					inputTableViewer.setSorter(new ExchangeViewerSorter(c
							.getText(), iDirection));
					iLastColumnSelect = c.getText();
					c.getParent().setSortColumn(c);
					c.getParent().setSortDirection(
							iDirection == 1 ? SWT.DOWN : SWT.UP);
				}
			});
		}
		inputTableViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.getFirstElement() != null) {
					final Flow flow = ((Exchange) selection.getFirstElement())
							.getFlow();
					openAction.setModelComponent(getDatabase(), flow);
					openAction.run();
				}
			}
		});

		for (final TableColumn c : outputTableViewer.getTable().getColumns()) {
			c.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					// no action on default selection
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (c.getText().equals(oLastColumnSelect)) {
						oDirection = oDirection * -1;
					} else {
						oDirection = 1;
					}
					outputTableViewer.setSorter(new ExchangeViewerSorter(c
							.getText(), oDirection));
					oLastColumnSelect = c.getText();
					c.getParent().setSortColumn(c);
					c.getParent().setSortDirection(
							oDirection == 1 ? SWT.DOWN : SWT.UP);
				}
			});
		}
		outputTableViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.getFirstElement() != null) {
					final Flow flow = ((Exchange) selection.getFirstElement())
							.getFlow();
					openAction.setModelComponent(getDatabase(), flow);
					openAction.run();
				}
			}
		});
	}

	@Override
	protected void setData() {
		if (inputs != null) {
			inputTableViewer.setInput(inputs);
		}
		if (outputs != null) {
			outputTableViewer.setInput(outputs);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		inputTableViewer = null;
		outputTableViewer = null;
		openAction = null;
	}

	/**
	 * Sorter of the exchanges (The content of the input and output table
	 * viewer)
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class ExchangeViewerSorter extends ViewerSorter {

		/**
		 * The sort direction
		 */
		private int direction = 1;

		/**
		 * The property of the selected column
		 */
		private final String property;

		/**
		 * Creates a new instance
		 * 
		 * @param property
		 *            The property of the selected column
		 * @param direction
		 *            The sort direction
		 */
		public ExchangeViewerSorter(final String property, final int direction) {
			this.property = property;
			this.direction = direction;
		}

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {
			final Exchange ex1 = (Exchange) e1;
			final Exchange ex2 = (Exchange) e2;
			int result = 0;
			if (property.equals(FLOW)) {
				// compare flow names
				result = ex1.getFlow().getName().toLowerCase()
						.compareTo(ex2.getFlow().getName().toLowerCase());
			} else if (property.equals(CATEGORY)) {
				// compare category paths
				result = getCategoryString(ex1).toLowerCase().compareTo(
						getCategoryString(ex2).toLowerCase());
			} else if (property.equals(FLOWPROPERTY)) {
				// compare flow property names
				result = ex1
						.getFlowPropertyFactor()
						.getFlowProperty()
						.getName()
						.toLowerCase()
						.compareTo(
								ex2.getFlowPropertyFactor().getFlowProperty()
										.getName().toLowerCase());
			} else if (property.equals(AMOUNT)) {
				// compare resulting amounts
				result = Double.compare(ex1.getResultingAmount().getValue(),
						ex2.getResultingAmount().getValue());
			} else if (property.equals(UNIT)) {
				// compare unit names
				result = ex1.getUnit().getName().toLowerCase()
						.compareTo(ex2.getUnit().getName().toLowerCase());
			} else if (property.equals(SD)) {
				// compare standard deviation
				if (ex1.getDistributionType() != null
						&& ex1.getDistributionType() == UncertaintyDistributionType.NORMAL
						&& ex2.getDistributionType() != null
						&& ex2.getDistributionType() == UncertaintyDistributionType.NORMAL) {
					final Expression sd1 = ex1.getUncertaintyParameter2();
					final Expression sd2 = ex2.getUncertaintyParameter2();
					if (sd1.getValue() != 0 && sd2.getValue() != 0) {
						result = Double.compare(sd1.getValue(), sd2.getValue());
					}
				}
			}
			return result * direction;
		}
	}

	/**
	 * Label provider for the input and output table viewer
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class ResultLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			Image img = null;
			if (element instanceof Exchange && columnIndex == 0) {
				// flow icon
				img = ImageType.FLOW_ICON.get();
			}
			if (element instanceof Exchange && columnIndex == 2) {
				// flow property icon
				img = ImageType.FLOW_PROPERTY_ICON.get();
			}
			return img;
		}

		@Override
		public String getColumnText(final Object element, final int columnIndex) {
			String text = null;
			if (element instanceof Exchange) {
				final Exchange exchange = (Exchange) element;
				switch (columnIndex) {

				case 0:
					// get flow name
					text = exchange.getFlow().getName();
					break;
				case 1:
					// get category path
					text = getCategoryString(exchange);
					break;
				case 2:
					// get flow property name
					text = exchange.getFlowPropertyFactor().getFlowProperty()
							.getName();
					break;
				case 3:
					// get resulting amount
					text = Numbers.format(exchange.getResultingAmount()
							.getValue());
					break;
				case 4:
					// get unit name
					text = exchange.getUnit().getName();
					break;
				case 5:
					// get uncertainty distribution type
					if (exchange.getDistributionType() != null
							&& exchange.getDistributionType() == UncertaintyDistributionType.NORMAL) {
						if (exchange.getUncertaintyParameter2().getValue() != 0) {
							text = Numbers.format(exchange
									.getUncertaintyParameter2().getValue());
						}
					}
					break;
				}
			}
			return text;
		}
	}

}
