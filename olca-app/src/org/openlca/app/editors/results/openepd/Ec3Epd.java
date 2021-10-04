package org.openlca.app.editors.results.openepd;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.jsonld.Json;

public class Ec3Epd {

  public String id;
  public String name;
  public String description;

  public String declaredUnit;
  public String massPerDeclaredUnit;
  public String gwp;
  public String gwpPerKg;

  public Ec3Certifier reviewer;
  public Ec3Certifier developer;
  public Ec3Certifier verifier;

  public String docUrl;

  public static Optional<Ec3Epd> fromJson(JsonElement elem) {
    if (elem == null || !elem.isJsonObject())
      return Optional.empty();
    var obj = elem.getAsJsonObject();
    var epd = new Ec3Epd();
    epd.id = Json.getString(obj, "id");
    epd.name = Json.getString(obj, "name");
    epd.description = Json.getString(obj, "description");

    epd.declaredUnit = Json.getString(obj, "declared_unit");
    epd.massPerDeclaredUnit = Json.getString(obj, "mass_per_declared_unit");
    epd.gwp = Json.getString(obj, "gwp");
    epd.gwpPerKg = Json.getString(obj, "gwp_per_kg");

    epd.reviewer = Ec3Certifier.fromJson(obj.get("reviewer")).orElse(null);
    epd.developer = Ec3Certifier.fromJson(obj.get("developer")).orElse(null);
    epd.verifier = Ec3Certifier.fromJson(obj.get("verifier")).orElse(null);
    epd.docUrl = Json.getString(obj, "doc");

    return Optional.of(epd);
  }

  public JsonObject toJson() {
    var obj = new JsonObject();

    Json.put(obj, "id", id);
    Json.put(obj, "name", name);
    Json.put(obj, "description", description);

    Json.put(obj, "declared_unit", declaredUnit);
    Json.put(obj, "mass_per_declared_unit", massPerDeclaredUnit);
    Json.put(obj, "gwp", gwp);
    Json.put(obj, "gwp_per_kg", gwpPerKg);

    Json.put(obj, "doc", docUrl);

    if (reviewer != null) {
      obj.add("reviewer", reviewer.toJson());
    }
    if (developer != null) {
      obj.add("developer", developer.toJson());
    }
    if (verifier != null) {
      obj.add("verifier", verifier.toJson());
    }
    return obj;
  }

}
