package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;

/**
 * Used to compare three states of the same ModelRef, e.g. HEAD state, workspace
 * state and remote state of a model.<br>
 */
public class TriDiff extends Reference {

	public final DiffType leftDiffType;
	public final ObjectId leftNewObjectId;
	public final DiffType rightDiffType;
	public final ObjectId rightNewObjectId;

	public TriDiff(Diff left, Diff right) {
		super(any(left, right).path, any(left, right).oldCommitId, any(left, right).oldObjectId);
		this.leftDiffType = left != null ? left.diffType : null;
		this.leftNewObjectId = left != null ? left.newObjectId : ObjectId.zeroId();
		this.rightDiffType = right != null ? right.diffType : null;
		this.rightNewObjectId = right != null ? right.newObjectId : ObjectId.zeroId();
	}
	
	private static Diff any(Diff left, Diff right) {
		return right != null ? right : left;
	}
	
	public boolean noAction() {
		if (leftDiffType == null && rightDiffType == null)
			return true;
		if (leftDiffType == null || rightDiffType == null)
			return false;
		if (leftDiffType == DiffType.DELETED)
			return rightDiffType == DiffType.DELETED;
		return hasEqualObjectId();
	}

	public boolean conflict() {
		if (leftDiffType == null || rightDiffType == null)
			return false;
		switch (leftDiffType) {
			case ADDED, MODIFIED:
				return !hasEqualObjectId();
			case DELETED:
				return rightDiffType != DiffType.DELETED;
		}
		return false;
	}

	private boolean hasEqualObjectId() {
		return leftDiffType != null && rightDiffType != null && leftNewObjectId.equals(rightNewObjectId);
	}

}
