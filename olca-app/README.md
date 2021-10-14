## Updating the target platform
The target platform is the Eclipse RPC platform with the plugins
and features on which the openLCA desktop application runs. It
is defined in the `platform.target` file. We update the platform
from time to time in order to get the latest features and bug-fixes.
The platform components are fetched from the respective Eclipse
update sites.

### Eclipse RCP platform
When you enter the following URL in the browser it should forward
you to the current release:

```
https://download.eclipse.org/eclipse/updates
```

At the time of writing this document, this was:

```
https://download.eclipse.org/eclipse/updates/4.21/
```

This is the URL of the respective repository `location` in the
platform definition:  

``` 
<location 
  includeAllPlatforms="true"
  includeConfigurePhase="true"
  includeMode="slicer"
  includeSource="true"
  type="InstallableUnit">
  <repository location="https://download.eclipse.org/eclipse/updates/4.19/"/>
  <unit id="org.eclipse.equinox.sdk.feature.group" version="3.22.0.v20210227-0235"/>
  <unit id="org.eclipse.platform.sdk" version="4.19.0.I20210303-1800"/>
  <unit id="org.eclipse.rcp.feature.group" version="4.19.0.v20210303-1800"/>
</location>
```

The easiest way to update this, is to use the graphical editor in Eclipse.
Select the current location and click on `Edit`. Change the URL of the
location to point to the current version. Select the following components:

* Eclipse Platform SDK
* Eclipse RCP (under Eclipse RCP Target Components)
* Equinox Target Components

Make sure that `Include required software` is unchecked and that
`Include all environments` is checked. This is necessary to get all components
for out cross-platform builds.

When you click finish, it will fetch the current components. And this can take
quite some time.

### Updating the other components

Updating the other components is basically the same. You just need to know
where you get the current URL from.

#### EMF
Go to https://download.eclipse.org/modeling/emf/emf/builds/release/ to find
the latest release. Edit the location to point to the latest release and
select the `EMF - Eclipse Modeling Framework SDK` component.

### Updating the runtime

```
<configurations>
  <plugin id="org.apache.felix.scr" autoStart="true" startLevel="2" />
  <plugin id="org.eclipse.core.runtime" autoStart="true" startLevel="4" />
  <plugin id="org.eclipse.equinox.common" autoStart="true" startLevel="2" />
  <plugin id="org.eclipse.equinox.event" autoStart="true" startLevel="2" />
</configurations>
```

