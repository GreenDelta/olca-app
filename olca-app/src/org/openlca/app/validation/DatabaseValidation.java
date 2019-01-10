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
import org.openlca.core.database.Daos;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.database.references.IReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch.Reference;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class DatabaseValidation {

	private IProgressMonitor monitor;
	private FlowChainValidation flowChainValidation;

	public static DatabaseValidation with(IProgressMonitor monitor) {
		DatabaseValidation e = new DatabaseValidation();
		e.monitor = monitor;
		return e;
	}

	public List<ModelStatus> evaluate(Collection<CategorizedDescriptor> descriptors) {
		Map<ModelType, Set<Long>> byType = new HashMap<>();
		for (CategorizedDescriptor descriptor : descriptors) {
			Set<Long> forType = byType.get(descriptor.type);
			if (forType == null) {
				byType.put(descriptor.type, forType = new HashSet<>());
			}
			forType.add(descriptor.id);
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
		if (monitor != null && monitor.isCanceled())
			return new ArrayList<>();
		List<Reference> references = findReferences(type, ids);
		if (monitor != null && monitor.isCanceled())
			return new ArrayList<>();
		if (monitor != null)
			monitor.worked(2);
		Set<Reference> notExisting = checkExistence(references);
		if (monitor != null && monitor.isCanceled())
			return new ArrayList<>();
		if (type == ModelType.PROCESS || type == ModelType.IMPACT_METHOD) {
			if (flowChainValidation == null) {
				flowChainValidation = new FlowChainValidation(Database.get());
			}
			notExisting.addAll(flowChainValidation.run(type.getModelClass(), references));
		}
		Map<Long, Boolean> referenceSet = checkReferenceSet(type, ids);
		if (monitor != null && monitor.isCanceled())
			return new ArrayList<>();
		List<ModelStatus> result = new ArrayList<>();
		for (Long id : ids) {
			if (monitor != null && monitor.isCanceled())
				continue;
			boolean validReferenceSet = referenceSet == null || referenceSet.get(id);
			List<Reference> missing = filter(notExisting, id);
			ModelStatus status = new ModelStatus(type, id, new ArrayList<>(missing), validReferenceSet);
			result.add(status);
		}
		if (monitor != null && !monitor.isCanceled())
			monitor.worked(1);
		return result;
	}

	private Map<Long, Boolean> checkReferenceSet(ModelType type, Set<Long> ids) {
		switch (type) {
		case PRODUCT_SYSTEM:
			return new ProductSystemDao(Database.get()).hasReferenceProcess(ids);
		case PROCESS:
			return new ProcessDao(Database.get()).hasQuantitativeReference(ids);
		case FLOW:
			return new FlowDao(Database.get()).hasReferenceFactor(ids);
		case UNIT_GROUP:
			return new UnitGroupDao(Database.get()).hasReferenceUnit(ids);
		default:
			return null;
		}
	}

	private List<Reference> filter(Collection<Reference> references, long ownerId) {
		List<Reference> filtered = new ArrayList<>();
		for (Reference ref : references)
			if (ref.ownerId == ownerId)
				filtered.add(ref);
		return filtered;
	}

	private Set<Reference> checkExistence(List<Reference> references) {
		Set<Reference> notExisting = new HashSet<>();
		Map<Class<? extends AbstractEntity>, Map<Long, List<Reference>>> byType = splitByType(references);
		for (Class<? extends AbstractEntity> type : byType.keySet()) {
			if (monitor != null && monitor.isCanceled())
				continue;
			Map<Long, List<Reference>> refMap = byType.get(type);
			Collection<List<Reference>> values = refMap.values();
			Set<Long> ids = toIdSet(values);
			Map<Long, Boolean> map = Daos.base(Database.get(), type).contains(ids);
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
		return IReferenceSearch.FACTORY.createFor(type, Database.get(), true).findReferences(ids);
	}

}
