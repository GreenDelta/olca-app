package org.openlca.app.tools.mapping;

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
import org.openlca.app.tools.mapping.model.FlowProvider;
import org.openlca.app.tools.mapping.replacer.Replacer;
import org.openlca.app.util.Actions;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.core.io.maps.FlowMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingMenu extends EditorActionBarContributor {

	@Override
	public void contributeToMenu(IMenuManager root) {
		MenuManager menu = new MenuManager(M.FlowMapping);
		root.add(menu);
		menu.add(Actions.create(M.SaveAs, this::onSave));
		menu.add(Actions.create(M.GenerateMappings, this::onGenerate));
		menu.add(Actions.create(M.ApplyOnDatabase, this::onApply));
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
		App.runWithProgress(M.SaveFlowMappingDots, () -> {
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

		FlowProvider source = tool.sourceSystem;
		FlowProvider target = tool.targetSystem;
		if (source == null || target == null) {
			MsgBox.error(M.NoSystemSelected, M.NoSystemSelectedErr);
			return;
		}

		boolean b = Question.ask(M.GenerateMappingsQ, M.GenerateMappingsQuestion);
		if (!b)
			return;
		Generator gen = new Generator(source, target, tool.mapping);
		App.runWithProgress(M.GenerateMappingsDots, gen, () -> {
			tool.refresh();
		});
	}

	private void onApply() {
		MappingTool tool = Editors.getActive();
		if (tool == null || tool.mapping == null)
			return;

		// check if we can apply the mapping
		FlowProvider source = tool.sourceSystem;
		if (!(source instanceof DBProvider)) {
			MsgBox.error(M.SourceSystemShouldBeADatabase,
					M.SourceSystemShouldBeADatabaseErr);
			return;
		}
		FlowProvider target = tool.targetSystem;
		if (target == null) {
			MsgBox.error(M.NoTargetSystemSelected, M.NoTargetSystemSelectedErr);
			return;
		}
		if (!tool.checked.get()) {
			MsgBox.error(M.UncheckedMappings, M.UncheckedMappingsErr);
			return;
		}

		var opt = ReplacerDialog.open(tool.mapping, target);
		if (opt.isEmpty())
			return;
		var replacer = new Replacer(opt.get());
		App.runWithProgress(M.ReplaceFlowDots, replacer, () -> {
			tool.refresh();
			Navigator.refresh();
		});
	}
}
