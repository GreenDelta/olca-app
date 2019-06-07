package org.openlca.app.tools.mapping.model;

import org.openlca.core.model.FlowType;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.Status;
import org.openlca.util.Strings;

final class Sync {

	private Sync() {
	}

	static boolean isInvalidFlowRef(FlowRef ref) {
		if (ref == null)
			return true;
		if (ref.flow == null || ref.flow.refId == null) {
			ref.status = Status.error("missing flow reference with UUID");
			return true;
		}
		return false;
	}

	static void checkFlowName(FlowRef ref, String name) {
		if (ref == null || ref.flow == null)
			return;
		if (Strings.nullOrEmpty(ref.flow.name)) {
			ref.flow.name = name;
		} else if (!Strings.nullOrEqual(ref.flow.name, name)) {
			addWarning(ref,
					"the flow in the mapping has a different name");
		}
	}

	static void checkFlowCategory(FlowRef ref, String path) {
		if (ref == null)
			return;
		if (Strings.nullOrEmpty(ref.flowCategory)) {
			ref.flowCategory = path;
		} else if (!Strings.nullOrEqual(path, ref.flowCategory)) {
			addWarning(ref, "the flow in the mapping "
					+ "has a different category path");
		}
	}

	static void checkFlowType(FlowRef ref, FlowType type) {
		if (ref == null || ref.flow == null)
			return;
		if (ref.flow.flowType == null) {
			ref.flow.flowType = type;
		} else if (ref.flow.flowType != type) {
			addWarning(ref, "the flow in the mapping has a different type");
		}
	}

	static void checkFlowLocation(FlowRef ref, String code) {
		if (ref == null)
			return;
		if (Strings.nullOrEmpty(ref.flowLocation)) {
			ref.flowLocation = code;
		} else if (!Strings.nullOrEqual(code, ref.flowLocation)) {
			addWarning(ref, "the flow in the mapping "
					+ "has a different location code");
		}
	}

	private static void addWarning(FlowRef ref, String message) {
		if (ref == null)
			return;
		if (ref.status == null || ref.status.isOk()) {
			ref.status = Status.warn(message);
			return;
		}
		if (ref.status.isError())
			// do not overwrite an error
			return;
		// combine the warnings
		String m = ref.status.message + "; " + message;
		ref.status = Status.warn(m);
	}

}
