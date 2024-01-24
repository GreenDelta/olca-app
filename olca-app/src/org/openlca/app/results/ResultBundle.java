package org.openlca.app.results;

import org.openlca.app.results.slca.SocialResult;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;

import java.util.Objects;

public class ResultBundle {

	private final CalculationSetup setup;
	private final LcaResult result;
	private final ResultItemOrder items;
	private DQResult dqResult;
	private SocialResult socialResult;

	private ResultBundle(CalculationSetup setup, LcaResult result) {
		this.setup = Objects.requireNonNull(setup);
		this.result = Objects.requireNonNull(result);
		this.items = ResultItemOrder.of(result);
		Sort.sort(items);
	}

	public static ResultBundle of(CalculationSetup setup, LcaResult result) {
		return new ResultBundle(setup, result);
	}

	public ResultBundle with(DQResult dqResult) {
		this.dqResult = dqResult;
		return this;
	}

	public ResultBundle with(SocialResult socialResult) {
		this.socialResult = socialResult;
		return this;
	}

	public CalculationSetup setup() {
		return setup;
	}

	public LcaResult result() {
		return result;
	}

	public ResultItemOrder items() {
		return items;
	}

	public DQResult dqResult() {
		return dqResult;
	}

	public SocialResult socialResult() {
		return socialResult;
	}

	public boolean hasSocialResult() {
		return socialResult != null;
	}
}
