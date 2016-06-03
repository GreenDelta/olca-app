olca-refdata
============
This project contains the reference data of the openLCA application. The data
are stored in plain CSV files in a simple format which is described 
[here](https://github.com/GreenDelta/olca-modules/blob/master/olca-io/REF_DATA.md).

The project contains a Maven task that directly generates the openLCA reference
databases from these files just run `mvn package` and it will generate the 
databases in the `dist` folder.

The CSV files are equally formatted to track changes via version control:

* non-numeric values are always quoted
* the entries are sorted by the UUID which is the first column of the file

The following Python script can be used to format a file:
	
```python
"""
format openLCA reference data CSV files to track changes via git
empty values are currently quoted, see
http://stackoverflow.com/questions/19315366/csv-writer-in-python-with-custom-quoting
for custom quoting
"""

import csv

file = 'locations'
num_cols = [4, 5]

rows = []
with open(file + '_raw.csv', 'r', encoding='utf8', newline='\n') as f:
    reader = csv.reader(f, delimiter=';')
    handled = {}
    for row in reader:
        uid = row[0]
        if uid in handled:
            print('Duplicate: ' + uid)
            continue
        for num_col in num_cols:
            fval = float(row[num_col])
            if abs(fval - int(fval)) < 1e-16:
                row[num_col] = int(fval)
            else:
                row[num_col] = fval
        handled[uid] = True
        rows.append(row)

rows.sort(key=lambda row: row[0])

with open(file + '.csv', 'w', encoding='utf8', newline='\n') as f:
    writer = csv.writer(f, delimiter=';', quoting=csv.QUOTE_NONNUMERIC)
    for row in rows:
        writer.writerow(row)

print('Converted to %s.csv' % file)

```