/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.process;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.evaluation.EvaluationListener;
import org.openlca.core.editors.PropertyProviderPage;
import org.openlca.core.math.FormulaParseException;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.SelectObjectDialog;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.Viewers;
import org.openlca.ui.dnd.IModelDropHandler;
import org.openlca.ui.viewer.AllocationMethodViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FormPage for displaying and editing the inputs and outputs of an exchange
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangePage extends PropertyProviderPage implements
		EvaluationListener, PropertyChangeListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private AllocationMethodViewer allocationViewer;
	private TableViewer inputViewer;
	private TableViewer outputViewer;
	private IMessageManager messageManager;
	private Process process = null;
	private Section inputSection;
	private Section outputSection;

	public ExchangePage(ProcessEditor editor) {
		super(editor, "ExchangePage", Messages.Processes_InputOutputPageLabel);
		this.process = (Process) editor.getModelComponent();
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		messageManager = getForm().getMessageManager();
		createAllocationSection(body, toolkit);

		// create section for inputs
		inputSection = UI.section(body, toolkit, Messages.Common_Inputs);
		UI.gridData(inputSection, true, true);
		Composite composite = UI.sectionClient(inputSection, toolkit);
		UI.gridLayout(composite, 1);
		createInputTableViewer(composite, toolkit);

		// create section for outputs
		outputSection = UI.section(body, toolkit, Messages.Common_Outputs);
		UI.gridData(outputSection, true, true);
		Composite composite2 = UI.sectionClient(outputSection, toolkit);
		UI.gridLayout(composite2, 1);
		createOutputTableViewer(composite2, toolkit);

		ExchangeTable.setInput(process, inputViewer, outputViewer);
	}

	private void createAllocationSection(final Composite body,
			final FormToolkit toolkit) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Processes_DefaultAMSectionLabel);

		UI.formLabel(composite, Messages.AllocationMethod);
		allocationViewer = new AllocationMethodViewer(composite);
	}

	private void addExchanges(long[] flowIds, boolean input) {
		Exchange[] exchanges = new Exchange[flowIds.length];
		for (int i = 0; i < flowIds.length; i++) {
			if (!process.contains(flowIds[i], input)) {
				Flow flow = null;
				UnitGroup unitGroup = null;
				try {
					flow = Database.createDao(Flow.class).getForId(flowIds[i]);
					unitGroup = flow.getReferenceFlowProperty().getUnitGroup();
				} catch (final Exception e) {
					log.error(
							"Loading flow, flow information and unit group from db failed",
							e);
				}
				if (unitGroup != null) {
					Exchange exchange = new Exchange();
					exchange.setFlow(flow);
					exchange.setFlowPropertyFactor(flow.getReferenceFactor());
					exchange.setUnit(unitGroup.getReferenceUnit());
					exchange.setInput(input);
					exchanges[i] = exchange;
					process.getExchanges().add(exchange);
				}
			}
		}
		ExchangeTable.setInput(process, inputViewer, outputViewer);
		if (input)
			inputViewer.setSelection(new StructuredSelection(exchanges));
		else
			outputViewer.setSelection(new StructuredSelection(exchanges));

		// check allocation
		final String text = checkAllocation();
		messageManager.removeMessage("allocationMethod");
		if (text != null) {
			messageManager.addMessage("allocationMethod", text, null,
					IMessageProvider.WARNING);
		}
	}

	private void createInputTableViewer(Composite parent, FormToolkit toolkit) {
		IModelDropHandler exchangesInputHandler = new FlowDropHandler(true);
		inputViewer = UIFactory.createTableViewer(parent, ModelType.FLOW,
				exchangesInputHandler, toolkit, ExchangeTable.INPUT_PROPERTIES);
		ExchangeLabelProvider labelProvider = new ExchangeLabelProvider(
				Database.get(), process);
		inputViewer.setLabelProvider(labelProvider);
		bindActions(inputSection, inputViewer, true);
		GridData exchangesInputGridData = new GridData(SWT.FILL, SWT.FILL,
				true, true);
		exchangesInputGridData.widthHint = 300;
		exchangesInputGridData.heightHint = 200;
		inputViewer.getTable().setLayoutData(exchangesInputGridData);
		ExchangeTable.addInputEditors(inputViewer, Database.get());
		ExchangeTable.addSorting(inputViewer, Database.get());

	}

	private void createOutputTableViewer(final Composite parent,
			final FormToolkit toolkit) {
		IModelDropHandler exchangesOutputHandler = new FlowDropHandler(false);
		outputViewer = UIFactory.createTableViewer(parent, ModelType.FLOW,
				exchangesOutputHandler, toolkit,
				ExchangeTable.OUTPUT_PROPERTIES);
		ExchangeLabelProvider labelProvider = new ExchangeLabelProvider(
				Database.get(), process);
		outputViewer.setLabelProvider(labelProvider);
		bindActions(outputSection, outputViewer, false);
		GridData exchangesOutputGridData = new GridData(SWT.FILL, SWT.FILL,
				true, true);
		exchangesOutputGridData.widthHint = 300;
		exchangesOutputGridData.heightHint = 200;
		outputViewer.getTable().setLayoutData(exchangesOutputGridData);
		ExchangeTable.addOutputEditors(outputViewer, Database.get());
		ExchangeTable.addSorting(outputViewer, Database.get());
	}

	private void bindActions(Section section, TableViewer viewer,
			boolean forInputs) {
		Action add = new AddExchangeAction(forInputs);
		RemoveExchangeAction remove = new RemoveExchangeAction(forInputs,
				process);
		remove.setViewer(viewer);
		Action valueViewSwitch = new SwitchValueViewAction(viewer);
		UI.bindActions(section, add, remove, valueViewSwitch);
	}

	/**
	 * Looks up if all product flows contain at least one equal flow property
	 * which can be used by the allocation method. In case of economic or
	 * physical allocation the allocation factors will be calculated and set to
	 * each exchange
	 * 
	 * @return error message if allocation cannot be applied, null else
	 */
	String checkAllocation() {

		String text = null;

		//@formatter:off
		// TODO: this code is crazy: please the the AllocationSwitch class
		// which does the same thing
		
//		try {
//			final AllocationMethod method = process.getAllocationMethod();
//			if (method == AllocationMethod.Physical
//					|| method == AllocationMethod.Economic) {
//				// check conditions of physical/economic method
//				final Map<String, Flow> flowInformations = new HashMap<>();
//				final Map<String, List<String>> productToFlowProperties = new HashMap<>();
//				String productWithLessFlowProperties = null;
//				int flowPropertyCount = Integer.MAX_VALUE;
//				final List<Exchange> exchanges = new ArrayList<>();
//				for (final Exchange product : process
//						.getOutputs(FlowType.PRODUCT_FLOW)) {
//					exchanges.add(product);
//				}
//				for (final Exchange product : process
//						.getOutputs(FlowType.WASTE_FLOW)) {
//					exchanges.add(product);
//				}
//				for (final Exchange product : exchanges) {
//					List<String> flowProperties = productToFlowProperties
//							.get(product.getId());
//					// collect flow property ids of each product flow
//					if (flowProperties == null) {
//						flowProperties = new ArrayList<>();
//					}
//					Flow flowInformation = product.getFlow();
//
//					flowInformations.put(product.getId(), flowInformation);
//					for (final FlowPropertyFactor flowPropertyFactor : flowInformation
//							.getFlowPropertyFactors()) {
//						if (flowPropertyFactor.getFlowProperty()
//								.getFlowPropertyType() == (method == AllocationMethod.Physical ? FlowPropertyType.PHYSICAL
//								: FlowPropertyType.ECONOMIC)) {
//							flowProperties.add(flowPropertyFactor
//									.getFlowProperty().getId());
//						}
//					}
//					if (flowPropertyCount > flowProperties.size()) {
//						productWithLessFlowProperties = product.getId();
//						flowPropertyCount = flowProperties.size();
//					}
//					productToFlowProperties
//							.put(product.getId(), flowProperties);
//				}
//				int i = 0;
//				final List<String> flowProperties = productToFlowProperties
//						.get(productWithLessFlowProperties);
//				String flowPropertyIntersectionId = null;
//				if (flowProperties != null && flowProperties.size() > 0) {
//					while (flowPropertyIntersectionId == null
//							&& i < flowProperties.size()) {
//						// check if all products contain at least one of the
//						// flow properties.
//						// The product flow with the least flow properties is
//						// used.
//						final String flowPropertyId = flowProperties.get(i);
//						boolean isFoundInAll = true;
//						for (final List<String> flowPropertiesToCompare : productToFlowProperties
//								.values()) {
//							if (!flowPropertiesToCompare
//									.contains(flowPropertyId)) {
//								isFoundInAll = false;
//								break;
//							}
//						}
//						if (isFoundInAll) {
//							flowPropertyIntersectionId = flowPropertyId;
//						}
//						i++;
//					}
//				}
//				process.clearAllocationFactors();
//				if (flowPropertyIntersectionId == null) {
//					text = NLS
//							.bind(Messages.Processes_NoFlowPropertyUsedByAll,
//									(method == AllocationMethod.Physical ? Messages.Processes_Physical
//											: Messages.Processes_Economic));
//				} else {
//					// there is a common flow property
//					double totalAmount = 0;
//					final Map<String, Double> amounts = new HashMap<>();
//					// for each product exchange
//					for (final Exchange product : exchanges) {
//						// append total amount
//						Flow flow = product.getFlow();
//						final FlowPropertyFactor factor = flow
//								.getFlowPropertyFactor(flowPropertyIntersectionId);
//						final double amount = product.getConvertedResult()
//								/ factor.getConversionFactor();
//						totalAmount += amount;
//						amounts.put(product.getId(), amount);
//					}
//
//					// for each exchange of the process
//					for (final Exchange exchange : process.getExchanges()) {
//						// is exchange is input or elementary
//						if (exchange.isInput()
//								|| exchange.getFlow().getFlowType() == FlowType.ElementaryFlow) {
//							// for each amount
//							for (final Entry<String, Double> entry : amounts
//									.entrySet()) {
//								// create new allocation factor
//								final AllocationFactor allocationFactor = new AllocationFactor(
//										UUID.randomUUID().toString(),
//										entry.getKey(), entry.getValue()
//												/ totalAmount);
//								exchange.add(allocationFactor);
//							}
//						}
//					}
//				}
//			} else if (method == AllocationMethod.Causal) {
//				boolean causalCanBeApplied = true;
//				// for each exchange
//				for (final Exchange exchange : process.getExchanges()) {
//					// if exchange is input or elementary
//					if (exchange.isInput()
//							|| exchange.getFlow().getFlowType() == FlowType.ElementaryFlow) {
//						double allocationSum = 0;
//						// for each allocation factor of the exchange
//						for (final AllocationFactor factor : exchange
//								.getAllocationFactors()) {
//							// add value to sum
//							allocationSum += factor.getValue();
//						}
//						// if not summing up to 1 (100%)
//						if (allocationSum != 1) {
//							// causal allocation can not be applied
//							causalCanBeApplied = false;
//							break;
//						}
//					}
//				}
//				if (!causalCanBeApplied) {
//					text = Messages.Processes_NotSumTo100;
//				}
//			} else {
//				process.clearAllocationFactors();
//			}
//		} catch (final Exception e) {
//			log.error("Checking allocation failed", e);
//		}
		
		// @formatter:on
		return text;
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Processes_FormText
				+ ": "
				+ (process != null ? process.getName() != null ? process
						.getName() : "" : "");
		return title;
	}

	@Override
	protected void initListeners() {
		allocationViewer
				.addSelectionChangedListener(new org.openlca.ui.viewer.ISelectionChangedListener<AllocationMethod>() {

					@Override
					public void selectionChanged(AllocationMethod selection) {
						process.setAllocationMethod(selection);
						final String text = checkAllocation();
						messageManager.removeMessage("allocationMethod");
						if (text != null) {
							messageManager.addMessage("allocationMethod", text,
									null, IMessageProvider.WARNING);
						}
					}
				});

		inputViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						setSelection(getEditor(), event.getSelection());
					}
				});

		outputViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						setSelection(getEditor(), event.getSelection());
					}
				});

		registerDoubleClick(inputViewer);
		registerDoubleClick(outputViewer);
	}

	private void registerDoubleClick(final TableViewer viewer) {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Exchange exchange = Viewers.getFirstSelected(viewer);
				if (exchange == null)
					return;
				App.openEditor(exchange.getFlow());
			}
		});
	}

	@Override
	protected void setData() {
		allocationViewer.select(process.getAllocationMethod());
	}

	@Override
	public void error(final FormulaParseException exception) {
		if (messageManager != null) {
			messageManager.addMessage(exception.toString(),
					exception.getMessage(), null, IMessageProvider.ERROR);
		}
	}

	@Override
	public void evaluated() {
		if (inputViewer != null && outputViewer != null) {
			messageManager.removeAllMessages();
			inputViewer.refresh();
			outputViewer.refresh();
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getNewValue() instanceof AllocationFactor
				|| evt.getSource() instanceof AllocationFactor) {
			if (process.getAllocationMethod() == AllocationMethod.Causal) {
				final String text = checkAllocation();
				messageManager.removeMessage("allocationMethod");
				if (text != null) {
					messageManager.addMessage("allocationMethod", text, null,
							IMessageProvider.WARNING);
				}
			}
		} else if (evt.getPropertyName().equals("quantitativeReference")
				|| evt.getPropertyName().equals("value")
				|| evt.getPropertyName().equals("pedigreeUncertainty")
				|| evt.getPropertyName().equals("baseUncertainty")) {
			if (inputViewer != null) {
				inputViewer.refresh();
			}
			if (outputViewer != null) {
				outputViewer.refresh();
			}
		}
	}

	/**
	 * Adds an exchange object to this process
	 * 
	 * @see Action
	 */
	private class AddExchangeAction extends Action {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.core.editors.process.InputOutputPage.AddExchangeAction";

		/**
		 * Indicates if the new exchange should be an input or output
		 */
		private final boolean input;

		/**
		 * The text of the action
		 */
		public String TEXT;

		/**
		 * Creates a new AddExchangeAction and sets the ID, TEXT and
		 * ImageDescriptor
		 * 
		 * @param input
		 *            Indicates if the new exchange should be an input or output
		 */
		public AddExchangeAction(final boolean input) {
			setId(ID);
			if (input) {
				TEXT = Messages.Processes_AddInputText;
			} else {
				TEXT = Messages.Processes_AddOutputText;
			}
			setText(TEXT);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
			this.input = input;
		}

		@Override
		public void run() {
			SelectObjectDialog dialog = new SelectObjectDialog(UI.shell(),
					ModelType.FLOW, true);
			int code = dialog.open();
			if (code == Window.OK && dialog.getMultiSelection() != null) {
				final long[] ids = new long[dialog.getMultiSelection().length];
				for (int i = 0; i < ids.length; i++) {
					ids[i] = dialog.getMultiSelection()[i].getId();
				}
				addExchanges(ids, input);
			}
		}
	}

	private class FlowDropHandler implements IModelDropHandler {

		private final boolean input;

		public FlowDropHandler(boolean input) {
			this.input = input;
		}

		@Override
		public void handleDrop(List<BaseDescriptor> droppedComponents) {
			if (droppedComponents == null)
				return;
			long[] ids = new long[droppedComponents.size()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = droppedComponents.get(i).getId();
			}
			addExchanges(ids, input);
		}

	}

	/**
	 * Switch between the value and formula view of exchanges.
	 */
	private class SwitchValueViewAction extends Action {

		private TableViewer viewer;

		public SwitchValueViewAction(TableViewer viewer) {
			this.viewer = viewer;
			setText(Messages.Processes_FormulaViewMode);
			setImageDescriptor(ImageType.FORMULA_ICON.getDescriptor());
		}

		@Override
		public void run() {
			Object label = viewer.getLabelProvider();
			if (!(label instanceof ExchangeLabelProvider))
				return;
			ExchangeLabelProvider labelProvider = (ExchangeLabelProvider) label;
			boolean showFormulas = !labelProvider.isShowFormulas();
			labelProvider.setShowFormulas(showFormulas);
			viewer.refresh();
			setTextImage(showFormulas);
		}

		private void setTextImage(boolean showFormulas) {
			if (showFormulas) {
				setText(Messages.Processes_FormulaViewMode);
				setImageDescriptor(ImageType.FORMULA_ICON.getDescriptor());
			} else {
				setText(Messages.Processes_ValueViewMode);
				setImageDescriptor(ImageType.NUMBER_ICON.getDescriptor());
			}
		}
	}

}
