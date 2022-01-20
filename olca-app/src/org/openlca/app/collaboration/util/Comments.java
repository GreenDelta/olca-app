package org.openlca.app.collaboration.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openlca.app.collaboration.model.Comment;

public class Comments {

	private final Map<String, List<Comment>> byRefId = new HashMap<>();
	private final Map<String, List<Comment>> byPath = new HashMap<>();

	public Comments(List<Comment> comments) {
		initialize(comments);
	}

	public List<Comment> getForRefId(String refId) {
		return get(byRefId, refId);
	}

	public List<Comment> getForPath(String path) {
		return get(byPath, PathMap.get(path));
	}

	private List<Comment> get(Map<String, List<Comment>> map, String key) {
		var comments = map.get(key);
		if (comments == null)
			return new ArrayList<>();
		sort(comments);
		return comments;
	}

	public boolean hasRefId(String refId) {
		return has(byRefId, refId);
	}

	public boolean hasPath(String path) {
		return has(byPath, PathMap.get(path));
	}

	private boolean has(Map<String, List<Comment>> map, String key) {
		var comments = map.get(key);
		return comments != null && !comments.isEmpty();
	}

	public boolean hasAnyPath(String path) {
		for (String key : byPath.keySet()) {
			var nKey = key;
			if (!nKey.contains("["))
				continue;
			while (nKey.contains("[")) {
				nKey = nKey.substring(0, nKey.indexOf("[")) + nKey.substring(nKey.indexOf("]") + 1);
			}
			if (nKey.equals(PathMap.get(path)) && !byPath.get(key).isEmpty())
				return true;
		}
		return false;
	}

	private void initialize(List<Comment> comments) {
		for (var comment : comments) {
			put(byRefId, comment.refId(), comment);
			put(byPath, comment.path(), comment);
		}
	}

	private void put(Map<String, List<Comment>> map, String key, Comment value) {
		var list = map.get(key);
		if (list == null) {
			map.put(key, list = new ArrayList<>());
		}
		list.add(value);
	}

	public static void sort(List<Comment> list) {
		var sorted = new ArrayList<Comment>();
		var added = new HashSet<Long>();
		Collections.sort(list, (a, b) -> b.date().compareTo(a.date()));
		for (var comment : list) {
			if (added.contains(comment.id()))
				continue;
			if (comment.replyTo() != 0)
				continue;
			sorted.add(comment);
			added.add(comment.id());
			var replies = new ArrayList<Comment>();
			for (var c : list) {
				if (c.replyTo() != comment.id())
					continue;
				replies.add(c);
				added.add(c.id());
			}
			Collections.sort(replies, (a, b) -> a.date().compareTo(b.date()));
			sorted.addAll(replies);
		}
		list.clear();
		list.addAll(sorted);
	}

}
