package org.openlca.app.editors.lcia_methods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Event;
import org.openlca.app.Messages;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.util.Strings;

import com.google.common.eventbus.Subscribe;

class ImpactFactorPage extends ModelPage<ImpactMethod> {

	private ImpactMethodEditor editor;
	private FormToolkit toolkit;
	private ImpactFactorTable factorTable;
	private ImpactCategoryViewer categoryViewer;

	ImpactFactorPage(ImpactMethodEditor editor) {
		super(editor, "ImpactFactorsPage", Messages.ImpactFactors);
		this.editor = editor;
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
		createCategoryViewer(client);
		factorTable = new ImpactFactorTable(editor);
		factorTable.render(client, section);
		categoryViewer.selectFirst();
		form.reflow(true);
	}

	private void createCategoryViewer(Composite client) {
		Composite container = toolkit.createComposite(client);
		UI.gridLayout(container, 2, 10, 0);
		UI.gridData(container, true, false);
		new Label(container, SWT.NONE).setText(Messages.ImpactCategory);
		categoryViewer = new ImpactCategoryViewer(container);
		CategoryChange categoryChange = new CategoryChange();
		categoryViewer.addSelectionChangedListener(categoryChange);
		categoryViewer.setInput(getDescriptorList());
		editor.getEventBus().register(categoryChange);
	}

	private List<ImpactCategoryDescriptor> getDescriptorList() {
		List<ImpactCategoryDescriptor> list = new ArrayList<>();
		for (ImpactCategory category : getModel().getImpactCategories())
			list.add(Descriptors.toDescriptor(category));
		Collections.sort(list, new Comparator<ImpactCategoryDescriptor>() {
			@Override
			public int compare(ImpactCategoryDescriptor o1,
					ImpactCategoryDescriptor o2) {
				return Strings.compare(o1.getName(), o2.getName());
			}
		});
		return list;
	}

	private class CategoryChange implements
			ISelectionChangedListener<ImpactCategoryDescriptor> {

		@Override
		public void selectionChanged(ImpactCategoryDescriptor selection) {
			if (selection == null)
				factorTable.setImpactCategory(null);
			else {
				for (ImpactCategory cat : getModel().getImpactCategories())
					if (cat.getId() == selection.getId()) {
						factorTable.setImpactCategory(cat);
						break;
					}
			}
		}

		@Subscribe
		public void categoryChange(Event event) {
			if (!event.match(editor.IMPACT_CATEGORY_CHANGE))
				return;
			categoryViewer.setInput(getDescriptorList());
			factorTable.setImpactCategory((ImpactCategory) null);
		}
	}

}
