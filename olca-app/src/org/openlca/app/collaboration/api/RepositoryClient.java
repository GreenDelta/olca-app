package org.openlca.app.collaboration.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.model.Announcement;
import org.openlca.app.collaboration.model.Comment;
import org.openlca.app.collaboration.model.LibraryRestriction;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.Diff;
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
		try {
			// TODO find a better way
			var response = WebRequests.call(Type.GET, config.apiUrl + "/public", null, null);
			if (response.getStatus() != Status.OK.getStatusCode())
				return false;
			return response.getEntity(String.class).startsWith("{\"id\":}");
		} catch (WebRequestException e) {
			return false;
		}
	}

	private boolean login() throws WebRequestException {
		var invocation = new LoginInvocation();
		invocation.baseUrl = config.apiUrl;
		invocation.credentials = config.credentials;
		sessionId = invocation.execute();
		return sessionId != null;
	}

	public void logout() throws WebRequestException {
		if (sessionId == null)
			return;
		var invocation = new LogoutInvocation();
		invocation.baseUrl = config.apiUrl;
		invocation.sessionId = sessionId;
		try {
			invocation.execute();
		} catch (WebRequestException e) {
			if (e.getErrorCode() != Status.UNAUTHORIZED.getStatusCode())
				if (e.getErrorCode() != Status.CONFLICT.getStatusCode())
					throw e;
		}
		sessionId = null;
	}

	public boolean hasAccess() throws WebRequestException {
		var result = executeLoggedIn(() -> {
			var invocation = new CheckAccessInvocation();
			invocation.baseUrl = config.apiUrl;
			invocation.sessionId = sessionId;
			invocation.repositoryId = config.repositoryId;
			try {
				invocation.execute();
				return true;
			} catch (WebRequestException e) {
				if (e.getErrorCode() == Status.FORBIDDEN.getStatusCode())
					return false;
				throw e;
			}
		});
		if (result == null)
			return false;
		return result;
	}

	public List<LibraryRestriction> performLibraryCheck(List<Diff> diffs) throws WebRequestException {
		var result = executeLoggedIn(() -> {
			var invocation = new LibraryCheckInvocation();
			invocation.baseUrl = config.apiUrl;
			invocation.sessionId = sessionId;
			invocation.repositoryId = config.repositoryId;
			invocation.diffs = diffs;
			return invocation.execute();
		});
		if (result == null)
			return new ArrayList<>();
		return result;
	}

	public List<Comment> getAllComments() throws WebRequestException {
		try {
			return executeLoggedIn(() -> {
				var invocation = new CommentsInvocation();
				invocation.baseUrl = config.apiUrl;
				invocation.sessionId = sessionId;
				invocation.repositoryId = config.repositoryId;
				return invocation.execute();
			});
		} catch (WebRequestException e) {
			if (e.isConnectException())
				return new ArrayList<>();
			throw e;
		}
	}

	public Comments getComments(ModelType type, String refId) throws WebRequestException {
		try {
			return executeLoggedIn(() -> {
				var invocation = new CommentsInvocation();
				invocation.baseUrl = config.apiUrl;
				invocation.sessionId = sessionId;
				invocation.repositoryId = config.repositoryId;
				invocation.type = type;
				invocation.refId = refId;
				return new Comments(invocation.execute());
			});
		} catch (WebRequestException e) {
			if (e.isConnectException())
				return new Comments(new ArrayList<>());
			throw e;
		}
	}

	public List<String> listRepositories() throws WebRequestException {
		return executeLoggedIn(() -> {
			var invocation = new ListRepositoriesInvocation();
			invocation.baseUrl = config.apiUrl;
			invocation.sessionId = sessionId;
			return invocation.execute();
		});
	}

	// TODO migrate to new search api
	// public SearchResult<DsEntry> search(String query, int page, int pageSize,
	// ModelType type)
	// throws WebRequestException {
	// return executeLoggedIn(() -> {
	// SearchInvocation invocation = new SearchInvocation();
	// invocation.baseUrl = config.baseUrl;
	// invocation.sessionId = sessionId;
	// invocation.query = query;
	// invocation.page = page;
	// invocation.pageSize = pageSize;
	// invocation.type = type;
	// invocation.repositoryId = getConfig().repositoryId;
	// return invocation.execute();
	// });
	// }

	public Announcement getAnnouncement() throws WebRequestException {
		return executeLoggedIn(() -> {
			var invocation = new AnnouncementInvocation();
			invocation.baseUrl = config.apiUrl;
			invocation.sessionId = sessionId;
			return invocation.execute();
		});
	}

	private <T> T executeLoggedIn(InvocationWithResult<T> runnable) throws WebRequestException {
		if (sessionId == null && config.credentials != null)
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
			return runnable.run();
		} catch (WebRequestException e) {
			if (e.getErrorCode() == Status.UNAUTHORIZED.getStatusCode() && config.credentials != null) {
				login();
				return runnable.run();
			} else {
				if (e.isConnectException()) {
					log.warn("Could not connect to repository server " + config.serverUrl + ", " + e.getMessage());
				}
				throw e;
			}
		}
	}

	public void close() {
		if (sessionId != null) {
			try {
				logout();
			} catch (WebRequestException e) {
				log.error("Error logging out from repository", e);
			}
		}

	}

	private interface InvocationWithResult<T> {
		public T run() throws WebRequestException;
	}

}
