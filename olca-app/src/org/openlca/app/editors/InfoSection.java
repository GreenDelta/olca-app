package org.openlca.app.editors;

import java.util.Stack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.Messages;
import org.openlca.app.editors.DataBinding.TextBindType;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Images;
import org.openlca.app.util.UI;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;

/**
 * This is the general info section that each editor has: name, description,
 * etc.
 */
class InfoSection {

	private CategorizedEntity entity;
	private DataBinding binding;
	private Composite container;

	InfoSection(CategorizedEntity entity, DataBinding binding) {
		this.entity = entity;
		this.binding = binding;
	}

	void render(Composite body, FormToolkit toolkit) {
		container = UI.formSection(body, toolkit, Messages.GeneralInformation);
		Text nameText = UI.formText(container, toolkit, Messages.Name);
		binding.on(entity, "name", TextBindType.STRING, nameText);
		Text descriptionText = UI.formMultiText(container, toolkit,
				Messages.Description);
		binding.on(entity, "description", TextBindType.STRING, descriptionText);
		new Label(container, SWT.NONE).setText(Messages.Category);

		createBreadcrumb(container);
	}

	private void createBreadcrumb(Composite parent) {
		Stack<Category> stack = new Stack<>();
		Category current = entity.getCategory();
		while (current != null) {
			stack.push(current);
			current = current.getParentCategory();
		}

		Composite breadcrumb = new Composite(parent, SWT.NONE);
		UI.gridLayout(breadcrumb, stack.size() * 2 - 1, 0, 0);
		while (!stack.isEmpty()) {
			current = stack.pop();
			Hyperlink link = null;
			if (current.getParentCategory() == null) {
				link = new ImageHyperlink(breadcrumb, SWT.NONE);
				((ImageHyperlink) link).setImage(Images.getIcon(current));
			} else {
				new Label(breadcrumb, SWT.NONE).setText(" > ");
				link = new Hyperlink(breadcrumb, SWT.NONE);
			}
			link.setText(current.getName());
			link.addHyperlinkListener(new CategoryLinkSelectionListener(current));
			link.setForeground(Colors.getLinkBlue());
		}
	}

	Composite getContainer() {
		return container;
	}

	private class CategoryLinkSelectionListener extends HyperlinkAdapter {

		private Category category;

		private CategoryLinkSelectionListener(Category category) {
			this.category = category;
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			Navigator.select(category);
		}
	}

}
