# data_validation_admin_1.json

CLUSTER at0002 that has occurrences 1..3 was removed, so the validation should detect it violates the occurrences constraint because there is no CLUSTER at002.

# data_validation_admin_2.json

CLUSTER at0002 that has occurrences 1..3 has 4 occurrences, so the validation should detect it violates the occurrences constraint because there are more CLUSTERs than the ones that are allowed to occur there.
