package org.openlca.app.tools.mapping;

import java.io.File;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.Editors;
import org.openlca.app.tools.mapping.generator.Generator;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.Question;
import org.openlca.io.maps.FlowMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingMenu extends EditorActionBarContributor {

	@Override
	public void contributeToMenu(IMenuManager root) {
		MenuManager menu = new MenuManager("Flow mapping");
		root.add(menu);
		menu.add(Actions.onSave(this::onSave));
		menu.add(Actions.create("Generate ...", this::onGenerate));
	}

	private void onSave() {
		MappingTool tool = Editors.getActive();
		if (tool == null || tool.mapping == null)
			return;
		FlowMap map = tool.mapping;
		String name = map.name;
		if (name == null) {
			name = "flow_map.csv";
		} else {
			if (!name.endsWith(".csv")) {
				name += ".csv";
			}
		}
		File file = FileChooser.forExport("*.csv", name);
		if (file == null)
			return;
		App.runWithProgress("Save flow mapping ...", () -> {
			try {
				FlowMap.toCsv(map, file);
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(MappingMenu.class);
				log.error("failed to save flow mapping", e);
			}
		});
	}

	private void onGenerate() {
		MappingTool tool = Editors.getActive();
		if (tool == null || tool.mapping == null)
			return;

		IProvider source = tool.sourceSystem;
		IProvider target = tool.targetSystem;
		if (source == null || target == null) {
			Error.showBox("No source or target system selected",
					"In order to generate a mapping you need to "
							+ "assign a data provider (database, "
							+ "JSON-LD, or ILCD package) for the "
							+ "source and target system.");
			return;
		}

		boolean b = Question.ask("Generate mappings?",
				"Do you want to (try to) generate mappings for all "
						+ "unmapped flows of the source system?");
		if (!b)
			return;
		Generator gen = new Generator(source, target, tool.mapping);
		App.runWithProgress("Generate mappings ...", gen, () -> {
			tool.refresh();
		});
	}
}
