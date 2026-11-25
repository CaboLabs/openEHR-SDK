# RM invariants gap

Gap record for the OPT diff work (see `opt_diff_rules.md`, sections 2 and 8.1). It lists **all** the
invariants of the RM classes (hybrid scope) and of the BASE classes the RM uses, as written in the
specifications, which of them the SDK implements in code, and a first check of which ones the OPT diff
may need, specially for the fallbacks to the RM rules. Nothing here is implemented or decided: the
rows are to be checked one by one later.

## 1. The gap

An OPT (ADL 1.4) states existence, occurrences, cardinality, types and values. An RM invariant is a
rule inside an RM object, between its attributes or on the domain of one attribute (for example "if
`type` is percent, then `denominator` is 100"). An OPT cannot express it, so `complete()` does not put
it in the completed OPT. The SDK only implements the 6 `DV_PROPORTION` invariants of RM 1.0.2 in
`RmValidator2` (and `Valid_denominator` of RM 1.0.4). The other 217 invariants of the list are not
implemented anywhere: no validation of RM instances, no use in the completed OPT, no use in the diff.

## 2. Scope and method

- **RM version:** hybrid scope. Demographic classes (`PARTY`, `CONTACT`, `ADDRESS`, `PARTY_IDENTITY`,
  `ROLE`, `ACTOR`, `CAPABILITY`, `AGENT`, `ORGANISATION`, `GROUP`, `PERSON`, `PARTY_RELATIONSHIP`,
  `VERSIONED_PARTY`) from RM 1.0.4, all other classes from RM 1.0.2 (161 invariants, plus
  `DV_PROPORTION.Valid_denominator` of 1.0.4 in its own row).
- **BASE:** 62 invariants of the BASE classes that the RM uses and that are not inlined in the RM 1.0.2
  BMM (`INTERVAL`, `ISO8601_*`, the `I*` ISO 8601 interfaces, `ACCESS_GROUP_REF`, `Boolean`, `List`),
  from BASE 1.0.4 (BASE 1.0.2 has no BMM). Not included: invariants of the BMM and expression classes
  of BASE (`BMM_PARAMETER_TYPE`, `P_BMM_GENERIC_PARAMETER`, `EXPR_OPERATOR`: model language, not RM) and
  of the AOM (`specifications-AM`, archetype validity rules: different work).
- **Source:** the `invariants` of the BMM files (`release/1.0.2`, `release/1.0.4` of
  `specifications-RM`; `release/1.0.4` of `specifications-BASE`) checked against the "Invariants" rows of
  the class tables (`partials/definitions/*.adoc`) of the same release. **The BMMs have all of them:**
  same classes and same invariant names in both sources, in the RM and in BASE. The spec pages have no
  invariant outside the class tables. Not covered: function preconditions and postconditions.
- **Not in scope: `DV_PARAGRAPH`** (deprecated, not used: narrative text goes in `DV_TEXT`, which allows line
  breaks from RM 1.0.4). Its invariant is not listed (decided).
- **Own invariants only.** Each class lists its own. A subclass also has the ones of its ancestors (every
  `DV_AMOUNT` descendant has `Accuracy_is_percent_validity`).
- **Typos of the spec, fixed on our side:** `ACTIVITY.Action_archteype_id_valid` is written
  `Action_archetype_id_valid`; `VERSION.Lifecycle_state_ valid` is written `Lifecycle_state_valid`;
  `orginal_language` in `AUTHORED_RESOURCE.Translations_valid` is written `original_language`.
  Everything else is copied as is.

## 3. First check: what the OPT diff may need

Last column of the table: relevance for the diff (high, medium, low, none) and the kind. Counts: 12
high, 22 medium, 113 low, 77 none. *None* means the class is not in an OPT (versioning, EHR extract,
identifiers, reference integrity, OPT metadata that is out of scope in rules doc section 8). *Low* means
lookup-only attributes, formats, aliases and consistency between values.

The groups that matter, in order of impact on the fallback to the RM rules (group 1 is decided, no change to the baseline):

1. **Container values (low, decided).** For the containers whose invariant says "not empty when
   present" (`COMPOSITION.content`, `SECTION.items`, `HISTORY.events` or `summary`,
   `INSTRUCTION.activities`, `LOCATABLE.links`, `ENTRY.other_participations`,
   `EVENT_CONTEXT.participations`, `PARTY.contacts`, `ROLE.capabilities`, `ACTOR.roles`,
   `PARTY.relationships`, `PARTY.reverse_relationships`, `PARTY_IDENTIFIED.identifiers`,
   `DV_TEXT.mappings`, `DV_ORDERED.other_reference_ranges`), the invariant is a rule on the **value** of
   the attribute: null or a list with items, never `[]`. It does not change the cardinality bounds, so
   the RM baseline stays as the BMM has it: existence `0..1`, cardinality `0..*`. At implementation
   level `[]` and null mean the same for a multiple attribute (no technical or clinical difference), so
   the SDK accepts `[]` internally and normalizes it to null to be compliant with the RM. The
   normalization belongs to the RM instance handling (parser, validator), not to the OPT diff.
   (Decided by Pablo; an earlier reading of this check, `1..*` as RM lower bound, was wrong.)
2. **Code sets and terminology groups (high, decided).** The invariants fix the codes of coded
   attributes to an openEHR terminology group or to a code set (section 4). The RM binds the attribute
   to the set, so without a code constraint in the OPT only the codes of the set are valid, in the same
   way as if the `DV_CODED_TEXT` or `CODE_PHRASE` constraint were in the OPT. **Decided (Pablo,
   2026-11-25):** do not materialize it in the completed OPT (every OPT would carry the same constraint,
   a waste of memory); look it up. `Model` gets metadata only: which class and attribute is bound to
   which term set id, never the codes (Atomik already loads and caches the terminologies in memory). An
   OPT constraint that selects a subset of the set narrows the RM constraint, for every coded attribute
   bound to an openEHR set (rules doc, principle 2 and 6.3). **Checked (2026-11-25):** the openEHR
   terminology files contain every group and code set the invariants name (section 4).
