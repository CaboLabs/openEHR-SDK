# OPT diff: GAP of `complete()` and refactor plan

**Status:** proposal, no code changed. Companion of [opt_diff_rules.md](opt_diff_rules.md) (the rules are the
reference; this document says what `complete()` and the diff must change to follow them).
**Scope:** openEHR-SDK `OperationalTemplate.complete()` and `rm_1_0_2/Model.groovy`, as the baseline of the OPT diff.

## 1. Review of the rules (are they complete?)

The rules were re-read end to end. Findings:

| # | Finding | Action |
|---|---|---|
| R1 | Table 6.4, first row, said "exactly one unmatched node on each side" for every RM type change, which contradicts section 7 rule 2 (only id-less nodes). A member with a `node_id` pairs by `node_id`, so `ELEMENT` -> `CLUSTER` with the same `node_id` is a type change. | **Fixed** in the rules doc (the row has both cases). |
| R2 | Row "Text, description or ontology labels changed only" used the old vocabulary. | **Fixed**: "Only `lexical` changes". |
| R3 | No rule for `C_CODE_PHRASE` terminology changes (`terminologyId`, `terminologyRef`, `reference`), `DV_ORDINAL` symbol changes, assumed values. The SDK reports some of them as plain field changes without a level. | Added to open point 4 (rules not yet written). |
| R4 | 6.2 depends on what `complete()` re-creates, but nothing says what happens when the RM type is not in the SDK RM model (`Model.isMandatoryAttribute` returns `null`, `impliedExistence` returns `null`). | Added to open point 4. Proposal: treat the paths as not re-creatable (data `affected`, queries `potential`): the safe side. |
| R5 | Rules for `ontology` changes (term and constraint bindings, codes added or removed) are not written: kind defined, levels not. | Part of open point 4 (terminology and value-set bindings). |
| R6 | Principle 2 says both OPTs are compared **after** `complete()`. The current diff does not call it (section 3). | Gap G1 below. |

Everything else is consistent: levels (`none`/`affected`/`potential`), kinds of change, pairing rules 1-4, the
`archetype_id` rows, the 6.1-6.3 tables and the fixtures. The rules are complete for the first implementation except
open point 4, whose items are not needed to start (they need a table each before they are implemented).

## 2. What the rules require from the baseline

| ID | Requirement | Source |
|---|---|---|
| B1 | After `complete()`, every RM attribute of every constrained RM type is present, with RM existence, RM occurrences and, for containers, RM cardinality (`0..*`, ordered/unique as in the RM). | Principle 2, 6.2 |
| B2 | "No constraint" has one canonical representation: `null` (or an absent attribute / alternatives set) means open/any. The diff normalizes `null` at comparison time (intervals `(*..*)`, lists of codes `any`, patterns `any`, booleans `[true, false]`, RM enumerations the full set). Open instances are NOT materialized (decision 2026-11-20). | Principle 2, 6.3 |
| B3 | The injected nodes are marked (`fromRm`), so the diff can tell an RM default from a constraint written in the OPT. | 6.2, 6.3 ("from no constraint") |
| B4 | A path is addressable if the completed OPT defines it: the diff needs, per removed node, the list of its paths that `complete()` re-creates and those it does not. | 6.2 |
| B5 | Unknown RM types are explicit (not silently skipped). | R4 |
| B6 | `complete()` is idempotent and does not alter constraints the OPT states. | Principle 2 |
| B7 | The diff compares two OPTs completed against the same RM version; different RM versions are out of scope. | Section 8 |

## 3. Current behavior (audit of SDK 2.7.7)

- `complete()` (`OperationalTemplate.groovy:231`) adds attributes that the OPT does not have:
  1. from `Model.rm_attributes_not_in_opt`: a curated list of 41 RM types (EHR_STATUS, FOLDER, COMPOSITION, EVENT_CONTEXT,
     SECTION, OBSERVATION, EVALUATION, ADMIN_ENTRY, ACTION, INSTRUCTION, ACTIVITY, HISTORY, EVENT, ITEM_TREE, CLUSTER, ELEMENT,
     parties, ids, ...), mostly `name`, `archetype_details`, `uid`; **not** the structural attributes (`ELEMENT.value`,
     `ITEM_TREE.items`, `OBSERVATION.data`, ...);
  2. from `Model.rm_mandatory_attributes`: 14 data value types (DV_TEXT, DV_CODED_TEXT, DV_QUANTITY, DV_COUNT,
     DV_PROPORTION, DV_ORDINAL, DV_DATE_TIME, ...); "not exhaustive: types missing here are not covered".
