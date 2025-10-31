package org.openlca.app.wizards.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.io.ImportLog;
import org.openlca.core.io.ImportLog.Message;
import org.openlca.core.io.ImportLog.State;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

public class ImportLogView extends SimpleFormEditor {

	private Collection<Message> messages;

	public static void open(ImportLog log) {
		var id = AppContext.put(log);
		var input = new SimpleEditorInput(id, M.ImportDetails);
		Editors.open(input, "ImportLogView");
	}

	@Override
	public void init(IEditorSite site, IEditorInput raw)
			throws PartInitException {
		super.init(site, raw);
		if (!(raw instanceof SimpleEditorInput input))
			return;
		var obj = AppContext.remove(input.id);
		messages = obj instanceof ImportLog log
				? log.messages()
				: Collections.emptySet();
	}

	@Override
	protected FormPage getPage() {
		setTitleImage(Icon.IMPORT.get());
		return new Page();
	}

	private class Page extends FormPage {

		Page() {
			super(ImportLogView.this, "ImportLogView.Page", M.ImportDetails);
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.header(mForm, M.ImportDetails, Icon.IMPORT.get());
			var tk = mForm.getToolkit();
			var body = UI.body(form, tk);

			// filter
			var filter = new Filter(messages);
			filter.render(body, tk);

			// table
			var table = Tables.createViewer(
					body, M.Status, M.DataSet, M.Message);
			table.setLabelProvider(new MessageLabel());
			Tables.bindColumnWidths(table, 0.2, 0.4, 0.4);
			filter.apply(table);

			// actions
			var onOpen = Actions.onOpen(() -> {
				Message message = Viewers.getFirstSelected(table);
				if (!message.hasDescriptor())
					return;
				App.open(message.descriptor());
			});

			var copy = TableClipboard.onCopySelected(table);
			Actions.bind(table, onOpen, copy);
			Tables.onDoubleClick(table, $ -> onOpen.run());

			var menu = table.getTable().getMenu();
			menu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					Message message = Viewers.getFirstSelected(table);
					onOpen.setEnabled(message.hasDescriptor());
				}

