/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;

/**
 * Container node for the {@link ExchangeNode}s
 * 
 * @author Sebastian Greve
 * 
 */
public class ExchangeContainerNode extends Node {

	/**
	 * Constructor for a new ExchangeContainerNode
	 * 
	 * @param exchanges
	 *            - The exchanges for which {@link ExchangeNode}s will be
	 *            created an added to this container
	 */
	public ExchangeContainerNode(final Exchange[] exchanges) {
		final List<Exchange> inputs = new ArrayList<>();
		// for each non elementary input
		for (final Exchange e : exchanges) {
			if (e.isInput()
					&& (e.getFlow().getFlowType() == FlowType.PRODUCT_FLOW || e
							.getFlow().getFlowType() == FlowType.WASTE_FLOW)) {
				inputs.add(e);
			}
		}
		final List<Exchange> outputs = new ArrayList<>();
		// for each non elementary output
		for (final Exchange e : exchanges) {
			if (!e.isInput()
					&& (e.getFlow().getFlowType() == FlowType.PRODUCT_FLOW || e
							.getFlow().getFlowType() == FlowType.WASTE_FLOW)) {
				outputs.add(e);
			}
		}

		final boolean inputsAreBiggerThanOutputs = inputs.size() > outputs
				.size();
		final int min = Math.min(inputs.size(), outputs.size());

		// sort inputs and outputs
		Collections.sort(inputs, new ExchangeComparator());
		Collections.sort(outputs, new ExchangeComparator());

		// create exchange nodes
		for (int i = 0; i < min; i++) {
			addChild(new ExchangeNode(inputs.get(i)));
			addChild(new ExchangeNode(outputs.get(i)));
		}

		final int max = Math.max(inputs.size(), outputs.size());
		// create additional left or right node
		for (int i = min; i < max; i++) {
			if (inputsAreBiggerThanOutputs) {
				addChild(new ExchangeNode(inputs.get(i)));
				addChild(new DummyNode());
			} else {
				addChild(new DummyNode());
				addChild(new ExchangeNode(outputs.get(i)));
			}
		}
	}

	@Override
	public void dispose() {
		for (final Node node : getChildrenArray()) {
			node.dispose();
		}
		getChildrenArray().clear();
	}

	/**
	 * Comparator for exchanges
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private final class ExchangeComparator implements Comparator<Exchange> {

		@Override
		public int compare(final Exchange o1, final Exchange o2) {
			final String s1 = o1.getFlow().getName().toLowerCase();
			final String s2 = o2.getFlow().getName().toLowerCase();
			int length = s1.length();
			if (length > s2.length()) {
				length = s2.length();
			}
			for (int i = 0; i < length; i++) {
				if (s1.charAt(i) > s2.charAt(i)) {
					return 1;
				} else if (s1.charAt(i) < s2.charAt(i)) {
					return -1;
				}
			}
			return 0;
		}

	};

}
