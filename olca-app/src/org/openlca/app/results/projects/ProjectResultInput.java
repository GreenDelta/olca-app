package org.openlca.app.results.projects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.ProjectDescriptor;

/** The editor input of a project result. */
public class ProjectResultInput implements IEditorInput {

	private long projectId;
	private String resultKey;

	public ProjectResultInput(long projectId, String resultKey) {
		this.projectId = projectId;
		this.resultKey = resultKey;
	}

	public long getProjectId() {
		return projectId;
	}

	public String getResultKey() {
		return resultKey;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		EntityCache cache = Cache.getEntityCache();
		if (cache == null)
			return "";
		ProjectDescriptor d = cache.get(ProjectDescriptor.class, projectId);
		return Messages.Results + ": " + Labels.getDisplayName(d);
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

}