3. **Attribute interplay (medium, low).** Some invariants are rules between the values of the
   attributes of an instance. They are checks against data, not against the OPT, because an OPT
   constrains each attribute on its own. Example: `ELEMENT.value` and `null_flavour` are exclusive
   (`is_null xor null_flavour = Void`): `null_flavour` explains why there is no `value`, so when there is
   data in `value` no explanation is needed. The OPT can constrain `value` and `null_flavour`
   independently, and the diff compares each constraint as is (a `null_flavour` code list is the
   code-set case of group 2, a narrowing of the RM set). Also in this group: `HISTORY` (`events`,
   `summary`, `period`), `COMPOSITION` (`is_persistent` and `context`, removed in 1.0.4), `DV_PROPORTION` (`type` and
   `denominator`), `DV_MULTIMEDIA` (data or uri), `PARTY_IDENTIFIED`, `PARTY.uid`.
4. **String and numeric domains (high and medium).** The RM fallback of an open string is not "any
   string": `DV_TEXT.value` is not empty (CR and LF are allowed, decided); `DV_IDENTIFIER` fields, `DV_URI.value`
   and `CODE_PHRASE.code_string` are not empty. `DV_ENCAPSULATED.size >= 0`; `DV_PROPORTION` denominator
   is not 0 and fixed by the type (1 and 100). Matters for `C_STRING` and numeric open expressions in
   the primitives table (not written yet).
5. **Type alternatives (medium).** `ITEM_LIST.Valid_structure` and `ITEM_TABLE.Valid_structure` (items
   are ELEMENT) are already in the BMM typing (`ITEM_LIST.items` is `List<ELEMENT>`; `ITEM_TABLE.rows`
   is `CLUSTER` in the 1.0.2 BMM instead of `List<CLUSTER>`, a known erratum in `rm_bmm_version_gap.md`).
   `ENTRY.Subject_validity` ties `subject` to `PARTY_SELF`.
6. **Dates and intervals (low).** ISO 8601 validity of `DV_DATE`, `DV_TIME`, `DV_DATE_TIME`,
   `DV_DURATION` and the BASE interfaces, and `Interval` limits. They help the date pattern and
   `DV_DURATION` tables (rules doc, open point 4).
7. **Aliases and derived values (low, none).** `type = name`, `purpose = name` of the demographic
   classes; `EVENT.offset`, `INTERVAL_EVENT.interval_start_time` and `is_simple` are derived.

Also found: the 1.0.2 BMM has no `ELEMENT.null_flavour` property (errata overlay of decision D2), so the
generator cannot get that invariant's attribute from the BMM alone.

## 4. Code sets and groups named by the invariants

Sources: SDK `src/main/resources/terminology/` (`openehr_terminology_en.xml`, version 3.1.0, date
2024-04-11; `openehr_external_terminologies.xml`, same) and `../specifications-TERM`
`computable/XML/` (`en/openehr_terminology.xml`, version 3.1.0, date 2026-10-09;
`openehr_external_terminologies.xml`, date 2026-10-10). Compared by code (group concept ids and
code set values): all 16 groups and 4 code sets of the invariants are in both files. The SDK copies are
older, with the same version number 3.1.0.

| Invariant reference | Terminology id | In SDK files | In specifications-TERM | Difference |
|---|---|---|---|---|
| `Group_id_attestation_reason` | group `attestation_reason` | yes | yes | none |
| `Group_id_audit_change_type` | group `audit_change_type` | yes | yes | none |
| `Group_id_composition_category` | group `composition_category` | yes | yes | none |
| `Group_id_event_math_function` | group `event_math_function` | yes | yes | none |
| `Group_id_instruction_states` | group `instruction_states` | yes | yes | none |
| `Group_id_instruction_transitions` | group `instruction_transitions` | yes | yes | none |
| `Group_id_null_flavour` | group `null_flavours` (plural) | yes | yes | none; the id of the invariant is singular |
| `Group_id_participation_function` | group `participation_function` | yes | yes | none; the group has 1 concept |
| `Group_id_participation_mode` | group `participation_mode` | yes | yes | none |
| `Group_id_setting` | group `setting` | yes | yes | none |
| `Group_id_subject_relationship` | group `subject_relationship` | yes | yes | none |
| `Group_id_term_mapping_purpose` | group `term_mapping_purpose` | yes | yes | TERM has `672 interoperability`, SDK does not |
| `Group_id_version_lifecycle_state` | group `version_lifecycle_state` | yes | yes | none |
| `Code_set_id_compression_algorithms` | code set `compression_algorithms` | yes | yes | none |
| `Code_set_id_integrity_check_algorithms` | code set `integrity_check_algorithms` | yes | yes | none |
| `Code_set_id_normal_statuses` | code set `normal_statuses` | yes | yes | none |
| `Code_set_id_character_sets` | code set `character_sets` (IANA) | yes | yes | none |
| `Code_set_id_countries` | code set `countries` (ISO 3166-1) | yes | yes | none |
| `Code_set_id_languages` | code set `languages` (ISO 639-1) | yes | yes | TERM has `ro-md` and `ru-md`, SDK does not |
| `Code_set_id_media_types` | code set `media_types` (IANA) | yes | yes | none |

Also different: group `property` has 5 more concepts in TERM (`761` to `765`), no invariant uses it.

Not terminology data, the invariants use functions with literal values: `valid_proportion_kind`
(`DV_PROPORTION.type` 0 to 4), `valid_magnitude_status` (`=`, `<`, `>`, `<=`, `>=`, `~`),
`is_valid_match_code` (`TERM_MAPPING.match`: `?`, `=`, `>`, `<`) and `valid_percentage` (between 0 and 100). They are not in
any terminology file.

Refreshing the SDK terminology files: issue https://github.com/CaboLabs/openEHR-SDK/issues/235 (not part of
this work).

## 5. Invariants

Columns: class, invariant name, rule as in the spec (typos fixed, see section 2), source, implemented in
code (`RmValidator2`), relevance for the diff and kind of invariant. 224 rows: 161 RM, 1 RM 1.0.4 outside
the hybrid scope, 62 BASE; 7 implemented.

