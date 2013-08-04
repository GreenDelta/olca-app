/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AllocationMethodViewer;
import org.openlca.app.viewers.table.ExchangeViewer;
import org.openlca.app.viewers.table.ExchangeViewer.Direction;
import org.openlca.app.viewers.table.ExchangeViewer.Type;
import org.openlca.core.model.Process;

class ProcessExchangePage extends ModelPage<Process> {

	private FormToolkit toolkit;

	ProcessExchangePage(ProcessEditor editor) {
		super(editor, "ProcessExchangePage", Messages.InputOutputPageLabel);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);

		createAllocationSection(body);
		Composite inputs = createIOSection(body, Messages.Inputs, 3);
		createExchangeSection(inputs, Direction.INPUT, Type.PRODUCT,
				Messages.Products);
		createExchangeSection(inputs, Direction.INPUT, Type.ELEMENTARY,
				Messages.Resources);
		createExchangeSection(inputs, Direction.INPUT, Type.WASTE,
				Messages.Wastes);
		Composite outputs = createIOSection(body, Messages.Outputs, 4);
		createExchangeSection(outputs, Direction.OUTPUT, Type.PRODUCT,
				Messages.Products);
		createExchangeSection(outputs, Direction.INPUT, Type.AVOIDED_PRODUCT,
				Messages.AvoidedProducts);
		createExchangeSection(outputs, Direction.OUTPUT, Type.ELEMENTARY,
				Messages.Emissions);
		createExchangeSection(outputs, Direction.OUTPUT, Type.WASTE,
				Messages.Wastes);

		body.setFocus();
		form.reflow(true);
	}

	private void createAllocationSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit,
				Messages.AllocationMethod);
		AllocationMethodViewer viewer = new AllocationMethodViewer(composite);
		getBinding().on(getModel(), "allocationMethod", viewer);
	}

	private Composite createIOSection(Composite parent, String label,
			int nrOfContainer) {
		Section section = UI.section(parent, toolkit, label);
		Composite composite = toolkit.createComposite(section);
		UI.gridLayout(composite, 1);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.heightHint = nrOfContainer * 500;
		composite.setLayoutData(data);
		section.setClient(composite);
		return composite;
	}

	private void createExchangeSection(Composite parent, Direction direction,
			Type type, String label) {
		Section section = UI.section(parent, toolkit, label);
		Composite composite = toolkit.createComposite(section);
		UI.gridLayout(composite, 1);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.heightHint = 500;
		composite.setLayoutData(data);
		section.setClient(composite);

		ExchangeViewer viewer = new ExchangeViewer(composite, Database.get(),
				direction, type);
		viewer.bindTo(section);
		getBinding().on(getModel(), "exchanges", viewer);
	}
}
