package org.openlca.app.editors.processes.social;

import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.Source;

class SocialAspect extends AbstractEntity {
	SocialIndicator indicator;
	String rawAmount;
	String comment;
	Source source;
}