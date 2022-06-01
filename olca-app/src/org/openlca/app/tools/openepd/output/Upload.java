package org.openlca.app.tools.openepd.output;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.openlca.app.App;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.io.openepd.Ec3Client;
import org.openlca.io.openepd.Ec3Response;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.EpdImpactResult;
import org.openlca.io.openepd.EpdIndicatorResult;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

import com.google.gson.JsonObject;

public record Upload(Ec3Client client, EpdDoc epd) {

	ExportState update(String id) {
		try {

			// fetch an existing EPD
			var json = App.exec("Get EPD",
				() -> NullDiff.clearNulls(jsonOf(client.getEpd(id))));
			if (json == null) {
				var create = errorAsk("EPD does not exist",
					"An EPD with this ID does not exist. " +
						"Do you want to create a new EPD instead?");
				return create
					? newDraft()
					: ExportState.canceled();
			}

			// check the unit of an existing EPD
			var oldDoc = EpdDoc.fromJson(json).orElse(null);
			if (oldDoc == null) {
				MsgBox.error("Could not read EPD from server");
				return ExportState.error();
			}

			// update the JSON object
			update(json, oldDoc);

			// update the EPD on the server
			var resp = App.exec("Upload EPD", () -> client.putEpd(id, json));
			return resp.isError()
				? error(resp, "Failed to update EPD " + id)
				: ExportState.updated(id);
		} catch (Exception e) {
			ErrorReporter.on("Failed to update EPD: " + id, e);
			return ExportState.error();
		}
	}

	private void update(JsonObject json, EpdDoc oldDoc) {

		// increment version
		json.addProperty("version", oldDoc.version + 1);

		// declared unit
		if (epd.declaredUnit != null) {
			json.add("declared_unit", epd.declaredUnit.toJson());
		}
		if (epd.kgPerDeclaredUnit != null) {
			json.add("kg_per_declared_unit", epd.kgPerDeclaredUnit.toJson());
		}

		// indicator results
		var impacts = EpdImpactResult.toJson(epd.impactResults);
		NullDiff.apply(json.get("impacts"), impacts);
		json.add("impacts", impacts);
		var resources = EpdIndicatorResult.toJson(epd.resourceUses);
		NullDiff.apply(json.get("resource_uses"), resources);
		json.add("resource_uses", resources);
		var outputs = EpdIndicatorResult.toJson(epd.outputFlows);
		NullDiff.apply(json.get("output_flows"), outputs);
		json.add("output_flows", outputs);

		if (json.has("plants")) {
			// TODO: temporary workaround until is fixed in the EC3 API
			json.remove("plants");
		}
	}

	ExportState newDraft() {
		try {

			// clear contact and PCR references
			epd.manufacturer = null;
			epd.programOperator = null;
			epd.pcr = null;
			epd.verifier = null;

			var resp = App.exec("Upload EPD", () -> client.postEpd(epd.toJson()));
			var json = jsonOf(resp);
			if (resp.isError() || json == null)
				return error(resp, "Failed to upload EPD to EC3.");

			// extract the ID from the response
			String id = Json.getString(json.getAsJsonObject(), "id");
			return Strings.nullOrEmpty(id)
				? error(resp, "No ID returned from server.")
				: ExportState.created(id);
		} catch (Exception e) {
			ErrorReporter.on("Failed to upload EPD", e);
			return ExportState.error();
		}
	}

	private JsonObject jsonOf(Ec3Response r) {
		if (r == null || r.isError() || !r.hasJson())
			return null;
		var json = r.json();
		return json != null && json.isJsonObject()
			? json.getAsJsonObject()
			: null;
	}

	private boolean errorAsk(String title, String question) {
		int r = MessageDialog.open(
			MessageDialog.ERROR, UI.shell(), title, question, SWT.NONE,
			"Continue", IDialogConstants.CANCEL_LABEL);
		return r == IDialogConstants.OK_ID;
	}

	private ExportState error(Ec3Response resp, String message) {
		if (resp.hasJson()) {
			JsonErrorDialog.show(message, resp.json());
		} else {
			MsgBox.error("Upload failed", message);
		}
		return ExportState.error();
	}

	/**
	 * Set {@code null} explicitly for properties that where deleted in an update.
	 */
	private record NullDiff(JsonObject origin, JsonObject update) {

		static JsonObject clearNulls(JsonObject obj) {
			if (obj == null)
				return null;
			var gson = new Gson();
			var json = new Gson().toJson(obj);
			return gson.fromJson(json, JsonObject.class);
		}

		static void apply(JsonElement origin, JsonElement update) {
			if (origin == null
				|| !origin.isJsonObject()
				|| update == null
				|| !update.isJsonObject())
				return;
			new NullDiff(
				origin.getAsJsonObject(),
				update.getAsJsonObject())
				.exec();
		}

		private void exec() {
			for (var prop : origin.keySet()) {
				if ("dist".equals(prop) || "rsd".equals(prop))
					continue;
				var oProp = origin.get(prop);
				var uProp = update.get(prop);
				if (uProp == null || uProp.isJsonNull()) {
					update.add(prop, JsonNull.INSTANCE);
				} else {
					NullDiff.apply(oProp, uProp);
				}
			}
		}
	}

}

