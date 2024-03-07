package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.Reference;
import org.openlca.git.util.GitUtil;

/**
 * Used to compare three states of the same ModelRef, e.g. HEAD state, workspace
 * state and remote state of a model.<br>
 */
public class TriDiff extends Reference {

	public final String leftOldPath;
	public final DiffType leftDiffType;
	public final ObjectId leftNewObjectId;
	public final String rightOldPath;
	public final DiffType rightDiffType;
	public final ObjectId rightNewObjectId;

	public TriDiff(Diff left, Diff right) {
		super(getPath(left, right), oldCommitId(any(left, right)), oldObjectId(any(left, right)));
		this.leftOldPath = oldPath(left);
		this.leftDiffType = left != null ? left.diffType : null;
		this.leftNewObjectId = newObjectId(left);
		this.rightOldPath = oldPath(right);
		this.rightDiffType = right != null ? right.diffType : null;
		this.rightNewObjectId = newObjectId(right);
	}
	
	private static String oldPath(Diff diff) {
		if (diff == null || diff.oldRef == null)
			return null;
		return diff.oldRef.path;
	}

	private static ObjectId oldObjectId(Diff diff) {
		if (diff == null || diff.oldRef == null || diff.oldRef.objectId == null)
			return ObjectId.zeroId();
		return diff.oldRef.objectId;
	}
	
	private static String oldCommitId(Diff diff) {
		if (diff == null || diff.oldRef == null || diff.oldRef.commitId == null)
			return null;
		return diff.oldRef.commitId;
	}

	private static ObjectId newObjectId(Diff diff) {
		if (diff == null || diff.newRef == null || diff.newRef.objectId == null)
			return ObjectId.zeroId();
		return diff.newRef.objectId;
	}

	private static String getPath(Diff left, Diff right) {
		var any = any(left, right);
		if (any.isEmptyCategory)
			return any.path + "/" + GitUtil.EMPTY_CATEGORY_FLAG;
		return any.path;
	}

	private static Diff any(Diff left, Diff right) {
		return right != null ? right : left;
	}

	Reference left() {
		return new Reference(path, commitId, leftNewObjectId);
	}

	Reference right() {
		return new Reference(path, commitId, rightNewObjectId);
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
			case ADDED, MODIFIED, MOVED:
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
