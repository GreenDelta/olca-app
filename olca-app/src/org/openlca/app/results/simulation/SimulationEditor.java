package org.openlca.app.results.simulation;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.Simulator;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulationEditor extends SimpleFormEditor {

	public static String ID = "SimulationEditor";

	CalculationSetup setup;
	Simulator simulator;

	/**
	 * Initializes the Monte Carlo simulation of a product system and opens the
	 * editor.
	 */
	public static void open(CalculationSetup setup, MatrixCache mcache) {
		try {
			Simulator sim = Simulator.create(
					setup, mcache, App.getSolver());
			String setupKey = Cache.getAppCache().put(setup);
			String simKey = Cache.getAppCache().put(sim);
			SimulationInput input = new SimulationInput(setupKey, simKey);
			Editors.open(input, ID);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(SimulationEditor.class);
			log.error("Simulation initialisation failed", e);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		super.init(site, editorInput);
		SimulationInput input = (SimulationInput) editorInput;
		setup = Cache.getAppCache().remove(
				input.setupKey, CalculationSetup.class);
		setPartName(Strings.cut(setup.productSystem.name, 75));
		simulator = Cache.getAppCache()
				.remove(input.solverKey, Simulator.class);
	}

	@Override
	protected FormPage getPage() {
		return new SimulationPage(this);
	}

	private static class SimulationInput implements IEditorInput {

		final String setupKey;
		final String solverKey;

		public SimulationInput(String setupKey, String solverKey) {
			this.setupKey = setupKey;
			this.solverKey = solverKey;
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.SIMULATE.descriptor();
		}

		@Override
		public String getName() {
			return M.MonteCarloSimulation;
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return getName();
		}

	}

}
