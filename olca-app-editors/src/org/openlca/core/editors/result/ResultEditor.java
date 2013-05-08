/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.result;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.application.views.ResultEditorInput;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.results.LCIAResult;
import org.openlca.core.model.results.LCIResult;

/**
 * Editor for displaying the results of a calculation of a product system
 * 
 * @author Sebastian Greve
 * 
 */
public class ResultEditor extends ModelEditor {

	public static String ID = "org.openlca.core.editors.result.ResultEditor";
	private LCIAResult impactResult;
	private LCIResult inventoryResult;

	protected LCIAResult getLCIAResult() {
		return impactResult;
	}

	protected LCIResult getLCIResult() {
		return inventoryResult;
	}

	@Override
	protected ModelEditorPage[] initPages() {
		List<ModelEditorPage> pages = new ArrayList<>();
		ResultInfo info = createResultInfo();
		pages.add(new ResultInfoPage(this, info));
		if (inventoryResult != null)
			pages.add(new InventoryPage(this, inventoryResult
					.getProductSystemName(), inventoryResult.getInventory()));
		if (impactResult != null && impactResult.getLCIAMethod() != null)
			addImpactPages(pages);
		return pages.toArray(new ModelEditorPage[pages.size()]);
	}

	private void addImpactPages(List<ModelEditorPage> pages) {
		pages.add(new CharacterizationPage(this, impactResult.getName(),
				impactResult.getLCIACategoryResults(), null,
				Messages.Results_Characterization, "CharacterizationPage",
				CharacterizationPage.IMPACT_ASSESSMENT));
		if (impactResult.getNormalizationWeightingSet() != null) {
			pages.add(new CharacterizationPage(this, impactResult.getName(),
					impactResult.getLCIACategoryResults(), null,
					Messages.Results_Normalization, "NormalizationPage",
					CharacterizationPage.NORMALIZATION));
			pages.add(new CharacterizationPage(this, impactResult.getName(),
					impactResult.getLCIACategoryResults(), impactResult
							.getWeightingUnit(), Messages.Results_Weighting,
					"WeightingPage", CharacterizationPage.WEIGHTING));
		}
	}

	private ResultInfo createResultInfo() {
		ResultInfo info = new ResultInfo();
		if (inventoryResult != null) {
			info.setProductSystem(inventoryResult.getProductSystemName());
			String product = inventoryResult.getTargetAmount() + " "
					+ inventoryResult.getUnitName() + " "
					+ inventoryResult.getProductName();
			info.setProductFlow(product);
		}
		if (impactResult != null) {
			info.setProductSystem(impactResult.getProductSystem());
			String product = impactResult.getTargetAmount() + " "
					+ impactResult.getUnit() + " " + impactResult.getProduct();
			info.setProductFlow(product);
			info.setImpactMethod(impactResult.getLCIAMethod());
			info.setNwSet(impactResult.getNormalizationWeightingSet());
		}
		if (inventoryResult != null) {
			String calc = inventoryResult.getCalculationMethod();
			info.setCalculationMethod(calc);
		}
		return info;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		SaveAsProcessHandler handler = new SaveAsProcessHandler(getDatabase(),
				inventoryResult);
		handler.run();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input instanceof ResultEditorInput) {
			ResultEditorInput eInput = (ResultEditorInput) input;
			setDatabase(eInput.getDatabase());
			this.impactResult = eInput.getImpactResult();
			this.inventoryResult = eInput.getLCIResult();
		} else if (input instanceof ModelEditorInput) {
			ModelEditorInput eInput = (ModelEditorInput) input;
			setDatabase(eInput.getDatabase());
			try {
				this.impactResult = getDatabase().select(LCIAResult.class,
						eInput.getDescriptor().getId());
			} catch (DataProviderException e) {
				log.error("Reading LCIA result from db failed", e);
			}
		}
		setSite(site);
		setInput(input);
		setName();
	}

	private void setName() {
		String name = "";
		if (inventoryResult != null) {
			name = inventoryResult.getProductSystemName() + " - ";
			if (impactResult != null && impactResult.getLCIAMethod() != null) {
				name += impactResult.getLCIAMethod();
				if (impactResult.getNormalizationWeightingSet() != null) {
					name += " - " + impactResult.getNormalizationWeightingSet();
				}
			} else {
				name += "LCI";
			}
		} else if (impactResult != null) {
			name = impactResult.getProductSystem();
			if (impactResult.getLCIAMethod() != null) {
				name += " - " + impactResult.getLCIAMethod();
			}
		}
		setPartName(name);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return inventoryResult != null;
	}

	@Override
	public void setFocus() {
	}

}
