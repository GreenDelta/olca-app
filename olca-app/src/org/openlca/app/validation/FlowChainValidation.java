package org.openlca.app.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.references.IReferenceSearch.Reference;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Process;
import org.openlca.core.model.Unit;

class FlowChainValidation {

	// flow.id -> flowPropertyFactor.id
	private final Map<Long, Set<Long>> factors = new HashMap<>();
	// flowProperty.id -> unit.id
	private final Map<Long, Set<Long>> units = new HashMap<>();

	FlowChainValidation(IDatabase database) {
		for (Flow flow : new FlowDao(database).getAll()) {
			Set<Long> factors = get(this.factors, flow.id);
			for (FlowPropertyFactor factor : flow.flowPropertyFactors) {
				factors.add(factor.id);
				if (this.units.containsKey(factor.flowProperty.id))
					continue;
				Set<Long> units = get(this.units, factor.flowProperty.id);
				for (Unit unit : factor.flowProperty.unitGroup.units) {
					units.add(unit.id);
				}
			}
		}
	}

	private Set<Long> get(Map<Long, Set<Long>> map, long id) {
		Set<Long> values = map.get(id);
		if (values == null) {
			map.put(id, values = new HashSet<>());
		}
		return values;
	}

	private void put(Map<Long, Set<Long>> map, long id, long value) {
		Set<Long> values = map.get(id);
		if (values == null) {
			map.put(id, values = new HashSet<>());
		}
		values.add(value);
	}

	List<Reference> run(Class<? extends AbstractEntity> ownerType, Collection<Reference> references) {
		List<FlowRefs> flowRefs = getRefs(ownerType, references);
		List<Reference> broken = new ArrayList<>();
		for (FlowRefs refs : flowRefs) {
			if (refs.flowId == 0l || refs.factorId == 0l)
				continue;
			if (!factors.containsKey(refs.flowId) || !factors.get(refs.flowId).contains(refs.factorId)) {
				broken.add(new Reference("flowPropertyFactor", FlowPropertyFactor.class, refs.factorId, ownerType,
						refs.ownerId, refs.nestedProperty, refs.nestedOwnerType, refs.nestedOwnerId, false));
			}
			if (refs.factorId == 0l || refs.propertyId == 0l || refs.unitId == 0l)
				continue;
			if (!units.containsKey(refs.propertyId) || !units.get(refs.propertyId).contains(refs.unitId)) {
				broken.add(new Reference("unit", Unit.class, refs.unitId, ownerType,
						refs.ownerId, refs.nestedProperty, refs.nestedOwnerType, refs.nestedOwnerId, false));
			}
		}
		return broken;
	}

	private List<FlowRefs> getRefs(Class<? extends AbstractEntity> ownerType, Collection<Reference> references) {
		Class<? extends AbstractEntity> nestedType = null;
		String nestedProperty = null;
		if (ownerType == Process.class) {
			nestedType = Exchange.class;
			nestedProperty = "exchanges";
		} else if (ownerType == ImpactMethod.class) {
			nestedType = ImpactFactor.class;
			nestedProperty = "impactFactors";
		}
		Map<Long, FlowRefs> flowRefs = new HashMap<>();
		Map<Long, Set<Long>> factorToOwner = new HashMap<>();
		Map<Long, Long> factorToProperty = new HashMap<>();
		for (Reference ref : references) {
			if (ref.property.equals("flowProperty")
					&& ref.nestedOwnerType.equals(FlowPropertyFactor.class.getCanonicalName())) {
				factorToProperty.put(ref.nestedOwnerId, ref.id);
				continue;
			}
			if (ref.nestedOwnerType == null || !ref.nestedOwnerType.equals(nestedType.getCanonicalName()))
				continue;
			FlowRefs flowRef = flowRefs.get(ref.nestedOwnerId);
			if (flowRef == null) {
				flowRef = new FlowRefs();
				flowRef.ownerId = ref.ownerId;
				flowRef.nestedOwnerId = ref.nestedOwnerId;
				flowRef.nestedOwnerType = nestedType;
				flowRef.nestedProperty = nestedProperty;
				flowRefs.put(ref.nestedOwnerId, flowRef);
			}
			if (ref.property.equals("flow")) {
				flowRef.flowId = ref.id;
			} else if (ref.property.equals("flowPropertyFactor")) {
				flowRef.factorId = ref.id;
				put(factorToOwner, ref.id, ref.nestedOwnerId);
			} else if (ref.property.equals("unit")) {
				flowRef.unitId = ref.id;
			}
		}
		for (Long factorId : factorToProperty.keySet()) {
			for (long ownerId : factorToOwner.get(factorId)) {
				FlowRefs refs = flowRefs.get(ownerId);
				refs.propertyId = factorToProperty.get(factorId);
			}
		}
		return new ArrayList<>(flowRefs.values());
	}

	private class FlowRefs {

		private long ownerId;
		private Class<? extends AbstractEntity> nestedOwnerType;
		private long nestedOwnerId;
		private String nestedProperty;
		private long flowId;
		private long factorId;
		private long unitId;
		private long propertyId;

	}
}
