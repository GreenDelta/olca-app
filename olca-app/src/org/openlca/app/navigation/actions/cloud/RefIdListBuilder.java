package org.openlca.app.navigation.actions.cloud;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffUtil;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.cloud.ui.diff.DiffResult.DiffResponse;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.core.model.Category;
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
		Set<String> refIds = new HashSet<>();
		for (INavigationElement<?> element : selection)
			refIds.addAll(toRefIdList(element));
		for (DiffResult result : changes)
			if (result.getType() == DiffResponse.DELETE_FROM_REMOTE) {
				if (findExistingParent(result.getDataset()) != null)
					refIds.add(result.getDataset().getRefId());
			}
		return refIds;
	}

	private INavigationElement<?> findExistingParent(Dataset dataset) {
		if (dataset.getCategoryRefId() == null)
			if (dataset.getType() == ModelType.CATEGORY)
				return map.get(dataset.getCategoryType().name());
			else
				return map.get(dataset.getType().name());
		Dataset parent = index.get(dataset.getCategoryRefId()).getDataset();
		if (map.containsKey(parent.getRefId()))
			return map.get(parent.getRefId());
		return findExistingParent(parent);
	}

	private Set<String> toRefIdList(INavigationElement<?> element) {
		if (!DiffUtil.hasChanged(element))
			return Collections.emptySet();
		String id = toId(element);
		if (id != null)
			map.put(id, element);
		Set<String> refIds = new HashSet<>();
		if (element instanceof CategoryElement) {
			Category category = ((CategoryElement) element).getContent();
			Diff diff = DiffUtil.getDiff(CloudUtil.toDataset(category));
			if (diff.hasChanged())
				refIds.add(category.getRefId());
		}
		if (element instanceof ModelElement)
			refIds.add(((ModelElement) element).getContent().getRefId());
		for (INavigationElement<?> child : element.getChildren())
			refIds.addAll(toRefIdList(child));
		return refIds;
	}

	private String toId(INavigationElement<?> element) {
		if (element instanceof ModelElement)
			return ((ModelElement) element).getContent().getRefId();
		if (element instanceof CategoryElement)
			return ((CategoryElement) element).getContent().getRefId();
		if (element instanceof ModelTypeElement)
			return ((ModelTypeElement) element).getContent().name();
		return null;
	}

}
