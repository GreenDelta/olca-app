package org.openlca.app.collaboration.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openlca.app.collaboration.model.Comment;
import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;

import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Invokes a web service call to retrieve comments of a dataset from a
 * repository
 */
class CommentsInvocation extends Invocation<Map<String, Object>, List<Comment>> {

	private final String repositoryId;
	private final ModelType type;
	private final String refId;

	CommentsInvocation(String repositoryId) {
		this(repositoryId, null, null);
	}

	@SuppressWarnings("unchecked")
	CommentsInvocation(String repositoryId, ModelType type, String refId) {
		super(Type.GET, "comment", (Class<Map<String, Object>>) new TypeToken<Map<String, Object>>() {
		}.getType());
		this.repositoryId = repositoryId;
		this.type = type;
		this.refId = refId;
	}

	@Override
	protected void checkValidity() {
		Valid.checkNotEmpty(repositoryId, "repository id");
	}

	@Override
	protected String query() {
		var query = "/" + repositoryId;
		if (type != null && refId != null) {
			query += "/" + type.name() + "/" + refId;
		} else {
			query += "?includeReplies=true";
		}
		return query;
	}

	@Override
	protected List<Comment> process(Map<String, Object> data) {
		var field = type != null && refId != null ? "comments" : "data";
		return parseComments(data.get(field));
	}

	@Override
	protected List<Comment> handleError(WebRequestException e) throws WebRequestException {
		if (e.isConnectException() || e.getErrorCode() == Status.SERVICE_UNAVAILABLE.getStatusCode())
			return new ArrayList<>();
		throw e;
	}

	private List<Comment> parseComments(Object value) {
		if (value == null)
			return new ArrayList<>();
		if (!(value instanceof Collection))
			return new ArrayList<>();
		var comments = new ArrayList<Comment>();
		for (Object o : (Collection<?>) value) {
			@SuppressWarnings("unchecked")
			var map = (Map<String, Object>) o;
			var fieldMap = toMap(map, "field");
			comments.add(new Comment(
					toLong(map, "id"),
					toString(toMap(map, "user"), "name"),
					toString(map, "text"),
					toString(fieldMap, "refId"),
					toType(fieldMap, "modelType"),
					toString(fieldMap, "path"),
					toDate(map, "date"),
					is(map, "released"),
					is(map, "approved"),
					toLong(map, "replyTo")//
			));
		}
		return comments;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> toMap(Map<String, Object> data, String property) {
		if (!data.containsKey(property))
			return null;
		var value = data.get(property);
		if (value == null)
			return null;
		if (!(value instanceof Map))
			return null;
		return (Map<String, Object>) value;
	}

	private String toString(Map<String, Object> data, String property) {
		if (data == null || !data.containsKey(property))
			return null;
		var value = data.get(property);
		if (value == null)
			return null;
		return value.toString();
	}

	private ModelType toType(Map<String, Object> data, String property) {
		var type = toString(data, property);
		if (type == null)
			return null;
		return ModelType.valueOf(type);
	}

	private long toLong(Map<String, Object> data, String property) {
		if (data == null || !data.containsKey(property))
			return 0;
		var value = data.get(property);
		if (value == null)
			return 0;
		if (value instanceof Long)
			return (Long) value;
		if (value instanceof Integer)
			return ((Integer) value).longValue();
		if (value instanceof Double)
			return ((Double) value).longValue();
		if (value instanceof Float)
			return ((Float) value).longValue();
		if (value instanceof String)
			return Long.parseLong(value.toString());
		return 0;
	}

	private Date toDate(Map<String, Object> data, String property) {
		var time = toLong(data, property);
		if (time == 0)
			return null;
		var calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		return calendar.getTime();
	}

	private boolean is(Map<String, Object> data, String property) {
		if (data == null || !data.containsKey(property))
			return false;
		var value = data.get(property);
		if (value == null)
			return false;
		if (value instanceof Boolean)
			return (Boolean) value;
		if (value instanceof String)
			return Boolean.parseBoolean(value.toString());
		return false;
	}

}