- Every added attribute is a `C_SINGLE_ATTRIBUTE` (even for collections, see `collection_attrs`), existence `1..1` if the
  attribute is in the mandatory list, `0..1` otherwise; its object has occurrences `1..1` or `0..1` and **no cardinality**.
- An injected primitive has an empty `CPrimitive` (no range, list or pattern, so `null`, not the open expression), except
  `DV_PROPORTION.type` (the RM enumeration `[0..4]`, the only entry of `rm_enumerated_values`). `CBoolean` keeps
  `trueValid`/`falseValid` as `null`.
- It never marks the injected nodes as coming from the RM.
- `complete()` also changes what the validator and Atomik indexing see (`OptManager`, `RmValidator2`, the flat node maps);
  there is a comment about double indexing of `COMPOSITION.context`.
- The diff (`SemanticOperationalTemplateDiffAlgorithm`) does **not** call `complete()`. It substitutes by itself an RM
  "implied existence" for added attributes (`impliedExistence`, only for types in `rm_mandatory_attributes`) and
  `intervalStr(null) == '(*..*)'` for intervals only; for lists, patterns and booleans `null` stays `null`.
- `attribute_removed` is reported even when the new OPT leaves the RM defaults (documented tech debt in the code).

## 4. GAP

| Gap | Rule | Current | Needed | Impact if not done |
|---|---|---|---|---|
| G1 | Principle 2, B1 | The diff compares the OPTs as parsed; it does not call `complete()`. Whether the CLI and Atomik pass completed OPTs is to verify. | The diff works on completed copies and does not mutate its inputs. | A removed constraint that equals the RM default is reported as a removal, and an added one as a new constraint (tech debt in the code). |
| G2 | B1 | `rm_attributes_not_in_opt` covers 41 types but not the structural attributes; `rm_mandatory_attributes` covers 14 types. Other RM types are unknown. | The attributes of every RM type used by templates, with RM existence, occurrences and cardinality, from one source (the RM model), not two curated tables. | `impliedExistence` returns `null` for unknown types: removals and additions are reported without the RM baseline. |
| G3 | B1 | Added attributes are always single, no cardinality. | Container attributes are `C_MULTIPLE_ATTRIBUTE` with the RM cardinality. | 6.1 cardinality rows cannot compare a constraint against the RM default. |
| G4 | B2 | Injected primitives have `null` constraints. Only `DV_PROPORTION.type` has the enumeration. | `null` stays the canonical "open". The diff gets one normalizer that treats `null` as open for intervals, lists, patterns, booleans and RM enumerations (today only `intervalStr(null)`). | "From no constraint" rows of 6.3 depend on ad hoc checks (`checkConstraintOverRmDefault`) and lists/patterns/booleans keep `null`. |
| G5 | B3 | No marker. | `fromRm = true` on injected attributes and nodes (and their children). | The diff cannot tell an RM default from an OPT constraint (6.2 first vs second row). |
| G6 | B4 | Not available. | A function "paths below node N that `complete()` re-creates", or the completed tree itself. | 6.2 rows cannot be decided; today `attribute_removed` is always reported. |
| G7 | B5 | Unknown types are silently skipped (`Model.rm_attributes_not_in_opt[obn.rmTypeName]` is `null`). | An explicit result (log or marker), and the diff uses the rule R4. | Silent wrong levels. |
| G8 | B6 | Idempotence is not tested; `isCompleted` is a flag. | Idempotence and "does not change stated constraints" tests. | Regressions in validator and indexing when changing `complete()`. |
| G9 | Section 8 | Other RM versions use `rm_1_0_2` only. | Out of scope. | None now. |

Gaps in the diff itself (not in `complete()`), for completeness; they are the work of the implementation plan in section 6:
pairing (rules 1-4 of section 7, archetype root pairing by model/package/class/concept, no positional pairing), the item model
(direction + data + query levels + kind of change + explanation instead of `certain` and the categories of `BreakingChange`),
all rows of 6.1-6.4, and the report in the CLI and Atomik.

## 5. Decisions

