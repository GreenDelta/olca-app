package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.ui.IEditorInput;
import org.openlca.app.editors.sd.editor.SdModelEditor;

/**
 * Editor input for the SdGraphEditor.
 */
public class SdGraphEditorInput implements IEditorInput {

	private final SdModelEditor modelEditor;

	public SdGraphEditorInput(SdModelEditor modelEditor) {
		this.modelEditor = modelEditor;
	}

	public SdModelEditor getModelEditor() {
		return modelEditor;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public String getName() {
		return "System Dynamics Model Graph";
	}

	@Override
	public String getToolTipText() {
		return "System Dynamics Model Graph";
	}

	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public org.eclipse.ui.IPersistableElement getPersistable() {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
}
