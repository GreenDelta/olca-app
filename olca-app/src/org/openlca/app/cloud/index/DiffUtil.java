package org.openlca.app.cloud.index;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;

public class DiffUtil {

	public static boolean hasChanged(INavigationElement<?> element) {
		if (element instanceof CategoryElement)
			return hasChanged(CloudUtil.toDataset(element));
		if (element instanceof ModelElement)
			return hasChanged(CloudUtil.toDataset(element));
		if (element instanceof ModelTypeElement)
			return hasChanged(((ModelTypeElement) element).getContent());
		for (INavigationElement<?> child : element.getChildren())
			if (hasChanged(child))
				return true;
		return false;
	}

	private static boolean hasChanged(Dataset dataset) {
		Diff diff = getDiff(dataset);
		if (diff == null)
			return false;
		return diff.hasChanged() || diff.childrenHaveChanged();
	}

	private static boolean hasChanged(ModelType type) {
		DiffIndex index = Database.getDiffIndex();
		if (index == null )
			return false;
		return index.hasChanged(type);
	}

	public static Diff getDiff(Dataset dataset) {
		DiffIndex index = Database.getDiffIndex();
		if (index == null)
			return null;
		return index.get(dataset);
	}

}
