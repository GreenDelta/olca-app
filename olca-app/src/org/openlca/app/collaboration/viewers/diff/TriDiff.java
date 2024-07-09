package org.openlca.app.collaboration.viewers.diff;

import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.ModelRef;
import org.openlca.git.util.GitUtil;

/**
 * Used to compare three states of the same ModelRef, e.g. HEAD state, workspace
 * state and remote state of a model.<br>
 */
public class TriDiff extends ModelRef {

	public final Diff left;
	public final Diff right;

	public TriDiff(Diff left, Diff right) {
		super(getPath(left, right));
		this.left = left;
		this.right = right;
	}

	private static String getPath(Diff left, Diff right) {
		var any = right != null ? right : left;
		if (any.isEmptyCategory)
			return any.path + "/" + GitUtil.EMPTY_CATEGORY_FLAG;
		return any.path;
	}

	public boolean noAction() {
		if (left == null && right == null)
			return true;
		if (left == null || right == null)
			return false;
		if (left.diffType == DiffType.DELETED)
			return right.diffType == DiffType.DELETED;
		return hasEqualObjectId();
	}

	public boolean conflict() {
		if (left == null || right == null)
			return false;
		switch (left.diffType) {
			case ADDED, MODIFIED, MOVED:
				return !hasEqualObjectId();
			case DELETED:
				return right.diffType != DiffType.DELETED;
		}
		return false;
	}

	private boolean hasEqualObjectId() {
		if (left.newRef == null && right.newRef == null)
			return true;
		if (left.newRef == null || right.newRef == null)
			return false;
		return left.newRef.objectId.equals(right.newRef.objectId);
	}

}
