package org.openlca.app.results.slca;

import org.openlca.core.model.descriptors.SocialIndicatorDescriptor;

public class SocialIndex extends DescriptorIndex<SocialIndicatorDescriptor> {

	public static SocialIndex of(Iterable<SocialIndicatorDescriptor> descriptors) {
		var index = new SocialIndex();
		if (descriptors == null)
			return index;
		for (var d : descriptors) {
			index.add(d);
		}
		return index;
	}

	@Override
	public SocialIndex copy() {
		var copy = new SocialIndex();
		for (var c : content) {
			copy.add(c);
		}
		return copy;
	}
}
