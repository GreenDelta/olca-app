package org.openlca.app.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.Daos;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.database.references.ExchangeReferenceSearch;
import org.openlca.core.database.references.FlowPropertyFactorReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch.Reference;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class DatabaseValidation {

	private IProgressMonitor monitor;
	private static final List<Class<? extends AbstractEntity>> nesting = new ArrayList<>();
	private static final Map<Class<? extends AbstractEntity>, BaseDao<?>> daos = new HashMap<>();

	static {
		nesting.add(FlowPropertyFactor.class);
		nesting.add(Exchange.class);
	}

	public static DatabaseValidation with(IProgressMonitor monitor) {
		DatabaseValidation e = new DatabaseValidation();
		e.monitor = monitor;
		return e;
	}

	public List<ModelStatus> evaluate(Collection<CategorizedDescriptor> descriptors) {
		Map<ModelType, Set<Long>> byType = new HashMap<>();
		for (CategorizedDescriptor descriptor : descriptors) {
			Set<Long> forType = byType.get(descriptor.getModelType());
			if (forType == null) {
				byType.put(descriptor.getModelType(), forType = new HashSet<>());
			}
			forType.add(descriptor.getId());
		}
		List<ModelStatus> result = new ArrayList<>();
		if (monitor != null && !monitor.isCanceled())
			monitor.beginTask(M.ValidatingDatabase, byType.size() * 3);
		for (ModelType type : byType.keySet()) {
			if (monitor != null && monitor.isCanceled())
				continue;
			if (monitor != null)
				monitor.subTask(Labels.modelType(type));
			result.addAll(evaluate(type, byType.get(type)));
		}
		if (monitor != null)
			monitor.done();
		return result;
	}

	private List<ModelStatus> evaluate(ModelType type, Set<Long> ids) {
		List<ModelStatus> result = new ArrayList<>();
		if (monitor != null && monitor.isCanceled())
			return result;
		List<Reference> references = findReferences(type, ids);
		if (monitor != null && monitor.isCanceled())
			return result;
		if (monitor != null)
			monitor.worked(2);
		List<Reference> notExisting = checkExistence(references);
		if (monitor != null && monitor.isCanceled())
			return result;
		Map<Long, Boolean> referenceSet = checkReferenceSet(type, ids);
		if (monitor != null && monitor.isCanceled())
			return result;
		for (Long id : ids) {
			if (monitor != null && monitor.isCanceled())
				continue;
			boolean validReferenceSet = referenceSet == null || referenceSet.get(id);
			ModelStatus status = new ModelStatus(type, id, filter(notExisting, id), validReferenceSet);
			result.add(status);
		}
		if (monitor != null && !monitor.isCanceled())
			monitor.worked(1);
		return result;
	}

	private Map<Long, Boolean> checkReferenceSet(ModelType type, Set<Long> ids) {
		switch (type) {
		case PRODUCT_SYSTEM:
			return ((ProductSystemDao) getDao(ProductSystem.class)).hasReferenceProcess(ids);
		case PROCESS:
			return ((ProcessDao) getDao(Process.class)).hasQuantitativeReference(ids);
		case FLOW:
			return ((FlowDao) getDao(Flow.class)).hasReferenceFactor(ids);
		case UNIT_GROUP:
			return ((UnitGroupDao) getDao(UnitGroup.class)).hasReferenceUnit(ids);
		default:
			return null;
		}
	}

	private List<Reference> evalNested(List<Reference> references) {
		List<Reference> result = new ArrayList<>();
		result.addAll(references);
		Map<Class<? extends AbstractEntity>, List<Reference>> nested = new HashMap<>();
		for (Reference ref : references) {
			if (monitor != null && monitor.isCanceled())
				continue;
			if (!nesting.contains(ref.getType()))
				continue;
			List<Reference> ids = nested.get(ref.getType());
			if (ids == null)
				nested.put(ref.getType(), ids = new ArrayList<>());
			ids.add(ref);
		}
		for (Class<? extends AbstractEntity> nestingType : nesting) {
			if (monitor != null && monitor.isCanceled())
				continue;
			if (!nested.containsKey(nestingType))
				continue;
			List<Reference> nestedRefs = findReferences(nestingType, nested.get(nestingType));
			result.addAll(nestedRefs);
		}
		return result;
	}

	private List<Reference> filter(List<Reference> references, long ownerId) {
		List<Reference> filtered = new ArrayList<>();
		for (Reference ref : references)
			if (ref.ownerId == ownerId)
				filtered.add(ref);
		return filtered;
	}

	private List<Reference> checkExistence(List<Reference> references) {
		List<Reference> notExisting = new ArrayList<>();
		Map<Class<? extends AbstractEntity>, Map<Long, List<Reference>>> byType = splitByType(references);
		for (Class<? extends AbstractEntity> type : byType.keySet()) {
			if (monitor != null && monitor.isCanceled())
				continue;
			Map<Long, List<Reference>> refMap = byType.get(type);
			Collection<List<Reference>> values = refMap.values();
			Set<Long> ids = toIdSet(values);
			Map<Long, Boolean> map = getDao(type).contains(ids);
			for (Long id : map.keySet())
				if (map.get(id) == false)
					notExisting.addAll(refMap.get(id));
		}
		for (Reference ref : references)
			if (!ref.optional && ref.id == 0l)
				notExisting.add(ref);
		return notExisting;
	}

	private Map<Class<? extends AbstractEntity>, Map<Long, List<Reference>>> splitByType(List<Reference> references) {
		Map<Class<? extends AbstractEntity>, Map<Long, List<Reference>>> byType = new HashMap<>();
		for (Reference reference : references) {
			Map<Long, List<Reference>> forType = byType.get(reference.getType());
			if (forType == null)
				byType.put(reference.getType(), forType = new HashMap<>());
			add(reference, forType);
		}
		return byType;
	}

	private void add(Reference reference, Map<Long, List<Reference>> map) {
		List<Reference> list = map.get(reference.id);
		if (list == null)
			map.put(reference.id, list = new ArrayList<>());
		list.add(reference);
	}

	private Set<Long> toIdSet(Collection<List<Reference>> references) {
		Set<Long> ids = new HashSet<>();
		for (List<Reference> referenceList : references)
			for (Reference reference : referenceList)
				if (reference.id != 0l)
					ids.add(reference.id);
		return ids;
	}

	private List<Reference> findReferences(ModelType type, Set<Long> ids) {
		List<Reference> refs = IReferenceSearch.FACTORY.createFor(type, Database.get(), true).findReferences(ids);
		return evalNested(refs);
	}

	private List<Reference> findReferences(Class<? extends AbstractEntity> type, List<Reference> refs) {
		List<Reference> nestedRefs = null;
		Map<Long, Class<? extends AbstractEntity>> ownerTypes = new HashMap<>();
		Map<Long, Long> ownerIds = new HashMap<>();
		Set<Long> ids = new HashSet<>();
		for (Reference ref : refs) {
			ids.add(ref.id);
			ownerIds.put(ref.id, ref.ownerId);
			ownerTypes.put(ref.id, ref.getOwnerType());
		}
		if (type == FlowPropertyFactor.class)
			nestedRefs = new FlowPropertyFactorReferenceSearch(Database.get(),
					ownerTypes, ownerIds).findReferences(ids);
		else if (type == Exchange.class)
			nestedRefs = new ExchangeReferenceSearch(Database.get(),
					ownerTypes, ownerIds).findReferences(ids);
		return evalNested(nestedRefs);
	}

	@SuppressWarnings("unchecked")
	private <T extends AbstractEntity> BaseDao<T> getDao(Class<T> type) {
		if (!daos.containsKey(type)) {
			daos.put(type, Daos.base(Database.get(), type));
		}
		return (BaseDao<T>) daos.get(type);
	}

}
