# data_validation_admin_1.json

CLUSTER at0002 that has occurrences 1..3 was removed, so the validation should detect it violates the occurrences constraint because there is no CLUSTER at0002.

CLUSTER at0010 items has cardinality 3..5 and there is only one ELEMENT, which violates cardinality.

ELEMENT at0011 with occurrences 3..* occurs 1 time, that violates occurrences.

# data_validation_admin_2.json

CLUSTER at0002 that has occurrences 1..3 has 4 occurrences, so the validation should detect it violates the occurrences constraint because there are more CLUSTERs than the ones that are allowed to occur there.

# data_validation_admin_3.json

CLUSTER at0004 has items with cardinality 1..3 in the OPT, this COMPOSITION has no items to violate the cardinality constraint.

# data_validation_admin_4.json

CLUSTER at0004 has items with cardinality 1..3 in the OPT, this COMPOSITION has 4 items to violate the cardinality constraint.

# data_validation_admin_5.json

CLUSTER at0004 has items with existence in the OPT, this COMPOSITION has no items to violate the existence constraint.

# data_validation_evaluation_1.json

ELEMENT at0002 with occurrences 1..1 is not present in the data.

# data_validation_evaluation_2.json

DV_CODED_TEXT in ELEMENT at0003 value has a code_string that doesn't exist in the local list of options.

# data_validation_evaluation_3.json

External terminologyID 'SNOMED-XXXXX' in ELEMENT at0007 doesn't exist in the DV_CODED_TEXT external terminology coinstraint that uses 'SNOMED-CT'.

# data_validation_evaluation_4.json

DV_CODED_TEXT value 'xxxxx' in ELEMENT at0003 doesn't correspond to the text associated with the code 'at0004' in the local terminology.
