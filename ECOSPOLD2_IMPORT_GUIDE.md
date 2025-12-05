# EcoSpold2 Import Guide for openLCA

This fork re-enables the EcoSpold2 import functionality that was disabled in the official openLCA releases.

## Prerequisites

To import EcoSpold2 data, you need to obtain it from ecoinvent:
- Purchase a license from https://ecoinvent.org/
- Download the EcoSpold2 format dataset (not the pre-converted openLCA format)

## Importing EcoSpold2 Data

### Step 1: Create a Database with Reference Data

1. Launch openLCA
2. Create a new database
3. **Important:** Select "Complete reference data (with flows)" template
   - Do NOT use an empty database - you'll get UUID warnings

### Step 2: Import EcoSpold2 Files

1. **File → Import → Other → EcoSpold 2**
2. Select your EcoSpold2 files:
   - Individual `.spold` files
   - `.zip` archives containing `.spold` files
   - `.xml` files from EcoSpold2 datasets
3. Click **Finish**

### Step 3: Optional - Flow Mapping

If you want to control how flows are mapped:

1. During import, click the **Flow mapping** dropdown
2. Select **From file...**
3. Browse to your custom mapping CSV file

## Generating Flow Mapping CSV (Optional)

If you have an ecoinvent dataset with MasterData folder, you can generate a flow mapping CSV:

### Step 1: Extract Flow IDs from EcoSpold2 Data

**Script: `generate_flow_mapping.py`**

```python
import xml.etree.ElementTree as ET
import csv
import os

def extract_flows(xml_file, flow_type):
    """Extract flows from ElementaryExchanges.xml or IntermediateExchanges.xml"""
    tree = ET.parse(xml_file)
    root = tree.getroot()
    
    flows = []
    # Handle both with and without namespace
    for exchange in root.findall('.//{*}exchange') or root.findall('.//exchange'):
        flow_id = exchange.get('id')
        
        # Get name
        name_elem = exchange.find('.//{*}name') or exchange.find('.//name')
        name = name_elem.text if name_elem is not None else ''
        
        # Get unit
        unit_elem = exchange.find('.//{*}unitName') or exchange.find('.//unitName')
        unit = unit_elem.text if unit_elem is not None else ''
        
        # Get category
        category_parts = []
        for comp in exchange.findall('.//{*}compartment') or exchange.findall('.//compartment'):
            if comp.text:
                category_parts.append(comp.text)
        category = '/'.join(category_parts) if category_parts else flow_type
        
        flows.append({
            'id': flow_id,
            'name': name,
            'unit': unit,
            'category': category
        })
    
    return flows

def main():
    # Adjust these paths to your ecoinvent dataset location
    masterdata_path = 'path/to/ecoinvent/datasets/MasterData'
    
    elementary_file = os.path.join(masterdata_path, 'ElementaryExchanges.xml')
    intermediate_file = os.path.join(masterdata_path, 'IntermediateExchanges.xml')
    
    all_flows = []
    
    if os.path.exists(elementary_file):
        all_flows.extend(extract_flows(elementary_file, 'Elementary flows'))
        print(f"Extracted elementary flows from {elementary_file}")
    
    if os.path.exists(intermediate_file):
        all_flows.extend(extract_flows(intermediate_file, 'Intermediate flows'))
        print(f"Extracted intermediate flows from {intermediate_file}")
    
    # Write CSV
    output_file = 'ecospold2_flow_mapping.csv'
    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f, delimiter=';')
        writer.writerow([
            'Source Flow UUID', 'Source Flow Name', 'Source Flow Unit', 'Source Flow Category',
            'Target Flow UUID', 'Target Flow Name', 'Target Flow Unit', 'Target Flow Category',
            'Conversion Factor'
        ])
        
        for flow in all_flows:
            writer.writerow([
                flow['id'],      # Source UUID (from ecoinvent)
                flow['name'],    # Source name
                flow['unit'],    # Source unit
                flow['category'],# Source category
                '',              # Target UUID (will be filled by next script)
                '',              # Target name (will be filled by next script)
                '',              # Target unit (leave empty to use source)
                '',              # Target category (leave empty to use source)
                ''               # Conversion factor (leave empty for 1.0)
            ])
    
    print(f"\nGenerated {output_file} with {len(all_flows)} flows")
    print("\nNext: Run fill_flow_mapping.py to match with openLCA reference data")

if __name__ == '__main__':
    main()
```

### Step 2: Match with openLCA Reference Flows

**Script: `fill_flow_mapping.py`**

