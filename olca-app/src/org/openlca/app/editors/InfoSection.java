package org.openlca.app.editors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
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
	private final ModelEditor<?> editor;

	private Composite container;
	private Label versionLabel;

	public InfoSection(ModelEditor<?> editor) {
		this.entity = editor.getModel();
		this.editor = editor;
	}

	public void render(Composite body, FormToolkit tk) {
		container = UI.formSection(body, tk, M.GeneralInformation, 3);
		Widgets.text(container, M.Name, "name", editor, tk);
		Widgets.multiText(container, M.Description, "description", editor, tk);
		if (entity.category != null) {
			new Label(container, SWT.NONE).setText(M.Category);
			createBreadcrumb(container);
			new CommentControl(container, tk, "category", editor.getComments());
		} else if (editor.hasComment("category")) {
			new Label(container, SWT.NONE).setText(M.Category);
			UI.filler(container);
			new CommentControl(container, tk, "category", editor.getComments());
		}
		createVersionText(tk);
		Text uuidText = UI.formText(container, tk, "UUID");
		uuidText.setEditable(false);
		if (entity.refId != null)
			uuidText.setText(entity.refId);
		UI.filler(container, tk);
		createDateText(tk);
		createTags(tk);
	}

	private void createVersionText(FormToolkit tk) {
		UI.formLabel(container, tk, M.Version);
		var comp = tk.createComposite(container);
		var layout = UI.gridLayout(comp, 3);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		versionLabel = tk.createLabel(comp,
				Version.asString(entity.version));
		editor.onSaved(() -> {
			entity = editor.getModel();
			versionLabel.setText(Version.asString(entity.version));
		});
		new VersionLink(comp, tk, VersionLink.MAJOR);
		new VersionLink(comp, tk, VersionLink.MINOR);
		UI.filler(container, tk);
	}

	private void createDateText(FormToolkit tk) {
		var format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		UI.formLabel(container, tk, M.LastChange);
		Label text = UI.formLabel(container, tk, "");
		if (entity.lastChange != 0) {
			text.setText(format.format(new Date(entity.lastChange)));
		}
		editor.onSaved(() -> {
			entity = editor.getModel();
			text.setText(format.format(new Date(entity.lastChange)));
		});
		UI.filler(container, tk);
	}

	private void createBreadcrumb(Composite parent) {
		Stack<Category> stack = new Stack<>();
		Category current = entity.category;
		while (current != null) {
			stack.push(current);
			current = current.category;
		}
		Composite breadcrumb = new Composite(parent, SWT.NONE);
		UI.gridLayout(breadcrumb, stack.size() * 2 - 1, 0, 0);
		while (!stack.isEmpty()) {
			current = stack.pop();
			Hyperlink link;
			if (current.category == null) {
				link = new ImageHyperlink(breadcrumb, SWT.NONE);
				((ImageHyperlink) link).setImage(Images.get(current));
			} else {
				new Label(breadcrumb, SWT.NONE).setText(" > ");
				link = new Hyperlink(breadcrumb, SWT.NONE);
			}
			link.setText(current.name);
			link.addHyperlinkListener(new CategoryLinkClick(current));
			link.setForeground(Colors.linkBlue());
		}
	}

	private void createTags(FormToolkit tk) {
		UI.formLabel(container, tk, "Tags");
		var comp = tk.createComposite(container);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2, 10, 0);
		var btn = tk.createButton(comp, "Add a tag", SWT.NONE);

		var tagComp = tk.createComposite(comp);
		UI.gridLayout(tagComp, 1);
		UI.gridData(tagComp, true, false);

		// render function for the tag list
		var innerComp = new AtomicReference<Composite>();
		var recursion = new AtomicReference<Consumer<String[]>>();
		Consumer<String[]> render = tags -> {
			var inner = innerComp.get();
			if (inner != null) {
				inner.dispose();
			}
			inner = tk.createComposite(tagComp);
			UI.gridData(inner, true, false);
			innerComp.set(inner);

			UI.gridLayout(inner, tags.length, 5, 0);
			for (var t : tags) {
				new Tag(t, inner, tk).onRemove(tag -> {
					var fn = recursion.get();
					if (fn == null)
						return;
					fn.accept(Tags.remove(editor.getModel(), tag));
					editor.setDirty(true);
				});
			}
			List.of(inner, tagComp, comp, container)
					.forEach(Composite::layout);
		};
		recursion.set(render);
		render.accept(Tags.of(editor.getModel()));

		Controls.onSelect(btn, _e -> {
			var tags = Tags.add(editor.getModel(),
					UUID.randomUUID().toString().substring(0, 8));
			render.accept(tags);
			editor.setDirty(true);
		});

		UI.filler(container, tk);
	}

	public Composite getContainer() {
		return container;
	}

	private static class CategoryLinkClick extends HyperlinkAdapter {

		private final Category category;

		private CategoryLinkClick(Category category) {
			this.category = category;
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			Navigator.select(category);
		}
	}

	private class VersionLink extends HyperlinkAdapter {

		static final int MAJOR = 1;
		static final int MINOR = 2;
		private final int type;
		private final ImageHyperlink link;

		private Image hoverIcon = null;
		private Image icon = null;

		public VersionLink(Composite parent, FormToolkit toolkit, int type) {
			this.type = type;
			link = toolkit.createImageHyperlink(parent, SWT.TOP);
			link.addHyperlinkListener(this);
			configureLink();
		}

		private void configureLink() {
			String tooltip;
			if (type == MAJOR) {
				tooltip = M.UpdateMajorVersion;
				hoverIcon = Icon.UP.get();
				icon = Icon.UP_DISABLED.get();
			} else {
				tooltip = M.UpdateMinorVersion;
				hoverIcon = Icon.UP_DOUBLE.get();
				icon = Icon.UP_DOUBLE_DISABLED.get();
			}
			link.setToolTipText(tooltip);
			link.setActiveImage(hoverIcon);
			link.setImage(icon);
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			if (entity == null || versionLabel == null)
				return;
			Version version = new Version(entity.version);
			if (type == MAJOR)
				version.incMajor();
			else
				version.incMinor();
			entity.version = version.getValue();
			versionLabel.setText(version.toString());
			editor.setDirty(true);
		}

		@Override
		public void linkEntered(HyperlinkEvent e) {
			link.setImage(hoverIcon);
		}

		@Override
		public void linkExited(HyperlinkEvent e) {
			link.setImage(icon);
		}
	}

	/**
	 * Our tag widget that we use in the info-section.
	 */
	private static class Tag {

		private final ImageHyperlink link;
		private Consumer<String> clickFn;

		Tag(String text, Composite comp, FormToolkit tk) {

			link = tk.createImageHyperlink(comp, SWT.NONE);
			link.setText(text);
			link.setImage(Icon.DELETE_DISABLED.get());
			link.setBackground(Colors.fromHex("#e8eaf6"));

			link.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseEnter(MouseEvent e) {
					link.setImage(Icon.DELETE.get());
				}

				@Override
				public void mouseExit(MouseEvent e) {
					link.setImage(Icon.DELETE_DISABLED.get());
				}
			});

			Controls.onClick(link, _e -> {
				if (clickFn != null) {
					clickFn.accept(text);
				}
			});
		}

		void onRemove(Consumer<String> fn) {
			clickFn = fn;
		}
	}

	/**
	 * A dialog for selecting a new tag for a model.
	 */
	private static class TagDialog extends FormDialog {

		private final String[] candidates;

		TagDialog(String[] candidates) {
			super(UI.shell());
			this.candidates = candidates;
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			UI.gridLayout(body, 1, 10, 10);

			var textComp = tk.createComposite(body);
			UI.gridLayout(textComp, 2, 10, 0);
			UI.gridData(textComp, true, false);
			var label = UI.formLabel(textComp, tk, "New tag:");
			label.setFont(UI.boldFont());
			var text = UI.formText(textComp, SWT.SEARCH);
			UI.gridData(text, true, false);

			UI.formLabel(body, tk, "Used tags:");
			var list = new org.eclipse.swt.widgets.List(body, SWT.NONE);
			UI.gridData(list, true, true);

		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 500);
		}
	}
}
