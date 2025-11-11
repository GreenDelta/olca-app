package org.openlca.app.tools.mapping.model;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openlca.app.M;
import org.openlca.commons.Strings;
import org.openlca.core.io.maps.FlowRef;
import org.openlca.core.io.maps.MappingStatus;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.Descriptor;

final class Sync {

	private Sync() {
	}

	/**
	 * Synchronizes the given (external) flow references which are the target or
	 * source flow definitions of a mapping with the (internal) flow references
	 * provided by the given data package (which can be an ILCD or JSON-LD package).
	 * The sync.-state of the flow references of the mapping is mutated so that it
	 * can be displayed in the mapping tool.
	 */
	static void packageSync(FlowProvider pack, Stream<FlowRef> mappingRefs) {

		if (mappingRefs == null || pack == null)
			return;
		Map<String, FlowRef> packRefs = pack.getFlowRefs().stream()
				.collect(Collectors.toMap(ref -> ref.flow.refId, ref -> ref));

		mappingRefs.forEach(mapRef -> {

			if (Sync.isInvalidFlowRef(mapRef))
				return;

			// we update the status in the following sync. steps
			mapRef.status = null;

			// check the flow
			String flowID = mapRef.flow.refId;
			FlowRef packRef = packRefs.get(flowID);
			if (packRef == null) {
				mapRef.status = MappingStatus.error(
						M.NoFlowWithIdInDataPackageInfo  + " " + flowID);
				return;
			}

			// check the flow property; the mapping can contain a different
			// flow property than the ref. flow property of the flow but when
			// the IDs are equal, the flow properties should have the same name
			if (mapRef.property == null) {
				mapRef.property = packRef.property;
			} else {

				if (packRef.property == null) {
					mapRef.status = MappingStatus.error(
							M.FlowInDataPackageWithoutCorrespondingProperty);
					return;
				}

				Descriptor packProp = packRef.property;
				Descriptor mapProp = mapRef.property;
				if (Objects.equals(packProp.refId, mapProp.refId)
						&& packProp.name != null && mapProp.name != null
						&& !Objects.equals(packProp.name, mapProp.name)) {
					mapRef.status = MappingStatus.error(
							M.FlowPropertyInDataPackageHasDifferentName + " "
							+ packProp.name + " != " + mapProp.name);
					return;
				}
			}

			// check the unit; also the unit in the mapping can be different
			// than the reference unit of the flow in the data package but
			// when the IDs are equal, the names must be equal too
			if (mapRef.unit == null) {
				mapRef.unit = packRef.unit;
			} else if (packRef.unit != null) {

				// mapping packages not always contain their reference units
//				if (packRef.unit == null) {
//					mapRef.status = Status.error("the flow in the data package"
//							+ " has no corresponding unit");
//					return;
//				}

				Descriptor packUnit = packRef.unit;
				Descriptor mapUnit = mapRef.unit;
				if (Objects.equals(packUnit.refId, mapUnit.refId)
						&& packUnit.name != null && mapUnit.name != null
						&& !Objects.equals(packUnit.name, mapUnit.name)) {
					mapRef.status = MappingStatus.error(
							M.FlowUnitInDataPackageHasDifferentName + " "
							+ packUnit.name + " != " + mapUnit.name);
					return;
				}
			}

			Sync.checkFlowName(mapRef, packRef.flow.name);
			Sync.checkFlowCategory(mapRef, packRef.flowCategory);
			Sync.checkFlowType(mapRef, packRef.flow.flowType);
			Sync.checkFlowLocation(mapRef, packRef.flowLocation);

			if (mapRef.status == null) {
				mapRef.status = MappingStatus.ok(M.FlowInSyncWithDataPackage);
			}

		});
	}

	static boolean isInvalidFlowRef(FlowRef ref) {
		if (ref == null)
			return true;
		if (ref.flow == null || ref.flow.refId == null) {
			ref.status = MappingStatus.error(M.MissingFlowReferenceWithUuid);
			return true;
		}
		return false;
	}

	static void checkFlowName(FlowRef ref, String name) {
		if (ref == null || ref.flow == null)
			return;
		if (Strings.isBlank(ref.flow.name)) {
			ref.flow.name = name;
		} else if (!Objects.equals(ref.flow.name, name)) {
			addWarning(ref, M.FlowInMappingDifferentName);
		}
	}

	static void checkFlowCategory(FlowRef ref, String path) {
		if (ref == null)
			return;
		if (Strings.isBlank(ref.flowCategory)) {
			ref.flowCategory = path;
			return;
		}
		if (Strings.isBlank(path)) {
			addWarning(ref, M.FlowInMappingDifferentPath);
			return;
		}
		String p1 = ref.flowCategory.toLowerCase();
		if (p1.startsWith("elementary flows/")) {
			p1 = p1.substring(17);
		}
		String p2 = path.toLowerCase();
		if (p2.startsWith("elementary flows/")) {
			p2 = p2.substring(17);
		}
		if (!p1.equals(p2)) {
			addWarning(ref, M.FlowInMappingDifferentCategoryPath);
		}
	}

	static void checkFlowType(FlowRef ref, FlowType type) {
		if (ref == null || ref.flow == null)
			return;
		if (ref.flow.flowType == null) {
			ref.flow.flowType = type;
		} else if (ref.flow.flowType != type) {
			addWarning(ref, M.FlowInMappingDifferentType);
		}
	}

	static void checkFlowLocation(FlowRef ref, String code) {
		if (ref == null)
			return;
		if (Strings.isBlank(ref.flowLocation)) {
			ref.flowLocation = code;
		} else if (!Objects.equals(code, ref.flowLocation)) {
			addWarning(ref, M.FlowInMappingDifferentLocation);
		}
	}

	static void checkProviderLocation(FlowRef ref, String code) {
		if (ref == null)
			return;
		if (Strings.isBlank(ref.providerLocation)) {
			ref.providerLocation = code;
		} else if (!Objects.equals(code, ref.providerLocation)) {
			addWarning(ref, M.ProviderInMappingDifferentLocation);
		}
	}

	private static void addWarning(FlowRef ref, String message) {
		if (ref == null)
			return;
		if (ref.status == null || ref.status.isOk()) {
			ref.status = MappingStatus.warn(message);
			return;
		}
		if (ref.status.isError())
			// do not overwrite an error
			return;
		// combine the warnings
		String m = ref.status.message() + "; " + message;
		ref.status = MappingStatus.warn(m);
	}

}
