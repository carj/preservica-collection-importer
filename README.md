# preservica-collection-importer


Create new collections in a Preservica system from data held in a CSV file using the APIs.

The command line to run the program is:
```
collection-loader.cmd -i file.csv [-c config.properties] [--dry-run]
```

The CSV file must have the structure described below.

Column 1 is the name of the root collections. 
Column 2 if it exists will be created inside the collection in column 1.
Column 3 if it exists will be created inside the collection in column 2.
etc.

Note: You need to repeat the name of the parent collection in the left hand column.

```
GENERAL & ADMINISTRATIVE
CORPORATE GOVERNANCE & LEGAL,CORPORATE COMMUNICATIONS
CORPORATE GOVERNANCE & LEGAL,CORPORATE COMPLIANCE
CORPORATE GOVERNANCE & LEGAL,EXECUTIVE OFFICE
CORPORATE GOVERNANCE & LEGAL,GOVERNMENT
FINANCE,ACCOUNTING
FINANCE,AUDIT
FINANCE,INSURANCE
HUMAN RESOURCES,BENEFITS
HUMAN RESOURCES,EMPLOYEE MANAGEMENT 
HUMAN RESOURCES,BENEFITS,PENSIONS
```


You can test the collection structure which will be created in Preservica by using the --dry-run argument which will 
print the collection structure to the screen and not create it in Preservica.

The CSV above will generate a collection structure shown below.

```
GENERAL & ADMINISTRATIVE
CORPORATE GOVERNANCE & LEGAL
----|CORPORATE COMMUNICATIONS
----|CORPORATE COMPLIANCE
----|EXECUTIVE OFFICE
----|GOVERNMENT
FINANCE
----|ACCOUNTING
----|AUDIT
----|INSURANCE
HUMAN RESOURCES
----|BENEFITS
----|----|PENSIONS
----|EMPLOYEE MANAGEMENT
```

Notes:
* The collection title and collection code will have the same value.
* You cannot add descriptive metadata to each collection

