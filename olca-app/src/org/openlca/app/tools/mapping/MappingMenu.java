package org.openlca.app.tools.mapping;

import java.util.Optional;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.Editors;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.tools.mapping.generator.Generator;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.replacer.Replacer;
import org.openlca.app.tools.mapping.replacer.ReplacerConfig;
import org.openlca.app.util.Actions;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.io.maps.FlowMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingMenu extends EditorActionBarContributor {

	@Override
	public void contributeToMenu(IMenuManager root) {
		MenuManager menu = new MenuManager("Flow mapping");
		root.add(menu);
		menu.add(Actions.create(M.SaveAs, this::onSave));
		menu.add(Actions.create("Generate mappings", this::onGenerate));
		menu.add(Actions.create("Apply on database", this::onApply));
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
		var file = FileChooser.forSavingFile(M.Export, name);
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
			MsgBox.error("No source or target system selected",
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

	private void onApply() {
		MappingTool tool = Editors.getActive();
		if (tool == null || tool.mapping == null)
			return;

		// check if we can apply the mapping
		IProvider source = tool.sourceSystem;
		if (!(source instanceof DBProvider)) {
			MsgBox.error("Source system should be a database",
					"This only works when the source system "
							+ "is a database where the flows should "
							+ "be replaced with flows from the target "
							+ "system (which could be the same database).");
			return;
		}
		IProvider target = tool.targetSystem;
		if (target == null) {
			MsgBox.error("No target system selected",
					"No target system was selected.");
			return;
		}
		if (!tool.checked.get()) {
			MsgBox.error("Unchecked mappings",
					"You should first run a check before applying the mapping.");
			return;
		}

		Optional<ReplacerConfig> opt = ReplacerDialog.open(
				tool.mapping, target);
		if (!opt.isPresent())
			return;
		Replacer replacer = new Replacer(opt.get());
		App.runWithProgress("Replace flows ...", replacer, () -> {
			tool.refresh();
			Navigator.refresh();
		});
	}
}
