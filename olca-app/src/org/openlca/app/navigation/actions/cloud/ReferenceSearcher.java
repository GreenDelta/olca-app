package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.ui.diff.DiffResult;
import org.openlca.core.database.CategorizedEntityDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.references.ExchangeReferenceSearch;
import org.openlca.core.database.references.FlowPropertyFactorReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch.Reference;
import org.openlca.core.database.references.UnitReferenceSearch;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class ReferenceSearcher {

	private Set<Long> alreadyChecked = new HashSet<Long>();
	private List<DiffResult> results = new ArrayList<>();
	private IDatabase database;
	private DiffIndex index;

	ReferenceSearcher(IDatabase database, DiffIndex index) {
		this.database = database;
		this.index = index;
	}

	List<DiffResult> run(List<DiffResult> toCheck) {
		Map<ModelType, Map<DiffType, Set<Long>>> typeToIds = prepare(toCheck);
		while (!typeToIds.isEmpty())
			typeToIds = search(typeToIds);
		return results;
	}

	private Map<ModelType, Map<DiffType, Set<Long>>> search(
			Map<ModelType, Map<DiffType, Set<Long>>> toCheck) {
		Map<ModelType, Map<DiffType, Set<Long>>> next = new HashMap<>();
		for (ModelType type : toCheck.keySet()) {
			Map<DiffType, Set<Long>> values = toCheck.get(type);
			Set<CategorizedDescriptor> refs = search(type, values);
			List<Diff> diffs = getChanged(refs);
			for (Diff diff : diffs) {
				results.add(new DiffResult(diff));
				addId(next, type, diff);
			}
		}
		return next;
	}

	private Set<CategorizedDescriptor> search(ModelType type,
			Map<DiffType, Set<Long>> toCheck) {
		Set<Long> refSearchIds = new HashSet<>();
		Set<Long> useSearchIds = new HashSet<>();
		if (toCheck.containsKey(DiffType.NEW))
			refSearchIds.addAll(toCheck.get(DiffType.NEW));
		Set<CategorizedDescriptor> results = new HashSet<>();
		IReferenceSearch<?> refSearch = IReferenceSearch.FACTORY.createFor(
				type, database, true);
		results.addAll(loadDescriptors(refSearch.findReferences(refSearchIds)));
		alreadyChecked.addAll(refSearchIds);
		IUseSearch<?> useSearch = IUseSearch.FACTORY.createFor(type, database);
		results.addAll(useSearch.findUses(useSearchIds));
		alreadyChecked.addAll(useSearchIds);
		return results;
	}

	private Set<CategorizedDescriptor> loadDescriptors(
			List<Reference> references) {
		Map<Class<? extends AbstractEntity>, Set<Long>> map = new HashMap<>();
		for (Reference reference : references) {
			Set<Long> set = map.get(reference.type);
			if (set == null)
				map.put(reference.type, set = new HashSet<>());
			set.add(reference.id);
		}
		Set<CategorizedDescriptor> descriptors = new HashSet<>();
		List<Reference> newRefs = new ArrayList<>();
		for (Class<? extends AbstractEntity> clazz : map.keySet()) {
			ModelType type = ModelType.forModelClass(clazz);
			if (type != null && type.isCategorized()) {
				CategorizedEntityDao<?, ?> dao = Daos.createCategorizedDao(
						database, type);
				descriptors.addAll(dao.getDescriptors(map.get(clazz)));
			} else if (clazz == Unit.class) {
				newRefs.addAll(new UnitReferenceSearch(database)
						.findReferences(map.get(clazz)));
			} else if (clazz == FlowPropertyFactor.class) {
				newRefs.addAll(new FlowPropertyFactorReferenceSearch(database)
						.findReferences(map.get(clazz)));
			} else if (clazz == Exchange.class) {
				newRefs.addAll(new ExchangeReferenceSearch(database)
						.findReferences(map.get(clazz)));
			}
		}
		if (!newRefs.isEmpty())
			descriptors.addAll(loadDescriptors(newRefs));
		return descriptors;
	}

	private List<Diff> getChanged(Set<CategorizedDescriptor> refs) {
		List<Diff> relevant = new ArrayList<>();
		for (CategorizedDescriptor d : refs) {
			if (alreadyChecked.contains(d.getId()))
				continue;
			Diff diff = index.get(d.getRefId());
			if (!diff.hasChanged())
				continue;
			relevant.add(diff);
		}
		return relevant;
	}

	private Map<ModelType, Map<DiffType, Set<Long>>> prepare(
			List<DiffResult> toCheck) {
		Map<ModelType, Map<DiffType, Set<Long>>> typeToIds = new HashMap<>();
		for (DiffResult result : toCheck) {
			ModelType type = result.local.getDataset().getType();
			addId(typeToIds, type, result.local);
		}
		return typeToIds;
	}

	private void addId(Map<ModelType, Map<DiffType, Set<Long>>> map,
			ModelType type, Diff diff) {
		Map<DiffType, Set<Long>> inner = map.get(type);
		if (inner == null)
			map.put(type, inner = new HashMap<>());
		Set<Long> ids = inner.get(diff.type);
		if (ids == null)
			inner.put(diff.type, ids = new HashSet<>());
		ids.add(diff.localId);

	}

}
