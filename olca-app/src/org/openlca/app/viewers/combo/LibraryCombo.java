package org.openlca.app.viewers.combo;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.library.Library;

public class LibraryCombo {

	private final Combo combo;
	private final Predicate<Library> filter;
	private final Consumer<Library> onSelect;
	private List<Library> libraries;

	public LibraryCombo(
		Composite parent,
		FormToolkit tk,
		Predicate<Library> filter,
		Consumer<Library> onSelect) {

		this.combo = new Combo(parent, SWT.READ_ONLY);
		UI.gridData(combo, true, false);
		this.filter = filter;
		this.onSelect = onSelect;
		update();
		Controls.onSelect(combo, $ -> {
			if (onSelect == null)
				return;
			var idx = combo.getSelectionIndex();
			if (idx < 0 || libraries.size() <= idx)
				return;
			onSelect.accept(libraries.get(idx));
		});
	}

	public void update() {
		libraries = Workspace.getLibraryDir().getLibraries();
		if (filter != null) {
			libraries = libraries.stream()
					.filter(filter)
					.collect(Collectors.toList());
		}
		libraries.sort(Comparator.comparing(
				lib -> lib.name().toLowerCase()));
		var items = libraries.stream()
				.map(Library::name)
				.toArray(String[]::new);
		combo.setItems(items);
	}

	public void selectFirst() {
		if (libraries == null || libraries.isEmpty())
			return;
		select(libraries.getFirst());
	}

	public void select(Library lib) {
		if (lib == null || libraries == null)
			return;
		int idx = libraries.indexOf(lib);
		if (idx < 0)
			return;
		combo.select(idx);
		if (onSelect != null) {
			onSelect.accept(lib);
		}
	}

}