package org.openlca.app.collaboration.viewers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.rcp.images.Icon;
import org.openlca.git.model.Commit;

class HistoryImages {

	private final List<Commit> commits;
	private final List<Commit> left = new ArrayList<>();
	private final List<Commit> right = new ArrayList<>();
	private final Map<Integer, Image> images = new HashMap<Integer, Image>();
	private int index;
	private Commit commit;
	private boolean inBranch = false;

	HistoryImages(List<Commit> commits) {
		this.commits = commits;
		initModel();
		initImages();
	}

	Image get(int column) {
		return images.get(column);
	}

	private void initModel() {
		Commit nextRight = null;
		Commit prevRight = null;
		for (var i = commits.size() - 1; i >= 0; i--) {
			var commit = commits.get(i);
			if (nextRight == null || !nextRight.equals(commit)) {
				left.add(commit);
			} else {
				right.add(commit);
				prevRight = commit;
			}
			var children = commits.stream().filter(c -> c.parentIds.contains(commit.id)).toList();
			if (children.isEmpty())
				continue;
			var last = children.get(children.size() - 1);
			if (children.size() == 2 || (prevRight != null && last.parentIds.contains(prevRight.id) && last.parentIds.size() == 1)) {
				nextRight = last;
			}
		}
	}

	private void initImages() {
		for (index = commits.size() - 1; index >= 0; index--) {
			commit = commits.get(index);
			images.put(index, getNext());
		}
	}

	private Image getNext() {
		var noOfChildren = commits.stream().filter(c -> c.parentIds.contains(commit.id)).count();
		var noOfParents = commit.parentIds.size();
		if (isFirst()) {
			if (isOnly())
				return Icon.GIT_GRAPH_FIRST_LAST.get();
			if (noOfChildren == 1)
				return Icon.GIT_GRAPH_FIRST_LOCAL.get();
			inBranch = true;
			return Icon.GIT_GRAPH_FIRST_BRANCH_START.get();
		}
		if (isLast()) {
			if (isRight(commit))
				return Icon.GIT_GRAPH_LAST_REMOTE.get();
			if (noOfParents == 1)
				return Icon.GIT_GRAPH_LAST_LOCAL.get();
			return Icon.GIT_GRAPH_LAST_BRANCH_END.get();
		}
		if (noOfParents == 2) {
			if (noOfChildren == 1) {
				inBranch = false;
				return Icon.GIT_GRAPH_BRANCH_END.get();
			}
			return Icon.GIT_GRAPH_BRANCH_END_BRANCH_START.get();
		}
		if (noOfChildren == 2) {
			inBranch = true;
			return Icon.GIT_GRAPH_BRANCH_START.get();
		}
		if (!inBranch)
			return Icon.GIT_GRAPH_LOCAL.get();
		if (isRight(commit)) {
			if (isLastRight(commit) && !willMerge())
				return Icon.GIT_GRAPH_BRANCH_REMOTE_END.get();
			return Icon.GIT_GRAPH_BRANCH_REMOTE.get();
		}
		if (isLastLeft(commit))
			return Icon.GIT_GRAPH_BRANCH_LOCAL_END.get();
		return Icon.GIT_GRAPH_BRANCH_LOCAL.get();
	}

	private boolean isLast() {
		return index == 0;
	}

	private boolean isFirst() {
		return index == commits.size() - 1;
	}

	private boolean isOnly() {
		return commits.size() == 1;
	}

	private boolean isRight(Commit commit) {
		return right.contains(commit);
	}

	private boolean isLastLeft(Commit commit) {
		return left.get(left.size() - 1).equals(commit);
	}

	private boolean isLastRight(Commit commit) {
		return right.get(right.size() - 1).equals(commit);
	}

	private boolean willMerge() {
		for (int j = index - 1; j >= 0; j--)
			if (commits.get(j).parentIds.size() == 2)
				return true;
		return false;
	}

}
