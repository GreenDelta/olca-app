package org.openlca.app.navigation.actions.cloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffUtil;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.ModelType;

class RefIdListBuilder {

	private final List<INavigationElement<?>> selection;
	private final List<DiffResult> changes;
	private final DiffIndex index;
	private final Map<String, INavigationElement<?>> map = new HashMap<>();

	RefIdListBuilder(List<INavigationElement<?>> selection,
			List<DiffResult> changes, DiffIndex index) {
		this.selection = selection;
		this.changes = changes;
		this.index = index;
	}

	Set<String> build() {
		map.clear();
		Set<String> refIds = Navigator.collect(selection, this::unwrap);
		for (DiffResult result : changes)
			if (result.getType() == DiffResponse.DELETE_FROM_REMOTE) {
				if (findExistingParent(result.getDataset()) != null)
					refIds.add(result.getDataset().refId);
			}
		return refIds;
	}

	private String unwrap(INavigationElement<?> element) {
		String id = toId(element);
		if (id == null)
			return null;
		map.put(id, element);
		if (!DiffUtil.hasChanged(element))
			return null;
		if (!(element instanceof ModelElement || element instanceof CategoryElement))
			return null;
		return id;
	}

	private INavigationElement<?> findExistingParent(Dataset dataset) {
		if (dataset.categoryRefId == null)
			if (dataset.type == ModelType.CATEGORY)
				return map.get(dataset.categoryType.name());
			else
				return map.get(dataset.type.name());
		Dataset parent = index.get(dataset.categoryRefId).getDataset();
		if (map.containsKey(parent.refId))
			return map.get(parent.refId);
		return findExistingParent(parent);
	}

	private String toId(INavigationElement<?> element) {
		if (element instanceof ModelElement)
			return ((ModelElement) element).getContent().refId;
		if (element instanceof CategoryElement)
			return ((CategoryElement) element).getContent().refId;
		if (element instanceof ModelTypeElement)
			return ((ModelTypeElement) element).getContent().name();
		return null;
	}

}
