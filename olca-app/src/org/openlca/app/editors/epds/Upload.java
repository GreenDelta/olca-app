package org.openlca.app.editors.epds;

import java.util.function.Supplier;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.io.openepd.Ec3Client;
import org.openlca.io.openepd.Ec3Response;
import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.EpdImpactResult;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

import com.google.gson.JsonObject;

public record Upload(Ec3Client client, EpdDoc epd) {

	ExportState update(String id) {
		try {

			// fetch an existing EPD
			var json = jsonOf(client.getEpd(id));
			if (json == null) {
				var create = errorAsk("EPD does not exist",
					"An EPD with this ID does not exist. " +
						"Do you want to create a new EPD instead?");
				return create
					? newDraft()
					: ExportState.canceled();
			}

			// check the unit of an existing EPD
			var existing = EpdDoc.fromJson(json).orElse(null);
			if (existing == null) {
				MsgBox.error("Could not read EPD from server");
				return ExportState.error();
			}
			var unitErr = checkUnitOf(existing);
			if (unitErr != null) {
				var doit = errorAsk(
					"Different units",
					"The declared unit of the EPD on EC3 is different: " + unitErr
						+ ". Do you want to continue anyway and replace the results?");
				if (!doit)
					return ExportState.canceled();
			}

			// update the EPD on the server
			json.addProperty("version", existing.version + 1);
			var impacts = EpdImpactResult.toJson(epd.impactResults);
			json.add("impacts", impacts);
			if (json.has("plants")) {
				// TODO: temporary workaround until is fixed in the EC3 API
				json.remove("plants");
			}
			var resp = client.putEpd(id, json);
			return resp.isError()
				? error(resp, "Failed to update EPD " + id)
				: ExportState.updated(id);
		} catch (Exception e) {
			ErrorReporter.on("Failed to update EPD: " + id, e);
			return ExportState.error();
		}
	}

	ExportState newDraft() {
		try {

			// clear contact and PCR references
			epd.manufacturer = null;
			epd.programOperator = null;
			epd.pcr = null;
			epd.verifier = null;

			var resp = client.postEpd(epd.toJson());
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

	// Returns an error string when the declared units are not equal.
	private String checkUnitOf(EpdDoc other) {
		var a = epd.declaredUnit;
		var b = other.declaredUnit;
		Supplier<String> err = () -> {
			var sa = a != null ? a.toString() : "#none";
			var sb = b != null ? b.toString() : "#none";
			return sa + " \u2260 " + sb;
		};

		if (a == null || b == null
			|| (Math.abs(a.amount() - b.amount()) > 1e-3)
			|| !Strings.nullOrEqual(a.unit(), b.unit()))
			return err.get();
		return null;
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
}
