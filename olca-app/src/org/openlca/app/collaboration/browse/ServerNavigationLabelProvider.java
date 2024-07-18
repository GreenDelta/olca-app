package org.openlca.app.collaboration.browse;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.openlca.app.collaboration.browse.elements.EntryElement;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.RepositoryElement;
import org.openlca.app.collaboration.browse.elements.GroupElement;
import org.openlca.app.collaboration.browse.elements.ServerElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.core.model.ModelType;

public class ServerNavigationLabelProvider extends ColumnLabelProvider implements ICommonLabelProvider {

	@Override
	public Image getImage(Object obj) {
		if (!(obj instanceof IServerNavigationElement<?> elem))
			return null;
		if (elem instanceof ServerElement)
			return Icon.COLLABORATION_SERVER_LOGO.get();
		if (elem instanceof RepositoryElement)
			return Icon.REPOSITORY.get();
		if (elem instanceof GroupElement groupElem)
			return Images.get(groupElem.getContent());
		if (elem instanceof EntryElement entryElem) {
			if (entryElem.isLibrary())
				return Icon.LIBRARY.get();
			if (entryElem.isModelType() || entryElem.isCategory())
				return Images.getForCategory(entryElem.getModelType());
			if (entryElem.getModelType() == ModelType.FLOW)
				return Images.get(entryElem.getFlowType());
			if (entryElem.getModelType() == ModelType.PROCESS)
				return Images.get(entryElem.getProcessType());
			return Images.get(entryElem.getModelType());
		}
		return null;
	}

	@Override
	public String getText(Object obj) {
		if (!(obj instanceof IServerNavigationElement<?> elem))
			return null;
		if (elem instanceof ServerElement serverElem)
			return serverElem.getContent().url();
		if (elem instanceof RepositoryElement repoElem)
			return repoElem.getRepositoryId();
		if (elem instanceof GroupElement groupElem)
			return groupElem.getContent().label;
		if (elem instanceof EntryElement entryElem)
			return entryElem.getContent().name();
		return null;
	}

	@Override
	public String getDescription(Object obj) {
		return null;
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
	}

	@Override
	public void restoreState(IMemento aMemento) {
	}

	@Override
	public void saveState(IMemento aMemento) {
	}

}
