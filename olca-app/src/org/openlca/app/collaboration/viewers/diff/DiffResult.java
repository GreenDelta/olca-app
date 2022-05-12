package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.ModelRef;

public class DiffResult extends ModelRef {

	public final Diff left;
	public final Diff right;

	public DiffResult(Diff left, Diff right) {
		super(right != null ? right.ref() : left.ref());
		this.left = left;
		this.right = right;
	}

	public DiffType leftDiffType() {
		return left != null ? left.type : null;
	}

	public DiffType rightDiffType() {
		return right != null ? right.type : null;
	}

	public ObjectId leftObjectId() {
		if (left != null && left.right != null)
			return left.right.objectId;
		return null;
	}

	public ObjectId rightObjectId() {
		if (right != null && right.right != null)
			return right.right.objectId;
		return null;
	}

	public boolean noAction() {
		if (left == null && right == null)
			return true;
		if (left == null || right == null)
			return false;
		if (left.type == DiffType.DELETED)
			return right.type == DiffType.DELETED;
		return hasEqualObjectId();
	}

	public boolean conflict() {
		if (left == null || right == null)
			return false;
		switch (left.type) {
			case ADDED, MODIFIED:
				return !hasEqualObjectId();
			case DELETED:
				return right.type != DiffType.DELETED;
		}
		return false;
	}

	private boolean hasEqualObjectId() {
		return left != null && right != null && left.right.objectId != null && right.right.objectId != null
				&& left.right.objectId.equals(right.right.objectId);
	}

}
