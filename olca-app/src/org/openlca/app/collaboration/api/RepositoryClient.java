package org.openlca.app.collaboration.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.model.Announcement;
import org.openlca.app.collaboration.model.Comment;
import org.openlca.app.collaboration.model.Restriction;
import org.openlca.app.collaboration.model.SearchResult;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.db.Repository;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.ModelRef;
import org.openlca.git.util.TypedRefId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse.Status;

public class RepositoryClient {

	private static final Logger log = LoggerFactory.getLogger(RepositoryClient.class);
	public static final String API_VERSION = "2.0.0";
	public final String serverUrl;
	public final String repositoryId;
	private final String apiUrl;
	private String sessionId;

	public RepositoryClient(String serverUrl, String repositoryId) throws IOException {
		this.serverUrl = serverUrl;
		this.apiUrl = serverUrl + "/ws";
		this.repositoryId = repositoryId;
	}

	public boolean isCollaborationServer() {
		var invocation = new ServerCheckInvocation();
		invocation.baseUrl = apiUrl;
		try {
			var result = invocation.execute();
			if (result != null)
				return result;
			return false;
		} catch (WebRequestException e) {
			log.warn("isCollaborationServer call responded: " + e.getMessage());
			return false;
		}
	}

	public Announcement getAnnouncement() {
		try {
			var invocation = new AnnouncementInvocation();
			invocation.baseUrl = apiUrl;
			return invocation.execute();
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
			return null;
		}
	}

	public List<Restriction> checkRestrictions(Collection<? extends ModelRef> refs) {
		try {
			return executeLoggedIn(new RestrictionCheckInvocation(repositoryId, refs));
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
			return new ArrayList<>();
		}
	}

	public List<Comment> getAllComments() {
		try {
			return executeLoggedIn(new CommentsInvocation(repositoryId));
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
			return new ArrayList<>();
		}
	}

	public Comments getComments(ModelType type, String refId) {
		try {
			return new Comments(executeLoggedIn(new CommentsInvocation(repositoryId, type, refId)));
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
			return new Comments(new ArrayList<>());
		}
	}

	public InputStream downloadLibrary(String library) {
		try {
			return executeLoggedIn(new LibraryDownloadInvocation(library));
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
			return null;
		}
	}
	public boolean downloadJson(TypedRefId id, File toFile) {
		try {
			var token = executeLoggedIn(new DownloadJsonPrepareInvocation(repositoryId, id));
			executeLoggedIn(new DownloadJsonInvocation(token, toFile));
			return true;
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
			return false;
		}
	}

	public SearchResult search(String query, ModelType type, int page, int pageSize) {
		try {
			return executeLoggedIn(new SearchInvocation(repositoryId, query, type, page, pageSize));
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
			return null;
		}
	}

	private <T> T executeLoggedIn(Invocation<?, T> invocation) throws WebRequestException {
		invocation.baseUrl = apiUrl;
		if (sessionId == null && !login(false))
			return null;
		invocation.sessionId = sessionId;
		try {
			return invocation.execute();
		} catch (WebRequestException e) {
			if (e.getErrorCode() == Status.UNAUTHORIZED.getStatusCode()) {
				if (!login(true))
					return null;
				invocation.sessionId = sessionId;
				return invocation.execute();
			} else if (e.isConnectException()) {
				log.error("Repository client request failed", e);
				return null;
			}
			throw e;
		}
	}

	private boolean login(boolean forceCredentials) throws WebRequestException {
		var invocation = new LoginInvocation();
		invocation.baseUrl = apiUrl;
		var repo = Repository.get();
		var url = serverUrl + "/" + repositoryId;
		invocation.credentials = forceCredentials
				? repo != null
						? AuthenticationDialog.forcePromptCredentials(repo)
						: AuthenticationDialog.forcePromptCredentials(url)
				: repo != null
						? AuthenticationDialog.promptCredentials(repo)
						: AuthenticationDialog.promptCredentials(url);

		if (invocation.credentials == null)
			return false;
		try {
			sessionId = invocation.execute();
		} catch (WebRequestException e) {
			if (e.isConnectException()) {
				log.error("Repository client request failed", e);
				return false;
			}
			if (e.getErrorCode() == Status.UNAUTHORIZED.getStatusCode()
					|| e.getErrorCode() == Status.FORBIDDEN.getStatusCode())
				return login(true);
			throw e;
		}
		return sessionId != null;
	}

	public void close() {
		try {
			logout();
		} catch (WebRequestException e) {
			log.error("Repository client request failed", e);
		}
	}

	private void logout() throws WebRequestException {
		if (sessionId == null)
			return;
		try {
			var invocation = new LogoutInvocation();
			invocation.baseUrl = apiUrl;
			invocation.sessionId = sessionId;
			invocation.execute();
		} catch (WebRequestException e) {
			if (e.getErrorCode() != Status.UNAUTHORIZED.getStatusCode()
					&& e.getErrorCode() != Status.CONFLICT.getStatusCode())
				throw e;
		}
		sessionId = null;
	}
}
