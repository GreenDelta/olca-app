package org.openlca.app.components;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Combo;
import org.openlca.app.util.Labels;
import org.openlca.core.model.RootEntity;
import org.openlca.util.Strings;

public record EntityCombo<T>(Combo combo, List<T> entities) {

	public EntityCombo  {
		// TODO
	}

	public static <T extends RootEntity> EntityCombo<T> of(
		Combo combo, List<T> entities) {
		var list = new ArrayList<>(entities);
		list.sort((e1, e2) -> Strings.compare(Labels.name(e1), Labels.name(e2)));
		return new EntityCombo<>(combo, list);
	}

}
