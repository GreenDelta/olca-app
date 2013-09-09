package org.openlca.core.editors.analyze;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * Input for the analysis editor with a cache key for the calculation setup and
 * result.
 */
public class AnalyzeEditorInput implements IEditorInput {

	private String setupKey;
	private String resultKey;

	public AnalyzeEditorInput(String setupKey, String resultKey) {
		this.setupKey = setupKey;
		this.resultKey = resultKey;
	}

	public String getResultKey() {
		return resultKey;
	}

	public String getSetupKey() {
		return setupKey;
	}

	@Override
	public boolean exists() {
		return resultKey != null;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "";
	}

}
