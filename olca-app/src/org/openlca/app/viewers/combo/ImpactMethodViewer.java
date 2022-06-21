package org.openlca.app.viewers.combo;

import java.util.HashSet;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

public class ImpactMethodViewer extends
		AbstractComboViewer<ImpactMethodDescriptor> {

	private LabelProvider _label;
	private Boolean _withCategories;

	public ImpactMethodViewer(Composite parent) {
		super(parent);
		setInput(new ImpactMethodDescriptor[0]);
		var combo = getViewer().getTableCombo();
		combo.setShowTableHeader(false);
		// we only show the category column when there
		// are impact methods in different categories
		// in the database.
		if (!withCategories()) {
			combo.setShowTableLines(false);
		}
	}

	private boolean withCategories() {
		if (_withCategories != null)
			return _withCategories;
		var db = Database.get();
		if (db == null) {
			_withCategories = false;
			return false;
		}
		String sql = "select f_category from tbl_impact_methods";
		var ids = new HashSet<Long>();
		NativeSql.on(db).query(sql, r -> {
			var catID = r.getLong(1);
			ids.add(r.wasNull() ? 0L : catID);
			return true;
		});
		_withCategories = ids.size() > 1;
		return _withCategories;
	}

	@Override
	protected int getDisplayColumn() {
		return 0;
	}

	@Override
	protected String[] getColumnHeaders() {
		return withCategories()
				? new String[] { M.Name, M.Category }
				: new String[] { M.Name };
	}

	public void setInput(IDatabase db) {
		try {
			var dao = new ImpactMethodDao(db);
			var descriptors = dao.getDescriptors();
			descriptors.sort((m1, m2) -> Strings.compare(m1.name, m2.name));
			setInput(descriptors.toArray(new ImpactMethodDescriptor[0]));
		} catch (Exception e) {
			ErrorReporter.on("Failed to load impact method descriptors", e);
		}
	}

	@Override
	public Class<ImpactMethodDescriptor> getType() {
		return ImpactMethodDescriptor.class;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		if (_label == null) {
			_label = new LabelProvider();
		}
		return _label;
	}

	private static class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof ImpactMethodDescriptor method))
				return null;
			if (col == 0)
				return Images.get(method);
			if (col == 1 && method.category != null)
				return Images.getForCategory(ModelType.IMPACT_METHOD);
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ImpactMethodDescriptor method))
				return null;
			if (col == 0)
				return Labels.name(method);
			if (col != 1 || method.category == null)
				return null;
			var cache = Cache.getEntityCache();
			var category = cache.get(Category.class, method.category);
			return category == null
					? null
					: CategoryPath.getFull(category);
		}
	}

}
