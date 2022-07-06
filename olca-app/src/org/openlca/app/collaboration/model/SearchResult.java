package org.openlca.app.collaboration.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.ModelType;

public record SearchResult(List<Dataset> data, ResultInfo resultInfo) {

	public SearchResult() {
		this(new ArrayList<>(), 0, 10, 0);
	}

	public SearchResult(List<Dataset> data, int currentPage, int pageSize, int totalCount) {
		this(data, new ResultInfo(currentPage, pageSize, data.size(), totalCount));
	}

	public record Dataset(ModelType type, String refId, String name, String category, String repositoryId,
			String commitId) {
	}

	public record ResultInfo(int currentPage, int pageSize, int count, int totalCount) {

		public int pageCount() {
			return (int) Math.ceil(totalCount / (double) pageSize);
		}

	}

}
