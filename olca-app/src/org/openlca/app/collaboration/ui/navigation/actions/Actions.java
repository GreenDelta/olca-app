package org.openlca.app.collaboration.ui.navigation.actions;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.openlca.app.collaboration.ui.views.CompareView;
import org.openlca.app.collaboration.ui.views.HistoryView;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Actions {

	private static final Logger log = LoggerFactory.getLogger(Actions.class);

	static void refresh() {
		Navigator.refresh();
		HistoryView.refresh();
		CompareView.clear();
	}

	static void handleException(String message, Exception e) {
		log.error(message, e);
		MsgBox.error(e.getMessage());
	}

	static <V extends TransportCommand<C, T>, C extends GitCommand<T>, T> V withCredentialsProvider(V command) {
		var c = Repository.get().config.credentials;
		if (c != null && !Strings.nullOrEmpty(c.username()) && !Strings.nullOrEmpty(c.password())) {
			var credentials = new UsernamePasswordCredentialsProvider(c.username(), c.password());
			command.setCredentialsProvider(credentials);
		}
		return command;
	}

}
