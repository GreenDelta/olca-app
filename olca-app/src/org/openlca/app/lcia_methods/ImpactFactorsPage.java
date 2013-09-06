/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.app.lcia_methods;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.app.viewers.table.ImpactFactorViewer;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

class ImpactFactorsPage extends ModelPage<ImpactMethod> {

	private FormToolkit toolkit;
	private ImpactFactorViewer factorViewer;

	ImpactFactorsPage(ImpactMethodEditor editor) {
		super(editor, "ImpactFactorsPage", Messages.ImpactFactors);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ImpactMethod
				+ ": " + getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);

		Section section = UI.section(body, toolkit, Messages.ImpactFactors);
		UI.gridData(section, true, true);
		Composite client = toolkit.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);

		Composite container = toolkit.createComposite(client);
		UI.gridLayout(container, 2, 10, 0);
		UI.gridData(container, true, false);

		new Label(container, SWT.NONE).setText(Messages.ImpactCategory);
		ImpactCategoryViewer categoryViewer = new ImpactCategoryViewer(
				container);
		categoryViewer
				.addSelectionChangedListener(new CategorySelectionListener());
		categoryViewer.setInput(getDescriptorList());

		factorViewer = new ImpactFactorViewer(client, Database.get());
		factorViewer.bindTo(section);

		body.setFocus();
		form.reflow(true);
	}

	private List<ImpactCategoryDescriptor> getDescriptorList() {
		List<ImpactCategoryDescriptor> list = new ArrayList<>();
		for (ImpactCategory category : getModel().getImpactCategories())
			list.add(Descriptors.toDescriptor(category));
		return list;
	}

	private class CategorySelectionListener implements
			ISelectionChangedListener<ImpactCategoryDescriptor> {

		@Override
		public void selectionChanged(ImpactCategoryDescriptor selection) {
			getBinding().release(factorViewer);
			if (selection == null)
				factorViewer.setInput((ImpactCategory) null);
			else
				for (ImpactCategory cat : getModel().getImpactCategories())
					if (cat.getId() == selection.getId())
						getBinding().on(cat, "impactFactors", factorViewer);
		}
	}

}
