package org.openlca.app.navigation.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.core.database.MappingFileDao;
import org.openlca.core.model.ModelType;

public class GroupElement extends NavigationElement<Group> {

	GroupElement(INavigationElement<?> parent, Group group) {
		super(parent, group);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var group = getContent();
		if (group == null)
			return Collections.emptyList();

		// add the model type elements of this group
		var elements = new ArrayList<INavigationElement<?>>();
		for (ModelType type : getContent().types) {
			elements.add(new ModelTypeElement(this, type));
		}

		// add mapping files to the background data group
		// if available
		if (group.type != GroupType.BACKGROUND_DATA)
			return elements;
		var lib = getLibrary();
		if (lib.isPresent())
			return elements;
		var db = Database.get();
		if (db == null)
			return elements;
		var names = new MappingFileDao(db).getNames();
		if (!names.isEmpty()) {
			elements.add(new MappingDirElement(this, names));
		}

		return elements;
	}

}
