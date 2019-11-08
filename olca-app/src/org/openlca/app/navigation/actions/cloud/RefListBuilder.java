package org.openlca.app.navigation.actions.cloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.index.DiffUtil;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.cloud.model.data.Dataset;
import org.openlca.cloud.model.data.FileReference;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class RefListBuilder {

	private final List<INavigationElement<?>> selection;
	private final List<DiffResult> changes;
	private final DiffIndex index;
	private final Map<FileReference, INavigationElement<?>> map = new HashMap<>();

	RefListBuilder(List<INavigationElement<?>> selection,
			List<DiffResult> changes, DiffIndex index) {
		this.selection = selection;
		this.changes = changes;
		this.index = index;
	}

	Set<FileReference> build() {
		map.clear();
		Set<FileReference> refs = Navigator.collect(selection, this::unwrap);
		for (DiffResult result : changes) {
			if (!deleteFromRemote(result))
				continue;
			if (findExistingParent(result.getDataset()) == null)
				continue;
			refs.add(result.getDataset().asFileReference());
		}
		return refs;
	}

	private boolean deleteFromRemote(DiffResult result) {
		if (result.local.type != DiffType.DELETED)
			return false;
		if (result.remote.isDeleted())
			return false;
		return true;
	}

	private FileReference unwrap(INavigationElement<?> element) {
		FileReference ref = toRef(element);
		if (ref == null)
			return null;
		map.put(ref, element);
		if (!DiffUtil.hasChanged(element))
			return null;
		if (!(element instanceof ModelElement || element instanceof CategoryElement))
			return null;
		return ref;
	}

	private INavigationElement<?> findExistingParent(Dataset d) {
		if (d.categoryRefId == null) {
			if (d.type == ModelType.CATEGORY)
				return map.get(FileReference.from(ModelType.CATEGORY, d.categoryType.name()));
			return map.get(FileReference.from(ModelType.CATEGORY, d.type.name()));
		}
		Dataset parent = index.get(FileReference.from(ModelType.CATEGORY, d.categoryRefId)).getDataset();
		if (map.containsKey(parent.asFileReference()))
			return map.get(parent.asFileReference());
		return findExistingParent(parent);
	}

	private FileReference toRef(INavigationElement<?> element) {
		if (element instanceof ModelElement) {
			CategorizedDescriptor d = ((ModelElement) element).getContent();
			return FileReference.from(d.type, d.refId);
		}
		if (element instanceof CategoryElement) {
			Category c = ((CategoryElement) element).getContent();
			return FileReference.from(ModelType.CATEGORY, c.refId);
		}
		if (element instanceof ModelTypeElement) {
			ModelType t = ((ModelTypeElement) element).getContent();
			return FileReference.from(ModelType.CATEGORY, t.name());
		}
		return null;
	}

}
