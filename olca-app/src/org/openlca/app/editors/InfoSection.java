package org.openlca.app.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.editors.DataBinding.TextBindType;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.UI;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;

/**
 * This is the general info section that each editor has: name, description,
 * etc.
 */
public class InfoSection {

	private CategorizedEntity entity;
	private DataBinding binding;
	private Composite container;

	public InfoSection(CategorizedEntity entity, DataBinding binding) {
		this.entity = entity;
		this.binding = binding;
	}

	public void render(Composite body, FormToolkit toolkit) {
		container = UI.formSection(body, toolkit,
				Messages.Common_GeneralInformation);
		Text nameText = UI.formText(container, toolkit, Messages.Common_Name);
		binding.on(entity, "name", TextBindType.STRING, nameText);
		Text descriptionText = UI.formMultiText(container, toolkit,
				Messages.Common_Description);
		binding.on(entity, "description", TextBindType.STRING, descriptionText);
		new Label(container, SWT.NONE).setText(Messages.Common_Category);
		Link link = new Link(container, SWT.NONE);
		link.setText("<a>" + getBreadcrumb(entity.getCategory()) + "</a>");
		link.addSelectionListener(new CategoryLinkSelectionListener());
	}

	private String getBreadcrumb(Category category) {
		if (category == null)
			return "";
		String breadcrumb = category.getName();
		if (category.getParentCategory() != null)
			breadcrumb = getBreadcrumb(category.getParentCategory()) + " > "
					+ breadcrumb;
		return breadcrumb;
	}

	public Composite getContainer() {
		return container;
	}

	private class CategoryLinkSelectionListener implements SelectionListener {

		private void select() {
			Navigator.select(entity.getCategory());
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			select();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			select();
		}
	}

}
