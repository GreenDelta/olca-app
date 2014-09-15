package org.openlca.app.navigation.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Resources;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteModelAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());
	private List<ModelElement> elements;

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof ModelElement)) {
			elements = null;
			return false;
		}
		elements = Collections.singletonList((ModelElement) element);
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		if (elements == null)
			return false;
		List<ModelElement> candidates = new ArrayList<>();
		for (INavigationElement<?> candidate : elements) {
			if (candidate instanceof ModelElement)
				candidates.add((ModelElement) candidate);
		}
		if (candidates.isEmpty())
			return false;
		this.elements = candidates;
		return true;
	}

	@Override
	public String getText() {
		return Messages.Delete;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.DELETE_ICON.getDescriptor();
	}

	@Override
	public void run() {
		if (elements == null)
			return;
		for (ModelElement element : elements) {
			BaseDescriptor descriptor = element.getContent();
			if (descriptor == null)
				continue;
			if (!askDelete(descriptor))
				break;
			if (isUsed(descriptor))
				continue;
			App.closeEditor(descriptor);
			delete(descriptor);
			Navigator.refresh(element.getParent());
		}
		elements = null;
	}

	private boolean isUsed(BaseDescriptor descriptor) {
		IUseSearch<BaseDescriptor> search = IUseSearch.FACTORY.createFor(
				descriptor.getModelType(), Database.get());
		List<BaseDescriptor> descriptors = search.findUses(descriptor);
		if (descriptors.isEmpty())
			return false;
		Error.showBox(Messages.CannotDelete, Messages.CannotDeleteMessage);
		return true;
	}

	@SuppressWarnings("unchecked")
	private <T> void delete(BaseDescriptor descriptor) {
		try {
			log.trace("delete model {}", descriptor);
			IDatabase database = Database.get();
			Class<T> clazz = (Class<T>) descriptor.getModelType()
					.getModelClass();
			BaseDao<T> dao = database.createDao(clazz);
			T instance = dao.getForId(descriptor.getId());
			dao.delete(instance);
			Cache.evict(descriptor);
			Resources.cleanup(descriptor);
			log.trace("element deleted");
		} catch (Exception e) {
			log.error("failed to delete element " + descriptor, e);
		}
	}

	private boolean askDelete(BaseDescriptor descriptor) {
		if (descriptor == null)
			return false;
		String name = Labels.getDisplayName(descriptor);
		String message = NLS.bind(Messages.DoYouReallyWantToDelete, name);
		return Question.ask(Messages.Delete, message);
	}
}
