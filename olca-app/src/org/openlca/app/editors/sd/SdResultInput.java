package org.openlca.app.editors.sd;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.openlca.sd.eqn.Var;

public class SdResultInput implements IEditorInput {

	private final String modelName;
	private final List<Var> variables;
	private final String key;

	public SdResultInput(String modelName, List<Var> variables, String key) {
		this.modelName = modelName;
		this.variables = variables;
		this.key = key;
	}

	public String modelName() {
		return modelName;
	}

	public List<Var> variables() {
		return variables;
	}

	public String key() {
		return key;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return modelName + " - Simulation Result";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getName();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}