| Class | Invariant | Rule | Source | Implemented | Relevance for the OPT diff |
|---|---|---|---|---|---|
| `ACCESS_GROUP_REF` | `Type_validity` | `type.is_equal (“ACCESS_GROUP”)` | BASE 1.0.4 | no | none - reference, instance data |
| `ACTIVITY` | `Action_archetype_id_valid` | `not action_archetype_id.is_empty` | RM 1.0.2 | no | none - instance data (action_archetype_id) |
| `ACTOR` | `Roles_valid` | `roles /= Void implies not roles.is_empty` | RM 1.0.4 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `ADDRESS` | `Type_valid` | `type = name` | RM 1.0.4 | no | low: alias - type = name |
| `ARCHETYPED` | `Rm_version_valid` | `not rm_version.is_empty` | RM 1.0.2 | no | none - instance data (archetype_details) |
| `ATTESTATION` | `Items_valid` | `items /= Void implies not items.is_empty` | RM 1.0.2 | no | none - versioning audit, not in an OPT |
| `ATTESTATION` | `Reason_valid` | `(reason.generating_type.is_equal (“DV_CODED_TEXT”) implies terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_attestation_reason, reason.defining_code))` | RM 1.0.2 | no | none - versioning audit, not in an OPT |
| `AUDIT_DETAILS` | `Change_type_valid` | `terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_audit_change_type, change_type.defining_code)` | RM 1.0.2 | no | none - versioning audit, not in an OPT |
| `AUDIT_DETAILS` | `System_id_valid` | `not system_id.is_empty` | RM 1.0.2 | no | none - versioning audit, not in an OPT |
| `AUTHORED_RESOURCE` | `Current_revision_valid` | `(current_revision /= Void and not is_controlled) implies current_revision.is_equal (“(uncontrolled)”)` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `AUTHORED_RESOURCE` | `Description_valid` | `translations /= Void implies (description.details.for_all (d \| translations.has_key (d.language.code_string)))` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `AUTHORED_RESOURCE` | `Languages_available_valid` | `languages_available.has (original_language)` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `AUTHORED_RESOURCE` | `Original_language_valid` | `code_set (Code_set_id_languages).has_code (original_language.as_string)` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `AUTHORED_RESOURCE` | `Revision_history_valid` | `is_controlled xor revision_history = Void` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `AUTHORED_RESOURCE` | `Translations_valid` | `translations /= Void implies (not translations.is_empty and not translations.has (original_language.code_string))` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `Boolean` | `Completeness` | `self or else (not self)` | BASE 1.0.4 | no | none - logic of the BASE type, no OPT meaning |
| `Boolean` | `Involutive_negation` | `is_equal (not (not self))` | BASE 1.0.4 | no | none - logic of the BASE type, no OPT meaning |
| `Boolean` | `Non_contradiction` | `not (self and (not self))` | BASE 1.0.4 | no | none - logic of the BASE type, no OPT meaning |
| `CODE_PHRASE` | `Code_string_valid` | `not code_string.is_empty` | RM 1.0.2 | no | medium: string non-empty - C_CODE_PHRASE: code_string is never empty |
| `COMPOSITION` | `Category_validity` | `terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_composition_category, category.defining_code)` | RM 1.0.2 | no | high: code domain - category codes = openEHR group composition category |
| `COMPOSITION` | `Content_valid` | `content /= Void implies not content.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `COMPOSITION` | `Is_archetype_root` | `is_archetype_root` | RM 1.0.2 | no | medium: attribute interplay - archetype_details present |
| `COMPOSITION` | `Is_persistent_validity` | `is_persistent implies context = Void` | RM 1.0.2 | no | low: attribute interplay - removed in RM 1.0.4 (SPECRM-52): a persistent Composition may have a `context` |
| `COMPOSITION` | `Language_valid` | `code_set(Code_set_id_languages).has_code(language)` | RM 1.0.2 | no | high: code domain - language = ISO 639 code set |
| `COMPOSITION` | `Territory_valid` | `code_set(Code_set_id_countries).has_code(territory)` | RM 1.0.2 | no | high: code domain - territory = ISO 3166 country code set |
| `CONTACT` | `Purpose_valid` | `purpose = name` | RM 1.0.4 | no | low: alias - purpose = name |
| `DV_AMOUNT` | `Accuracy_is_percent_validity` | `accuracy = 0 implies not accuracy_is_percent` | RM 1.0.2 | no | low: attribute interplay - lookup-only attributes |
| `DV_AMOUNT` | `Accuracy_validity` | `accuracy_is_percent implies valid_percentage (accuracy)` | RM 1.0.2 | no | low: numeric bound - lookup-only attributes |
| `DV_DATE` | `Value_valid` | `valid_iso8601_date(value)` | RM 1.0.2 | no | low: format - valid ISO 8601; see the date pattern table |
| `DV_DATE_TIME` | `Value_valid` | `valid_iso8601_date_time (value)` | RM 1.0.2 | no | low: format - valid ISO 8601; see the date pattern table |
| `DV_DURATION` | `Value_valid` | `valid_iso8601_duration (value)` | RM 1.0.2 | no | low: format - valid ISO 8601; see the DV_DURATION table |
| `DV_EHR_URI` | `Scheme_valid` | `scheme.is_equal (Ehr_scheme)` | RM 1.0.2 | no | low: value domain - scheme is "ehr" |
| `DV_ENCAPSULATED` | `Charset_valid` | `charset /= Void implies code_set (Code_set_id_character_sets).has_code (charset)` | RM 1.0.2 | no | low: code domain - lookup-only attribute |
| `DV_ENCAPSULATED` | `Language_valid` | `language /= Void implies code_set (Code_set_id_languages).has_code (language)` | RM 1.0.2 | no | low: code domain - lookup-only attribute |
| `DV_ENCAPSULATED` | `Size_valid` | `size >= 0` | RM 1.0.2 | no | medium: numeric bound - in RM 1.0.4 moved to `DV_MULTIMEDIA` and `DV_PARSABLE` (SPECRM-44); size >= 0: RM fallback of an open size is 0..*, not any integer |
| `DV_IDENTIFIER` | `Assigner_valid` | `not assigner.is_empty` | RM 1.0.2 | no | medium: string non-empty - removed in RM 1.0.4 |
| `DV_IDENTIFIER` | `Id_valid` | `not id.is_empty` | RM 1.0.2 | no | medium: string non-empty |
| `DV_IDENTIFIER` | `Issuer_valid` | `not issuer.is_empty` | RM 1.0.2 | no | medium: string non-empty - removed in RM 1.0.4 |
| `DV_IDENTIFIER` | `Type_valid` | `not type.is_empty` | RM 1.0.2 | no | medium: string non-empty - removed in RM 1.0.4 |
| `DV_INTERVAL` | `Limits_consistent` | `(not upper_unbounded and not lower_unbounded) implies (lower.is_strictly_comparable_to(upper) and lower <= upper)` | RM 1.0.2 | no | low: value consistency - lower <= upper |
| `DV_MULTIMEDIA` | `Compression_algorithm_validity` | `compression_algorithm /= Void implies code_set (Code_set_id_compression_algorithms).has_code (compression_algorithm)` | RM 1.0.2 | no | low: code domain - lookup-only chain |
| `DV_MULTIMEDIA` | `Integrity_check_algorithm_validity` | `integrity_check_algorithm /= Void implies code_set (Code_set_id_integrity_check_algorithms).has_code (integrity_check_algorithm)` | RM 1.0.2 | no | low: code domain |
| `DV_MULTIMEDIA` | `Integrity_check_validity` | `integrity_check /= Void implies integrity_check_algorithm /= Void` | RM 1.0.2 | no | low: attribute interplay - integrity_check implies its algorithm |
| `DV_MULTIMEDIA` | `Media_type_valid` | `media_type /= Void and then code_set (Code_set_id_media_types).has_code (media_type)` | RM 1.0.2 | no | medium: code domain - media_type = IANA media type code set |
| `DV_MULTIMEDIA` | `Not_empty` | `is_inline or is_external` | RM 1.0.2 | no | medium: attribute interplay - data or uri present (is_inline or is_external) |
| `DV_ORDERED` | `Is_simple_validity` | `(normal_range = Void and other_reference_ranges = Void) implies is_simple` | RM 1.0.2 | no | none: derived value - is_simple derived |
| `DV_ORDERED` | `Normal_range_and_status_consistency` | `(normal_range /= Void and normal_status /= Void) implies (normal_status.code_string.is_equal (“N”) xor not normal_range.has (self))` | RM 1.0.2 | no | low: attribute interplay - lookup-only attributes |
| `DV_ORDERED` | `Normal_status_validity` | `normal_status /= Void implies code_set (Code_set_id_normal_statuses).has_code (normal_status)` | RM 1.0.2 | no | low: code domain - lookup-only attribute |
| `DV_ORDERED` | `Other_reference_ranges_validity` | `other_reference_ranges /= Void implies not other_reference_ranges.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `DV_PARSABLE` | `Formalism_valid` | `not formalism.is_empty` | RM 1.0.2 | no | low: string non-empty - RM-mandatory, non-empty |
| `DV_PERIODIC_TIME_SPECIFICATION` | `Value_valid` | `value.formalism.is_equal (“HL7:PIVL”) or value.formalism. is_equal (“HL7:EIVL”)` | RM 1.0.2 | no | low: value domain - formalism of `value` is HL7:PIVL or HL7:EIVL |
| `DV_PROPORTION` | `Fraction_validity` | `(type = pk_fraction or type = pk_integer_fraction) implies is_integral` | RM 1.0.2 | yes | medium: attribute interplay - implemented. type fraction fixes numerator and denominator to integers |
| `DV_PROPORTION` | `Is_integral_validity` | `is_integral implies (numerator.floor = numerator and denominator.floor = denominator)` | RM 1.0.2 | yes | low: attribute interplay - implemented |
| `DV_PROPORTION` | `Percent_validity` | `type = pk_percent implies denominator = 100` | RM 1.0.2 | yes | medium: attribute interplay - implemented. type percent fixes denominator = 100 |
| `DV_PROPORTION` | `Precision_validity` | `precision = 0 implies is_integral` | RM 1.0.2 | yes | low: attribute interplay - implemented |
| `DV_PROPORTION` | `Type_validity` | `valid_proportion_kind (type)` | RM 1.0.2 | yes | medium: value domain - implemented. type in 0..4: RM fallback of an open type |
| `DV_PROPORTION` | `Unitary_validity` | `type = pk_unitary implies denominator = 1` | RM 1.0.2 | yes | medium: attribute interplay - implemented. type unitary fixes denominator = 1: effective denominator depends on the type constraint |
| `DV_PROPORTION` | `Valid_denominator` | `denominator /= 0.0` | RM 1.0.4 (outside the hybrid scope) | yes | medium: numeric bound - RM fallback of an open denominator excludes 0 |
| `DV_QUANTIFIED` | `Magnitude_status_valid` | `magnitude_status /= Void implies valid_magnitude_status (magnitude_status)` | RM 1.0.2 | no | low: code domain - lookup-only attribute; values <, >, <=, >=, ~ |
| `DV_TEXT` | `Encoding_valid` | `encoding /= Void implies code_set (Code_set_id_character_sets).has_code (encoding)` | RM 1.0.2 | no | low: code domain - lookup-only attribute |
| `DV_TEXT` | `Formatting_valid` | `formatting /= void implies not formatting.is_empty` | RM 1.0.2 | no | low: string non-empty - lookup-only attribute |
| `DV_TEXT` | `Language_valid` | `language /= Void implies code_set (Code_set_id_languages).has_code (language)` | RM 1.0.2 | no | low: code domain - lookup-only attribute |
| `DV_TEXT` | `Mappings_valid` | `mappings /= void implies not mappings.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `DV_TEXT` | `Valid_value` | `not value.is_empty` | RM 1.0.4 text | no | low: string domain - decided: only the non-empty part, line breaks are allowed (1.0.2 also forbade CR and LF, removed in 1.0.4 by SPECRM-39; see section 6). The RM fallback of an open string is non-empty, nothing more |
| `DV_TIME` | `Value_valid` | `valid_iso8601_time (value)` | RM 1.0.2 | no | low: format - valid ISO 8601; see the date pattern table |
| `DV_URI` | `Value_valid` | `not value.is_empty` | RM 1.0.2 | no | medium: string non-empty - RM fallback of an open string is non-empty |
| `EHR` | `Compositions_valid` | `compositions.for_all (c \| c.type.is_equal (“VERSIONED_COMPOSITION”))` | RM 1.0.2 | no | none - versioning/EHR object, not constrained by an OPT |
| `EHR` | `Contributions_valid` | `contributions.for_all (c \| c.type.is_equal(“CONTRIBUTION”))` | RM 1.0.2 | no | none - versioning/EHR object, not constrained by an OPT |
| `EHR` | `Directory_valid` | `directory /= Void implies directory.type.is_equal (“VERSIONED_FOLDER”)` | RM 1.0.2 | no | none - versioning/EHR object, not constrained by an OPT |
| `EHR` | `Ehr_access_valid` | `ehr_access.type.is_equal (“VERSIONED_EHR_ACCESS”)` | RM 1.0.2 | no | none - versioning/EHR object, not constrained by an OPT |
| `EHR` | `Ehr_status_valid` | `ehr_status.type.is_equal(“VERSIONED_EHR_STATUS”)` | RM 1.0.2 | no | none - versioning/EHR object, not constrained by an OPT |
| `EHR_ACCESS` | `Is_archetype_root` | `is_archetype_root` | RM 1.0.2 | no | low: attribute interplay - archetype_details present |
| `EHR_ACCESS` | `Scheme_valid` | `not scheme.is_empty` | RM 1.0.2 | no | low: string non-empty - not in an OPT today |
| `EHR_STATUS` | `Is_archetype_root` | `is_archetype_root` | RM 1.0.2 | no | low: attribute interplay - archetype_details present |
| `ELEMENT` | `Is_null_valid` | `is_null = (value = Void)` | RM 1.0.2 | no | low: attribute interplay - data rule: `is_null` is `value = Void`; the OPT constrains `value` on its own |
| `ELEMENT` | `Null_flavour_indicated` | `is_null xor null_flavour = Void` | RM 1.0.2 | no | low: attribute interplay - data rule: `value` and `null_flavour` are exclusive, `null_flavour` explains the missing `value`; the OPT constrains each on its own |
| `ELEMENT` | `Null_flavour_valid` | `is_null implies terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_null_flavour, null_flavour.defining_code)` | RM 1.0.2 | no | high: code domain - null_flavour codes = openEHR group null_flavours; an OPT code list is a subset of the set (narrowing) |
| `ENTRY` | `Encoding_valid` | `code_set (Code_set_id_character_sets).has_code (encoding)` | RM 1.0.2 | no | high: code domain - encoding = IANA character set code set |
| `ENTRY` | `Is_archetype_root` | `is_archetype_root` | RM 1.0.2 | no | medium: attribute interplay - archetype_details present |
| `ENTRY` | `Language_valid` | `code_set (Code_set_id_languages).has_code (language)` | RM 1.0.2 | no | high: code domain - language = ISO 639 code set |
| `ENTRY` | `Other_participations_valid` | `other_participations /= Void implies not other_participations.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `ENTRY` | `Subject_validity` | `subject_is_self implies subject.generating_type = “PARTY_SELF”` | RM 1.0.2 | no | medium: type alternatives - subject_is_self implies PARTY_SELF; ties subject type to its content |
| `EVENT` | `Offset_validity1` | `offset /= Void and then offset = time.diff (parent.origin)` | RM 1.0.2 | no | none: derived value - derived from time, instance data |
| `EVENT_CONTEXT` | `Participations_validity` | `participations /= Void implies not participations.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `EVENT_CONTEXT` | `Setting_valid` | `Terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_setting, setting.defining_code)` | RM 1.0.2 | no | high: code domain - setting codes = openEHR group setting |
| `EVENT_CONTEXT` | `location_valid` | `location /= Void implies not location.is_empty` | RM 1.0.2 | no | low: string non-empty |
| `EXTRACT` | `Sequence_nr_valid` | `sequence_nr >= 1` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `EXTRACT_CONTENT_ITEM` | `Item_validity` | `is_masked xor item /= Void` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `EXTRACT_PARTICIPATION` | `Function_valid` | `function /= Void and then function.generating_type.is_equal (“DV_CODED_TEXT”) implies terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_participation_function, function.defining_code)` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `EXTRACT_PARTICIPATION` | `Mode_valid` | `mode /= Void and terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_participation_mode, mode.defining_code)` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `EXTRACT_UPDATE_SPEC` | `Overall_validity` | `repeat_period /= Void or trigger_events /= Void` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `EXTRACT_UPDATE_SPEC` | `Send_changes_only_validity` | `send_changes_only implies persist_in_server` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `EXTRACT_UPDATE_SPEC` | `Trigger_events_validity` | `trigger_events /= Void implies not trigger_events.is_empty` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `EXTRACT_VERSION_SPEC` | `Includes_revision_history_valid` | `not include_data implies include_revision_history` | RM 1.0.2 | no | none - EHR_EXTRACT, not in an OPT |
| `FEEDER_AUDIT_DETAILS` | `System_id_valid` | `not system_id.is_empty` | RM 1.0.2 | no | none - feeder audit, not constrained by an OPT |
| `FOLDER` | `Folders_valid` | `not folders.is_empty` | RM 1.0.2 | no | none - FOLDER, not in an OPT (directory) |
| `HISTORY` | `Events_valid` | `(events /= Void and then not events.is_empty) or summary /= Void` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `HISTORY` | `Period_consistency` | `is_periodic implies events.for_all (e: EVENT \| e.offset. to_seconds.mod(period.to_seconds) = 0)` | RM 1.0.2 | no | low: attribute interplay - event offsets multiple of period; instance data |
| `HISTORY` | `Periodic_validity` | `is_periodic xor period = Void` | RM 1.0.2 | no | medium: attribute interplay - `period` exists exactly when is_periodic |
| `IDate` | `Day_valid` | `valid_day(year, month, day)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDate` | `Month_valid` | `valid_month (month)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDate_time` | `Day_valid` | `valid_day(year, month, day)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDate_time` | `Hour_valid` | `valid_hour (hour, minute, second)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDate_time` | `Month_valid` | `valid_month (month)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDate_time` | `Year_valid` | `valid_year (year)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Days_valid` | `days >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Fractional_second_valid` | `fractional_second >= 0.0 and fractional_second < 1.0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Hours_valid` | `hours >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Minutes_valid` | `minutes >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Months_valid` | `months >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Seconds_valid` | `seconds >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Weeks_valid` | `weeks >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `IDuration` | `Years_valid` | `years >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `INSTRUCTION` | `Activities_valid` | `activities /= Void implies not activities.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `INSTRUCTION_DETAILS` | `Activity_path_valid` | `not activity_id.is_empty` | RM 1.0.2 | no | none - instance data (instruction_details of an ACTION) |
| `Interval` | `Limits_comparable` | `(not upper_unbounded and not lower_unbounded) implies lower.strictly_comparable_to (upper)` | BASE 1.0.4 | no | low: value consistency - limits of an interval (C_DV_INTERVAL ranges) |
| `Interval` | `Limits_consistent` | `(not upper_unbounded and not lower_unbounded) implies lower <= upper` | BASE 1.0.4 | no | low: value consistency - limits of an interval (C_DV_INTERVAL ranges) |
| `Interval` | `Lower_included_valid` | `lower_unbounded implies not lower_included` | BASE 1.0.4 | no | low: value consistency - limits of an interval (C_DV_INTERVAL ranges) |
| `Interval` | `Upper_included_valid` | `upper_unbounded implies not upper_included` | BASE 1.0.4 | no | low: value consistency - limits of an interval (C_DV_INTERVAL ranges) |
| `INTERVAL_EVENT` | `Interval_start_time_valid` | `interval_start_time = time - width` | RM 1.0.2 | no | none: derived value - derived from time and width, instance data |
| `INTERVAL_EVENT` | `Math_function_validity` | `terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_event_math_function, math_function.defining_code)` | RM 1.0.2 | no | high: code domain - math_function codes = openEHR group event math function |
| `ISM_TRANSITION` | `Current_state_valid` | `terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_instruction_states, current_state.defining_code)` | RM 1.0.2 | no | high: code domain - codes = openEHR group instruction states |
| `ISM_TRANSITION` | `Transition_valid` | `transition /= Void implies terminology (Terminology_id_openehr). has_code_for_group_id (Group_id_instruction_transitions, transition.defining_code)` | RM 1.0.2 | no | high: code domain - codes = openEHR group instruction transitions |
| `Iso8601_date` | `Day_valid` | `not day_unknown implies valid_day (year, month, day)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date` | `Month_valid` | `not month_unknown implies valid_month (month)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date` | `Partial_validity` | `month_unknown implies day_unknown` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date` | `Year_valid` | `valid_year (year)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Day_valid` | `valid_day(year, month, day)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Fractional_second_valid` | `has_fractional_second implies (not second_unknown and valid_fractional_second (fractional_second))` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Hour_valid` | `valid_hour (hour, minute, second)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Minute_valid` | `not minute_unknown implies valid_minute(minute)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Month_valid` | `valid_month (month)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Partial_validity_day` | `not day_unknown` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Partial_validity_hour` | `not hour_unknown` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Partial_validity_minute` | `minute_unknown implies second_unknown` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Partial_validity_month` | `not month_unknown` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Partial_validity_year` | `not month_unknown` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Second_valid` | `not second_unknown implies valid_second (second)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_date_time` | `Year_valid` | `valid_year (year)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Days_valid` | `days >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Fractional_second_valid` | `fractional_second >= 0.0 and fractional_second < 1.0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Hours_valid` | `hours >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Minutes_valid` | `minutes >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Months_valid` | `months >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Seconds_valid` | `seconds >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Weeks_valid` | `weeks >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_duration` | `Years_valid` | `years >= 0` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_time` | `Fractional_second_valid` | `has_fractional_second implies (not second_unknown and valid_fractional_second (fractional_second))` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_time` | `Hour_valid` | `valid_hour(hour, minute, second)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_time` | `Minute_valid` | `not minute_unknown implies valid_minute (minute)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_time` | `Partial_validity` | `minute_unknown implies second_unknown` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_time` | `Second_valid` | `not second_unknown implies valid_second (second)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_timezone` | `Max_hour_valid` | `sign = 1 implies hour > 0 and hour <= Max_timezone_hour` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_timezone` | `Min_hour_valid` | `sign = -1 implies hour > 0 and hour <= Min_timezone_hour` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_timezone` | `Minute_valid` | `not minute_unknown implies valid_minute (minute)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `Iso8601_timezone` | `Sign_valid` | `sign = 1 or sign = -1` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `ITEM_LIST` | `Valid_structure` | `items.forall (i:ITEM \| i.type = "ELEMENT")` | RM 1.0.2 | no | high: type alternatives - `items` can only be ELEMENT, not any ITEM |
| `ITEM_TABLE` | `Valid_structure` | `rows.for_all (items.for_all (instance_of ("ELEMENT")))` | RM 1.0.2 | no | high: type alternatives - rows are CLUSTER and their items ELEMENT, not any ITEM |
| `ITime` | `Hour_valid` | `valid_hour(hour, minute, second)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `ITimezone` | `Max_hour_valid` | `sign = 1 implies hour > 0 and hour <= Max_timezone_hour` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `ITimezone` | `Min_hour_valid` | `sign = -1 implies hour > 0 and hour <= Min_timezone_hour` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `ITimezone` | `Minute_valid` | `not minute_unknown implies valid_minute (minute)` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `ITimezone` | `Sign_valid` | `sign = 1 or sign = -1` | BASE 1.0.4 | no | low: format - valid ISO 8601 parts; see the date pattern table |
| `List` | `First_validity` | `not is_empty implies first /= Void` | BASE 1.0.4 | no | none - BASE container behaviour, no OPT meaning |
| `List` | `Last_validity` | `not is_empty implies last /= Void` | BASE 1.0.4 | no | none - BASE container behaviour, no OPT meaning |
| `LOCATABLE` | `Archetype_node_id_valid` | `not archetype_node_id.is_empty` | RM 1.0.2 | no | low: string non-empty - node_id is never empty |
| `LOCATABLE` | `Archetyped_valid` | `is_archetype_root xor archetype_details = Void` | RM 1.0.2 | no | low: attribute interplay - data rule: `archetype_details` is present exactly for archetype roots. An archetype root is created by including an archetype in the template and removed only by excluding it; it is replaced by a new version or a specialisation of the archetype (`archetype_id` rules, rules doc 6.4); anything else is a removal and, maybe, an unrelated addition. A node does not stop being an archetype root |
| `LOCATABLE` | `Links_valid` | `links /= Void implies not links.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `ORIGINAL_VERSION` | `Attestations_valid` | `attestations /= Void implies not attestations.is_empty` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `ORIGINAL_VERSION` | `Is_merged_validity` | `other_input_version_ids = Void xor is_merged` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `ORIGINAL_VERSION` | `Other_input_version_uids_valid` | `other_input_version_uids /= Void implies not other_input_version_uids.is_empty` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `PARTICIPATION` | `Function_valid` | `function.generating_type.is_equal (“DV_CODED_TEXT”) implies terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_participation_function, function.defining_code)` | RM 1.0.2 | no | medium: code domain - ENTRY.other_participations; coded function = openEHR group |
| `PARTICIPATION` | `Mode_valid` | `terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_participation_mode, mode.defining_code)` | RM 1.0.2 | no | medium: code domain - in RM 1.0.4 `mode` is optional (`mode /= Void implies ...`); ENTRY.other_participations; mode = openEHR group |
| `PARTY` | `Contacts_valid` | `contacts /= Void implies not contacts.is_empty` | RM 1.0.4 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `PARTY` | `Identities_valid` | `not identities.is_empty` | RM 1.0.4 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `PARTY` | `Is_archetype_root` | `is_archetype_root` | RM 1.0.4 | no | low: attribute interplay - archetype_details present |
| `PARTY` | `Relationships_validity` | `relationships /= Void implies (not relationships.is_empty and then relationships.for_all (r \| r.source = self)` | RM 1.0.4 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `PARTY` | `Reverse_relationships_validity` | `reverse_relationships /= Void implies (not reverse_relationships.empty and then reverse_relationships.for_all (item \| repository ("demographics").all_party_relationships.has_object (item) and then repository("demographics").all_party_relationships.object (item).target = self))` | RM 1.0.4 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `PARTY` | `Type_valid` | `type = name` | RM 1.0.4 | no | low: alias - type = name |
| `PARTY` | `Uid_mandatory` | `uid /= Void` | RM 1.0.4 | no | medium: attribute interplay - uid is mandatory in RM 1.0.4: belongs to the mandatory set of complete() |
| `PARTY_IDENTIFIED` | `Basic_validity` | `name /= Void or identifiers /= Void or external_ref /= Void` | RM 1.0.2 | no | medium: attribute interplay - name, identifiers or external_ref: at least one |
| `PARTY_IDENTIFIED` | `Identifiers_valid` | `identifiers /= Void implies not identifiers.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `PARTY_IDENTIFIED` | `Name_valid` | `name /= Void implies not name.is_empty` | RM 1.0.2 | no | low: string non-empty |
| `PARTY_IDENTITY` | `Purpose_valid` | `purpose = name` | RM 1.0.4 | no | low: alias - purpose = name |
| `PARTY_REF` | `Type_validity` | `type.is_equal(“PERSON”) or type.is_equal(“ORGANISATION”) or type.is_equal(“GROUP”) or type.is_equal(“AGENT”)or type.is_equal(“ROLE”) or type.is_equal(“PARTY”) or type.is_equal(“ACTOR”)` | RM 1.0.2 | no | none - reference, instance data |
| `PARTY_RELATED` | `Relationship_valid` | `terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_subject_relationship, relationship.defining_code)` | RM 1.0.2 | no | none - subject relationship, see ENTRY.Subject_validity |
| `PARTY_RELATIONSHIP` | `Source_valid` | `source /= Void and then source.relationships.has (self)` | RM 1.0.4 | no | none - demographic reference integrity, instance data |
| `PARTY_RELATIONSHIP` | `Target_valid` | `target /= Void and then not target.reverse_relationships.has (self)` | RM 1.0.4 | no | none - demographic reference integrity, instance data |
| `PARTY_RELATIONSHIP` | `Type_validity` | `type = name` | RM 1.0.4 | no | none - demographic reference integrity, instance data |
| `REFERENCE_RANGE` | `Range_is_simple` | `(range.lower_unbounded or else range.lower.is_simple) and (range.upper_unbounded or else range.upper.is_simple)` | RM 1.0.2 | no | low: value consistency - lookup-only attribute |
| `RESOURCE_DESCRIPTION` | `Details_valid` | `not details.is_empty` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION` | `Language_valid` | `parent_resource /= Void implies details.for_all (d \| parent_resource.languages_available.has (d.language.code_string))` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION` | `Lifecycle_state_valid` | `not lifecycle_state.is_empty` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION` | `Original_author_valid` | `not original_author.is_empty` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION` | `Parent_resource_valid` | `parent_resource /= Void implies parent_resource.description = self` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION_ITEM` | `Language_valid` | `code_set (Code_set_id_languages).has_code (language)` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION_ITEM` | `Purpose_valid` | `not purpose.is_empty` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION_ITEM` | `Use_valid` | `use /= Void implies not use.is_empty` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION_ITEM` | `copyright_valid` | `copyright /= Void implies not copyright.is_empty` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `RESOURCE_DESCRIPTION_ITEM` | `misuse_valid` | `misuse /= Void implies not misuse.is_empty` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `REVISION_HISTORY_ITEM` | `Audit_valid` | `not audits.is_empty` | RM 1.0.2 | no | none - revision history, not in an OPT |
| `ROLE` | `Capabilities_valid` | `capabilities /= Void implies not capabilities.empty` | RM 1.0.4 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `SECTION` | `Items_valid` | `items /= Void implies not items.is_empty` | RM 1.0.2 | no | low: container non-empty - empty list is normalized to null, the RM baseline stays 0..* |
| `TERM_MAPPING` | `Match_valid` | `is_valid_match_code (match)` | RM 1.0.2 | no | none - mapping of a term, not constrained by an OPT |
| `TERM_MAPPING` | `Purpose_valid` | `purpose /= Void implies terminology (Terminology_id_openehr).has_code_for_group_id (Group_id_term_mapping_purpose, purpose.defining_code)` | RM 1.0.2 | no | none - mapping of a term, not constrained by an OPT |
| `TRANSLATION_DETAILS` | `Language_valid` | `code_set (Code_set_id_languages).has_code (language)` | RM 1.0.2 | no | none - OPT metadata (rules doc section 8) |
| `UID` | `Value_valid` | `not value.empty` | RM 1.0.2 | no | none - identifier format, instance data |
| `UID_BASED_ID` | `Has_extension_valid` | `extension.is_empty xor has_extension` | RM 1.0.2 | no | none - identifier format, instance data |
| `VERSION` | `Lifecycle_state_valid` | `lifecycle_state /= Void and then terminology (Term_id_openehr).has_code_for_group_id (Group_id_version_lifecycle_state, lifecycle_state.defining_code)` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSION` | `Owner_id_valid` | `owner_id.value.is_equal (uid.object_id.value)` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSION` | `Preceding_version_uid_validity` | `uid.version_tree_id.is_first xor preceding_version_uid /= Void` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSIONED_COMPOSITION` | `Archetype_node_id_valid` | `all_versions.for_all (v \| v.archetype_node_id.is_equal (all_versions.first.archetype_node_id))` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSIONED_COMPOSITION` | `Persistent_validity` | `all_versions.for_all (v \| v.is_persistent = all_versions.first.data.is_persistent)` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSIONED_OBJECT` | `All_version_ids_valid` | `all_version_ids.count = version_count` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSIONED_OBJECT` | `All_versions_valid` | `all_versions.count = version_count` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSIONED_OBJECT` | `Latest_version_valid` | `version_count > 0 implies latest_version /= Void` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSIONED_OBJECT` | `Version_count_valid` | `version_count >= 0` | RM 1.0.2 | no | none - versioning, not in an OPT |
| `VERSION_TREE_ID` | `Branch_number_valid` | `branch_number /= Void implies branch_number.is_integer and then branch_number.as_integer >= 1` | RM 1.0.2 | no | none - identifier format, instance data |
| `VERSION_TREE_ID` | `Branch_validity` | `(branch_number = Void and branch_version = Void ) xor (branch_number /= Void and branch_version /= Void )` | RM 1.0.2 | no | none - identifier format, instance data |
| `VERSION_TREE_ID` | `Branch_version_valid` | `branch_version /= Void implies branch_version.is_integer and then branch_version.as_integer >= 1` | RM 1.0.2 | no | none - identifier format, instance data |
| `VERSION_TREE_ID` | `Is_branch_validity` | `is_branch xor branch_number = Void` | RM 1.0.2 | no | none - identifier format, instance data |
| `VERSION_TREE_ID` | `Is_first_validity` | `not is_first xor trunk_version.is_equal(“1”)` | RM 1.0.2 | no | none - identifier format, instance data |
| `VERSION_TREE_ID` | `Trunk_version_valid` | `trunk_version /= Void and then trunk_version.is_integer and then trunk_version.as_integer >= 1` | RM 1.0.2 | no | none - identifier format, instance data |
| `VERSION_TREE_ID` | `Value_valid` | `not value.is_empty` | RM 1.0.2 | no | none - identifier format, instance data |

## 6. Invariants that change between releases

Rule texts compared by name between the BMMs of RM 1.0.2, 1.0.4 and 1.1.0 (the hybrid scope uses 1.0.2
for everything but the demographic classes, so the rows above show the 1.0.2 rule where the release
differs). Names that only changed spelling or that moved to the BASE model (`UID`, `UID_BASED_ID`,
`VERSION_TREE_ID`, `PARTY_REF`, `FOLDER`, `ACTIVITY.Action_archteype_id_valid` typo) are not listed.

| Invariant | 1.0.2 | 1.0.4 | 1.1.0 | Source (spec history) |
|---|---|---|---|---|
| `DV_TEXT.Valid_value` | `not value.is_empty and then not (value.has(CR) or value.has(LF))` | `not value.is_empty` | same as 1.0.4 | SPECRM-39 (newlines and formatting in `DV_TEXT`); `DV_PARAGRAPH` is deprecated and not in this list (decided) |
| `COMPOSITION.Is_persistent_validity` | `is_persistent implies context = Void` | removed | removed | SPECRM-52 |
| `DV_IDENTIFIER.Issuer_valid`, `Assigner_valid`, `Type_valid` | `not <attribute>.is_empty` | removed (`Id_valid` stays) | removed | commit b412fc9 (SPECRM-23), to confirm |
| `DV_ENCAPSULATED.Size_valid` | `size >= 0` | moved to `DV_MULTIMEDIA.Size_valid` and `DV_PARSABLE.Size_valid` | same | SPECRM-44 |
| `PARTICIPATION.Mode_valid` | `terminology (...).has_code_for_group_id (Group_id_participation_mode, mode.defining_code)` | `mode /= Void implies ...` | same | 1.0.4 (optional `mode`) |
| `DV_PROPORTION.Valid_denominator` | none | `denominator /= 0.0` | same | SPECRM-32 |
| `PARTY.Uid_mandatory` | none | `uid /= Void` | same | SPECRM-82 |
| `ACTOR.Legal_identity_exists` | `has_legal_identity` | removed | removed | demographic: the 1.0.4 rule applies |
| `ELEMENT` null invariants | `Is_null_valid`, `Null_flavour_indicated`, `Null_flavour_valid` | same | renamed `Inv_*`, plus `Inv_null_reason_valid`: `null_reason /= Void implies is_null()` | 1.1.0 adds `ELEMENT.null_reason` |
| `EHR` | 5 invariants | same | adds `Folders_valid`, `Directory_in_folders` | not in an OPT |
| `ITEM_TAG` | does not exist | does not exist | `Inv_key_valid`, `Inv_value_valid` | not in an OPT |

Effect on the diff: the line break rule is not part of the RM fallback of a text (the baseline for an
open `DV_TEXT.value` string is non-empty text with any characters, see section 3, group 4). The other
rows keep their level; where 1.0.4 removes the invariant, the row says so.