```python
import csv
import urllib.request
import os

# Configuration
MAPPING_FILE = 'ecospold2_flow_mapping.csv'
OUTPUT_FILE = 'ecospold2_flow_mapping_filled.csv'
REF_DATA_URL = 'https://raw.githubusercontent.com/GreenDelta/data/master/refdata/flows.csv'
REF_FLOWS_FILE = 'flows.csv'

def download_reference_data():
    """Downloads the official flows.csv if not present."""
    if not os.path.exists(REF_FLOWS_FILE):
        print(f"Downloading reference flows from {REF_DATA_URL}...")
        try:
            urllib.request.urlretrieve(REF_DATA_URL, REF_FLOWS_FILE)
            print("Download complete.")
        except Exception as e:
            print(f"Error downloading file: {e}")
            return False
    return True

def load_reference_flows():
    """
    Loads openLCA reference flows into a dictionary.
    Format in repo is typically: UUID;Name;Category;...
    Returns: dict { 'flow_name': 'uuid' }
    """
    ref_flows = {}
    print("Loading reference flows...")
    try:
        with open(REF_FLOWS_FILE, 'r', encoding='utf-8') as f:
            reader = csv.reader(f, delimiter=';')
            for row in reader:
                if not row: continue
                # Skip headers
                if row[0].lower() in ['id', 'uuid', '#id']:
                    continue
                
                # Column 0 is UUID, Column 1 is Name
                if len(row) >= 2:
                    uuid = row[0].strip()
                    name = row[1].strip()
                    ref_flows[name.lower()] = {'uuid': uuid, 'name': name}
    except Exception as e:
        print(f"Error reading reference flows: {e}")
    
    print(f"Loaded {len(ref_flows)} reference flows.")
    return ref_flows

def fill_mapping(ref_flows):
    """Reads the mapping file and fills in Target UUIDs."""
    print(f"Processing {MAPPING_FILE}...")
    
    filled_rows = []
    headers = []
    
    try:
        with open(MAPPING_FILE, 'r', encoding='utf-8', newline='') as f_in:
            reader = csv.DictReader(f_in, delimiter=';')
            headers = reader.fieldnames
            
            matches = 0
            unmapped = []
            
            for row in reader:
                source_name = row['Source Flow Name'].strip()
                source_name_lower = source_name.lower()
                
                # Only fill if Target UUID is empty
                if not row['Target Flow UUID'] and source_name_lower in ref_flows:
                    ref_flow = ref_flows[source_name_lower]
                    row['Target Flow UUID'] = ref_flow['uuid']
                    row['Target Flow Name'] = ref_flow['name']
                    matches += 1
                elif not row['Target Flow UUID']:
                    unmapped.append(source_name)
                
                filled_rows.append(row)
                
            print(f"\nMatched and filled {matches} flows.")
            print(f"Unmapped flows: {len(unmapped)}")
            
            if unmapped and len(unmapped) <= 20:
                print("\nSample unmapped flows:")
                for name in unmapped[:20]:
                    print(f"  - {name}")
            
        # Write Output
        with open(OUTPUT_FILE, 'w', encoding='utf-8', newline='') as f_out:
            writer = csv.DictWriter(f_out, fieldnames=headers, delimiter=';')
            writer.writeheader()
            writer.writerows(filled_rows)
            
        print(f"\n✅ Success! Created {OUTPUT_FILE}")
        print(f"\nUnmapped flows will be auto-created during import.")
        
    except FileNotFoundError:
        print(f"Error: Could not find {MAPPING_FILE}")
        print("Run generate_flow_mapping.py first!")

if __name__ == "__main__":
    if download_reference_data():
        ref_map = load_reference_flows()
        fill_mapping(ref_map)
```

### Usage:

```bash
# Step 1: Extract flows from your ecoinvent data
python generate_flow_mapping.py

# Step 2: Match with openLCA reference data
python fill_flow_mapping.py

# Step 3: Use the filled mapping during import
# File → Import → Other → EcoSpold 2 → Flow mapping → From file...
# Select: ecospold2_flow_mapping_filled.csv
```

**Benefits:**
- ✅ Automatic matching with openLCA reference flows by name
- ✅ Downloads official flow list from GreenDelta repository
- ✅ Shows which flows couldn't be matched (will be auto-created)
- ✅ Safer than manual UUID entry

## Expected Warnings

When importing, you may see warnings like:
```
no unit or property found for '77ae64fa-7e74-4252-9c3b-889c1cd20bfc'
```

**These are normal.** Ecoinvent uses different UUIDs than openLCA's reference data. The import system automatically maps by name when UUIDs don't match. Your import will still succeed.

## Troubleshooting

### "No flows imported"
- Make sure you used "Complete reference data (with flows)" database template
- Check that you selected the correct file type (.spold, .zip, or .xml)

### "Too many warnings"
- This is expected when using an empty database
- Delete the database and recreate with "Complete reference data"

### "Import is very slow"
- Start with a subset (e.g., 10-50 processes) to test
- Full ecoinvent datasets (20,000+ processes) can take hours
- Consider importing by sectors if available

## Technical Details

- **Supported file formats:** `.spold`, `.xml`, `.zip`
- **Recommended database template:** Complete reference data (with flows)
- **Flow mapping:** Optional, auto-creates missing flows
- **UUID mapping:** Falls back to name-based matching

## License Compliance

**Important:** 
- EcoSpold2 datasets are proprietary and require an ecoinvent license
- Do not redistribute ecoinvent data files
- Do not commit ecoinvent data to version control
- This tool only provides the import capability, not the data

## Support

For issues with:
- **This import tool:** Open an issue on this GitHub repository
- **EcoSpold2 data:** Contact ecoinvent support
- **Official openLCA:** Visit https://www.openlca.org/

---

## Why was EcoSpold2 import disabled?

The official openLCA releases disabled direct EcoSpold2 import to promote sales of pre-converted databases. This fork re-enables it for users who have legitimate ecoinvent licenses and prefer to import the data themselves.
