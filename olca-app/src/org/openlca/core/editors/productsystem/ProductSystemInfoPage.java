/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.editors.productsystem;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.BaseLabelProvider;
import org.openlca.app.BaseNameSorter;
import org.openlca.app.UIFactory;
import org.openlca.app.viewer.ToolTipComboViewer;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorInfoPage;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An editor page with general information of a product system.
 * 
 * @author Sebastian Greve
 * @author Michael Srocka (made some refactorings and bug fixes)
 * 
 */
public class ProductSystemInfoPage extends ModelEditorInfoPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private IMessageManager messageManager;
	private ProductSystem productSystem;
	private Text amountText;

	private ToolTipComboViewer exchangeViewer;
	private ToolTipComboViewer processViewer;
	private ToolTipComboViewer flowPropertyViewer;
	private ToolTipComboViewer unitViewer;

	public ProductSystemInfoPage(ModelEditor editor) {
		super(editor, "ProductSystemInfoPage",
				Messages.Common_GeneralInformation,
				Messages.Common_GeneralInformation);
		productSystem = (ProductSystem) editor.getModelComponent();
	}

	@Override
	protected void createContents(Composite body, FormToolkit toolkit) {
		super.createContents(body, toolkit);
		messageManager = getForm().getMessageManager();
		createAdditionalInfoSection(body, toolkit);
	}

	private void createAdditionalInfoSection(Composite body, FormToolkit toolkit) {
		Composite composite = createSectionAndComposite(body, toolkit);
		processViewer = createViewer(toolkit, Messages.Common_ReferenceProcess,
				composite);
		exchangeViewer = createViewer(toolkit,
				Messages.Systems_ReferenceExchange, composite);
		amountText = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Common_TargetAmount, false);
		flowPropertyViewer = createViewer(toolkit,
				Messages.Systems_FlowProperty, composite);
		unitViewer = createViewer(toolkit, Messages.Common_Unit, composite);
	}

	private Composite createSectionAndComposite(Composite body,
			FormToolkit toolkit) {
		Section section = UIFactory.createSection(body, toolkit,
				Messages.Systems_ProductSystemInfoSectionLabel, true, false);
		Composite infoComposite = UIFactory.createSectionComposite(section,
				toolkit, UIFactory.createGridLayout(2));
		return infoComposite;
	}

	private ToolTipComboViewer createViewer(FormToolkit tk, String label,
			Composite composite) {
		tk.createLabel(composite, label);
		ToolTipComboViewer viewer = new ToolTipComboViewer(composite, SWT.NONE);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new BaseLabelProvider());
		viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		viewer.setSorter(new BaseNameSorter());
		return viewer;
	}

	@Override
	protected String getFormTitle() {
		String title = Messages.Systems_FormText + ": ";
		if (productSystem != null) {
			title += productSystem.getName();
		}
		return title;
	}

	@Override
	protected void initListeners() {

		super.initListeners();

		processViewer.addSelectionChangedListener(new Change<Process>() {
			@Override
			void handleChange(Process value) {
				processChanged(value);
			};
		});

		exchangeViewer.addSelectionChangedListener(new Change<Exchange>() {
			@Override
			void handleChange(Exchange value) {
				exchangeChanged(value);
			}
		});

		flowPropertyViewer
				.addSelectionChangedListener(new Change<FlowPropertyFactor>() {
					@Override
					void handleChange(FlowPropertyFactor value) {
						flowPropertyChanged(value);
					}
				});

		unitViewer.addSelectionChangedListener(new Change<Unit>() {
			@Override
			void handleChange(Unit value) {
				unitChanged(value);
			}
		});

		amountText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				amountChanged();
			}
		});
	}

	private void processChanged(Process newProcess) {
		productSystem.setReferenceProcess(newProcess);
		fillExchangeViewer(newProcess);
		Exchange referenceProduct = newProcess.getQuantitativeReference();
		select(exchangeViewer, referenceProduct);
		exchangeChanged(referenceProduct);
	}

	private void exchangeChanged(Exchange newExchange) {
		productSystem.setReferenceExchange(newExchange);
		fillFlowPropertyViewer(newExchange);
		FlowPropertyFactor referenceFactor = newExchange
				.getFlowPropertyFactor();
		select(flowPropertyViewer, referenceFactor);
		flowPropertyChanged(referenceFactor);
	}

	private void flowPropertyChanged(FlowPropertyFactor factor) {
		productSystem.setTargetFlowPropertyFactor(factor);
		fillUnitViewer(factor);
		Unit refUnit = getReferenceUnit(factor);
		select(unitViewer, refUnit);
		unitChanged(refUnit);
	}

	private Unit getReferenceUnit(FlowPropertyFactor factor) {
		try {
			return factor.getFlowProperty().getUnitGroup().getReferenceUnit();
		} catch (Exception e) {
			throw new RuntimeException("Cannot load unit group.", e);
		}
	}

	private void unitChanged(Unit unit) {
		productSystem.setTargetUnit(unit);
	}

	private void amountChanged() {
		Double value = null;
		try {
			value = Double.parseDouble(amountText.getText());
		} catch (NumberFormatException e) {
			messageManager.addMessage("targetAmount",
					Messages.Systems_DoubleError, null, IMessageProvider.ERROR,
					amountText);
		}
		if (value != null) {
			messageManager.removeMessage("targetAmount", amountText);
			productSystem.setTargetAmount(value);
		}
	}

	@Override
	protected void setData() {
		super.setData();
		amountText.setText(Double.toString(productSystem.getTargetAmount()));
		setViewerData();
	}

	private void setViewerData() {
		processViewer.setInput(productSystem.getProcesses());

		Process refProcess = productSystem.getReferenceProcess();
		if (refProcess != null) {
			select(processViewer, refProcess);
			fillExchangeViewer(refProcess);
		}

		Exchange refExchange = productSystem.getReferenceExchange();
		if (refExchange != null) {
			select(exchangeViewer, refExchange);
			fillFlowPropertyViewer(refExchange);
		}

		FlowPropertyFactor refPropertyFactor = productSystem
				.getTargetFlowPropertyFactor();
		if (refPropertyFactor != null) {
			select(flowPropertyViewer, refPropertyFactor);
			fillUnitViewer(refPropertyFactor);
		}

		Unit refUnit = productSystem.getTargetUnit();
		if (refUnit != null) {
			select(unitViewer, refUnit);
		}
	}

	private void fillExchangeViewer(Process process) {
		Exchange[] products = process.getOutputs(FlowType.PRODUCT_FLOW);
		exchangeViewer.setInput(products);
	}

	private void fillFlowPropertyViewer(Exchange exchange) {
		try {
			Flow flow = exchange.getFlow();
			flowPropertyViewer.setInput(flow.getFlowPropertyFactors());
		} catch (Exception e) {
			throw new RuntimeException("Cannot load flow properties.", e);
		}
	}

	private void fillUnitViewer(FlowPropertyFactor propertyFactor) {
		try {
			UnitGroup unitGroup = propertyFactor.getFlowProperty()
					.getUnitGroup();
			unitViewer.setInput(unitGroup.getUnits());
		} catch (Exception e) {
			log.error("Filling unit viewer failed", e);
		}
	}

	private void select(ToolTipComboViewer viewer, Object obj) {
		viewer.setSelection(new StructuredSelection(obj));
	}

	private abstract class Change<T> implements ISelectionChangedListener {

		@Override
		@SuppressWarnings("unchecked")
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection _selection = event.getSelection();
			if (_selection instanceof IStructuredSelection
					&& !_selection.isEmpty()) {
				IStructuredSelection selection = (IStructuredSelection) _selection;
				T value = (T) selection.getFirstElement();
				handleChange(value);
			}
		}

		abstract void handleChange(T value);

	}

}
