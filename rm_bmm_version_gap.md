# RM version GAP from the BMM files (1.0.2, 1.0.4, 1.1.0)

**Status:** generated analysis, 2026-11-23, nothing implemented. Companion of [opt_diff_complete_gap_plan.md](opt_diff_complete_gap_plan.md) (G9, RM data source D2).
**Decision (2026-11-23):** the OPT diff work proceeds with RM 1.0.2 as-is; this document is scope for the later RM migration and does not change the diff baseline. Nothing here comes from `master` of the RM repo (RM 1.2.0, in development).
**Purpose:** scope the migration of the SDK (and Atomik) from RM 1.0.2 to the last stable release 1.1.0, in two phases (1.0.2 -> 1.0.4 -> 1.1.0).

## 1. Sources and method

- BMM files: `../specifications-RM`, branches `origin/release/1.0.2`, `release/1.0.4`, `release/1.1.0`, file `computable/BMM/openehr_rm_<version>.bmm.json` (BMM 2.4; RM 1.0.3 has no BMM on its branch and is skipped: 1.0.4 includes it). `master` is RM 1.2.0 in development and is out of scope.
- Compared per class: properties added/removed, existence (`is_mandatory`: present = 1..1, absent = 0..1), type (`type`/`type_def`, generics, container type), container cardinality, ancestors, abstract flag.
- Base classes (identifiers, `OBJECT_REF`, `UID`, ...) were inlined in the 1.0.2 BMM and moved to the included base BMM from 1.0.4 on (`openehr_base_1.1.0`, `openehr_base_1.2.0` in `../specifications-BASE`); they are not compared here, see section 5.
- The last column checks, by name, whether the SDK RM class `rm_1_0_2/**` has the property (`no class` = the class does not exist in the SDK). It is a text match, not a semantic check.
- **Verified against the spec text (2026-11-23).** The spec class tables of each release (`partials/definitions/*.adoc` on the `release/*` branch: attributes with existence, type, `(redefined)` rows) were parsed and compared with the BMM of that release: 0 differences in classes, attributes, existence, abstract flag and container type for 1.0.2 (161 classes), 1.0.4 (146) and 1.1.0 (155). Not covered: container cardinality (the tables only give existence; it comes from invariants) and generic type parameters. So the BMM is a faithful copy of the published spec, and the differences below are real changes of the spec text between releases, including corrections of errata of the earlier release.
- Regenerate: the script is `gen_gap.py` (scratchpad of the session); the tables below are its output.

## 2. Phase 1: RM 1.0.2 -> 1.0.4

No classes added or removed (apart from the base classes of section 5). 40 property or class level differences:

