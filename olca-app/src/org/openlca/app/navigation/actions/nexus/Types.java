package org.openlca.app.navigation.actions.nexus;

public interface Types {

	public enum ModelType {
		PROCESS,
		PRODUCT_SYSTEM;
	}

	public enum ProcessType {
		UNIT,
		PARTIALLY_AGGREGATED,
		FULLY_AGGREGATED,
		BRIDGE,
		UNKNOWN;
	}

	public enum RepresentativenessType {
		SCIENTIFIC,
		EXPERT_BASED;
	}

	public enum ModelingType {
		ATTRIBUTIONAL,
		CONSEQUENTIAL,
		UNKNOWN;
	}

	public enum MultifunctionalModeling {
		PHYSICAL,
		ECONOMIC,
		CAUSAL,
		SYSTEM_EXPANSION,
		NONE,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum BiogenicCarbonModeling {
		OMITTED,
		DISTINGUISHED,
		AGGREGATED,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum EndOfLifeModeling {
		CUT_OFF,
		PHYSICAL_APOS,
		ECONOMIC_APOS,
		SUBSTITUTION,
		OTHER,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum WaterModeling {
		AMOUNTS,
		AMOUNTS_AND_AVAILABILITY,
		AMOUNTS_AND_QUALITY,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum InfrastructureModeling {
		INCLUDED_AND_DISTINGUISHED,
		INCLUDED_AND_NOT_VISIBLE,
		NOT_INCLUDED,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum EmissionModeling {
		INCLUDED_AND_DISTINGUISHED,
		INCLUDED_AND_NOT_VISIBLE,
		NOT_INCLUDED,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum CarbonStorageModeling {
		INCLUDED_AND_DISTINGUISHED_CORRECTION,
		INCLUDED_AND_DISTINGUISHED_OTHER,
		INCLUDED_AND_NOT_VISIBLE,
		NOT_INCLUDED,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum SourceReliability {
		MEASURED_VERIFIED,
		PARTLY_MEASURED_VERIFIED,
		PARTLY_MEASURED_PARTLY_ESTIMATED,
		ESTIMATED_QUALIFIED,
		ESTIMATED_UNQUALIFIED;
	}

	public enum AggregationType {
		HORIZONTAL,
		VERTICAL,
		COMBINED,
		UNKNOWN,
		NOT_APPLICABLE;
	}

	public enum ReviewType {
		INTERNAL,
		EXTERNAL,
		PANEL,
		UNKNOWN,
		NONE;
	}

	public enum ReviewSystem {
		ILCD,
		PEF,
		GHG,
		LCA_UN,
		ISO,
		OTHER,
		UNKNOWN,
		NOT_APPLICABLE;
	}

}
