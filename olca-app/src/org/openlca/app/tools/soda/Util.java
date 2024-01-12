package org.openlca.app.tools.soda;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ModelType;
import org.openlca.ilcd.commons.DataSetType;
import org.openlca.ilcd.commons.IDataSet;
import org.openlca.ilcd.contacts.Contact;
import org.openlca.ilcd.flowproperties.FlowProperty;
import org.openlca.ilcd.flows.Flow;
import org.openlca.ilcd.methods.ImpactMethod;
import org.openlca.ilcd.models.Model;
import org.openlca.ilcd.processes.Process;
import org.openlca.ilcd.sources.Source;
import org.openlca.ilcd.units.UnitGroup;

class Util {

	private Util() {
	}

	static ModelType modelTypeOf(DataSetType type) {
		if (type == null)
			return null;
		return switch (type) {
			case CONTACT -> ModelType.ACTOR;
			case MODEL -> ModelType.PRODUCT_SYSTEM;
			case FLOW -> ModelType.FLOW;
			case SOURCE -> ModelType.SOURCE;
			case PROCESS -> ModelType.PROCESS;
			case FLOW_PROPERTY -> ModelType.FLOW_PROPERTY;
			case UNIT_GROUP -> ModelType.UNIT_GROUP;
			case IMPACT_METHOD -> ModelType.IMPACT_CATEGORY;
			case EXTERNAL_FILE -> null;
		};
	}

	static Image imageOf(DataSetType type) {
		var t = modelTypeOf(type);
		return t != null ? Images.get(t) : null;
	}

	static String labelOf(DataSetType type) {
		var t = modelTypeOf(type);
		return t != null ? Labels.plural(t) : "";
	}

	static Class<? extends IDataSet> classOf(DataSetType type) {
		if (type == null)
			return null;
		return switch (type) {
			case CONTACT -> Contact.class;
			case FLOW -> Flow.class;
			case FLOW_PROPERTY -> FlowProperty.class;
			case IMPACT_METHOD -> ImpactMethod.class;
			case PROCESS -> Process.class;
			case SOURCE -> Source.class;
			case UNIT_GROUP -> UnitGroup.class;
			case MODEL -> Model.class;
			case EXTERNAL_FILE -> null;
		};
	}

}
