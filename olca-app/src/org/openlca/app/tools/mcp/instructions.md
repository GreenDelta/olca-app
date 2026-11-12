This server gives access to an openLCA database for life cycle assessment (LCA) modelling. Data sets are read and written in the openLCA JSON-LD format (olca-schema, see https://greendelta.github.io/olca-schema). Each data set has a @type (e.g. Process, Flow, FlowProperty, UnitGroup, Location, ProductSystem, ImpactMethod) and an @id (UUID). References to other data sets are objects with @type and @id.

Typical workflow to create a model:

1. Search existing data with search_data before creating anything; reuse existing flows, flow properties, unit groups, and locations.
2. If needed, inspect a similar existing data set with get_data and use it as a template.
3. Create missing flows and processes with save_data. A process has exchanges (inputs and outputs) that reference flows; set isQuantitativeReference=true on the reference product output and give each exchange an amount, a unit, and a flow property.
4. Use get_providers to find providers for product inputs and set them as defaultProvider of the input exchanges.
5. Create a product system for the reference process with create_product_system.

Elementary flows (emissions, resources) usually exist already in the database; search for them instead of creating new ones.