| # | Decision | Status |
|---|---|---|
| D1 | `complete()` itself or a separate baseline function. | **Open, decided by benchmark in phase 2.** With the lazy RM fallback (section 9) the diff needs no completed copy, so `complete()` only gains `fromRm` and container cardinality; the validator/indexing risk is small. Start separate (safe), merge if validation/indexing time is not affected. |
| D2 | Source and method for the RM data. | **Closed (2026-11-24).** Version: hybrid (demographic package from RM 1.0.4, the rest RM 1.0.2; EVENT-family generics resolved to their bound, rm_bmm_version_gap.md 3.3). Method: an offline generator script **in Groovy** (not Python) reads the BMM JSONs (branches `release/*`, see 9.3) and writes the mapped data into `Model.groovy`, with the errata overlay (`null_flavour`, `DV_MULTIMEDIA` octets, ...). It adds what `Model` lacks: existence for every attribute, cardinality, ancestors, abstract flags. Curated maps stay hand-written (`rm_attributes_not_in_opt`, lookup-only list, open structural attributes). If a generated static map is too big, split it in two or move it to separate util classes. Run by hand on RM version changes, not part of the build. Migration scope in [rm_bmm_version_gap.md](rm_bmm_version_gap.md). |
| D3 | Abstract RM types (`ELEMENT.value: DATA_VALUE`) and structural attributes absent from the OPT. | **Closed, revised 2026-11-24:** no type alternatives are added (absent alternatives = `null` = `any`). `complete()` checks the structural attributes and, if one is missing, materializes the attribute node with no children (see 9.4). Diff detects `any` <-> something. Runtime subtype check is the validator's job (after the JSON Schema validation in Atomik). |
| D4 | Marker for injected nodes. | **Closed:** `fromRm` on injected attributes and nodes, in `complete()` too. |
| D5 | Types or attributes not in the RM model. | **Revised:** there are none in a typed model; if found (invalid OPT or RM version mismatch) the diff reports an error and does not guess. Rule 6.2 follows the materialized set of `complete()` (9.4), closed. |

## 6. Plan (no code until the decisions above and the rules are agreed)

| Phase | Work | Output | Gate |
|---|---|---|---|
| 0 | Decisions D1-D5, open point 4 tables (at least `DV_QUANTITY`, bindings) | rules doc updated | Pablo agrees |
| 1 | Characterization tests of today's `complete()`: golden output (added attributes, existence, occurrences, constraints) for COMPOSITION, OBSERVATION, ELEMENT, DV_QUANTITY, DV_PROPORTION, DV_CODED_TEXT, EVENT_CONTEXT, unknown types | Spock specs, green on SDK 2.7.7 | no code change |
| 2 | Baseline: marker (G5), most open expressions (G4), cardinality (G3), RM data source (G2), unknown types (G7) in the function chosen in D1; idempotence tests (G8) | `complete()` or baseline function | phase 1 specs still pass (if D1 = (a)) or validator and indexing specs unchanged (if D1 = (b)) |
| 3 | Diff on completed copies (G1), "paths re-created below a node" (G6) | diff uses the baseline | fixtures of table_6_1, 6_2, 6_3 (RM default cases) |
| 4 | New item model and the rules of 6.1-6.4, pairing of section 7, explanations | SDK diff, Spock tests per fixture pair (the fixtures in `src/test/resources/opts/diff_rules/` are the test cases) | all fixture pairs, per the test matrix (section 9 of the rules) |
| 5 | SDK version bump (Pablo releases), CLI report (text and JSON, JSON field names to define) | CLI release | CLI tests of the report |
| 6 | Atomik: consume the report, remove duplicated detectors, re-base the 5 failing planner specs | Atomik release | planner specs |

Phases 1-2 are independent of the diff and are the "refactor `complete()`" step asked for; phases 3-6 follow the
decisions of section 6 of the rules (report format agreed, field names pending).

## 7. Risks

- Changing `complete()` (D1 = (a)) changes validation and indexing results in Atomik. Mitigation: D1 = (b) first, characterization tests, run the Atomik suites.
- The RM data of the BMM files may not carry existence or cardinality in the form needed (D2). Mitigation: audit in phase 1; fall back to extending the curated maps.
- Versions to track: SDK (diff and baseline), CLI (report), Atomik (planner). The API break is accepted (section 11, point 5 of the rules).

## 8. Session handoff (2026-11-20)

