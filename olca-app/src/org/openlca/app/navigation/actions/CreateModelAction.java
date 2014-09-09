package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.Messages;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.INewModelWizard;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the 'New'-wizard for a model type.
 */
public class CreateModelAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private ModelType type;
	private INavigationElement<?> parent;

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (element instanceof ModelTypeElement) {
			ModelTypeElement e = (ModelTypeElement) element;
			parent = e;
			category = null;
			type = e.getContent();
			initDisplay();
			return true;
		}
		if (element instanceof CategoryElement) {
			CategoryElement e = (CategoryElement) element;
			parent = e;
			category = e.getContent();
			type = category.getModelType();
			initDisplay();
			return true;
		}
		return false;
	}

	private void initDisplay() {
		// force the display of the text and image of the current type
		setText(getText());
		setImageDescriptor(getImageDescriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		String wizardId = getWizardId();
		try {
			IWorkbenchWizard wizard = PlatformUI.getWorkbench()
					.getNewWizardRegistry().findWizard(wizardId).createWizard();
			if (wizard instanceof INewModelWizard) {
				INewModelWizard modelWizard = (INewModelWizard) wizard;
				modelWizard.setCategory(category);
			}
			WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
			dialog.open();
			Navigator.refresh(parent);
		} catch (final CoreException e) {
			log.error("Open model wizard failed", e);
		}
	}

	private String getWizardId() {
		if (type == null)
			return null;
		return "wizards.new."
				+ type.getModelClass().getSimpleName().toLowerCase();
	}

	@Override
	public String getText() {
		if (type == null)
			return Messages.Unknown + "?";
		switch (type) {
		case ACTOR:
			return Messages.NewActor;
		case FLOW:
			return Messages.NewFlow;
		case FLOW_PROPERTY:
			return Messages.NewFlowProperty;
		case IMPACT_METHOD:
			return Messages.NewLCIAMethod;
		case PROCESS:
			return Messages.NewProcess;
		case PRODUCT_SYSTEM:
			return Messages.NewProductSystem;
		case PROJECT:
			return Messages.NewProject;
		case SOURCE:
			return Messages.NewSource;
		case UNIT_GROUP:
			return Messages.NewUnitGroup;
		default:
			return Messages.Unknown + "?";
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (type == null)
			return null;
		switch (type) {
		case ACTOR:
			return ImageType.ACTOR_ICON_NEW.getDescriptor();
		case FLOW:
			return ImageType.FLOW_ICON_NEW.getDescriptor();
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_ICON_NEW.getDescriptor();
		case IMPACT_METHOD:
			return ImageType.LCIA_ICON_NEW.getDescriptor();
		case PROCESS:
			return ImageType.PROCESS_ICON_NEW.getDescriptor();
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_ICON_NEW.getDescriptor();
		case PROJECT:
			return ImageType.PROJECT_ICON_NEW.getDescriptor();
		case SOURCE:
			return ImageType.SOURCE_ICON_NEW.getDescriptor();
		case UNIT_GROUP:
			return ImageType.UNIT_GROUP_ICON_NEW.getDescriptor();
		default:
			return null;
		}
	}

}
