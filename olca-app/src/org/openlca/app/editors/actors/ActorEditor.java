package org.openlca.app.editors.actors;

import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorEditor extends ModelEditor<Actor> {

	public static String ID = "editors.actor";
	private Logger log = LoggerFactory.getLogger(getClass());

	public ActorEditor() {
		super(Actor.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ActorInfoPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
