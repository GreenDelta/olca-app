package org.openlca.app.editors;

import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.openlca.app.events.EventHandler;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Images;
import org.openlca.app.util.UI;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.Version;

/**
 * This is the general info section that each editor has: name, description,
 * etc.
 */
public class InfoSection {

	private CategorizedEntity entity;
	private final DataBinding binding;
	private final ModelEditor<?> editor;

	private Composite container;

	public InfoSection(ModelEditor<?> editor) {
		this.entity = editor.getModel();
		this.editor = editor;
		this.binding = new DataBinding(editor);
	}

	public void render(Composite body, FormToolkit toolkit) {
		container = UI.formSection(body, toolkit, Messages.GeneralInformation);
		Text nameText = UI.formText(container, toolkit, Messages.Name);
		binding.on(entity, "name", TextBindType.STRING, nameText);
		Text descriptionText = UI.formMultiText(container, toolkit,
				Messages.Description);
		binding.on(entity, "description", TextBindType.STRING, descriptionText);
		if (entity.getCategory() != null) {
			new Label(container, SWT.NONE).setText(Messages.Category);
			createBreadcrumb(container);
		}
		createVersionText(toolkit);
		createDateText(toolkit);
	}

	private void createVersionText(FormToolkit toolkit) {
		final Text text = UI.formText(container, toolkit, Messages.Version,
				SWT.READ_ONLY);
		text.setText(Version.asString(entity.getVersion()));
		editor.onSaved(new EventHandler() {
			@Override
			public void handleEvent() {
				entity = editor.getModel();
				text.setText(Version.asString(entity.getVersion()));
			}
		});
	}

	private void createDateText(FormToolkit toolkit) {
		final SimpleDateFormat format = new SimpleDateFormat();
		final Text text = UI.formText(container, toolkit, Messages.LastChange,
				SWT.READ_ONLY);
		if (entity.getLastChange() != 0)
			text.setText(format.format(new Date(entity.getLastChange())));
		editor.onSaved(new EventHandler() {
			@Override
			public void handleEvent() {
				entity = editor.getModel();
				text.setText(format.format(new Date(entity.getLastChange())));
			}
		});

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

	public Composite getContainer() {
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
