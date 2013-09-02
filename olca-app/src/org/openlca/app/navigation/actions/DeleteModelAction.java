package org.openlca.app.navigation.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.delete.DeleteWizard;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
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
		if (!(element instanceof ModelElement))
			return false;
		elements = Collections.singletonList((ModelElement) element);
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
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
		// TODO implement deletion of list
		// current list contains only one element
		ModelElement element = elements.get(0);
		DeleteWizard<BaseDescriptor> wizard = new DeleteWizard<>(
				IUseSearch.FACTORY.createFor(element.getContent()
						.getModelType(), Database.get()), element.getContent());
		boolean canDelete = true;
		if (wizard != null && wizard.hasProblems())
			canDelete = new WizardDialog(UI.shell(), wizard).open() == Window.OK;
		if (canDelete) {
			delete(element);
			App.closeEditor(element.getContent());
			Navigator.refresh(element.getParent());
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void delete(ModelElement element) {
		if (element == null || !askDelete(element))
			return;
		try {
			log.trace("delete model {}", element.getContent());
			IDatabase database = Database.get();
			BaseDescriptor descriptor = element.getContent();
			Class<T> clazz = (Class<T>) descriptor.getModelType()
					.getModelClass();
			BaseDao<T> dao = database.createDao(clazz);
			T instance = dao.getForId(descriptor.getId());
			dao.delete(instance);
			// TODO: evict element from cache
			log.trace("element deleted");
		} catch (Exception e) {
			log.error("failed to delete element " + element, e);
		}
	}

	private boolean askDelete(ModelElement element) {
		if (element == null || element.getContent() == null)
			return false;
		String name = element.getContent().getName();
		String message = NLS.bind(Messages.NavigationView_DeleteQuestion, name);
		return Question.ask(Messages.Delete, message);
	}
}
