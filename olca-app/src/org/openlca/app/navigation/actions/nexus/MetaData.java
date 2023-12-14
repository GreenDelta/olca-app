package org.openlca.app.navigation.actions.nexus;

import java.util.List;

import org.openlca.app.navigation.actions.nexus.Types.AggregationType;
import org.openlca.app.navigation.actions.nexus.Types.BiogenicCarbonModeling;
import org.openlca.app.navigation.actions.nexus.Types.CarbonStorageModeling;
import org.openlca.app.navigation.actions.nexus.Types.EmissionModeling;
import org.openlca.app.navigation.actions.nexus.Types.EndOfLifeModeling;
import org.openlca.app.navigation.actions.nexus.Types.InfrastructureModeling;
import org.openlca.app.navigation.actions.nexus.Types.ModelingType;
import org.openlca.app.navigation.actions.nexus.Types.MultifunctionalModeling;
import org.openlca.app.navigation.actions.nexus.Types.RepresentativenessType;
import org.openlca.app.navigation.actions.nexus.Types.ReviewSystem;
import org.openlca.app.navigation.actions.nexus.Types.ReviewType;
import org.openlca.app.navigation.actions.nexus.Types.SourceReliability;
import org.openlca.app.navigation.actions.nexus.Types.WaterModeling;

class MetaData {
	
	List<String> supportedNomenclatures;
	List<String> lciaMethods;
	List<ModelingType> modelingType;
	List<MultifunctionalModeling> multifunctionalModeling;
	List<BiogenicCarbonModeling> biogenicCarbonModeling;
	List<EndOfLifeModeling> endOfLifeModeling;
	List<WaterModeling> waterModeling;
	List<InfrastructureModeling> infrastructureModeling;
	List<EmissionModeling> emissionModeling;
	List<CarbonStorageModeling> carbonStorageModeling;
	List<ReviewType> reviewType;
	List<ReviewSystem> reviewSystem;

	// process
	List<RepresentativenessType> representativenessType;
	List<SourceReliability> sourceReliability;
	List<AggregationType> aggregationType;
	List<String> systemModel;

	// product system
	String creator;
	String intendedAudience;	
	
	boolean exportSystems;

}