1. Closed: D3, D4, B2 (null = open, diff normalizes), `complete()` gets `fromRm`. Open: D1 (benchmark), D2 (generate from BMM, confirm), D5 (confirm), open point 4 tables (`DV_QUANTITY` first).
2. Nothing is committed (same untracked files as before; this doc updated).
3. Atomik: `sdk_rm_primitive_types`, commit 87b3f76e not pushed, 5 known failing planner specs.

## 9. Findings of 2026-11-20

### 9.1 How Atomik uses the completed OPT (verified in EHRServerNG)
- All OPTs in `OptManager` are completed: `BootStrap.groovy:1620` (`complete_opts = true`, comment: for the query builder to show RM nodes not in the OPT like `null_flavour`), `OptManager.loadAll/load/getOpt` default `complete = true`, all Atomik call sites pass `true`.
- The Query Builder reads the in-memory completed OPT, not the DB indexes: `QueryController.getArchetypePaths2` serializes `opt.findRoot(archetypeId)` (JsonSerializer), `getTemplateJson` the whole OPT. `getPossibleValues` uses `optMan.getNodes(archetypeId, path)` (hash lookup in `arch.nodes[path]`) and `getNodesByDataPath` (linear scan).
- The DB path indexes (`OperationalTemplateIndexerService`) are a separate consumer used for query execution; for RM attributes not in the OPT it reads `Model.rm_attributes_not_in_opt` directly (skips attributes the OPT defines, to avoid double indexing).
- Consequence: materialized RM nodes in the completed OPT are the builder's addressable-path catalog. Rule 6.2 "re-created by `complete()`" = "addressable in the completed OPT"; the rule follows the `complete()` output (or a future `materialize()`), not the raw RM model.
- `complete()` deliberately does NOT materialize every RM attribute: rarely used attributes are skipped (memory, comparison cost; the OPTs are cached completed).

### 9.2 Design agreed (diff side)
- The diff does not need completed copies. It pairs attributes by the union of names present in either OPT; absent on both sides = nothing to compare (same result as completing both); present on one side = the counterpart comes from an RM model lookup (existence, occurrences, cardinality; empty alternatives = `any`). Nodes marked `fromRm` in the inputs are treated as absent, so `diff(complete(A), complete(B)) == diff(A, B)` (invariance test, replaces G1/G8 intent).
- RM model in memory: read-only, type graph resolved lazily per path (one entry point per top LOCATABLE: COMPOSITION, EHR_STATUS, FOLDER, ACTOR (covers PERSON, ORGANISATION, GROUP, AGENT; no extra attributes), ROLE, PARTY_RELATIONSHIP). Horizontal links are OBJECT_REF, so no cross-tree expansion. Descent stops at abstract-typed attributes (D3).
- Possible later refactor (not for the diff): `getNodes`/`materialize()` mixing hot materialized nodes and lookups for rare ones, to cut memory in `OptManager`; Atomik indexer and `complete()` would read the same RM model instead of the curated lists. Decide with measurements.

### 9.3 BMM audit (D2) - corrected 2026-11-23
BMM files per RM version live on the `release/*` branches of `../specifications-RM` (not in the tags, not only on master): `origin/release/1.0.2` -> `computable/BMM/openehr_rm_1.0.2.bmm.json` (161 classes, self-contained, base types inlined), `release/1.0.4` -> `openehr_rm_1.0.4.bmm.json` (146, includes `openehr_base_1.1.0`), `release/1.1.0` -> `openehr_rm_1.1.0.bmm.json` (155, includes `openehr_base_1.2.0`). `master` is RM 1.2.0 in development (106 commits after 1.1.0: SPECRM-124, -87, -107 ...), NOT 1.1.0. Base BMMs: `../specifications-BASE` (`release/1.1.0` has `openehr_base_1.1.0.bmm.json`; check which base version each RM release includes). Copies extracted for the audit: scratchpad `bmm/`.
- Per property: `_type`, `type`/`type_def`, `is_mandatory` (present = 1..1, absent = 0..1), `cardinality {lower, upper|upper_unbounded}` for containers; per class `ancestors`, `is_abstract`, `generic_parameter_defs`. Covers existence, cardinality, types and inheritance.
- Cross-check of the SDK curated maps against the **1.0.2 BMM only** (the version the SDK implements): `Model.rm_mandatory_attributes` (14 types) matches `is_mandatory` exactly (inherited properties included). `rm_attributes_not_in_opt`: 40 of 41 types are in the 1.0.2 BMM (base types inlined, so no base BMM is needed) and every attribute name exists in it. The only type not in the 1.0.2 BMM is `TEMPLATE_ID`, an SDK addition (it comes from the base model, not from RM 1.0.2).
- Differences with later RM versions (1.0.4, 1.1.0) are scope for the RM migration, not for the diff work: see [rm_bmm_version_gap.md](rm_bmm_version_gap.md). `master` of `../specifications-RM` is RM 1.2.0 in development and is not used.
- The SDK and Atomik implement RM 1.0.2. The diff work proceeds with RM 1.0.2 as-is.
- "Unknown type": there is none in a typed model. An OPT type or attribute missing from the RM model of the version is an error (invalid OPT or RM version mismatch), reported, never guessed. D5 revised accordingly.
- Recommendation D2: generate the RM model from the 1.0.2 BMM as-is (one source for `complete()`, the diff, later the Atomik indexer), parameterized by RM version so a later migration only regenerates it. The BMM was compared with the spec class tables of the same release (0 differences, see rm_bmm_version_gap.md section 1): it is a faithful copy of the spec, but the 1.0.2 spec has errata corrected in 1.0.4 (e.g. `ELEMENT.null_flavor` vs `null_flavour`), so the generator needs an overlay for them.

