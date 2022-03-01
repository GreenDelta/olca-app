package org.openlca.app.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.swt.widgets.Combo;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.RefEntity;
import org.openlca.util.Strings;

/**
 * A simple wrapper for a plain combo box that maps a list of
 * entities to the respective combo box items and simplifies
 * a bit the selection handling.
 */
public record EntityCombo<T>(Combo combo, List<T> entities,
														 Function<T, String> label) {

	public EntityCombo(Combo combo, List<T> entities, Function<T, String> label) {
		this.combo = combo;
		this.entities = new ArrayList<>(entities);
		this.label = Objects.requireNonNullElse(label, Objects::toString);
		fillItems();
	}

	public static <T extends RefEntity> EntityCombo<T> of(
		Combo combo, Class<T> type, IDatabase db) {
		var dao = Daos.base(db, type);
		List<T> all = dao == null
			? Collections.emptyList()
			: dao.getAll();
		return new EntityCombo<>(combo, all, Labels::name);
	}

	public static <T extends RefEntity> EntityCombo<T> of(
		Combo combo, List<T> entities) {
		var list = new ArrayList<>(entities);
		return new EntityCombo<>(combo, list, Labels::name);
	}

	public EntityCombo<T> onSelected(Consumer<T> fn) {
		if (combo == null || fn == null)
			return this;
		Controls.onSelect(combo, $ -> {
			var i = combo.getSelectionIndex();
			if (i < 0 || i > entities.size())
				return;
			fn.accept(entities.get(i));
		});
		return this;
	}

	public EntityCombo<T> select(T entity) {
		var idx = -1;
		for (int i = 0; i < entities.size(); i++) {
			var e = entities.get(i);
			if (Objects.equals(e, entity)) {
				idx = i;
				break;
			}
		}
		if (idx >= 0) {
			combo.select(idx);
		}
		return this;
	}

	public EntityCombo<T> update(List<T> newEntities) {
		entities.clear();
		entities.addAll(newEntities);
		fillItems();
		return this;
	}

	private void fillItems() {
		this.entities.sort(
			(e1, e2) -> Strings.compare(labelOf(e1), labelOf(e2)));
		var items = new String[this.entities.size()];
		for (int i = 0; i < this.entities.size(); i++) {
			items[i] = labelOf(this.entities.get(i));
		}
		combo.setItems(items);
	}

	private String labelOf(T e) {
		return e != null ? label.apply(e) : "";
	}
}
