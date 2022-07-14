package org.openlca.app.collaboration.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

	public boolean isCollaborationServer() throws WebRequestException {
		var invocation = new ServerCheckInvocation();
		invocation.baseUrl = apiUrl;
		var result = invocation.execute();
		if (result != null)
			return result;
		return false;
	}

	public Announcement getAnnouncement() throws WebRequestException {
		var invocation = new AnnouncementInvocation();
		invocation.baseUrl = apiUrl;
		return invocation.execute();
	}

	public InputStream downloadLibrary(String library) throws WebRequestException {
		return executeLoggedIn(new LibraryDownloadInvocation(library));
	}

	public boolean hasAccess() throws WebRequestException {
		return executeLoggedIn(new CheckAccessInvocation(repositoryId));
	}

	public List<Restriction> checkRestrictions(Collection<? extends ModelRef> refs) throws WebRequestException {
		return executeLoggedIn(new RestrictionCheckInvocation(repositoryId, refs));
	}

	public List<Comment> getAllComments() throws WebRequestException {
		return executeLoggedIn(new CommentsInvocation(repositoryId));
	}

	public Comments getComments(ModelType type, String refId) throws WebRequestException {
		return new Comments(executeLoggedIn(new CommentsInvocation(repositoryId, type, refId)));
	}

	public List<String> listRepositories() throws WebRequestException {
		return executeLoggedIn(new ListRepositoriesInvocation());
	}

	public void downloadJson(Collection<? extends TypedRefId> requestedData, File toFile)
			throws WebRequestException {
		var token = executeLoggedIn(new DownloadJsonPrepareInvocation(repositoryId, requestedData));
		executeLoggedIn(new DownloadJsonInvocation(token, toFile));
	}

	public SearchResult search(String query, ModelType type, int page, int pageSize) throws WebRequestException {
		return executeLoggedIn(new SearchInvocation(repositoryId, query, type, page, pageSize));
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
				log.warn("Could not connect to repository server " + serverUrl + ", " + e.getMessage());
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
				log.warn("Could not connect to repository server " + serverUrl + ", " + e.getMessage());
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
			log.error("Error logging out from repository", e);
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
