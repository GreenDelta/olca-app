package org.openlca.app.navigation.actions.libraries;


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.library.Library;
import org.openlca.core.library.MountAction;
import org.openlca.core.library.PreMountCheck;
import org.openlca.core.library.PreMountState;

class MountLibraryDialog extends FormDialog {

	private final Library library;
	private final PreMountCheck.Result checkResult;
	private final List<Section> sections = new ArrayList<>();


	static void show(Library library, PreMountCheck.Result checkResult) {
		if (checkResult.isError()) {
			ErrorReporter.on(
				"Failed to check library: " + library,
				checkResult.error());
			return;
		}
		var state = checkResult.getState(library).orElse(null);
		if (state == null) {
			MsgBox.info(
				"No libraries to add",
				"Found no libraries that could be added.");
			return;
		}
		if (state == PreMountState.PRESENT) {
			var b = Question.ask("Library already present",
				"The library was already added to the database. Continue anyhow?");
			if (!b)
				return;
		}
	}

	private MountLibraryDialog(Library library, PreMountCheck.Result checkResult) {
		super(UI.shell());
		this.library = library;
		this.checkResult = checkResult;

		// create the dialog sections for the states in the check-result
		var stateMap = new EnumMap<PreMountState, List<Library>>(PreMountState.class);
		for (var libState : checkResult.getStates()) {
			var lib = libState.first;
			var state = libState.second;
			stateMap.computeIfAbsent(state, s -> new ArrayList<>()).add(lib);
		}
		var stateOrder = new PreMountState[] {
			PreMountState.NEW,
			PreMountState.PRESENT,
			PreMountState.TAG_CONFLICT,
			PreMountState.CONFLICT,
		};
		for (var state : stateOrder) {
			var libs = stateMap.get(state);
			if (libs.isEmpty())
				continue;
			sections.add(Section.of(state, libs));
		}
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		UI.gridLayout(body, 1);
		sections.forEach(s -> s.render(body, tk));
	}

	private static class Section {
		final PreMountState state;
		final List<Library> libraries;
		final List<MountAction> actions;
		MountAction selectedAction;

		private Section(PreMountState state, List<Library> libraries) {
			this.state = state;
			this.libraries = libraries;
			this.actions = List.of(state.actions());
			this.selectedAction = state.defaultAction();
		}

		static Section of(PreMountState state, List<Library> libraries) {
			return new Section(state, libraries);
		}

		Section render(Composite parent, FormToolkit tk) {
			var group = new Group(parent, SWT.NONE);
			tk.adapt(group);
			group.setText(title());
			UI.gridData(group, true, false);
			UI.gridLayout(group, 1);
			tk.createFormText(group, false)
				.setText(info(), false, false);
			tk.createFormText(group, false)
				.setText(libraryList(), true, false);
			createCombo(tk, group);
			return this;
		}

		private void createCombo(FormToolkit tk, Group group) {
			if (actions.size() < 2)
				return;
			var comp = tk.createComposite(group);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 2);
			var combo = UI.formCombo(comp, tk, "Action:");
			var items = new String[actions.size()];
			var selectedIdx = -1;
			for (int i = 0; i < actions.size(); i++) {
				var action = actions.get(i);
				if (action == state.defaultAction()) {
					selectedIdx = i;
				}
				items[i] = labelOf(action);
			}
			combo.setItems(items);
			if (selectedIdx >= 0) {
				combo.select(selectedIdx);
			}
			Controls.onSelect(combo, $ -> {
				var idx = combo.getSelectionIndex();
				selectedAction = actions.get(idx);
			});
		}

		private String title() {
			return switch (state) {
				case NEW -> "New libraries";
				case PRESENT -> "Already existing libraries";
				case TAG_CONFLICT -> "Tag conflicts";
				case CONFLICT -> "Data conflicts";
			};
		}

		private String info() {
			return switch (state) {
				case NEW -> "The following libraries will be added to the database:";
				case PRESENT -> "The following libraries are already present:";
				case TAG_CONFLICT -> "The data sets of these libraries are already"
					+ " present in the database but under a different or no library tag:";
				case CONFLICT -> "The data sets of these libraries are partly present,"
					+ " have different library tags, or have other data conflicts. The"
					+ " data sets in the database will be updated.";
			};
		}

		private String libraryList() {
			var text = new StringBuilder("<ul>");
			for (var lib : libraries) {
				text.append("<li>")
					.append(lib.name())
					.append("<li>");
			}
			return text + "</ul>";
		}

		private String labelOf(MountAction action) {
			var label = switch (action) {
				case ADD -> "Add library";
				case SKIP -> "Keep existing";
				case RETAG -> "Update library tags only";
				case UPDATE -> "Update data sets";
			};
			if (action == state.defaultAction()) {
				label += " (recommended)";
			}
			return label;
		}
	}

}
