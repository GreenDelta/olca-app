package org.openlca.app.util;

import org.openlca.core.model.Actor;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;

public class CloneUtil {

	public static CategorizedEntity clone(CategorizedEntity entity) {
		if (entity instanceof Actor)
			return ((Actor) entity).clone();
		else if (entity instanceof Source)
			return ((Source) entity).clone();
		else if (entity instanceof UnitGroup)
			return ((UnitGroup) entity).clone();
		else if (entity instanceof FlowProperty)
			return ((FlowProperty) entity).clone();
		else if (entity instanceof Flow)
			return ((Flow) entity).clone();
		else if (entity instanceof Process)
			return ((Process) entity).clone();
		else if (entity instanceof ProductSystem)
			return ((ProductSystem) entity).clone();
		else if (entity instanceof Project)
			return ((Project) entity).clone();
		return null;
	}
}