### 9.4 Materialization set of `complete()` (Pablo, 2026-11-24)
- `Model.full` is a queryable RM typing schema (attribute -> type or list of types). Users: `complete()` (`OperationalTemplate.groovy:561`, mandatory attribute type), `FlatMapParser`, SDK tests. Atomik and openehr-toolkit (SDK v2.2.9) do not use it; both use `Model.rm_attributes_not_in_opt` (the Atomik DB indexer and the toolkit `OperationalTemplateIndexerService`).
- No formal criterion exists for `rm_attributes_not_in_opt`; the current `Model` maps are the criterion (attributes sometimes in the OPT, never in the OPT).
- EHR model, lookup only (not materialized, rarely archetyped): `DV_TEXT` hyperlink, formatting, mappings, encoding, language; `DV_ORDERED` normal_status, normal_range, other_reference_ranges; `DV_QUANTIFIED` magnitude_status, accuracy; `DV_AMOUNT` accuracy_is_percent; `DV_ENCAPSULATED` charset, language; `DV_MULTIMEDIA` thumbnail; `HISTORY.summary`; `CARE_ENTRY.guideline_id`; `INSTRUCTION.wf_definition`; `INSTRUCTION_DETAILS.wf_details`. Superclass attributes apply to all subclasses (expand with BMM ancestors).
- EHR model, everything else is materialized by `complete()` (includes ENTRY subject, provider, language, encoding, other_participations, not in `rm_attributes_not_in_opt` today).
- Demographic model: all attributes materialized (small model, no extra complexity).
- Structural attributes (ELEMENT.value, *.items, HISTORY.events, EVENT.data, OBSERVATION.data, COMPOSITION.content, ...): practically always in the OPT (COMPOSITION without content is a border case). `complete()` checks them and, if missing, materializes the attribute node with no children (open, B2). For ELEMENT.value: the attribute, not the type alternatives.
- Consequence: the Atomik indexer and the toolkit indexer must read the same set (follow-up, no code now). Scope of consumers: Atomik, SDK, CLI, later toolkit. Amplify is out of scope.

### 9.5 LOCATABLE.name constraint (Pablo, 2026-11-24)
- The `name` of a LOCATABLE is never unconstrained. Without an explicit constraint (attribute/object node in the OPT) it defaults to the text of the node's `node_id` in the OPT `term_definitions`: a `DV_TEXT` with that fixed value. The source is the OPT, never the RM. `complete()` materializes it as a constraint (marker `fromRm`; the source of the injected node is the OPT terms, to consider when defining the marker).
- The diff compares both sides in the materialized form. Explicit -> fallback and fallback -> explicit are the same case: the constraint changes (for example `DV_CODED_TEXT` -> `DV_TEXT` with a fixed value). It is `changed`, data `potential`, queries `potential` (rule 6.2). Removing a constraint is not narrowing by itself; this case differs because the fallback is automatic.
- Term text of the `node_id` changed with no explicit name constraint: the materialized fixed value changes, so it is a value change of that constraint (6.3 and ontology rules), also potential on data and queries. Row written in rules 6.2 (Pablo asked to write it, to review later).
