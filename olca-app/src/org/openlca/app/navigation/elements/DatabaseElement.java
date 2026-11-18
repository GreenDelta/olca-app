package org.openlca.app.navigation.elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.ModelType;
import org.openlca.util.CategoryContentTest;
import org.openlca.util.Dirs;

/**
 * Navigation element for databases.
 */
public class DatabaseElement extends NavigationElement<DatabaseConfig> {

	private CategoryContentTest contentTest;
	private boolean contentChecked;

	public DatabaseElement(INavigationElement<?> parent,
		DatabaseConfig config) {
		super(parent, config);
	}

	/// Returns the test for library content in the categories of this database.
	/// It returns `null` when the database is not active or when it has no
	/// libraries, as there cannot be library content in the categories then. The
	/// callers should handle `null` as "no library content". This avoids loading
	/// the category trees just to find out that there is no library content.
	/// Mounting or removing a library refreshes the full navigation, which
	/// creates new database elements, so we can cache this decision here.
	@Nullable
	public CategoryContentTest categoryContentTest() {
		if (contentChecked)
			return contentTest;
		var db = Database.get();
		if (db == null || !Database.isActive(getContent()))
			return null;
		contentChecked = true;
		contentTest = db.getLibraries().isEmpty()
			? null
			: new CategoryContentTest(db);
		return contentTest;
	}

	static CategoryContentTest categoryTesterOf(INavigationElement<?> elem) {
		if (elem instanceof DatabaseElement dbElem)
			return dbElem.categoryContentTest();
		return elem != null
			? categoryTesterOf(elem.getParent())
			: null;
	}

	@Override
	public void update() {
		super.update();
		// the test is created again (with an empty cache) on the next access
		contentTest = null;
		contentChecked = false;
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
		list.add(new ModelTypeElement(this, ModelType.EPD));
		list.add(new ModelTypeElement(this, ModelType.RESULT));

		list.add(new GroupElement(this, Group.of(M.IndicatorsAndParameters,
			GroupType.INDICATORS,
			ModelType.IMPACT_METHOD,
			ModelType.IMPACT_CATEGORY,
			ModelType.DQ_SYSTEM,
			ModelType.SOCIAL_INDICATOR,
			ModelType.PARAMETER)));
		list.add(new GroupElement(this, Group.of(M.BackgroundData,
			GroupType.BACKGROUND_DATA,
			ModelType.FLOW_PROPERTY,
			ModelType.UNIT_GROUP,
			ModelType.CURRENCY,
			ModelType.ACTOR,
			ModelType.SOURCE,
			ModelType.LOCATION)));

		addLibraryElements(list);
		addScriptElements(list);

		if (FeatureFlag.SD_SIM.isEnabled()) {
			list.add(new SdRootElement(this, Database.get()));
		}
		return list;
	}

	private void addLibraryElements(List<INavigationElement<?>> list) {
		var db = Database.get();
		if (db == null)
			return;
		var libs = db.getLibraries();
		if (!libs.isEmpty()) {
			var libDir = Workspace.getLibraryDir();
			var elem = new LibraryDirElement(this, libDir).only(libs);
			list.add(elem);
		}
	}

	private void addScriptElements(List<INavigationElement<?>> list) {
		var db = Database.get();
		if (db == null)
			return;
		var dir = db.getFileStorageLocation();
		if (dir == null || !dir.exists())
			return;
		var scriptDir = new File(dir, "Scripts");
		if (!scriptDir.exists() || Dirs.isEmpty(scriptDir))
			return;
		list.add(new ScriptElement(this, scriptDir));

	}

}
