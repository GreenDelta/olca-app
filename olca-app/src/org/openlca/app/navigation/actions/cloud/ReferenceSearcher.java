package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.references.ExchangeReferenceSearch;
import org.openlca.core.database.references.FlowPropertyFactorReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch.Reference;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class ReferenceSearcher {

	private Set<Long> alreadyChecked = new HashSet<>();
	private Set<Long> initialIds = new HashSet<>();
	private Set<DiffResult> results = new HashSet<>();
	private IDatabase database;
	private DiffIndex index;

	ReferenceSearcher(IDatabase database, DiffIndex index) {
		this.database = database;
		this.index = index;
	}

	List<DiffResult> run(List<DiffResult> toCheck) {
		initialIds.clear();
		for (DiffResult result : toCheck)
			initialIds.add(result.local.localId);
		Map<ModelType, Set<Long>> typeToIds = prepare(toCheck);
		while (!typeToIds.isEmpty())
			typeToIds = search(typeToIds);
		return new ArrayList<>(results);
	}

	private Map<ModelType, Set<Long>> search(Map<ModelType, Set<Long>> toCheck) {
		Map<ModelType, Set<Long>> next = new HashMap<>();
		for (ModelType type : toCheck.keySet()) {
			Set<Long> values = toCheck.get(type);
			Set<CategorizedDescriptor> refs = search(type, values);
			List<Diff> diffs = getChanged(refs);
			for (Diff diff : diffs) {
				results.add(new DiffResult(diff));
				addId(next, type, diff);
			}
		}
		return next;
	}

	private Set<CategorizedDescriptor> search(ModelType type, Set<Long> toCheck) {
		Set<CategorizedDescriptor> results = new HashSet<>();
		IReferenceSearch<?> refSearch = IReferenceSearch.FACTORY.createFor(type, database, true);
		results.addAll(loadDescriptors(refSearch.findReferences(toCheck)));
		IUseSearch<?> useSearch = IUseSearch.FACTORY.createFor(type, database);
		results.addAll(useSearch.findUses(toCheck));
		alreadyChecked.addAll(toCheck);
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
		List<Reference> newRefs = new ArrayList<>();
		for (Class<? extends AbstractEntity> clazz : map.keySet()) {
			ModelType type = ModelType.forModelClass(clazz);
			if (type != null && type.isCategorized()) {
				CategorizedEntityDao<?, ?> dao = Daos.createCategorizedDao(database, type);
				descriptors.addAll(dao.getDescriptors(map.get(clazz)));
			} else if (clazz == FlowPropertyFactor.class) {
				newRefs.addAll(new FlowPropertyFactorReferenceSearch(database).findReferences(map.get(clazz)));
			} else if (clazz == Exchange.class) {
				newRefs.addAll(new ExchangeReferenceSearch(database).findReferences(map.get(clazz)));
			}
		}
		if (!newRefs.isEmpty())
			descriptors.addAll(loadDescriptors(newRefs));
		return descriptors;
	}

	private List<Diff> getChanged(Set<CategorizedDescriptor> refs) {
		List<Diff> relevant = new ArrayList<>();
		for (CategorizedDescriptor d : refs) {
			if (initialIds.contains(d.getId()))
				continue;
			if (alreadyChecked.contains(d.getId()))
				continue;
			Diff diff = index.get(d.getRefId());
			if (!diff.hasChanged())
				continue;
			relevant.add(diff);
		}
		return relevant;
	}

	private Map<ModelType, Set<Long>> prepare(List<DiffResult> toCheck) {
		Map<ModelType, Set<Long>> typeToIds = new HashMap<>();
		for (DiffResult result : toCheck) {
			ModelType type = result.local.getDataset().type;
			addId(typeToIds, type, result.local);
		}
		return typeToIds;
	}

	private void addId(Map<ModelType, Set<Long>> map, ModelType type, Diff diff) {
		Set<Long> ids = map.get(type);
		if (ids == null)
			map.put(type, ids = new HashSet<>());
		ids.add(diff.localId);
	}

}
