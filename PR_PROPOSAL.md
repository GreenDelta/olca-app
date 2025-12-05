# Pull Request: Re-enable EcoSpold2 Import Feature

## Summary
This PR re-enables the EcoSpold2 import wizard that was previously commented out in `plugin.xml`. The feature allows users with legitimate ecoinvent licenses to import EcoSpold2 format data directly into openLCA.

## Motivation
Currently, users with ecoinvent licenses who want to use EcoSpold2 data must either:
1. Purchase pre-converted databases from GreenDelta
2. Use older versions of openLCA that still have this feature
3. Manually convert the data themselves

This creates barriers for:
- **Academic researchers** with institutional ecoinvent licenses
- **Educational institutions** teaching LCA methodology
- **Organizations** that already pay for ecoinvent but want flexibility in database management
- **Users in regions** where purchasing pre-converted databases may be difficult

## Changes Made

### 1. Re-enabled EcoSpold2ImportWizard (`olca-app/plugin.xml`)
- **Lines 781-789:** Uncommented the wizard registration
- **Why it was disabled:** Business decision to promote pre-converted database sales
- **Impact:** Makes the wizard available in File → Import → Other → EcoSpold 2

### 2. Updated FileImport Handler (`olca-app/src/org/openlca/app/tools/FileImport.java`)
- **Line 30:** Added import for `EcoSpold2ImportWizard`
- **Line 98:** Changed from showing info message to actually invoking the wizard
- **Impact:** Enables drag-drop and double-click import of `.spold` files

### 3. Documentation (`ECOSPOLD2_IMPORT_GUIDE.md`)
- Comprehensive user guide with:
  - Clear license compliance warnings
  - Step-by-step import instructions
  - Python script for flow mapping generation (from user's own licensed data)
  - Troubleshooting section
  - Expected behavior documentation

## Technical Details

### Existing Implementation
The EcoSpold2 import functionality **already exists** in the codebase:
- `org.openlca.app.wizards.io.EcoSpold2ImportWizard` - Full implementation
- `org.openlca.io.ecospold2.input.EcoSpold2Import` - Core import logic
- Supports `.spold`, `.xml`, and `.zip` file formats
- Includes flow mapping support via CSV

**This PR only removes the UI blocks** - no new code required.

### Testing Performed
- ✅ Application builds successfully
- ✅ Wizard appears in Import menu
- ✅ Drag-drop `.spold` files works
- ✅ Import with "Complete reference data" template succeeds
- ✅ Flow mapping via CSV works
- ✅ Tested with ecoinvent 3.10.1 cutoff dataset (2 processes)

### Backward Compatibility
- ✅ No breaking changes
- ✅ No API modifications
- ✅ Existing database templates unchanged
- ✅ Optional feature - doesn't affect users who don't use it

## Legal & Licensing

### Clear Boundaries
1. **This PR does NOT include any ecoinvent data**
2. **Documentation clearly states users need ecoinvent license**
3. **Python script is a generic tool** (no proprietary data)
4. **Maintains MPL 2.0 license** (openLCA's existing license)

### User Compliance Requirements
Users must:
- Have a valid ecoinvent license
- Obtain EcoSpold2 data from ecoinvent.org
- Comply with ecoinvent's terms of use

The tool only provides import capability, not the data itself.

## Benefits to openLCA Community

### Educational Value
- Universities can use their institutional licenses more easily
- Students learn the full LCA workflow (data import → analysis)
- Reduces barriers to LCA education

### Research Flexibility
- Researchers can work with latest ecoinvent versions immediately
- Custom database configurations for specific research needs
- Reproducible workflows (import scripts can be shared)

### User Control
- Users choose their own update schedule
- Maintain multiple database versions for comparison
- Customize flow mappings for specific use cases

## Business Model Compatibility

This feature **does not conflict** with GreenDelta's business model:

### Pre-converted Databases Still Valuable For:
1. **Users without technical expertise** - Pre-converted is easier
2. **Time-sensitive projects** - Immediate availability
3. **Quality assurance** - GreenDelta-verified conversions
4. **Support included** - Professional assistance
5. **Special configurations** - LCIA methods, customizations

### This Feature Serves:
- Users who **already have** ecoinvent licenses
- Technical users who **prefer control** over their workflows
- Educational institutions with **budget constraints**
- Researchers who need **cutting-edge data** before pre-converted versions available

## Alternative Approaches Considered

### Option 1: Keep Disabled (Current State)
- ❌ Forces users to older openLCA versions
- ❌ Fragments the user base
- ❌ Reduces openLCA's value proposition

### Option 2: Make Premium Feature
- ⚠️ Requires license key system
- ⚠️ Complicates codebase
- ⚠️ Doesn't serve educational users

### Option 3: Re-enable with Documentation (This PR)
- ✅ Serves legitimate ecoinvent license holders
- ✅ Minimal code changes (just uncomment)
- ✅ Clear compliance documentation
- ✅ Preserves pre-converted database value for other users

## Community Feedback

This feature has been requested by users in:
- GitHub issues
- openLCA forum discussions
- Academic mailing lists

Many users are currently:
- Using openLCA 1.x versions (older, just for EcoSpold2 import)
- Switching to other LCA software that supports EcoSpold2
- Spending significant time on manual workarounds

## Proposal

### Immediate Action
Merge this PR to re-enable EcoSpold2 import for users with ecoinvent licenses.

### Long-term Considerations
1. **Monitor usage** - Track if this affects pre-converted database sales
2. **Gather feedback** - Understand user needs better
3. **Consider hybrid model** - E.g., basic import free, advanced features premium
4. **Maintain documentation** - Keep import guide updated

## Files Changed

```
.gitignore                                          # Added sample/ folder
README.md                                           # Fork information
ECOSPOLD2_IMPORT_GUIDE.md                          # New: User documentation
olca-app/plugin.xml                                # Uncommented wizard (lines 781-789)
olca-app/src/org/openlca/app/tools/FileImport.java # Invoke wizard (line 98)
```

## Checklist

- [x] Code changes are minimal (uncomment + invoke)
- [x] No new dependencies added
- [x] Documentation includes legal compliance warnings
- [x] No proprietary data included
- [x] Tested with real ecoinvent dataset
- [x] Backward compatible
- [x] Follows existing code style
- [x] Ready for review

## Questions for Maintainers

1. **Business Model:** Are there concerns about impact on pre-converted database sales?
2. **Licensing:** Is the current documentation sufficient for legal compliance?
3. **Support:** Should there be disclaimers about community-only support for this feature?
4. **Versioning:** Should this be in a major/minor release, or patch?

## Conclusion

This PR restores a valuable feature for legitimate ecoinvent license holders without compromising openLCA's business model. Pre-converted databases remain the best option for most users, while this serves the technical and educational community.

I'm happy to make any adjustments based on maintainer feedback.

---

**Author:** Ankur Pandey (@ankur-paan)  
**Issue:** N/A (feature request from community)  
**License:** MPL 2.0 (unchanged)
