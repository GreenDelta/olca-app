package org.openlca.app.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.database.Daos;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.database.references.ExchangeReferenceSearch;
import org.openlca.core.database.references.FlowPropertyFactorReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch;
import org.openlca.core.database.references.IReferenceSearch.Reference;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

public class DatabaseValidation {

	private IProgressMonitor monitor;
	private static final List<Class<? extends AbstractEntity>> nesting = new ArrayList<>();

	static {
		nesting.add(FlowPropertyFactor.class);
		nesting.add(Exchange.class);
	}

	public static DatabaseValidation with(IProgressMonitor monitor) {
		DatabaseValidation e = new DatabaseValidation();
		e.monitor = monitor;
		return e;
	}

	public List<ModelStatus> evaluateAll() {
		IDatabase db = Database.get();
		if (db == null)
			return Collections.emptyList();
		Map<ModelType, Set<Long>> toEval = new HashMap<>();
		if (monitor != null)
			monitor.beginTask("#Preparing", IProgressMonitor.UNKNOWN);
		for (ModelType type : ModelType.values()) {
			if (!type.isCategorized())
				continue;
			Set<Long> ids = getAll(type);
			if (ids.isEmpty())
				continue;
			toEval.put(type, ids);
		}
		if (monitor != null)
			monitor.beginTask("#Indexing database", toEval.size() * 3);
		List<ModelStatus> result = new ArrayList<>();
		for (ModelType type : toEval.keySet()) {
			if (monitor != null)
				monitor.subTask(Labels.modelType(type));
			result.addAll(eval(type, toEval.get(type)));
		}
		if (monitor != null)
			monitor.done();
		return result;
	}

	private Set<Long> getAll(ModelType type) {
		Set<Long> ids = new HashSet<>();
		for (CategorizedDescriptor d : loadDescriptors(type))
			ids.add(d.getId());
		return ids;
	}

	private List<? extends CategorizedDescriptor> loadDescriptors(ModelType type) {
		IDatabase db = Database.get();
		if (type == ModelType.PARAMETER)
			return new ParameterDao(db).getGlobalDescriptors();
		return Daos.createCategorizedDao(db, type).getDescriptors();
	}

	public ModelStatus evaluate(ModelType type, long id) {
		IDatabase db = Database.get();
		if (db == null)
			return null;
		if (monitor != null)
			monitor.beginTask("Indexing model", IProgressMonitor.UNKNOWN);
		ModelStatus result = eval(type, Collections.singleton(id)).get(0);
		if (monitor != null)
			monitor.done();
		return result;
	}

	public List<ModelStatus> evaluate(ModelType type) {
		IDatabase db = Database.get();
		if (db == null)
			return Collections.emptyList();
		if (monitor != null)
			monitor.beginTask("Indexing model", IProgressMonitor.UNKNOWN);
		Set<Long> ids = getAll(type);
		List<ModelStatus> result = eval(type, ids);
		if (monitor != null)
			monitor.done();
		return result;
	}

	private List<ModelStatus> eval(ModelType type, Set<Long> ids) {
		List<ModelStatus> result = new ArrayList<>();
		List<Reference> references = findReferences(type, ids);
		if (monitor != null)
			monitor.worked(2);
		List<Reference> notExisting = checkExistence(references);
		Map<Long, Boolean> referenceSet = checkReferenceSet(type, ids);
		for (Long id : ids) {
			boolean validReferenceSet = referenceSet == null || referenceSet.get(id);
			ModelStatus status = new ModelStatus(type, id, filter(notExisting, id), validReferenceSet);
			result.add(status);
		}
		if (monitor != null)
			monitor.worked(1);
		return result;
	}

	private Map<Long, Boolean> checkReferenceSet(ModelType type, Set<Long> ids) {
		switch (type) {
		case PRODUCT_SYSTEM:
			return new ProductSystemDao(Database.get())
					.hasReferenceProcess(ids);
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

	private List<Reference> evalNested(List<Reference> references) {
		List<Reference> result = new ArrayList<>();
		result.addAll(references);
		Map<Class<? extends AbstractEntity>, List<Reference>> nested = new HashMap<>();
		for (Reference ref : references) {
			if (!nesting.contains(ref.getType()))
				continue;
			List<Reference> ids = nested.get(ref.getType());
			if (ids == null)
				nested.put(ref.getType(), ids = new ArrayList<>());
			ids.add(ref);
		}
		for (Class<? extends AbstractEntity> nestingType : nesting)
			if (nested.containsKey(nestingType)) {
				List<Reference> nestedRefs = findReferences(nestingType,
						nested.get(nestingType));
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
		IDatabase db = Database.get();
		List<Reference> notExisting = new ArrayList<>();
		Map<Class<? extends AbstractEntity>, Map<Long, Reference>> byType = splitByType(references);
		for (Class<? extends AbstractEntity> type : byType.keySet()) {
			Map<Long, Reference> refMap = byType.get(type);
			Collection<Reference> values = refMap.values();
			Set<Long> ids = toIdSet(values);
			Map<Long, Boolean> map = Daos.createBaseDao(db, type).contains(ids);
			for (Long id : map.keySet())
				if (map.get(id) == false)
					notExisting.add(refMap.get(id));
		}
		for (Reference ref : references)
			if (!ref.optional && ref.id == 0l)
				notExisting.add(ref);
		return notExisting;
	}

	private Map<Class<? extends AbstractEntity>, Map<Long, Reference>> splitByType(
			List<Reference> references) {
		Map<Class<? extends AbstractEntity>, Map<Long, Reference>> byType = new HashMap<>();
		for (Reference reference : references) {
			Map<Long, Reference> forType = byType.get(reference.getType());
			if (forType == null)
				byType.put(reference.getType(), forType = new HashMap<>());
			forType.put(reference.id, reference);
		}
		return byType;
	}

	private Set<Long> toIdSet(Collection<Reference> references) {
		Set<Long> ids = new HashSet<>();
		for (Reference reference : references)
			if (reference.id != 0l)
				ids.add(reference.id);
		return ids;
	}

	private List<Reference> findReferences(ModelType type, Set<Long> ids) {
		IDatabase db = Database.get();
		List<Reference> refs = IReferenceSearch.FACTORY.createFor(type, db,
				true).findReferences(ids);
		return evalNested(refs);
	}

	private List<Reference> findReferences(
			Class<? extends AbstractEntity> type, List<Reference> refs) {
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
}
