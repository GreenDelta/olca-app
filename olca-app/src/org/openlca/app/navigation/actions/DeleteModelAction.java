package org.openlca.app.navigation.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
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
import org.openlca.core.database.BaseDao;
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
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.results.ImpactResult;

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
		return Messages.Common_Delete;
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
		DeleteWizard<? extends RootEntity> wizard = getWizard(element);
		boolean canDelete = true;
		if (wizard != null && wizard.hasProblems())
			canDelete = new WizardDialog(UI.shell(), wizard).open() == Window.OK;
		if (canDelete) {
			delete(element);
			App.closeEditor(element.getContent());
			Navigator.refresh(element.getParent());
		}
	}

	private DeleteWizard<? extends RootEntity> getWizard(ModelElement element) {
		switch (element.getContent().getModelType()) {
		case ACTOR:
			return new DeleteWizard<Actor>(IUseSearch.FACTORY.createFor(
					Actor.class, Database.get()), Descriptors.toActor(element
					.getContent()));
		case SOURCE:
			return new DeleteWizard<Source>(IUseSearch.FACTORY.createFor(
					Source.class, Database.get()), Descriptors.toSource(element
					.getContent()));
		case UNIT_GROUP:
			return new DeleteWizard<UnitGroup>(IUseSearch.FACTORY.createFor(
					UnitGroup.class, Database.get()),
					Descriptors.toUnitGroup(element.getContent()));
		case FLOW_PROPERTY:
			return new DeleteWizard<FlowProperty>(IUseSearch.FACTORY.createFor(
					FlowProperty.class, Database.get()),
					Descriptors.toFlowProperty(element.getContent()));
		case FLOW:
			return new DeleteWizard<Flow>(IUseSearch.FACTORY.createFor(
					Flow.class, Database.get()), Descriptors.toFlow(element
					.getContent()));
		case PROCESS:
			return new DeleteWizard<Process>(IUseSearch.FACTORY.createFor(
					Process.class, Database.get()),
					Descriptors.toProcess(element.getContent()));
		case IMPACT_METHOD:
			return new DeleteWizard<ImpactMethod>(IUseSearch.FACTORY.createFor(
					ImpactMethod.class, Database.get()),
					Descriptors.toImpactMethod(element.getContent()));
		case IMPACT_RESULT:
			return new DeleteWizard<ImpactResult>(IUseSearch.FACTORY.createFor(
					ImpactResult.class, Database.get()),
					Descriptors.toImpactResult(element.getContent()));
		default:
			return null;
		}
	}

	private void delete(ModelElement element) {
		IDatabase database = Database.get();
		switch (element.getContent().getModelType()) {
		case ACTOR:
			new ActorDao(database).delete(Descriptors.toActor(element
					.getContent()));
			break;
		case SOURCE:
			new SourceDao(database).delete(Descriptors.toSource(element
					.getContent()));
			break;
		case UNIT_GROUP:
			new UnitGroupDao(database).delete(Descriptors.toUnitGroup(element
					.getContent()));
			break;
		case FLOW_PROPERTY:
			new FlowPropertyDao(database).delete(Descriptors
					.toFlowProperty(element.getContent()));
			break;
		case FLOW:
			new FlowDao(database).delete(Descriptors.toFlow(element
					.getContent()));
			break;
		case PROCESS:
			new ProcessDao(database).delete(Descriptors.toProcess(element
					.getContent()));
			break;
		case PRODUCT_SYSTEM:
			new ProductSystemDao(database).delete(Descriptors
					.toProductSystem(element.getContent()));
			break;
		case PROJECT:
			new ProjectDao(database).delete(Descriptors.toProject(element
					.getContent()));
			break;
		case IMPACT_METHOD:
			new ImpactMethodDao(database).delete(Descriptors
					.toImpactMethod(element.getContent()));
			break;
		case IMPACT_RESULT:
			new BaseDao<ImpactResult>(ImpactResult.class, database)
					.delete(Descriptors.toImpactResult(element.getContent()));
			break;
		default:
			break;
		}
	}

}
