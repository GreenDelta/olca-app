package org.openlca.app.editors.actors;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Actor;

public class ActorEditor extends ModelEditor<Actor> {

	public static String ID = "editors.actor";

	public ActorEditor() {
		super(Actor.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ActorPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("Failed to open actor", e);
		}
	}

}
