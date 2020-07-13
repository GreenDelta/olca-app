package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.model.ModelType;

/** Navigation element for databases. */
public class DatabaseElement extends NavigationElement<IDatabaseConfiguration> {

	public DatabaseElement(INavigationElement<?> parent,
			IDatabaseConfiguration config) {
		super(parent, config);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		if (!Database.isActive(getContent()))
			return Collections.emptyList();
		var list = new ArrayList<INavigationElement<?>>();
		list.add(new ModelTypeElement(this, ModelType.PROJECT));
		list.add(new ModelTypeElement(this, ModelType.PRODUCT_SYSTEM));
		list.add(new ModelTypeElement(this, ModelType.PROCESS));
		list.add(new ModelTypeElement(this, ModelType.FLOW));
		list.add(new GroupElement(this, g(M.IndicatorsAndParameters,
				GroupType.INDICATORS,
				ModelType.IMPACT_METHOD,
				ModelType.IMPACT_CATEGORY,
				ModelType.DQ_SYSTEM,
				ModelType.SOCIAL_INDICATOR,
				ModelType.PARAMETER)));
		list.add(new GroupElement(this, g(M.BackgroundData,
				GroupType.BACKGROUND_DATA,
				ModelType.FLOW_PROPERTY,
				ModelType.UNIT_GROUP,
				ModelType.CURRENCY,
				ModelType.ACTOR,
				ModelType.SOURCE,
				ModelType.LOCATION)));

		// add libraries
		var db = Database.get();
		if (db != null) {
			var libs = db.getLibraries();
			if (!libs.isEmpty()) {
				var libDir = Workspace.getLibraryDir();
				var elem = new LibraryDirElement(this, libDir).only(libs);
				list.add(elem);
			}
		}
		return list;
	}

	private Group g(String label, GroupType type, ModelType... types) {
		return new Group(label, type, types);
	}

}
