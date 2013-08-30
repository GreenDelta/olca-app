package org.openlca.app.navigation.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.openlca.app.util.UI;
import org.openlca.core.database.ActorDao;
import org.openlca.core.database.Cache;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.database.ProjectDao;
import org.openlca.core.database.SourceDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class DeleteModelAction extends Action implements INavigationAction {

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
		// List<ModelElement> models = new ArrayList<>();
		// for (INavigationElement<?> element : elements)
		// if (!(element instanceof ModelElement))
		// return false;
		// else
		// models.add((ModelElement) element);
		// this.elements = models;
		// return true;
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

	private void delete(ModelElement element) {
		if (createMessageDialog(element).open() != MessageDialog.OK)
			return;
		IDatabase database = Database.get();
		Cache cache = Database.getCache();
		switch (element.getContent().getModelType()) {
		case ACTOR:
			ActorDao actorDao = new ActorDao(database);
			actorDao.delete(actorDao.getForId(element.getContent().getId()));
			break;
		case SOURCE:
			SourceDao sourceDao = new SourceDao(database);
			sourceDao.delete(sourceDao.getForId(element.getContent().getId()));
			break;
		case UNIT_GROUP:
			UnitGroupDao unitGroupDao = new UnitGroupDao(database);
			unitGroupDao.delete(cache
					.getUnitGroup(element.getContent().getId()));
			break;
		case FLOW_PROPERTY:
			FlowPropertyDao flowPropertyDao = new FlowPropertyDao(database);
			flowPropertyDao.delete(cache.getFlowProperty(element.getContent()
					.getId()));
			break;
		case FLOW:
			FlowDao flowDao = new FlowDao(database);
			flowDao.delete(flowDao.getForId(element.getContent().getId()));
			break;
		case PROCESS:
			ProcessDao processDao = new ProcessDao(database);
			processDao
					.delete(processDao.getForId(element.getContent().getId()));
			break;
		case PRODUCT_SYSTEM:
			ProductSystemDao productSystemDao = new ProductSystemDao(database);
			productSystemDao.delete(productSystemDao.getForId(element
					.getContent().getId()));
			break;
		case PROJECT:
			ProjectDao projectDao = new ProjectDao(database);
			projectDao
					.delete(projectDao.getForId(element.getContent().getId()));
			break;
		case IMPACT_METHOD:
			ImpactMethodDao impactMethodDao = new ImpactMethodDao(database);
			impactMethodDao.delete(impactMethodDao.getForId(element
					.getContent().getId()));
			break;
		default:
			break;
		}
	}

	private MessageDialog createMessageDialog(ModelElement element) {
		String name = element.getContent().getName();
		return new MessageDialog(UI.shell(), Messages.Delete, null, NLS.bind(
				Messages.NavigationView_DeleteQuestion, name),
				MessageDialog.QUESTION, new String[] {
						Messages.NavigationView_YesButton,
						Messages.NavigationView_NoButton, },
				MessageDialog.CANCEL);
	}
}
