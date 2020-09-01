package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.model.ModelType;

public class LibraryElement extends NavigationElement<Library> {

	private ModelType only;

	LibraryElement(INavigationElement<?> parent, Library library) {
		super(parent, library);
	}

	/**
	 * Constructs a library element that only contains models of the given type.
	 * Such elements are used in model selection dialogs and behave a bit different
	 * than library elements in the normal navigation.
	 */
	static LibraryElement of(Library library, ModelType only) {
		var elem = new LibraryElement(null, library);
		elem.only = only;
		return elem;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		if (only != null) {
			var typeElem = new ModelTypeElement(this, only);
			return typeElem.getChildren();
		}

		var db = getDatabase();
		if (db.isEmpty())
			return Collections.emptyList();

		var list = new ArrayList<INavigationElement<?>>();
		list.add(new ModelTypeElement(this, ModelType.PROCESS));
		list.add(new ModelTypeElement(this, ModelType.FLOW));
		list.add(new GroupElement(this,
				new Group(M.IndicatorsAndParameters,
						GroupType.INDICATORS,
						ModelType.IMPACT_METHOD,
						ModelType.IMPACT_CATEGORY,
						ModelType.DQ_SYSTEM,
						ModelType.SOCIAL_INDICATOR,
						ModelType.PARAMETER)));
		list.add(new GroupElement(this,
				new Group(M.BackgroundData,
						GroupType.BACKGROUND_DATA,
						ModelType.FLOW_PROPERTY,
						ModelType.UNIT_GROUP,
						ModelType.CURRENCY,
						ModelType.ACTOR,
						ModelType.SOURCE,
						ModelType.LOCATION)));
		return list;
	}

	/**
	 * Returns the corresponding database if this element is under an active
	 * database.
	 */
	public Optional<IDatabase> getDatabase() {
		var parent = getParent();
		while (parent != null) {
			if (parent instanceof DatabaseElement) {
				var dbElem = (DatabaseElement) parent;
				var config = dbElem.getContent();
				return Database.isActive(config)
						? Optional.of(Database.get())
						: Optional.empty();
			}
			parent = parent.getParent();
		}
		return Optional.empty();
	}
}
