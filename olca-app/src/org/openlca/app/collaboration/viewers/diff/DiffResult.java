package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.openlca.git.model.Diff;
import org.openlca.git.model.DiffType;
import org.openlca.git.model.ModelRef;

public class DiffResult extends ModelRef {

	public final DiffType leftDiffType;
	public final ObjectId leftObjectId;
	public final DiffType rightDiffType;
	public final ObjectId rightObjectId;

	public DiffResult(Diff left, Diff right) {
		super(right != null ? right.ref() : left.ref());
		this.leftDiffType = left != null ? left.type : null;
		this.leftObjectId = left != null && left.right != null ? left.right.objectId : null;
		this.rightDiffType = right != null ? right.type : null;
		this.rightObjectId = right != null && right.right != null ? left.right.objectId : null;
	}

	public DiffResult(DiffEntry entry) {
		super(entry);
		this.leftDiffType = getDiffType(entry.getChangeType());
		this.leftObjectId = getObjectId(entry);
		this.rightDiffType = null;
		this.rightObjectId = null;
	}

	private DiffType getDiffType(ChangeType type) {
		return switch (type) {
			case ADD -> DiffType.ADDED;
			case MODIFY -> DiffType.MODIFIED;
			case DELETE -> DiffType.DELETED;
			default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	private ObjectId getObjectId(DiffEntry entry) {
		var abbrId = leftDiffType == DiffType.DELETED
				? entry.getOldId()
				: entry.getNewId();
		if (abbrId == null)
			return null;
		return abbrId.toObjectId();
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
		return leftObjectId != null && rightObjectId != null && leftObjectId.equals(rightObjectId);
	}

}