| Class | Property | Change | Detail | In SDK model today |
|---|---|---|---|---|
| `ACTIVITY` | `timing` | changed: existence | `1..1 DV_PARSABLE` -> `0..1 DV_PARSABLE` | yes |
| `ACTOR` | `languages` | changed: existence, cardinality | `1..1 List<DV_TEXT> 1..*` -> `0..1 List<DV_TEXT> 0..*` | yes |
| `ACTOR` | `roles` | changed: existence, cardinality | `1..1 List<PARTY_REF> 1..*` -> `0..1 List<PARTY_REF> 0..*` | yes |
| `AUTHORED_RESOURCE` | `translations` | changed: type, cardinality | `0..1 List<TRANSLATION_DETAILS> 0..*` -> `0..1 Hash<String, TRANSLATION_DETAILS>` | no class |
| `DV_COUNT` | `normal_range` | added | `0..1 DV_INTERVAL<DV_COUNT>` | no |
| `DV_COUNT` | `other_reference_ranges` | added | `0..1 List<{"_type": "P_BMM_GENERIC_TYPE", "root_type": "REFERENCE_RANGE", "generic_parameters": ["DV_COUNT"]}> 0..*` | no |
| `DV_COUNT` | `magnitude` | changed: type | `1..1 Integer` -> `1..1 Integer64` | yes |
| `DV_IDENTIFIER` | `assigner` | changed: existence | `1..1 String` -> `0..1 String` | yes |
| `DV_IDENTIFIER` | `issuer` | changed: existence | `1..1 String` -> `0..1 String` | yes |
| `DV_IDENTIFIER` | `type` | changed: existence | `1..1 String` -> `0..1 String` | yes |
| `DV_MULTIMEDIA` | `size` | added | `1..1 Integer` | yes |
| `DV_MULTIMEDIA` | `data` | changed: type, cardinality | `0..1 Byte` -> `0..1 Array<Octet> 0..*` | yes |
| `DV_MULTIMEDIA` | `integrity_check` | changed: type, cardinality | `0..1 Byte` -> `0..1 Array<Octet> 0..*` | yes |
| `DV_PROPORTION` | `normal_range` | added | `0..1 DV_INTERVAL<DV_PROPORTION>` | no |
| `DV_PROPORTION` | `other_reference_ranges` | added | `0..1 List<{"_type": "P_BMM_GENERIC_TYPE", "root_type": "REFERENCE_RANGE", "generic_parameters": ["DV_PROPORTION"]}> 0..*` | no |
| `DV_QUANTITY` | `normal_range` | added | `0..1 DV_INTERVAL<DV_QUANTITY>` | no |
| `DV_QUANTITY` | `other_reference_ranges` | added | `0..1 List<{"_type": "P_BMM_GENERIC_TYPE", "root_type": "REFERENCE_RANGE", "generic_parameters": ["DV_QUANTITY"]}> 0..*` | no |
| `DV_TEMPORAL` | `accuracy` | added | `0..1 DV_DURATION` | no |
| `ELEMENT` | `null_flavour` | added | `0..1 DV_CODED_TEXT` | yes |
| `ELEMENT` | `null_flavor` | removed | was `0..1 DV_CODED_TEXT` | no |
| `EVENT` | `data` | changed: type | `1..1 ITEM_STRUCTURE` -> `1..1 T` | yes |
| `EXTRACT_CONTENT_ITEM` | `uid` | removed | was `1..1 HIER_OBJECT_ID` | no class |
| `FOLDER` | `details` | added | `0..1 ITEM_STRUCTURE` | no |
| `GENERIC_CONTENT_ITEM` | `other_details` | changed: type, cardinality | `0..1 List<String> 0..*` -> `0..1 Hash<String, String>` | no class |
| `ISM_TRANSITION` | `reason` | added | `0..1 List<DV_TEXT> 0..*` | no |
| `ITEM_TABLE` | `rows` | changed: type, cardinality | `0..1 CLUSTER` -> `0..1 List<CLUSTER> 0..*` | yes |
| `PARTICIPATION` | `mode` | changed: existence | `1..1 DV_CODED_TEXT` -> `0..1 DV_CODED_TEXT` | yes |
| `PARTY` | `uid` | removed | was `1..1 HIER_OBJECT_ID` | no |
| `PATHABLE` | `(ancestors)` | changed: ancestors | `None` -> `['Any']` | - |
| `RESOURCE_DESCRIPTION` | `details` | changed: type, cardinality | `1..1 List<RESOURCE_DESCRIPTION_ITEM> 1..*` -> `1..1 Hash<String, RESOURCE_DESCRIPTION_ITEM>` | no class |
| `VERSIONED_COMPOSITION` | `(ancestors)` | changed: ancestors | `None` -> `['VERSIONED_OBJECT']` | - |
| `VERSIONED_EHR_ACCESS` | `(ancestors)` | changed: ancestors | `None` -> `['VERSIONED_OBJECT']` | - |
| `VERSIONED_EHR_STATUS` | `(ancestors)` | changed: ancestors | `None` -> `['VERSIONED_OBJECT']` | - |
| `VERSIONED_FOLDER` | `(ancestors)` | changed: ancestors | `None` -> `['VERSIONED_OBJECT']` | - |
| `VERSIONED_PARTY` | `(ancestors)` | changed: ancestors | `None` -> `['VERSIONED_OBJECT']` | - |
| `X_VERSIONED_COMPOSITION` | `(ancestors)` | changed: ancestors | `None` -> `['X_VERSIONED_OBJECT']` | - |
| `X_VERSIONED_EHR_ACCESS` | `(ancestors)` | changed: ancestors | `None` -> `['X_VERSIONED_OBJECT']` | - |
| `X_VERSIONED_EHR_STATUS` | `(ancestors)` | changed: ancestors | `None` -> `['X_VERSIONED_OBJECT']` | - |
| `X_VERSIONED_FOLDER` | `(ancestors)` | changed: ancestors | `None` -> `['X_VERSIONED_OBJECT']` | - |
| `X_VERSIONED_PARTY` | `(ancestors)` | changed: ancestors | `None` -> `['X_VERSIONED_OBJECT']` | - |

## 3. Phase 2: RM 1.0.4 -> 1.1.0

Purely additive for existing classes (no property removed or changed). 6 differences on existing classes:

