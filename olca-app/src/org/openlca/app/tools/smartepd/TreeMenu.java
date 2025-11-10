package org.openlca.app.tools.smartepd;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.smartepd.TreeModel.EpdNode;
import org.openlca.app.tools.smartepd.TreeModel.ProjectNode;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.Question;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Epd;
import org.openlca.core.model.ModelType;
import org.openlca.io.smartepd.SmartEpd;
import org.openlca.io.smartepd.SmartEpdClient;
import org.openlca.io.smartepd.SmartEpdWriter;
import org.openlca.jsonld.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TreeMenu {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final SmartEpdClient client;
	private final TreeViewer tree;

	TreeMenu(SmartEpdClient client, TreeViewer tree) {
		this.client = client;
		this.tree = tree;
	}

	static void mountOn(SmartEpdClient client, TreeViewer tree) {
		new TreeMenu(client, tree).mount();
	}

	private void mount() {
		var manager = new MenuManager();
		var menu = manager.createContextMenu(tree.getControl());
		tree.getControl().setMenu(menu);

		tree.addSelectionChangedListener($ -> {
			manager.removeAll();
			var obj = Viewers.getFirstSelected(tree);
			if (obj instanceof ProjectNode pNode) {
				manager.add(new UploadEpdAction(pNode));
			} else if (obj instanceof EpdNode eNode) {
				manager.add(new UpdateEpdAction(eNode));
				manager.add(new DeleteEpdAction(eNode));
			}
		});
	}

	private SmartEpd getSmartEpd(EpdNode node) {
		if (node == null)
			return null;
		var res = App.exec("Get EPD...", () -> client.getEpd(node.id()));
		if (res.isError()) {
			MsgBox.error("Failed to get EPD from server", res.error());
			return null;
		}
		if (log.isTraceEnabled()) {
			log.trace("got EPD from server:\n{}",
					Json.toPrettyString(res.value().json()));
		}
		return res.value();
	}

	private Epd selectEpd() {
		var db = assertDb();
		if (db == null)
			return null;
		var d = ModelSelector.select(ModelType.EPD);
		if (d == null)
			return null;
		return db.get(Epd.class, d.id);
	}

	private IDatabase assertDb() {
		var db = Database.get();
		if (db == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NoDatabaseOpenedInfo);
			return null;
		}
		return db;
	}

	private class UploadEpdAction extends Action {

		private final ProjectNode node;

		UploadEpdAction(ProjectNode node) {
			this.node = node;
			setText("Upload EPD");
			setImageDescriptor(Icon.EXPORT.descriptor());
		}

		@Override
		public void run() {
			var epd = selectEpd();
			if (epd == null)
				return;
			var smartEpd = SmartEpdWriter.of(epd).write();
			smartEpd.project(node.id());
			log.info("uploading new EPD {}", smartEpd.productName());
			if (log.isTraceEnabled()) {
				log.trace("posting payload:\n{}", Json.toPrettyString(smartEpd.json()));
			}

			var res = client.postEpd(smartEpd);
			if (res.isError()) {
				MsgBox.error("Failed to upload EPD", res.error());
				return;
			}

			node.epdNodes().add(new EpdNode(node, smartEpd));
			tree.refresh();
			Popup.info("EPD uploaded", "The EPD was successfully uploaded.");
		}
	}

	private class UpdateEpdAction extends Action {

		private final EpdNode node;

		UpdateEpdAction(EpdNode node) {
			this.node = node;
			setText("Update EPD");
			setImageDescriptor(Icon.UPDATE.descriptor());
		}

		@Override
		public void run() {
			var epd = selectEpd();
			if (epd == null)
				return;
			var smartEpd = getSmartEpd(node);
			if (smartEpd == null)
				return;

			var q = Question.ask("Update EPD?",
					"Do you want to update the results of the EPD?");
			if (!q)
				return;

			SmartEpdWriter.of(epd).update(smartEpd);
			log.info("updating EPD: name='{}' id='{}'",
					smartEpd.productName(), smartEpd.id());
			if(log.isTraceEnabled()) {
				log.trace("sending patch payload:\n{}",
						Json.toPrettyString(smartEpd.json()));
			}

			var res = client.putEpd(smartEpd);
			if (res.isError()) {
				MsgBox.error("Failed to update EPD", res.error());
			} else {
				Popup.info("EPD updated", "The EPD was successfully updated.");
			}
		}
	}

	private class DeleteEpdAction extends Action {

		private final EpdNode node;

		DeleteEpdAction(EpdNode node) {
			this.node = node;
			setText("Delete EPD");
			setImageDescriptor(Icon.DELETE.descriptor());
		}

		@Override
		public void run() {
			var b = Question.ask("Delete EPD?",
					"Do you really want to delete this EPD?");
			if (!b)
				return;
			log.info("deleting EPD: name='{}' id='{}'",
					node.name(), node.id());
			var res = client.deleteEpd(node.id());
			if (res.isError()) {
				MsgBox.error("Failed to delete EPD", res.error());
				return;
			}

			node.parent().epdNodes().remove(node);
			tree.refresh();
			Popup.info("EPD deleted", "The EPD was successfully deleted.");
		}
	}

}