				@Override
				public void menuHidden(MenuEvent e) {
				}
			});
		}
	}

	private static class MessageLabel extends BaseLabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Message message))
				return null;
			if (col == 0)
				return iconOf(message.state());
			if (col == 1 && message.hasDescriptor())
				return Images.get(message.descriptor());
			return null;
		}

		private Image iconOf(State state) {
			if (state == null)
				return null;
			return switch (state) {
				case IMPORTED, UPDATED -> Icon.IMPORT.get();
				case ERROR -> Icon.ERROR.get();
				case WARNING -> Icon.WARNING.get();
				case INFO -> Icon.INFO.get();
				case SKIPPED -> Icon.UNDO.get();
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Message message))
				return null;
			return switch (col) {
				case 0 -> of(message.state());
				case 1 -> Labels.name(message.descriptor());
				case 2 -> message.message();
				default -> null;
			};
		}

		private static String of(State state) {
			if (state == null)
				return null;
			return switch (state) {
				case IMPORTED -> "Imported";
				case UPDATED -> "Updated";
				case ERROR -> "Error";
				case WARNING -> "Warning";
				case INFO -> "Information";
				case SKIPPED -> "Ignored";
			};
		}
	}

	private static class Filter {

		private final Collection<Message> messages;

		private TableViewer table;
		private int maxCount = 1000;
		private String text;
		private ModelType type;
		private final Set<State> states = EnumSet.noneOf(State.class);

		Filter(Collection<Message> messages) {
			this.messages = messages;
		}

		void render(Composite body, FormToolkit tk) {

			var comp = UI.composite(body, tk);
			UI.gridLayout(comp, 2);
			UI.fillHorizontal(comp);

			var icon = UI.imageHyperlink(comp, tk, SWT.BORDER);
			icon.setToolTipText(M.ClickToSearchOrPressEnter);
			icon.setImage(Icon.SEARCH.get());
			Controls.onClick(icon, $ -> update());

			// search text
			var searchComp = UI.composite(comp, tk);
			UI.fillHorizontal(searchComp);
			UI.gridLayout(searchComp, 2, 10, 0);
			var searchText = UI.text(searchComp, tk, SWT.SEARCH);
			UI.fillHorizontal(searchText);
			searchText.addModifyListener($ -> text = searchText.getText());
			Controls.onReturn(searchText, $ -> update());

			// type button
			var typeBtn = UI.button(searchComp, tk, M.AllTypes);
			var typeItems = TypeItem.allOf(messages);
			typeBtn.setImage(Icon.DOWN.get());
			var typeMenu = new Menu(typeBtn);
			for (var item : typeItems) {
				item.mountTo(typeMenu, selectedType -> {
					type = selectedType;
					var text = selectedType == null
							? M.AllTypes
							: Labels.of(selectedType);
					typeBtn.setText(text);
					typeBtn.pack();
					typeBtn.getParent().layout();
					update();
				});
			}

			typeBtn.setMenu(typeMenu);
			Controls.onSelect(typeBtn, e -> typeMenu.setVisible(true));

			// checkboxes
			UI.filler(comp, tk);
			var optComp = UI.composite(comp, tk);
			UI.gridLayout(optComp, 6, 10, 0);
			var errCheck = UI.button(optComp, tk, M.Errors, SWT.CHECK);
			var warnCheck = UI.button(optComp, tk, M.Warnings, SWT.CHECK);
			var allCheck = UI.button(optComp, tk, M.All, SWT.CHECK);
			allCheck.setSelection(true);

			BiConsumer<Button, State> stateCheck = (button, state) -> {
				if (button.getSelection()) {
					states.add(state);
					allCheck.setSelection(false);
				} else {
					states.remove(state);
					allCheck.setSelection(states.isEmpty());
				}
				update();
			};

			Controls.onSelect(errCheck, $ -> stateCheck.accept(errCheck, State.ERROR));
			Controls.onSelect(warnCheck, $ -> stateCheck.accept(warnCheck, State.WARNING));

			Controls.onSelect(allCheck, $ -> {
				if (allCheck.getSelection()) {
					states.clear();
					errCheck.setSelection(false);
					warnCheck.setSelection(false);
				} else {
					states.add(State.ERROR);
					errCheck.setSelection(true);
				}
				update();
			});

			UI.label(optComp, tk, " | ");
			UI.label(optComp, tk, M.MaxNumberOfMessages);
			var spinner = UI.spinner(optComp, tk);
			spinner.setValues(maxCount, 1000, 1_000_000, 0, 1000, 5000);
			spinner.addModifyListener($ -> {
				maxCount = spinner.getSelection();
				update();
			});
		}

		void apply(TableViewer table) {
			this.table = table;
			update();
		}

		private void update() {
			if (table == null)
				return;

			// states & type
			var stream = messages.stream();
			if (!states.isEmpty()) {
				stream = stream.filter(
						m -> m.state() != null && states.contains(m.state()));
			}
			if (type != null) {
				stream = stream.filter(
						m -> {
							var d = m.descriptor();
							return d != null && d.type == type;
						});
			}

			// text filter
			var phrase = text == null
					? null
					: text.trim();
			if (Strings.isNotBlank(phrase)) {
				var words = Arrays.stream(phrase.split(" "))
					.map(s -> s.trim().toLowerCase())
					.filter(Strings::isNotBlank)
					.toList();
				stream = stream.filter(m -> matches(m, words));
			}

			var list = stream.limit(maxCount).toList();
			table.setInput(list);
		}

		private boolean matches(Message message, List<String> words) {
			if (words.isEmpty())
				return true;
			for (var word : words) {
				var s = message.message();
				if (s != null && s.toLowerCase().contains(word))
					continue;
				s = message.descriptor() != null
						? Labels.name(message.descriptor())
						: null;
				if (s != null && s.toLowerCase().contains(word))
					continue;
				s = MessageLabel.of(message.state());
				if (s != null && s.toLowerCase().contains(word))
					continue;
				return false;
			}
			return true;
		}

	}

	private record TypeItem(ModelType type, int count) {

		static List<TypeItem> allOf(Collection<Message> messages) {
			var map = new EnumMap<ModelType, Integer>(ModelType.class);
			for (var message : messages) {
				var d = message.descriptor();
				if (d == null || d.type == null)
					continue;
				map.compute(d.type,
						(type, count) -> count != null ? count + 1 : 1);
			}

			var items = new ArrayList<TypeItem>(map.size() + 1);
			items.add(new TypeItem(null, messages.size()));
			map.entrySet().stream()
					.filter(e -> e.getValue() != null)
					.map(e -> new TypeItem(e.getKey(), e.getValue()))
					.sorted(Comparator.comparingInt(TypeItem::count).reversed())
					.forEach(items::add);
			return items;
		}

		@Override
		public String toString() {
			return type == null
					? M.AllTypes + " (" + count + ")"
					: Labels.of(type) + " (" + count + ")";
		}

		void mountTo(Menu menu, Consumer<ModelType> fn) {
			var item = new MenuItem(menu, SWT.NONE);
			var label = toString();
			item.setText(label);
			item.setToolTipText(label);
			item.setImage(Images.get(type));
			Controls.onSelect(item, $ -> {
				if (fn != null) {
					fn.accept(type);
				}
			});
		}
	}
}
