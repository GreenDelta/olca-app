package org.openlca.app.tools.openepd.export;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.Epd;
import org.openlca.core.model.ImpactMethod;
import org.openlca.io.openepd.Vocab;

public class MappingModel {

	private ImpactMethod method;
	private Vocab.Method epdMethod;

	private final List<String> scopes = new ArrayList<>();
	private final List<MappingRow> rows = new ArrayList<>();

	public static List<MappingModel> allOf(Epd epd) {
		return MappingBuilder.buildFrom(epd);
	}

	public ImpactMethod method() {
		return method;
	}

	public MappingModel method(ImpactMethod method) {
		this.method = method;
		return this;
	}

	public Vocab.Method epdMethod() {
		return epdMethod;
	}

	public MappingModel epdMethod(Vocab.Method epdMethod) {
		this.epdMethod = epdMethod;
		return this;
	}

	public List<String> scopes() {
		return scopes;
	}

	public List<MappingRow> rows() {
		return rows;
	}

}
