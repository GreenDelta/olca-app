package org.openlca.app.collaboration.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.model.Announcement;
import org.openlca.app.collaboration.model.Comment;
import org.openlca.app.collaboration.model.Restriction;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.ModelRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse.Status;

public class RepositoryClient {

	private static final Logger log = LoggerFactory.getLogger(RepositoryClient.class);
	public static final String API_VERSION = "2.0.0";
	public final RepositoryConfig config;
	private String sessionId;

	public RepositoryClient(RepositoryConfig config) throws IOException {
		this.config = config;
	}

	public static boolean isCollaborationServer(RepositoryConfig config) {
		var invocation = new ServerCheckInvocation();
		invocation.baseUrl = config.apiUrl;
		try {
			return invocation.execute();
		} catch (WebRequestException e) {
			return false;
		}
	}

	private boolean login() throws WebRequestException {
		var invocation = new LoginInvocation();
		invocation.baseUrl = config.apiUrl;
		invocation.credentials = AuthenticationDialog.promptCredentials();
		if (invocation.credentials == null)
			return false;
		sessionId = invocation.execute();
		return sessionId != null;
	}

	public void logout() throws WebRequestException {
		if (sessionId == null)
			return;
		try {
			new LogoutInvocation().execute();
		} catch (WebRequestException e) {
			if (e.getErrorCode() != Status.UNAUTHORIZED.getStatusCode()
					&& e.getErrorCode() != Status.CONFLICT.getStatusCode())
				throw e;
		}
		sessionId = null;
	}

	public File downloadLibrary(String library) throws WebRequestException {
		return executeLoggedIn(new LibraryDownloadInvocation(library));
	}

	public boolean hasAccess() throws WebRequestException {
		return executeLoggedIn(new CheckAccessInvocation(config.repositoryId));
	}

	public List<Restriction> checkRestrictions(List<? extends ModelRef> refs) throws WebRequestException {
		return executeLoggedIn(new RestrictionCheckInvocation(config.repositoryId, refs));
	}

	public List<Comment> getAllComments() throws WebRequestException {
		return executeLoggedIn(new CommentsInvocation(config.repositoryId));
	}

	public Comments getComments(ModelType type, String refId) throws WebRequestException {
		return new Comments(executeLoggedIn(new CommentsInvocation(config.repositoryId, type, refId)));
	}

	public List<String> listRepositories() throws WebRequestException {
		return executeLoggedIn(new ListRepositoriesInvocation());
	}

	public Announcement getAnnouncement() throws WebRequestException {
		return executeLoggedIn(new AnnouncementInvocation());
	}

	private <T> T executeLoggedIn(Invocation<?, T> invocation) throws WebRequestException {
		invocation.baseUrl = config.apiUrl;
		invocation.sessionId = sessionId;
		if (sessionId == null)
			try {
				if (!login())
					return null;
			} catch (WebRequestException e) {
				if (e.isConnectException()) {
					log.warn("Could not connect to repository server " + config.serverUrl + ", " + e.getMessage());
				}
				throw e;
			}
		try {
			return invocation.execute();
		} catch (WebRequestException e) {
			if (e.getErrorCode() == Status.UNAUTHORIZED.getStatusCode()) {
				login();
				return invocation.execute();
			} else {
				if (e.isConnectException()) {
					log.warn("Could not connect to repository server " + config.serverUrl + ", " + e.getMessage());
				}
				throw e;
			}
		}
	}

	public void close() {
		if (sessionId == null)
			return;
		try {
			logout();
		} catch (WebRequestException e) {
			log.error("Error logging out from repository", e);
		}
	}

}
