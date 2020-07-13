package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.core.library.Library;
import org.openlca.core.model.ModelType;

public class LibraryElement extends NavigationElement<Library> {

	LibraryElement(INavigationElement<?> parent, Library library) {
		super(parent, library);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
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
}
