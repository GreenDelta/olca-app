package org.openlca.app.tools.params;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.ParameterRedef;

class ParamSeq {

	private final List<Param> params;
	private final List<List<ParameterRedef>> seqs;

	private ParamSeq(List<Param> params, List<List<ParameterRedef>> seqs) {
		this.seqs = seqs;
		this.params = params;
	}

	static ParamSeq of(List<Param> params, int count) {
		var seqs = new ArrayList<List<ParameterRedef>>(count);
		for (int i = 0; i < count; i++) {
			seqs.add(new ArrayList<>());
		}
		for (var param : params) {
			double incx = (param.end - param.start) / (count - 1);
			for (int i = 0; i < count; i++) {
				var list = seqs.get(i);
				var redef = param.redef.copy();
				redef.value = param.start + i*incx;
				list.add(redef);
			}
		}
		return new ParamSeq(params, seqs);
	}

	List<ParameterRedef> get(int i) {
		return i >= 0 && i < seqs.size()
				? seqs.get(i)
				: List.of();
	}

	List<Param> params() {
		return params;
	}

	int count() {
		return seqs.size();
	}

}
