package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.references.IReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch.Reference;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class ReferenceSearcher {

	private Set<Long> allChanged = new HashSet<>();
	private Map<Long, String> idToRefId = new HashMap<>();
	private Set<DiffResult> results = new HashSet<>();
	private IDatabase database;
	private DiffIndex index;
	private Set<Long> alreadySearchedReferences = new HashSet<>();
	private Set<Long> alreadySearchedUsages = new HashSet<>();

	ReferenceSearcher(IDatabase database, DiffIndex index) {
		this.database = database;
		this.index = index;
	}

	List<DiffResult> run(List<DiffResult> toCheck) {
		allChanged.clear();
		int zeroCount = 0;
		for (DiffResult result : toCheck)
			if (result.local.localId != 0)
				allChanged.add(result.local.localId);
			else
				zeroCount++;
		if (allChanged.size() + zeroCount == index.getChanged().size())
			return Collections.emptyList();
		Map<ModelType, Set<Long>> typeToIds = prepareFromResults(toCheck);
		search(typeToIds);
		return new ArrayList<>(results);
	}

	private void search(Map<ModelType, Set<Long>> toCheck) {
		Set<CategorizedDescriptor> allFound = new HashSet<>();
		for (ModelType type : toCheck.keySet()) {
			Set<Long> values = toCheck.get(type);
			Set<CategorizedDescriptor> refs = search(type, values);
			allFound.addAll(refs);
			List<Diff> diffs = getChanged(refs);
			for (Diff diff : diffs) {
				DiffResult diffResult = new DiffResult(diff);
				if (diffResult.local.localId == 0)
					continue;
				diffResult.ignoreRemote = true;
				allChanged.add(diffResult.local.localId);
				results.add(diffResult);
			}
		}
		if (allFound.isEmpty() || allChanged.size() == index.getChanged().size())
			return;
		Map<ModelType, Set<Long>> next = prepareFromDescriptors(allFound);
		search(next);
	}

	private Set<CategorizedDescriptor> search(ModelType type, Set<Long> toCheck) {
		Set<CategorizedDescriptor> results = searchReferences(type, toCheck);
		results.addAll(searchUsage(type, toCheck));
		return results;
	}

	private Set<CategorizedDescriptor> searchReferences(ModelType type, Set<Long> toCheck) {
		Set<CategorizedDescriptor> results = new HashSet<>();
		for (Long id : new HashSet<>(toCheck)) {
			if (alreadySearchedReferences.contains(id)) {
				toCheck.remove(id);
				continue;
			}
			alreadySearchedReferences.add(id);
		}
		if (toCheck.isEmpty())
			return Collections.emptySet();
		IReferenceSearch<?> refSearch = IReferenceSearch.FACTORY.createFor(type, database, true);
		results.addAll(loadDescriptors(refSearch.findReferences(toCheck)));
		return results;
	}

	private Set<CategorizedDescriptor> searchUsage(ModelType type, Set<Long> toCheck) {
		for (Long id : new HashSet<>(toCheck)) {
			if (alreadySearchedUsages.contains(id)) {
				toCheck.remove(id);
				continue;
			}
			alreadySearchedUsages.add(id);
			Diff diff = index.get(idToRefId.get(id));
			if (diff == null || diff.type != DiffType.CHANGED)
				continue;
			toCheck.remove(id);
		}
		Set<CategorizedDescriptor> results = new HashSet<>();
		if (toCheck.isEmpty())
			return results;
		IUseSearch<?> useSearch = IUseSearch.FACTORY.createFor(type, database);
		List<CategorizedDescriptor> usedIn = useSearch.findUses(toCheck);
		for (CategorizedDescriptor descriptor : usedIn) {
			Diff diff = index.get(descriptor.refId);
			if (diff == null || diff.type == DiffType.NO_DIFF || diff.type == DiffType.NEW)
				continue;
			results.add(descriptor);
		}
		return results;
	}

	private Set<CategorizedDescriptor> loadDescriptors(List<Reference> references) {
		Map<Class<? extends AbstractEntity>, Set<Long>> map = new HashMap<>();
		for (Reference reference : references) {
			Set<Long> set = map.get(reference.getType());
			if (set == null)
				map.put(reference.getType(), set = new HashSet<>());
			set.add(reference.id);
		}
		Set<CategorizedDescriptor> descriptors = new HashSet<>();
		for (Class<? extends AbstractEntity> clazz : map.keySet()) {
			ModelType type = ModelType.forModelClass(clazz);
			if (type != null && type.isCategorized()) {
				descriptors.addAll(Daos.categorized(database, type).getDescriptors(map.get(clazz)));
			}
		}
		for (CategorizedDescriptor descriptor : descriptors)
			idToRefId.put(descriptor.id, descriptor.refId);
		return descriptors;
	}

	private List<Diff> getChanged(Set<CategorizedDescriptor> refs) {
		List<Diff> relevant = new ArrayList<>();
		for (CategorizedDescriptor d : refs) {
			if (allChanged.contains(d.id))
				continue;
			Diff diff = index.get(d.refId);
			if (diff == null || !diff.hasChanged())
				continue;
			relevant.add(diff);
		}
		return relevant;
	}

	private Map<ModelType, Set<Long>> prepareFromResults(List<DiffResult> toCheck) {
		Map<ModelType, Set<Long>> typeToIds = new HashMap<>();
		for (DiffResult result : toCheck) {
			if (result.local.localId == 0)
				continue;
			ModelType type = result.local.getDataset().type;
			addId(typeToIds, type, result.local.localId);
			idToRefId.put(result.local.localId, result.local.getDataset().refId);
		}
		return typeToIds;
	}

	private Map<ModelType, Set<Long>> prepareFromDescriptors(Set<CategorizedDescriptor> toCheck) {
		Map<ModelType, Set<Long>> typeToIds = new HashMap<>();
		for (CategorizedDescriptor descriptor : toCheck) {
			addId(typeToIds, descriptor.type, descriptor.id);
			idToRefId.put(descriptor.id, descriptor.refId);
		}
		return typeToIds;
	}

	private void addId(Map<ModelType, Set<Long>> map, ModelType type, long id) {
		Set<Long> ids = map.get(type);
		if (ids == null)
			map.put(type, ids = new HashSet<>());
		ids.add(id);
	}
}
