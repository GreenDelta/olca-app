package org.openlca.app.editors.projects.reports.model;

import java.util.Arrays;
import java.util.Objects;

import com.google.gson.JsonObject;
import org.openlca.core.model.Currency;
import org.openlca.core.model.ProjectVariant;
import org.openlca.jsonld.Json;
import org.slf4j.LoggerFactory;


class ReportCostResult {

  final String variant;
  final String currency;
  final double value;

  private ReportCostResult(String variant, String currency, double value) {
    this.variant = variant;
    this.currency = currency;
    this.value = value;
  }

	static ReportCostResult fromJson(JsonObject obj) {
		if (obj == null)
			return null;

		var log = LoggerFactory.getLogger(ReportCostResult.class);

		// Make sure the report is not of an old version.
		for (String constructorFieldName
			: Arrays.asList("variant", "currency", "value")) {
			if (!(obj.has(constructorFieldName))) {
				log.warn("Failed to parse the {} of the cost result of the report.",
					constructorFieldName);
				return null;
			}
		}

		return new ReportCostResult(
			Json.getString(obj, "variant"),
			Json.getString(obj, "currency"),
			Json.getDouble(obj, "value", 0)
		);
	}

  static ReportCostResult of(
    ProjectVariant variant, Currency currency, double value) {
    var v = variant == null || variant.name == null
      ? ""
      : variant.name;
    var c = currency == null
      ? "?"
      : Objects.requireNonNullElseGet(currency.code,
      () -> Objects.requireNonNullElse(currency.name, "?"));
    return new ReportCostResult(v, c, value);
  }
}
