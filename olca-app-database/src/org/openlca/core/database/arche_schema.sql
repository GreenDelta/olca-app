-- DROP DATABASE IF EXISTS openlca;
-- CREATE DATABASE openlca;
-- USE openLCA;

-- the version of the openLCA client
DROP TABLE IF EXISTS openlca_version;
CREATE TABLE openlca_version (

	id VARCHAR(36) NOT NULL,
	no VARCHAR(255),	
	name VARCHAR(255), 
	eclipse VARCHAR(255), 
	PRIMARY KEY (ID)
	
)ENGINE = MYISAM;


-- the category tree of the database
DROP TABLE IF EXISTS tbl_categories;
CREATE TABLE tbl_categories (

	id VARCHAR(255) NOT NULL, 
	name VARCHAR(255), 
	componentclass VARCHAR(255), 
	f_parentcategory VARCHAR(255), 
	
	PRIMARY KEY (id)

)ENGINE = MYISAM;


-- references between child and parent categories
ALTER TABLE tbl_categories ADD CONSTRAINT FK_tbl_categories_f_parentcategory 
	FOREIGN KEY (f_parentcategory) REFERENCES tbl_categories (id);


-- actors (= contact data sets) for administrative information
DROP TABLE IF EXISTS tbl_actors;
CREATE TABLE tbl_actors (

	id VARCHAR(36) NOT NULL, 
	telefax VARCHAR(255), 
	website VARCHAR(255), 
	address VARCHAR(255), 
	description TEXT(64000), 
	zipcode VARCHAR(255), 
	name VARCHAR(255), 
	categoryid VARCHAR(255), 
	email VARCHAR(255), 
	telephone VARCHAR(255), 
	country VARCHAR(255), 
	city VARCHAR(255), 
	
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- geographical locations
DROP TABLE IF EXISTS tbl_locations;
CREATE TABLE tbl_locations (

	id VARCHAR(36) NOT NULL, 
	description TEXT(64000), 
	name VARCHAR(255), 
	longitude DOUBLE, 
	code VARCHAR(255), 
	latitude DOUBLE, 
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- data sources for modelling and administrative information of processes
DROP TABLE IF EXISTS tbl_sources;
CREATE TABLE tbl_sources (

	id VARCHAR(36) NOT NULL, 
	description TEXT(64000), 
	categoryid VARCHAR(36), 
	name VARCHAR(255), 
	year SMALLINT, 
	textreference TEXT(64000), 
	doi VARCHAR(255), 
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- units
DROP TABLE IF EXISTS tbl_units;
CREATE TABLE tbl_units (

	id VARCHAR(36) NOT NULL,
	conversionfactor DOUBLE, 
	description TEXT(64000), 
	name VARCHAR(255),
	synonyms VARCHAR(255),
	f_unitgroup VARCHAR(36),
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- unit groups 
DROP TABLE IF EXISTS tbl_unitgroups;
CREATE TABLE tbl_unitgroups (

	id VARCHAR(36) NOT NULL, 
	description TEXT(64000), 
	categoryid VARCHAR(36), 
	name VARCHAR(255), 
	f_referenceunit VARCHAR(36),
	f_defaultflowproperty VARCHAR(36), 
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_unitgroups_f_referenceunit 
		FOREIGN KEY (f_referenceunit) REFERENCES tbl_units (id)
	
)ENGINE = MYISAM;


-- set the reference between units and unit group
ALTER TABLE tbl_units ADD CONSTRAINT FK_tbl_units_f_unitgroup 
	FOREIGN KEY (f_unitgroup) REFERENCES tbl_unitgroups (id);

	
-- flow properties
DROP TABLE IF EXISTS tbl_flowproperties;
CREATE TABLE tbl_flowproperties (

	id VARCHAR(36) NOT NULL, 
	flowpropertytype INTEGER, 
	description TEXT(64000), 
	unitgroupid VARCHAR(36), 
	categoryid VARCHAR(36), 
	name VARCHAR(255), 
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- reference for a default flow property of a unit group
ALTER TABLE tbl_unitgroups ADD CONSTRAINT FK_tbl_unitgroups_f_defaultflowproperty 
	FOREIGN KEY (f_defaultflowproperty) REFERENCES tbl_flowproperties (id);



-- flows (elementary, product, or waste flows)
DROP TABLE IF EXISTS tbl_flows;
CREATE TABLE tbl_flows (

	id VARCHAR(36) NOT NULL, 
	flowtype INTEGER, 
	description TEXT(64000),
	categoryid VARCHAR(36), 
	name VARCHAR(255),
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- flow information
DROP TABLE IF EXISTS tbl_flowinformations;
CREATE TABLE tbl_flowinformations (

	id VARCHAR(36) NOT NULL, 
	infrastructureflow TINYINT(1) default 0, 
	casnumber VARCHAR(255), 
	formula VARCHAR(255), 
	f_referenceflowproperty VARCHAR(36), 
	f_location VARCHAR(36), 
	
	PRIMARY KEY (id),
	
	CONSTRAINT FK_tbl_flowinformations_f_location 
		FOREIGN KEY (f_location) REFERENCES tbl_locations (id),
	CONSTRAINT FK_tbl_flowinformations_f_referenceflowproperty 
		FOREIGN KEY (f_referenceflowproperty) REFERENCES tbl_flowproperties (id)
)ENGINE = MYISAM;


-- conversion factors between flow properties related to a flow
DROP TABLE IF EXISTS tbl_flowpropertyfactors;
CREATE TABLE tbl_flowpropertyfactors (

	id VARCHAR(36) NOT NULL, 
	conversionfactor DOUBLE, 
	f_flowproperty VARCHAR(36),
	f_flowinformation VARCHAR(36),
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_flowpropertyfactors_f_flowproperty 
		FOREIGN KEY (f_flowproperty) REFERENCES tbl_flowproperties (id),
	CONSTRAINT FK_tbl_flowpropertyfactors_f_flowinformation 
		FOREIGN KEY (f_flowinformation) REFERENCES tbl_flowinformations (id)
	
)ENGINE = MYISAM;


-- processes
DROP TABLE IF EXISTS tbl_processes;
CREATE TABLE tbl_processes (

	id VARCHAR(36) NOT NULL, 
	processtype INTEGER, 
	allocationmethod INTEGER, 
	infrastructureprocess TINYINT(1) default 0, 
	geographycomment TEXT(64000), 
	description TEXT(64000), 
	name VARCHAR(255), 
	categoryid VARCHAR(36), 
	f_quantitativereference VARCHAR(36), 
	f_location VARCHAR(36), 
	
	PRIMARY KEY (id),	
	CONSTRAINT FK_tbl_processes_f_location 
		FOREIGN KEY (f_location) REFERENCES tbl_locations (id)

)ENGINE = MYISAM;

-- process technologies
DROP TABLE IF EXISTS tbl_technologies;
CREATE TABLE tbl_technologies (
	
	id VARCHAR(36) NOT NULL,
	description TEXT(64000),
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- valid time spans of processes
DROP TABLE IF EXISTS tbl_times;
CREATE TABLE tbl_times (

	id VARCHAR(36) NOT NULL, 
	startdate DATE, 
	enddate DATE, 
	comment TEXT(64000),
	
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- modelling and validation entries of processes
DROP TABLE IF EXISTS tbl_modelingandvalidations;
CREATE TABLE tbl_modelingandvalidations (

	id VARCHAR(36) NOT NULL,
	modelingconstants TEXT(64000),
	datatreatment TEXT(64000), 
	sampling TEXT(64000), 
	datacompleteness TEXT(64000),
	datasetotherevaluation TEXT(64000),
	lcimethod TEXT(64000), 
	datacollectionperiod TEXT(64000), 
	dataselection TEXT(64000), 
	f_reviewer VARCHAR(36), 
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_modelingandvalidations_f_reviewer 
		FOREIGN KEY (f_reviewer) REFERENCES tbl_actors (id)
)ENGINE = MYISAM;


-- sources referenced by the modelling and validation section of a process
DROP TABLE IF EXISTS tbl_modelingandvalidation_source;
CREATE TABLE tbl_modelingandvalidation_source (

	f_modelingandvalidation VARCHAR(36) NOT NULL, 
	f_source VARCHAR(36) NOT NULL,
	
	PRIMARY KEY (f_modelingandvalidation, f_source),
	CONSTRAINT tblmdelingandvalidationsourcefmdelingandvalidation 
		FOREIGN KEY (f_modelingandvalidation) REFERENCES tbl_modelingandvalidations (id),
	CONSTRAINT FK_tbl_modelingandvalidation_source_f_source 
		FOREIGN KEY (f_source) REFERENCES tbl_sources (id)

)ENGINE = MYISAM;


-- administrative information of processes
DROP TABLE IF EXISTS tbl_admininfos;
CREATE TABLE tbl_admininfos (

	id VARCHAR(36) NOT NULL, 
	project VARCHAR(255), 
	creationdate DATE, 
	intendedapplication TEXT(64000), 
	accessanduserestrictions TEXT(64000),
	copyright TINYINT(1) default 0, 
	lastchange DATE, 
	version VARCHAR(255), 
	f_datagenerator VARCHAR(36),
	f_datasetowner VARCHAR(36), 
	f_datadocumentor VARCHAR(36), 
	f_publication VARCHAR(36), 
	
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_admininfos_f_publication 
		FOREIGN KEY (f_publication) REFERENCES tbl_sources (id),
	CONSTRAINT FK_tbl_admininfos_f_datasetowner 
		FOREIGN KEY (f_datasetowner) REFERENCES tbl_actors (id),
	CONSTRAINT FK_tbl_admininfos_f_datadocumentor 
		FOREIGN KEY (f_datadocumentor) REFERENCES tbl_actors (id),
	CONSTRAINT FK_tbl_admininfos_f_datagenerator 
		FOREIGN KEY (f_datagenerator) REFERENCES tbl_actors (id)
		
)ENGINE = MYISAM;


-- process / product system inputs and outputs
DROP TABLE IF EXISTS tbl_exchanges;
CREATE TABLE tbl_exchanges (

	id VARCHAR(36) NOT NULL, 
	avoidedproduct TINYINT(1) default 0,
	distributionType INTEGER default 0, 
	input TINYINT(1) default 0, 
	f_flowpropertyfactor VARCHAR(36), 
	f_unit VARCHAR(36), 
	f_flow VARCHAR(36), 
	parametrized TINYINT(1) default 0, 
	resultingamount_value DOUBLE, 
	resultingamount_formula VARCHAR(255), 
	parameter1_value DOUBLE, 
	parameter1_formula VARCHAR(255), 
	parameter2_value DOUBLE, 
	parameter2_formula VARCHAR(255), 
	parameter3_value DOUBLE, 
	parameter3_formula VARCHAR(255), 
	f_owner VARCHAR(36), 
	
	PRIMARY KEY (id),
	INDEX FK_tbl_exchanges_f_owner(f_owner),
	CONSTRAINT FK_tbl_exchanges_f_flow 
		FOREIGN KEY (f_flow) REFERENCES tbl_flows (id),
	CONSTRAINT FK_tbl_exchanges_f_flowpropertyfactor 
		FOREIGN KEY (f_flowpropertyfactor) REFERENCES tbl_flowpropertyfactors (id),
	CONSTRAINT FK_tbl_exchanges_f_unit 
		FOREIGN KEY (f_unit) REFERENCES tbl_units (id)
	
)ENGINE = MYISAM;

-- reference for the quantitative reference
ALTER TABLE tbl_processes ADD CONSTRAINT FK_tbl_processes_f_quantitativereference 
	FOREIGN KEY (f_quantitativereference) REFERENCES tbl_exchanges (id);


-- an allocation factor of an allocated process
DROP TABLE IF EXISTS tbl_allocationfactors;
CREATE TABLE tbl_allocationfactors (

	id VARCHAR(36) NOT NULL, 
	value DOUBLE, 
	productid VARCHAR(36), 
	f_exchange VARCHAR(36), 
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_allocationfactors_f_exchange 
		FOREIGN KEY (f_exchange) REFERENCES tbl_exchanges (id)
	
)ENGINE = MYISAM;


-- product systems
DROP TABLE IF EXISTS tbl_productsystems;
CREATE TABLE tbl_productsystems (

	id VARCHAR(36) NOT NULL,
	name VARCHAR(255), 
	description TEXT(64000), 
	categoryid VARCHAR(36), 
	marked TEXT, 
	targetamount DOUBLE, 
	f_referenceprocess VARCHAR(36), 
	f_referenceexchange VARCHAR(36), 
	f_targetflowpropertyfactor VARCHAR(36), 
	f_targetunit VARCHAR(36), 
	
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_productsystems_f_targetunit 
		FOREIGN KEY (f_targetunit) REFERENCES tbl_units (id),
	CONSTRAINT FK_tbl_productsystems_f_referenceexchange 
		FOREIGN KEY (f_referenceexchange) REFERENCES tbl_exchanges (id),
	CONSTRAINT FK_tbl_productsystems_f_targetflowpropertyfactor 
		FOREIGN KEY (f_targetflowpropertyfactor) REFERENCES tbl_flowpropertyfactors (id),
	CONSTRAINT FK_tbl_productsystems_f_referenceprocess 
		FOREIGN KEY (f_referenceprocess) REFERENCES tbl_processes (id)
	
)ENGINE = MYISAM;


-- processes in a product system
DROP TABLE IF EXISTS tbl_productsystem_process;
CREATE TABLE tbl_productsystem_process (

	f_productsystem VARCHAR(36) NOT NULL, 
	f_process VARCHAR(36) NOT NULL, 
	
	PRIMARY KEY (f_productsystem, f_process),
	CONSTRAINT FK_tbl_productsystem_process_f_productsystem 
		FOREIGN KEY (f_productsystem) REFERENCES tbl_productsystems (id),
	CONSTRAINT FK_tbl_productsystem_process_f_process 
	FOREIGN KEY (f_process) REFERENCES tbl_processes (id)

)ENGINE = MYISAM;


-- process links of product systems
DROP TABLE IF EXISTS tbl_processlinks;
CREATE TABLE tbl_processlinks (

	id VARCHAR(36) NOT NULL, 
	f_recipientprocess VARCHAR(36), 
	f_recipientinput VARCHAR(36), 
	f_providerprocess VARCHAR(36), 
	f_provideroutput VARCHAR(36), 
	f_productsystem VARCHAR(36), 
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_processlinks_f_recipientprocess 
		FOREIGN KEY (f_recipientprocess) REFERENCES tbl_processes (id),
	CONSTRAINT FK_tbl_processlinks_f_productsystem 
		FOREIGN KEY (f_productsystem) REFERENCES tbl_productsystems (id),
	CONSTRAINT FK_tbl_processlinks_f_recipientinput 
		FOREIGN KEY (f_recipientinput) REFERENCES tbl_exchanges (id),
	CONSTRAINT FK_tbl_processlinks_f_providerprocess 
		FOREIGN KEY (f_providerprocess) REFERENCES tbl_processes (id),
	CONSTRAINT FK_tbl_processlinks_f_provideroutput 
		FOREIGN KEY (f_provideroutput) REFERENCES tbl_exchanges (id)
	
)ENGINE = MYISAM;


-- the scaling factors of processes of a calculated product system
DROP TABLE IF EXISTS tbl_scalingfactors;
CREATE TABLE tbl_scalingfactors (

	id VARCHAR(36) NOT NULL, 
	processid VARCHAR(36), 
	factor DOUBLE,
	uncertainty DOUBLE, 
	productid VARCHAR(36),
	f_productsystem VARCHAR(36), 
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_scalingfactors_f_productsystem 
		FOREIGN KEY (f_productsystem) REFERENCES tbl_productsystems(id)

)ENGINE = MYISAM;


-- LCI results of product systems
DROP TABLE IF EXISTS tbl_lciresults;
CREATE TABLE tbl_lciresults (

	id VARCHAR(36) NOT NULL, 
	targetamount DOUBLE, 
	product VARCHAR(255),
	productsystem VARCHAR(255), 
	calculationmethod VARCHAR(255), 
	unit VARCHAR(255), 
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- LCIA results of product systems
DROP TABLE IF EXISTS tbl_lciaresults;
CREATE TABLE tbl_lciaresults (

	id VARCHAR(36) NOT NULL, 
	targetamount DOUBLE, 
	product VARCHAR(255),
	productsystem VARCHAR(255), 
	unit VARCHAR(255), 
	lciamethod VARCHAR(255), 
	nwset VARCHAR(255), 
	weightingunit VARCHAR(255), 
	description TEXT, 
	categoryid VARCHAR(255), 
	name VARCHAR(255), 
	
	PRIMARY KEY (id)

)ENGINE = MYISAM;


-- a stored LCIA result
DROP TABLE IF EXISTS tbl_lciacategoryresults;
CREATE TABLE tbl_lciacategoryresults (

	id VARCHAR(36) NOT NULL, 
	category VARCHAR(255), 
	unit VARCHAR(255), 
	weightingunit VARCHAR(255), 
	value double, 
	standarddeviation double, 
	normalizationfactor double, 
	weightingfactor double, 
	f_lciaresult VARCHAR(36), 
	
	PRIMARY KEY (id),
	INDEX FK_tbl_lciacategoryresults_f_lciaresult(f_lciaresult)
)ENGINE = MYISAM;


-- LCIA methods
DROP TABLE IF EXISTS tbl_lciamethods;
CREATE TABLE tbl_lciamethods (

	id VARCHAR(36) NOT NULL, 
	description TEXT(64000), 
	categoryid VARCHAR(36), 
	name VARCHAR(255), 
	PRIMARY KEY (id)
	
)ENGINE = MYISAM;


-- LCIA categories
DROP TABLE IF EXISTS tbl_lciacategories;
CREATE TABLE tbl_lciacategories (

	id VARCHAR(36) NOT NULL, 
	description TEXT(64000), 
	name VARCHAR(255), 
	referenceunit VARCHAR(255),
	f_lciamethod VARCHAR(36), 
	
	PRIMARY KEY (id),	
	CONSTRAINT FK_tbl_lciacategories_f_lciamethod 
		FOREIGN KEY (f_lciamethod) REFERENCES tbl_lciamethods (id)

)ENGINE = MYISAM;


-- LCIA factors
DROP TABLE IF EXISTS tbl_lciafactors;
CREATE TABLE tbl_lciafactors (

	id VARCHAR(36) NOT NULL, 
	f_flowpropertyfactor VARCHAR(36), 
	f_flow VARCHAR(36), 
	f_unit VARCHAR(36), 
	value DOUBLE, 
	f_lciacategory VARCHAR(36), 
	
	PRIMARY KEY (id),
	CONSTRAINT FK_tbl_lciafactors_f_unit 
		FOREIGN KEY (f_unit) REFERENCES tbl_units (id),
	CONSTRAINT FK_tbl_lciafactors_f_flowpropertyfactor 
		FOREIGN KEY (f_flowpropertyfactor) REFERENCES tbl_flowpropertyfactors (id),
	CONSTRAINT FK_tbl_lciafactors_f_lciacategory 
		FOREIGN KEY (f_lciacategory) REFERENCES tbl_lciacategories (id),
	CONSTRAINT FK_tbl_lciafactors_f_flow 
		FOREIGN KEY (f_flow) REFERENCES tbl_flows (id)

)ENGINE = MYISAM;


-- normalisation and weighting sets of LCIA methods
DROP TABLE IF EXISTS tbl_normalizationweightingsets;
CREATE TABLE tbl_normalizationweightingsets (

	id VARCHAR(255) NOT NULL, 
	referencesystem VARCHAR(255),
	f_lciamethod VARCHAR(36), 
	unit VARCHAR(255),
	
	PRIMARY KEY (id),
	
	CONSTRAINT FK_tbl_normalizationweightingsets_f_lciamethod 
		FOREIGN KEY (f_lciamethod) REFERENCES tbl_lciamethods (id)

)ENGINE = MYISAM;


-- factors of normalisation and weighting sets of LCIA methods
DROP TABLE IF EXISTS tbl_normalizationweightingfactors;
CREATE TABLE tbl_normalizationweightingfactors (

	id VARCHAR(255) NOT NULL, 
	weightingfactor DOUBLE, 
	normalizationfactor DOUBLE,
	f_lciacategory VARCHAR(36),
	f_normalizationweightingset VARCHAR(255), 
	
	PRIMARY KEY (id),
	CONSTRAINT tblnrmlztionweightingfactorsfnrmlztionweightingset 
		FOREIGN KEY (f_normalizationweightingset) REFERENCES tbl_normalizationweightingsets (id),
	CONSTRAINT tbl_normalizationweightingfactors_f_lciacategory 
		FOREIGN KEY (f_lciacategory) REFERENCES tbl_lciacategories (id)

)ENGINE = MYISAM;


-- parameters
DROP TABLE IF EXISTS tbl_parameters;
CREATE TABLE tbl_parameters (

	id VARCHAR(36) NOT NULL, 
	description TEXT(64000), 
	name VARCHAR(255), 
	f_owner VARCHAR(36), 
	type INTEGER, 
	expression_parametrized TINYINT(1) default 0, 
	expression_value DOUBLE, 
	expression_formula VARCHAR(255),
	
	PRIMARY KEY (id),
	INDEX FK_tbl_parameters_f_owner(f_owner)
)ENGINE = MYISAM;


-- projects
DROP TABLE IF EXISTS tbl_projects;
CREATE TABLE tbl_projects (

	id VARCHAR(36) NOT NULL, 
	productsystems TEXT, 
	creationdate DATE, 
	description TEXT(64000), 
	categoryid VARCHAR(36), 
	functionalunit TEXT(64000), 
	name VARCHAR(255), 
	lastmodificationdate DATE,
	goal TEXT(64000), 
	f_author VARCHAR(36), 
	
	PRIMARY KEY (id),	
	CONSTRAINT FK_tbl_projects_f_author 
		FOREIGN KEY (f_author) REFERENCES tbl_actors (id)
)ENGINE = MYISAM;


-- the version entry
INSERT INTO openlca_version(id, no, name, eclipse) 
	VALUES('b3dae112-8c6f-4c0e-9843-4758af2441fe', '1.2', 'openLCA', 'Helios');