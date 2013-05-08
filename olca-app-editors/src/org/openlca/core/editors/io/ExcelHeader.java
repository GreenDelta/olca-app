package org.openlca.core.editors.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelHeader {

	private List<String> headers = new ArrayList<>();
	private List<IExcelHeaderEntry> entries = new ArrayList<>();
	private Map<Integer, Integer> indexMapping = new HashMap<>();

	int getHeaderSize() {
		return headers.size();
	}

	String getHeader(int count) {
		return headers.size() > count ? headers.get(count) : "";
	}

	int getEntryCount() {
		return entries.size();
	}

	IExcelHeaderEntry getEntry(int count) {
		return entries.size() > count ? entries.get(count)
				: new EmptyHeaderEntry();
	}

	int mapIndex(int from) {
		return indexMapping.containsKey(from) ? indexMapping.get(from) : from;
	}

	public void setHeaders(String[] headers) {
		if (headers != null) {
			for (String header : headers) {
				this.headers.add(header);
			}
		}
	}

	public void setEntries(IExcelHeaderEntry[] entries) {
		if (entries != null) {
			for (IExcelHeaderEntry entry : entries) {
				this.entries.add(entry);
			}
		}
	}

	public void putIndexMapping(int from, int to) {
		indexMapping.put(from, to);
	}

	private class EmptyHeaderEntry implements IExcelHeaderEntry {

		@Override
		public String getValue(int count) {
			return "";
		}

	}

}
