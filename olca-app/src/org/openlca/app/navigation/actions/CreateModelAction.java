package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.INewModelWizard;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

/**
 * Opens the 'New'-wizard for a model type.
 */
class CreateModelAction extends Action implements INavigationAction {

	private Category category;
	private ModelType type;
	private INavigationElement<?> parent;

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);

		type = null;
		category = null;

		if (first instanceof ModelTypeElement e) {
			type = e.getContent();
		}
		if (first instanceof CategoryElement e) {
			category = e.getContent();
			type = category.modelType;
		}

		if (type == null || first.getLibrary().isPresent())
			return false;
		parent = first;
		setText(getText());
		setImageDescriptor(getImageDescriptor());
		return true;
	}

	@Override
	public void run() {
		var wizardId = getWizardId();
		try {
			var wizard = PlatformUI.getWorkbench()
					.getNewWizardRegistry()
					.findWizard(wizardId)
					.createWizard();
			if (wizard instanceof INewModelWizard modelWizard) {
				modelWizard.setCategory(category);
			}
			var dialog = new WizardDialog(UI.shell(), wizard);
			dialog.open();
			Navigator.refresh(parent);
		} catch (CoreException e) {
			ErrorReporter.on("Open model wizard failed", e);
		}
	}

	private String getWizardId() {
		if (type == null)
			return null;
		return "wizards.new." + type.getModelClass().getSimpleName().toLowerCase();
	}

	@Override
	public String getText() {
		if (type == null)
			return M.Unknown + "?";
		return switch (type) {
			case ACTOR -> M.NewActor;
			case CURRENCY -> M.NewCurrency;
			case FLOW -> M.NewFlow;
			case FLOW_PROPERTY -> M.NewFlowProperty;
			case IMPACT_METHOD -> M.NewLCIAMethod;
			case IMPACT_CATEGORY -> M.NewImpactCategory;
			case PROCESS -> M.NewProcess;
			case PRODUCT_SYSTEM -> M.NewProductSystem;
			case PROJECT -> M.NewProject;
			case SOCIAL_INDICATOR -> M.NewSocialIndicator;
			case SOURCE -> M.NewSource;
			case UNIT_GROUP -> M.NewUnitGroup;
			case LOCATION -> M.NewLocation;
			case PARAMETER -> M.NewParameter;
			case DQ_SYSTEM -> M.NewDataQualitySystem;
			case RESULT -> "New result";
			case EPD -> "New EPD";
			default -> M.Unknown + "?";
		};
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		if (type == null)
			return null;
		return Images.descriptor(type, Overlay.NEW);
	}

}