| Class | Property | Change | Detail | In SDK model today |
|---|---|---|---|---|
| `CODE_PHRASE` | `preferred_term` | added | `0..1 String` | no |
| `DV_QUANTITY` | `units_display_name` | added | `0..1 String` | no |
| `DV_QUANTITY` | `units_system` | added | `0..1 String` | no |
| `EHR` | `folders` | added | `0..1 List<OBJECT_REF> 0..*` | no |
| `ELEMENT` | `null_reason` | added | `0..1 DV_TEXT` | no |
| `FEEDER_AUDIT_DETAILS` | `other_details` | added | `0..1 ITEM_STRUCTURE` | no class |

New classes (11):

| Class | Abstract | Ancestors | Properties (existence type) | In SDK |
|---|---|---|---|---|
| `CITATION` | no | VIEW_ENTRY | `source_type` 0..1 String; `source_ref` 1..1 LOCATABLE_REF; `source_parent_ref` 1..1 LOCATABLE_REF; `resolved` 1..1 L | no |
| `CONSUMABLE_USE` | no | RESOURCE_USE | `amount` 0..1 DATA_VALUE | no |
| `DV_SCALE` | no | DV_ORDERED | `symbol` 1..1 DV_CODED_TEXT; `value` 1..1 Real | no |
| `ITEM_TAG` | no |  | `target` 1..1 UID_BASED_ID; `target_path` 0..1 String; `key` 1..1 String; `value` 0..1 String; `owner_id` 1..1 OBJECT_REF | no |
| `RESOURCE_USAGE` | no |  | `prospective` 1..1 RESOURCE_USE; `actual` 1..1 RESOURCE_USE; `type` 1..1 DV_TEXT; `description` 0..1 String; `reference` 0..1 Any | no |
| `RESOURCE_USE` | yes |  | `cost_data` 0..1 ITEM_STRUCTURE | no |
| `SERVICE_USE` | no | RESOURCE_USE | `identifier` 0..1 GENERIC_ID; `start_time` 0..1 Iso8601_date_time; `duration` 0..1 Iso8601_duration | no |
| `VIEW_ENTRY` | yes | VIEW_ITEM |  | no |
| `VIEW_ITEM` | yes | CONTENT_ITEM | `meta_data` 0..1 CLUSTER | no |
| `VIEW_SECTION` | no | VIEW_ITEM | `items` 0..1 List<VIEW_ITEM> 0..* | no |
| `VIEW_STATUS` | no | String |  | no |

## 3.1 Reading the results

- **Many phase 1 rows are corrections of the 1.0.2 spec itself**, not BMM errors: `ELEMENT.null_flavor` -> `null_flavour` (the 1.0.2 spec text has the typo; the SDK, Atomik and canonical JSON use `null_flavour`), `AUTHORED_RESOURCE.translations`, `RESOURCE_DESCRIPTION.details` and `GENERIC_CONTENT_ITEM.other_details` from `List` to `Hash`, `DV_MULTIMEDIA.data` and `integrity_check` from `Byte` to `Array<Octet>`, `PATHABLE` and `VERSIONED_*` ancestors, `ACTOR.languages`/`roles` from 1..1 to 0..1. Each is what the 1.0.2 spec says, so generating the RM 1.0.2 model without changes would reproduce its errata (notably `null_flavor`). Whether each row is an erratum or a deliberate change must still be decided from the release notes / change requests.
- **The SDK already has some 1.0.4 properties** (column "In SDK model today" only checks that the property name exists, not its existence or type): `DV_MULTIMEDIA.size` and `ELEMENT.null_flavour`. Whether the SDK follows the 1.0.4 existence flips (`DV_IDENTIFIER`, `PARTICIPATION.mode`, `ACTIVITY.timing`, `ACTOR`) has not been checked.
- **Consequence for the RM data of the OPT diff (D2):** the diff works on RM 1.0.2. The BMM can be the source of truth (it matches the spec), but the 1.0.2 errata (at least `null_flavor`) need an explicit overlay in the generator, or the model needs a deliberate choice of version. For the RM attributes the diff and `complete()` use (existence of the 14 DV types, structural attributes, container cardinality), 1.0.2 and 1.0.4 agree except `ACTOR` (`roles`, `languages`), `ACTIVITY.timing`, `PARTICIPATION.mode` and `DV_IDENTIFIER`, none of which the OPT diff baseline injects today.
- Real additions between 1.0.2 and 1.0.4: `ISM_TRANSITION.reason`, `DV_MULTIMEDIA.size`, `FOLDER.details` (to confirm). The `normal_range`, `other_reference_ranges` and `accuracy` rows are redefinitions of inherited attributes, and the `uid` rows are removed redefinitions: see 3.2.

## 3.2 Classification of the phase 1 rows by the RM amendment records

