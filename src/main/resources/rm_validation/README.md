# data_validation_admin_1.json

CLUSTER at0002 that has occurrences 1..3 was removed, so the validation should detect it violates the occurrences constraint because there is no CLUSTER at002.

# data_validation_admin_2.json

CLUSTER at0002 that has occurrences 1..3 has 4 occurrences, so the validation should detect it violates the occurrences constraint because there are more CLUSTERs than the ones that are allowed to occur there.

# data_validation_admin_3.json

CLUSTER at0004 has items with cardinality 1..3 in the OPT, this COMPOSITION has no items to violate the cardinality constraint.

# data_validation_admin_4.json

CLUSTER at0004 has items with cardinality 1..3 in the OPT, this COMPOSITION has 4 items to violate the cardinality constraint.

# data_validation_admin_5.json

CLUSTER at0004 has items with existence in the OPT, this COMPOSITION has no items to violate the existence constraint.