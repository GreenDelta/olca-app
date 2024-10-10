package org.openlca.app.collaboration.viewers.diff;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;
import org.openlca.git.repo.OlcaRepository;
import org.openlca.jsonld.LibraryLink;
import org.openlca.util.Strings;

public class DiffNodeBuilder {

	private final Map<String, DiffNode> nodes = new LinkedHashMap<>();
	private final Map<String, TriDiff> diffs = new LinkedHashMap<>();
	private final OlcaRepository repo;
	private final IDatabase database;

	public DiffNodeBuilder(OlcaRepository repo, IDatabase database) {
		this.repo = repo;
		this.database = database;
	}

	public DiffNode build(Collection<TriDiff> diffs) {
		if (!init(diffs))
			return null;
		var root = new DiffNode(null, database.getName());
		nodes.put(null, root);
		for (var diff : this.diffs.values()) {
			build(diff);
		}
		return root;
	}

	private boolean init(Collection<TriDiff> diffs) {
		for (var result : diffs) {
			this.diffs.put(getKey(result), result);
		}
		nodes.clear();
		return this.diffs.size() != 0;
	}

	private void build(TriDiff diff) {
		if (nodes.containsKey(getKey(diff)))
			return;
		if (diff.noAction())
			return;
		if (diff.isRepositoryInfo) {
			createLibrariesNode(diff);
		} else {
			createNode(diff);
		}
	}

	private void createNode(TriDiff diff) {
		var parent = !Strings.nullOrEmpty(diff.category)
				? getOrCreateCategoryNode(diff.type, diff.category)
				: getOrCreateModelTypeNode(diff.type);
		var node = new DiffNode(parent, diff);
		parent.children.add(node);
		nodes.put(getKey(diff), node);
	}

	private DiffNode getOrCreateCategoryNode(ModelType type, String category) {
		var categoryPath = type.name() + "/" + category;
		var categoryNode = nodes.get(categoryPath);
		if (categoryNode != null)
			return categoryNode;
		var parent = category.contains("/")
				? getOrCreateCategoryNode(type, category.substring(0, category.lastIndexOf("/")))
				: getOrCreateModelTypeNode(type);
		categoryNode = new DiffNode(parent, categoryPath);
		parent.children.add(categoryNode);
		nodes.put(categoryPath, categoryNode);
		return categoryNode;

	}

	private DiffNode getOrCreateModelTypeNode(ModelType type) {
		var typeNode = nodes.get(type.name());
		if (typeNode != null)
			return typeNode;
		var root = nodes.get(null);
		typeNode = new DiffNode(root, type);
		root.children.add(typeNode);
		nodes.put(type.name(), typeNode);
		return typeNode;
	}

	private DiffNode createLibrariesNode(TriDiff diff) {
		var root = nodes.get(null);
		var librariesNode = new DiffNode(root, diff);
		root.children.add(librariesNode);
		nodes.put(diff.path, librariesNode);
		var repoLibraries = repo.getInfo().libraries()
				.stream().map(LibraryLink::id)
				.toList();
		var dbLibraries = database.getLibraries();
		repoLibraries.stream()
				.filter(Predicate.not(dbLibraries::contains))
				.forEach(library -> createLibraryNode(librariesNode, DiffType.DELETED, library));
		dbLibraries.stream()
				.filter(Predicate.not(repoLibraries::contains))
				.forEach(library -> createLibraryNode(librariesNode, DiffType.ADDED, library));
		return librariesNode;
	}

	private DiffNode createLibraryNode(DiffNode librariesNode, DiffType diffType, String library) {
		var path = librariesNode.contentAsTriDiff().path + "/" + library;
		var diff = diffType == DiffType.ADDED
				? new Diff(diffType, null, new Reference(path, null, null))
				: new Diff(diffType, new Reference(path, repo.commits.head().id, null), null);
		var libraryNode = new DiffNode(librariesNode, diff);
		librariesNode.children.add(libraryNode);
		nodes.put(path, libraryNode);
		return libraryNode;
	}

	private String getKey(TriDiff diff) {
		return diff.path;
	}

}