Source: `modules/*/partials/amendment_record.adoc` on `origin/release/1.0.4` (entries of RM 1.0.3 and 1.0.4), plus the comparison of **effective** attributes (own plus inherited) of the 1.0.2 and 1.0.4 BMM. Pablo confirmed (2026-11-23) that `null_flavour` is the correct name, and that `ACTOR.roles`/`languages` are containers of `PARTY_REF` (a reference, like a foreign key) that must not be `1..1 1..*`.

| Group | Rows (1.0.2 -> 1.0.4) | Amendment record | Reading |
|---|---|---|---|
| Erratum: UML conversion error / bug of the 1.0.2 spec | `ACTOR.languages`, `ACTOR.roles` existence `1..1` -> `0..1`, cardinality `1..*` -> `0..*` | SPECRM-82 (fixes SPECPR-282, SPECPR-283) | 1.0.2 is wrong; 1.0.4 is the correct default. |
| Erratum | `DV_MULTIMEDIA.data`, `integrity_check` `Byte` -> `Array<Octet>`; `integrity_check_algorithm` multiplicity `[0..1]` | SPECPUB-3, SPECPR-295 | 1.0.2 is wrong. |
| Erratum | `EVENT.data` `ITEM_STRUCTURE` -> generic parameter `T` | SPECPUB-3 | 1.0.2 is wrong. |
| Erratum | ancestors of `VERSIONED_*` and `X_VERSIONED_*` (inheritance re-instated) | SPECPUB-3 | 1.0.2 is wrong. |
| Erratum | `Hash<V,K>` -> `Hash<K,V>` (affects `resource` package) | SPECRM-50 | Type parameter order. |
| Erratum (not in the records) | `ELEMENT.null_flavor` -> `null_flavour` | not recorded | Typo of the 1.0.2 spec; the correct name is `null_flavour`. |
| Probably erratum (not in the records) | `AUTHORED_RESOURCE.translations`, `RESOURCE_DESCRIPTION.details`, `GENERIC_CONTENT_ITEM.other_details` `List` -> `Hash<String, ...>`; `ITEM_TABLE.rows` `CLUSTER` -> `List<CLUSTER>`; `PATHABLE` ancestors; `EXTRACT_CONTENT_ITEM.uid` / `PARTY.uid` (redefinition removed, effective `0..1 UID_BASED_ID` instead of `1..1 HIER_OBJECT_ID`, SPECRM-82 for `PARTY.uid`) | partly SPECRM-82 | To confirm per row. |
| Deliberate change of RM 1.0.3 | `PARTICIPATION.mode` optional (SPECRM-21); `DV_IDENTIFIER.issuer`, `assigner`, `type` optional (SPECRM-23); `ISM_TRANSITION.reason` added (SPECRM-37); `DV_COUNT.magnitude` `Integer` -> `Integer64` (SPECRM-29); `DV_ENCAPSULATED.size` abstract property removed (SPECRM-44) and `DV_MULTIMEDIA.size` concrete | listed | Real RM change; RM 1.0.3 is already newer than 1.0.2. |
| Deliberate change of RM 1.0.4 | `ACTIVITY.timing` optional (SPECRM-71); `DV_PROPORTION` zero denominator invariant (1.0.3, SPECRM-32) | listed | Real RM change. |
| Not new attributes | `DV_COUNT`, `DV_PROPORTION`, `DV_QUANTITY` `normal_range` and `other_reference_ranges`; `DV_TEMPORAL.accuracy` (`DV_AMOUNT` -> `DV_DURATION`) | n/a | They exist in 1.0.2 as inherited attributes (`DV_ORDERED`, `DV_QUANTIFIED`); the 1.0.4 BMM redefines them with the concrete generic parameter. The "added" rows of section 2 are retyped redefinitions. |
| Not in the records | `FOLDER.details` (effective attribute only in 1.0.4) | none found | To confirm. |

**Consequences for RM 1.0.2 as-is**
- The BMM 1.0.2 is the source of truth of what the 1.0.2 spec says, which contains bugs. A generated model needs an errata overlay for at least `ELEMENT.null_flavour`, `ACTOR.languages` and `ACTOR.roles` (`0..1`, `0..*`), and probably the other erratum rows. The deliberate changes of 1.0.3 and 1.0.4 stay out until the RM migration.
- None of the rows above changes the attributes `complete()` injects today (`rm_attributes_not_in_opt`, `rm_mandatory_attributes`) except `ELEMENT.null_flavour`, which the SDK already spells correctly.

## 3.3 Decisions for the current scope (2026-11-23)

