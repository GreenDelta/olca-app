package org.openlca.app.editors.projects.reports.model;

import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Process;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

class ReportParameter {

	final ParameterRedef redef;
  final Descriptor context;
	final Map<String, Double> variantValues = new HashMap<>();

	private ReportParameter(ParameterRedef redef, Descriptor context) {
	  this.redef = redef;
	  this.context = context;
  }

	static ReportParameter fromJson(JsonObject obj) {
		if (obj == null)
			return null;

		var log = LoggerFactory.getLogger(ReportParameter.class);

		// TODO (M) Synchronize with the database instead of parsing the Json.
		var redef = new Gson().fromJson(
			Json.getObject(obj, "redef"),
			ParameterRedef.class
		);

		var context = new Gson().fromJson(
			Json.getObject(obj, "context"),
			Descriptor.class
		);

		var reportParameter = new ReportParameter(redef, context);

		var variantValues = Json.getObject(obj, "variantValues");
		if (variantValues == null) {
			log.warn("Failed to parse the variant values of the report's parameters.");
		} else {
			Type variantValuesMapType = new TypeToken<Map<String, Double>>() {}
				.getType();
			Map<String, Double> variantValuesMap = new Gson()
				.fromJson(variantValues, variantValuesMapType);
			reportParameter.variantValues.putAll(variantValuesMap);
		}

		return reportParameter;
	}

  static List<ReportParameter> allOf(IDatabase db, Project project) {
	  if (db == null || project == null)
	    return Collections.emptyList();
    var created = new HashMap<String, ReportParameter>();
    for (var variant : project.variants) {
      if (variant.isDisabled)
        continue;
      for (var redef : variant.parameterRedefs) {
        var param = created.computeIfAbsent(
          keyOf(redef), $ -> createFor(db, redef));
        param.variantValues.put(variant.name, redef.value);
      }
    }

    var params = new ArrayList<>(created.values());
    params.sort((p1, p2) -> {
      int c = Strings.compare(p1.redef.name, p2.redef.name);
      if (c != 0)
        return c;
      if (p1.context == null && p2.context == null)
        return 0;
      if (p1.context == null)
        return -1;
      if (p2.context == null)
        return 1;
      return Strings.compare(p1.context.name, p2.context.name);
    });
    return params;
  }

  private static ReportParameter createFor(IDatabase db, ParameterRedef redef) {
    Descriptor context = null;
    if (redef.contextId != null) {
      context = redef.contextType == ModelType.PROCESS
        ? db.getDescriptor(Process.class, redef.contextId)
        : db.getDescriptor(ImpactCategory.class, redef.contextId);
    }
    return new ReportParameter(redef, context);
  }

  private static String keyOf(ParameterRedef redef) {
    if (redef == null || redef.name == null)
      return "";
    return redef.contextId != null
      ? redef.name + redef.contextId
      : redef.name;
  }

}
