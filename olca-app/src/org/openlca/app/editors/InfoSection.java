package org.openlca.app.editors;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Version;
import org.openlca.util.Categories;
import org.openlca.util.Strings;

/**
 * This is the general info section that each editor has: name, description,
 * etc.
 */
public class InfoSection {

	private RootEntity entity;
	private final ModelEditor<?> editor;

	private Section section;
	private Composite container;
	private FormText versionLabel;

	public InfoSection(ModelEditor<?> editor) {
		this.entity = editor.getModel();
		this.editor = editor;
	}

	public InfoSection render(Composite body, FormToolkit tk) {
		section = UI.section(body, tk, M.GeneralInformation);
		container = UI.sectionClient(section, tk, 3);

		// name and library
		ModelPage.text(container, M.Name, "name", editor, tk)
				.setEditable(editor.isEditable());
		if (entity.isFromLibrary()) {
			ModelPage.text(container, "Library", "library", editor, tk)
					.setEditable(false);
		}

		// category
		UI.label(container, tk, M.Category);
		List<String> path = entity.category != null
			? Categories.path(entity.category)
			: Collections.emptyList();
		if (path.isEmpty()) {
			UI.label(container, tk, "- none -");
		} else {
			var category = String.join("/", path);
			var link = UI.formCategoryLink(container, tk, category, Images.get(entity.category));
			Controls.onClick(link, $ -> Navigator.select(entity.category));
		}
		if (editor.hasComment("category")) {
			new CommentControl(container, tk, "category", editor.getComments());
		} else {
			UI.filler(container);
		}

		// description
		ModelPage.multiText(container, M.Description, "description", editor, tk)
			.setEditable(editor.isEditable());

		// version
		UI.filler(container, tk);
		var versionComp = UI.composite(container, tk);

		UI.gridLayout(versionComp, 8, 10, 0);
		createVersionText(versionComp, tk);

		// last change
		UI.label(versionComp, tk, " ");
		createDateText(versionComp, tk);

		// uuid
		if (entity.refId != null) {
			UI.label(versionComp, tk, " ");
			UI.label(versionComp, tk, "UUID");
			var uuidText = UI.text(versionComp, null, entity.refId,
					SWT.READ_ONLY);
			uuidText.setBackground(versionComp.getBackground());
			var gridData = new GridData(GridData.FILL_HORIZONTAL);
			uuidText.setLayoutData(gridData);
		}

		UI.filler(container, tk);

		// tags
		createTags(tk);

		return this;
	}

	public Composite composite() {
		return container;
	}

	public Section section() {
		return section;
	}

	private void createVersionText(Composite parent, FormToolkit tk) {
		UI.label(parent, tk, M.Version);
		var comp = UI.composite(parent, tk);
		var layout = UI.gridLayout(comp, 3);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		versionLabel = UI.formText(comp, tk, false);
		versionLabel.setText(Version.asString(entity.version), false, false);
		editor.onSaved(() -> {
			entity = editor.getModel();
			versionLabel.setText(Version.asString(entity.version), false, false);
		});
		new VersionLink(comp, tk, VersionLink.MAJOR);
		new VersionLink(comp, tk, VersionLink.MINOR);
	}

	private void createDateText(Composite parent, FormToolkit tk) {
		UI.label(parent, tk, M.LastChange);
		var text = UI.label(parent, tk, "");
		if (entity.lastChange != 0) {
			text.setText(Numbers.asTimestamp(entity.lastChange));
		} else {
			text.setText(" --- ");
		}
		editor.onSaved(() -> {
			entity = editor.getModel();
			text.setText(Numbers.asTimestamp(entity.lastChange));
		});
	}

	private void createTags(FormToolkit tk) {
		UI.label(container, tk, "Tags");
		var comp = UI.composite(container, tk);
		UI.gridData(comp, true, false);
		UI.gridLayout(comp, 2, 10, 0);
		var btn = UI.button(comp, tk, "Add a tag");
		btn.setEnabled(editor.isEditable());

		var tagComp = UI.composite(comp, tk);
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
			inner = UI.composite(tagComp, tk);
			UI.gridData(inner, true, false);
			innerComp.set(inner);
			UI.gridLayout(inner, tags.length, 5, 0);

			for (var t : tags) {
				new Tag(t, inner, tk)
						.setEnabled(editor.isEditable())
						.onRemove(tag -> {
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
			var model = editor.getModel();
			var candidates = Tags.searchFor(model, Database.get());
			new TagDialog(candidates, tag -> {
				var tags = Tags.add(model, tag);
				render.accept(tags);
				editor.setDirty(true);
			}).open();
		});

		UI.filler(container, tk);
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
			link.setEnabled(editor.isEditable());
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
			versionLabel.setText(version.toString(), false, false);
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
			link.setBackground(Colors.tagBackground());

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

		Tag setEnabled(boolean b) {
			link.setEnabled(b);
			return this;
		}

		Tag onRemove(Consumer<String> fn) {
			clickFn = fn;
			return this;
		}
	}

	/**
	 * A dialog for selecting a new tag for a model.
	 */
	private static class TagDialog extends FormDialog {

		private final String[] candidates;
		private final Consumer<String> onOk;
		private Text text;

		TagDialog(String[] candidates, Consumer<String> onOk) {
			super(UI.shell());
			this.candidates = candidates;
			this.onOk = onOk;
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var tk = mForm.getToolkit();
			var body = UI.dialogBody(mForm.getForm(), tk);

			// text for new tag
			var textComp = UI.composite(body, tk);
			UI.gridLayout(textComp, 2, 10, 0);
			UI.gridData(textComp, true, false);
			var label = UI.label(textComp, tk, "New tag");
			label.setFont(UI.boldFont());
			text = UI.text(textComp, SWT.SEARCH);
			UI.gridData(text, true, false);

			// list with existing tags
			UI.label(body, tk, "Used tags");
			var list = UI.list(body);
			UI.gridData(list, true, true);
			if (candidates != null) {
				list.setItems(candidates);
			}

			// filter function
			text.addModifyListener(_e -> {
				if (candidates == null || candidates.length == 0)
					return;
				var term = text.getText().trim().toLowerCase();
				if (term.isBlank()) {
					list.setItems(candidates);
					return;
				}

				var cands = Arrays.stream(candidates)
						.filter(c -> c.toLowerCase().contains(term))
						.sorted(Comparator.comparingInt(c -> c.indexOf(term)))
						.toArray(String[]::new);
				list.setItems(cands);
				list.getParent().layout();
			});

			// selection handler
			Controls.onSelect(list, _e -> {
				var selection = list.getSelection();
				if (selection == null || selection.length == 0)
					return;
				var tag = selection[0];
				text.setText(tag);
			});

		}

		@Override
		protected Point getInitialSize() {
			return new Point(450, 500);
		}

		@Override
		protected void configureShell(Shell shell) {
			super.configureShell(shell);
			shell.setText("Add a new tag");
			UI.center(UI.shell(), shell);
		}

		@Override
		protected void okPressed() {
			var tag = text.getText();
			super.okPressed(); // will dispose the text!
			if (Strings.nullOrEmpty(tag))
				return;
			if (onOk == null)
				return;
			onOk.accept(tag.trim());
		}
	}
}