- **Hybrid RM version (Pablo).** For the OPT diff work the demographic package (`PARTY`, `CONTACT`, `ADDRESS`, `PARTY_IDENTITY`, `ROLE`, `ACTOR`, `CAPABILITY`, `AGENT`, `ORGANISATION`, `GROUP`, `PERSON`, `PARTY_RELATIONSHIP`, `VERSIONED_PARTY`) is taken from RM 1.0.4 and everything else (EHR, composition, data structures, data types, common, support) stays on RM 1.0.2. Everything moves to the latest release later (this document). Effects: `ACTOR.languages`/`roles` are `0..1`, `0..*`; `PARTY.uid` is inherited (`0..1 UID_BASED_ID`) and no longer redefined as `1..1 HIER_OBJECT_ID`. A demographic class that uses a type of another package (e.g. `DV_IDENTIFIER`) gets the 1.0.2 shape of that type.
- **Generics of the EVENT family are not modelled.** In the BMM `T` of `EVENT`, `POINT_EVENT`, `INTERVAL_EVENT` and `HISTORY` conforms to `ITEM_STRUCTURE`; OPTs never write those generics (`HISTORY`, `EVENT`, `POINT_EVENT`, `INTERVAL_EVENT` appear plain in the sample OPTs). The generated model replaces `T` by its bound: `EVENT.data: ITEM_STRUCTURE`, `HISTORY.events: List<EVENT>`, `OBSERVATION.data` and `state`: `HISTORY`. The allowed `ITEM_STRUCTURE` descendant is decided by the archetype or template constraint of the child node. `DV_INTERVAL<T>` keeps its generic parameter (OPTs write `DV_INTERVAL<DV_QUANTITY>`, `DV_INTERVAL<DV_DATE_TIME>`, `DV_INTERVAL<DV_COUNT>`). Only the EVENT family, `HISTORY` and `OBSERVATION` are simplified (Pablo 2026-11-23); every other generic is kept as it is, including `DV_INTERVAL<T>`, `REFERENCE_RANGE<T>` and the versioning generics (`VERSION<T>`, `VERSIONED_OBJECT<T>`, `X_VERSIONED_OBJECT<T>`), which are not touched.
- **Errata overlay for the 1.0.2 side** (see 3.2): `ELEMENT.null_flavour`, `DV_MULTIMEDIA.data` and `integrity_check` as `Array<Octet>`, and the other erratum rows after the per-row check. `Byte` in the 1.0.2 text is a single value; the correct type is an array of octets.

## 4. Work per phase (what the GAP check has to cover)

| Area | Phase 1 (1.0.2 -> 1.0.4) | Phase 2 (1.0.4 -> 1.1.0) |
|---|---|---|
| RM classes (`rm_1_0_2/**`) | Add properties, change types/cardinalities, ancestors of `PATHABLE` and `VERSIONED_*`, remove `PARTY.uid` / `EXTRACT_CONTENT_ITEM.uid`. Verify each row against the spec text. | Add new classes and properties; no changes to existing ones. |
| JSON serialization / parsing | Follow the class changes (canonical JSON names). Verify `null_flavour` spelling. | New classes and optional properties. |
| JSON Schemas (`src/main/resources/json_schema`, improved copies of the official ones) | Merge the official 1.0.4 schema changes into our improved copies; existence flips change `required`. | Merge the official 1.1.0 additions. |
| `RmValidator2` / `Model.groovy` | Existence flips change mandatory attributes (`rm_mandatory_attributes`, validation of `ACTOR`, `DV_IDENTIFIER`, `PARTICIPATION`, `ACTIVITY`). | New types and attributes in the model maps (or generated from the BMM). |
| OPT parser / `complete()` / indexer | Injected attributes follow the RM model (D2). | Same. |
| Tests | Existing fixtures per class; new fixtures for flips. | New class fixtures. |

## 5. Open items

1. Base classes: compare the base BMMs (`openehr_base_1.1.0` -> `1.2.0`, and the inlined base classes of the 1.0.2 file) before the generator is written.
2. `ELEMENT.null_flavor` (1.0.2 spec and BMM) vs `null_flavour` (1.0.4 spec and BMM, SDK, Atomik): the 1.0.2 canonical JSON / JSON Schema spelling is to confirm; the generator needs an errata overlay for it.
3. Spec check of every row of section 2 is done in the sense that BMM = spec text; what is open is whether each difference is an erratum or a deliberate change (release notes).
4. Canonical JSON schemas of RM 1.0.4 / 1.1.0: where the official copies are (`../specifications-ITS-JSON`?) and how they differ from ours.
5. Atomik: persistence (GORM mappings, indexes) of the new properties, only if Atomik stores them.
