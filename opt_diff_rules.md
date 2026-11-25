# OPT semantic diff: rules

**Status:** agreed design, not yet implemented (the code in SDK 2.7.7 follows an earlier draft, see
[Differences with SDK 2.7.7](#differences-with-sdk-277)).
**Scope:** openEHR AOM / ADL 1.4 operational templates (OPT). ADL2 / AOM2 is out of scope.
Companion document: [aom14_attribute_existence_vs_occurrences.md](aom14_attribute_existence_vs_occurrences.md)
(meaning of existence, occurrences and cardinality).

## 1. Purpose and layering

The SDK compares two OPTs and classifies every difference. It works **only on the OPTs**: it is
deterministic, knows nothing about stored data, stored queries or migration plans, and reports as
much detail as possible about each difference.

Consumers (for example the data migration planner of Atomik, whose repository is EHRServerNG) take the report and add what
only they know: the stored data, the stored queries, and the migration options. They **must not
re-implement diff rules**. A consumer resolves the items the SDK reports as *potential* by checking
its data and queries, and builds the migration plan from the report.

## 2. Principles

1. **Open world.** A constraint that an OPT does not state falls back to what the reference model
   (RM) allows. Removing a constraint, or replacing it by `any` (`matches {*}`, or no child
   constraints), widens it to the RM rules. Nothing is "forbidden because it is not mentioned".
2. **The completed OPT is the baseline.** Both OPTs are compared after `complete()`, which fills the
   gaps between an OPT and the RM (RM-mandatory attributes, RM default existence and occurrences,
   RM value types, ...). "No constraint" is therefore shown as the most open expression of the
   constraint type: `(*..*)`, `any`, the full enumeration, `[true, false]`. The exception is the type
   alternatives of an attribute: an attribute with no constraint on its type has `null` (no
   alternatives) and means "any type the RM allows" (for example `ELEMENT.value`). The diff
   normalizes `null` to open; it never materializes the open alternatives.
   *Known gap:* RM invariants implemented only in code are not part of the completed OPT.
3. **Breaking means narrowing.** A change is a breaking change only if the new constraint is
   narrower than the old one on the same node (data valid before can be invalid now). Widening is
   never breaking.
4. **Three questions per diff item** (see section 5): what happened to the constraint (a), what is the
   impact on data (b), what is the impact on queries (c).
5. **"Data loss" is not a property of the diff.** Deleting data is a decision of a migration step.
   The diff only states facts such as "this path is no longer addressable from the OPT".

## 3. Terminology

| Term | Meaning |
|---|---|
| `C_ATTRIBUTE` | Constraint on an RM attribute. Has `existence`. `C_SINGLE_ATTRIBUTE` or `C_MULTIPLE_ATTRIBUTE` (adds `cardinality`). |
| `C_OBJECT` | Constraint on the value(s) of an attribute: `C_COMPLEX_OBJECT`, `C_PRIMITIVE_OBJECT`, `C_DOMAIN_TYPE`, slots, ... Has `occurrences`, `rm_type_name` and, sometimes, a `node_id`. |
| Template root | The top-level `LOCATABLE` of the template (`COMPOSITION`, `EHR_STATUS`, `FOLDER`, `PERSON`, `ROLE`, ...). |
| Archetype root | Any node whose `archetype_node_id` is an archetype id (`openEHR-EHR-OBSERVATION.x.v1`), including the template root. |
| Removed constraint node | The topmost `C_OBJECT` or `C_ATTRIBUTE` that is in the old OPT and not in the new one. Everything below it is gone as well, but only the topmost node is reported. |
| Added constraint node | Same, in the new OPT. Only the topmost node can be mandatory. |
| Identity of a constraint node | **(path, RM type).** `node_id` is part of the path only when it exists. Many `C_OBJECT`s have an empty `node_id`: leaf value objects such as `ELEMENT.value` (`C_COMPLEX_OBJECT DV_PROPORTION`) and its `C_PRIMITIVE_OBJECT` children. |
| Addressable | A path is addressable from an OPT if the (completed) OPT defines it. Queries and indexes use the paths an OPT defines, not every path the RM could define. |

## 4. Semantics of existence, occurrences and cardinality

A null or absent value counts as 0 occurrences of every child definition.

| Attribute kind | Existence | Children | Occurrences | Cardinality |
|---|---|---|---|---|
| Single | Whether the value is present (null vs not). | Alternatives for one value. | How many times that alternative occurs, 0 or 1. | n/a |
| Multiple | Whether the container exists (null vs not). | Members. | How many times each member definition occurs. | How many items the container holds (`[]` is size 0). |

Derived values, used to compare two versions of a constraint instead of the raw intervals:

- **Single attribute.** The value is *required* if existence min >= 1 or any alternative has
  occurrences min >= 1. It is *prohibited* if existence max = 0 or every alternative has occurrences
  max = 0.
- **Multiple attribute.** The *effective size range* is
  `[max(card.min, sum(child occ.min)) .. min(card.max, sum(child occ.max))]`. The container is
  *required* if existence min >= 1 or the effective min >= 1.
- **Inconsistent definition:** `sum(child occ.max) < card.min` or `sum(child occ.min) > card.max`.
  This is an error of the OPT itself, not a diff result. It belongs to OPT parsing (warning),
  to `RmValidator2` (data cannot be validated against an inconsistent definition) and, when the
  diff reads such an OPT, to a note on the OPT, never to a diff category.

## 5. Model of a diff item

Every item carries, in addition to path, node names, old and new values and the RM type:

**(a) Direction** of the constraint change: `widened`, `narrowed`, `changed` (neither: RM type,
`archetype_id`, `node_id`), `extended` or `same`. `extended` is for an added addressable node that does not
change an existing constraint (an added alternative with occurrences `0..1`, an added optional member); its
impact comes from the added node's own constraints, compared with the RM default (section 6.3).
Direction applies to constraints that exist in both OPTs, so an added node has none.

**(b) Data impact** and **(c) Query impact**, each with one of these levels:

| Level | Meaning | Resolved by |
|---|---|---|
| `none` | Nothing changes for this dimension. | |
| `affected` | The OPT structure and the item alone show it is relevant. A warning: nothing to verify, nothing breaks. | Nobody. The migration plan takes it into account. |
| `potential` | Breaks only if the data, or a query, actually uses the changed thing. It needs verification. | The consumer, against its data or query registry. |

There is no `confirmed` level. Whatever breaks is `potential`, because a simple check of the data (is there data at this path?) or of the queries (does a query use this path or value?) can always turn it into "no break", and then no migration step is needed. Even an RM type change is `potential`.

Also reported with the item: the archetype (root) the node belongs to, the kind of change
(below), and, for nodes that cannot be paired, an explanation (section 7).

**Compatibility metadata (optional).** Some items carry extra metadata for the migration plan that does not
change direction, kind or levels. Today only the removal of a unit of a `C_DV_QUANTITY` (6.5, Q5): `compatible`,
`incompatible` or `unknown`, with the same-property candidates and conversion factors. Field names are defined at
implementation.

**Kind of change.** Every item below a node also says what kind of thing changed. The terms say what we
know about the change, not what it means: the word *semantic* is used only when we know that the meaning
changed, and never as a classification.

| Kind | What changed |
|---|---|
| `lexical` | Only text: the text, description or comment of a term or constraint definition. |
| `ontology` | The items that live in the `ontology` section of an ADL archetype (ADL 1.4 section "Ontology Section"): `term_definitions`, `constraint_definitions`, `term_bindings`, `constraint_bindings`, terminologies available. OPTs have these items without the `ontology` section. A change of text only is `lexical`; codes added or removed, and bindings, are `ontology`. |
| `structural` | The structure of the constraints: nodes and attributes added or removed, existence, occurrences, cardinality, RM types, `node_id`, `archetype_id`. |
| `data constraint` | The constraints at the leaf level (`DV_xxx` and native types: String, Integer, ...): ranges, patterns, lists, units. |

The kind does not change the levels of the item. It tells the consumer how much work an
`archetype_id` change needs: an archetype root whose version changed with only `lexical` changes below
needs reindexing but no change in the data constraints, and one with `structural` or `data constraint`
changes below needs the plan to handle those items too.

## 6. Rules

### 6.1 Existence, occurrences, cardinality

Compare the derived values of section 4, not the raw intervals.

| Change | Direction | Data | Queries |
|---|---|---|---|
| Value or container becomes required (existence min up, alternative or member occurrences min up, effective cardinality min up) | narrowed | potential (only if there is data at the path without the value) | affected (e.g. a `NOT EXISTS` filter on the node can no longer match data created with the new OPT, and an `EXISTS` filter on it becomes redundant) |
| Cardinality min raised but already covered by the children's minimums | same | not reported | not reported |
| Occurrences max lowered, cardinality max lowered | narrowed | potential | potential (a query may use a member or index beyond the new max, or a count above it) |
| `unique` added | narrowed | potential | potential |
| `ordered` added | changed | none | none |
| Node or attribute becomes prohibited (`0..0`), was required or optional | narrowed | potential (only if there is data at the path; the report also says whether it was required) | potential |
| Constraint widened (existence, occurrences, cardinality) | widened | none | none |
| Added mandatory constraint node (topmost only) | narrowed | potential (only if there is data of the parent without the node) | affected |
| Added optional constraint node | extended | none | none |

A prohibited node is different from a removed one: its data is now invalid, not just unaddressable,
so it cannot stay where it is.

### 6.2 Removal and widening to the RM

A removed constraint node, or a constraint replaced by `any`, falls back to the RM. Direction is
`widened`: no old data becomes invalid, so nothing breaks. The one exception is the `name` of a
LOCATABLE (see the last row): it is never unconstrained, its fallback is a fixed value from the OPT
terms and not the RM, so going to or from the fallback is a change of constraint. The data impact depends on whether the data stays addressable by path. Occurrences of the removed node (`0..1` or `1..1`) do not matter: the direction decides.

| Change | Data | Queries |
|---|---|---|
| Removed constraint node whose paths below it do not exist in the completed OPT afterwards (it contains a `node_id`, an archetype slot or an archetype root: no RM or term source can rebuild them), optional or mandatory | affected (the data stays valid under the RM but is no longer addressable by path; the report also says whether the node was required) | potential (the paths are no longer addressable; verify the queries that use them) |
| Removed constraint node whose attribute stays in the completed OPT (an attribute materialized by `complete()` or found by the RM lookup, kept as an open attribute with `null` alternatives; id-less leaf value object whose RM type constraint is lost) | none (the path is still addressable) | affected (path still exists, the RM type or value restriction is gone) |
| Constraint replaced by `any` | none | affected |
| Alternative with occurrences `0..0` removed (subtype or type exclusion lifted) | none | none |
| `name` constraint of a LOCATABLE node changes between its explicit form and the fallback, in either direction (explicit -> none: for example a `DV_CODED_TEXT` with a list of allowed names removed; none -> explicit: a coded text or other name constraint added). There is never "no name constraint": without an explicit one the name is constrained to the text of the node's `node_id` in the `term_definitions` of the OPT, a `DV_TEXT` with that fixed value (`complete()` materializes it as a constraint, marked `fromRm`). Both sides are compared in that materialized form. `changed`, not `widened` or `narrowed` by the form alone: it is a different constraint (for example `DV_CODED_TEXT` -> `DV_TEXT` with a fixed value) | potential (legacy data whose name does not satisfy the new constraint) | potential (queries that use an option of the old constraint no longer match) |
| Text of the node's `node_id` in the `term_definitions` changed while the LOCATABLE has no explicit `name` constraint on either side. The materialized fallback is a fixed `DV_TEXT` with that text, so its value changes. Also reported as a change of the term text (`lexical` or `ontology`, see section 5) | potential (legacy data with the old name no longer satisfies the fixed value) | potential (queries that filter by the old name no longer match) |

The facts "path no longer addressable" and "valid under the RM" are reported on the item. What to do
with the data (keep, move, delete) is decided by the migration plan, not here.

### 6.3 Value constraints (leaf objects)

| Change | Direction | Data | Queries |
|---|---|---|---|
| Range, pattern, precision or list narrowed, including from no constraint (the RM default, shown as the most open expression). Examples: `DV_COUNT.magnitude (*..*) -> [0..*)`, `C_DV_QUANTITY_ITEM` magnitude `(*..*) -> [0.0..*)`, `DV_PROPORTION.type [0..4] -> [2]`, `DV_DATE_TIME.value any -> yyyy-mm-ddThh:mm:ss`, `precision [0..2] -> [1..1]` | narrowed | potential | potential (filters built with the old range or value) |
| Value dropped from a list (codes, strings, units, ordinal items, enumeration). Example: code list `[433, 434] -> [433]` | narrowed | potential | potential (a query may filter on the value) |
| Range, pattern or list widened, value added to a list. Examples: `magnitude [0..100] -> [0..200]`, code list `[433] -> [433, 434]`, `yyyy-mm-ddThh:mm:ss -> yyyy-mm-dd` | widened | none | none |

### 6.4 Type and identity changes

| Change | Direction | Data | Queries |
|---|---|---|---|
| RM type changed on a paired node: either the same `node_id` with a different RM type (member of a multiple attribute, e.g. `ELEMENT` -> `CLUSTER` at0002), or an id-less alternative with exactly one unmatched node on each side under the same attribute | changed | potential (only if data exists at the path; its meaning changes, e.g. count vs quantity) | potential (only if a query uses the path) |
| Nodes that cannot be paired: more than one unmatched id-less node on a side under the same attribute, or any change of the `node_id` of a node | two independent items: the old node removed (6.2) and the new node added (6.1) | see 6.2 and 6.1 | see 6.2 and 6.1 |
| Alternative added, occurrences `0..1`, matching anything (`{*}`), over `any` or beside other alternatives | extended | none | none |
| Alternative added, occurrences `0..1`, with real constraints in its subtree | extended | potential (old data of that type may fail, as in 6.3) | potential |
| Alternative added with occurrences min >= 1, or alternative occurrences min raised (e.g. `0..1` -> `1..1`) | narrowed | potential (other types become invalid) | potential |
| Alternative occurrences min lowered (e.g. `1..1` -> `0..1`) | widened | none | none |
| Alternative with occurrences `0..0` added (type or subtype exclusion, e.g. `DV_CODED_TEXT` beside `DV_TEXT`) | narrowed | potential | potential |
| Alternative or member removed, `0..0` alternative removed | widened | see 6.2 | see 6.2 |
| Member of a multiple attribute added | extended (optional) or narrowed (mandatory) | see 6.1 | see 6.1 |
| Only the **version** part of the `archetype_id` of an archetype root changed (`...problem_diagnosis.v1` -> `...problem_diagnosis.v2`, upgrade or downgrade) | changed | potential (the old data is indexed under the previous archetype id: reindexing, and the stored `archetype_node_id` update, are migration plan actions) | potential (queries may use the previous archetype id in projections, conditions or filters, unless they allow any archetype version) |
| The **specialisation** part of the `archetype_id` of an archetype root changed or was added or removed, with the same model, package, class and concept (`...device.v1` -> `...device-pacemaker.v1`), with or without a version change | changed | potential (same reasons as a version change) | potential (same reasons) |
| Any other part of the `archetype_id` changed (model, package, RM class, concept: `...OBSERVATION.blood_pressure.v2` -> `...OBSERVATION.pulse.v1`, `...EVALUATION.problem_diagnosis.v1`) | two independent items: the old archetype root removed (6.2) and the new one added (6.1) | see 6.2 and 6.1 | see 6.2 and 6.1 |
| Only `lexical` changes (text or description of term or constraint definitions) | changed | none | none |

Alternatives never list the allowed types (open world). A value is validated against the alternative of
its exact RM type if there is one (`0..0` means invalid, nothing else is checked), otherwise against the
alternative of a supertype (the supertype part of its structure), otherwise only against the RM rules, in
which case it is valid but not addressable by path (Atomik must not index it). Occurrences decide how
closed the set is: a sole alternative with min >= 1 excludes every other type.

**Structure of an archetype id:** `<rm_model>-<rm_package>-<rm_class>.<concept>[-<specialisation>].<version>`
(e.g. `openEHR-EHR-OBSERVATION.blood_pressure.v2`: model `openEHR`, package `EHR`, class `OBSERVATION`,
concept `blood_pressure`, version `v2`). With the same model, package, class and concept, a different
version or specialisation is an update of the same archetype: the roots pair and the item is
`changed`. If anything else differs (model, package, class or concept), it is a different archetype,
with a different meaning: removal plus addition, like a different `node_id`. Both cases are valid and
independent.

A version-only change of the `archetype_id` is reported as `changed` together with the kinds of change found below it. Whether the stored reference is updated in place, or the data below is
also migrated, is a decision of the migration plan.

**A `node_id` does not "change".** A `node_id` carries meaning: it is linked to a term (text and
description) in the archetype terminology. A different `node_id` is therefore a different constraint:
the old one is removed and a new one is added, as two independent diff items, each classified by its
own rules (6.2 and 6.1) and each with its own explanation. The diff does not infer that they are the
same concept. The migration plan asks the question (rename, move, remove and add).

*Specialisation.* Archetype specialisation (UK spelling `specialisation` in the specs) refines nodes of
a parent archetype. A specialised archetype has the id of its parent's concept plus `-<specialisation>`
(`device` -> `device-pacemaker`) and gives each refined node a specialised code with the same root and
dot extensions, e.g. `at0010` -> `at0010.2` (ADL 1.4 section on codes; AOM 1.4 ontology package:
`at0001.0.1`, `at0001.1.1`, new codes `at0.0.1`). Two cases:
- Both OPTs use the same archetype (the usual case): the specialised codes are already their
  `node_id`s, and a different `node_id` is a removal plus an addition.
- One OPT uses the parent archetype and the other the specialised one (a border case, but now
  reachable through the `archetype_id` row of 6.4): the roots pair, and the nodes inside need a pairing
  rule based on the shared root of the codes (`at0010` ~ `at0010.2`), so a refined node is paired as
  the same node, not as removed plus added. The pairing rule and its table (direction, data and query
  levels, the stored `archetype_node_id` update in the migration plan) are **open, to define** (section
  11). The OPT diff, the report and the migration plan must take this into account.

**Explanations.** Each item gets its own explanation, written for the user and without internals of
the SDK (no mention of `complete()`). For a removed node: "the path `<path>` no longer exists in the
new template; data stored at that path is still valid under the reference model but cannot be
addressed by path or by queries". For an added node: "a new constraint `<path>` was added; <optional or
mandatory>". For a group of unpaired id-less nodes: "N alternatives in the old version, M in the new
one, none matches by path and RM type".

### 6.5 Quantity constraints (`C_DV_QUANTITY`)

`C_DV_QUANTITY` (openEHR AOM profile) has a `property` (a code of the openEHR `property` terminology, e.g.
`122|length|`), a list of items and an assumed value (open point 4). Each item has `units`, an optional
`magnitude` interval and an optional `precision` interval. Items are paired by `units`; their order is not
relevant. `property` is not an RM attribute: it is a virtual attribute of the constraint model that restricts
the `units` String (which cannot carry a constraint itself) to the units of one physical property.

**"No constraint" (any).** A `DV_QUANTITY` without `C_DV_QUANTITY` constraints (a plain complex object), a
`C_DV_QUANTITY` with an empty list and no `property`, and a `null` list are the same state: any units, any
magnitude, any precision. Per item, a `null` magnitude or precision is open as well (6.3).

| # | Change | Direction | Data | Queries |
|---|---|---|---|---|
| Q1 | Unit item added to a non-empty list | widened | none | none |
| Q2 | Unit item removed, the list stays non-empty | narrowed | potential | potential (filters on that unit) |
| Q3 | From any (no items) to a non-empty list | narrowed | potential | potential |
| Q4 | From a non-empty list to any (all items removed, or the constraint removed) | widened | none | none |
| Q5 | An old unit is not in the new list and a new unit is (`mm[Hg]` -> `kPa`, `m` -> `cm`) | not a modification: a removed item (Q2) plus an added item (Q1), independent | potential (the removal) | potential (the removal) |
| Q6 | Item `magnitude` or `precision` narrowed, including from no constraint | narrowed | potential | potential |
| Q7 | Item `magnitude` or `precision` widened, including to no constraint | widened | none | none |
| Q8 | Inclusivity of an interval bound changed (`[0..100]` -> `(0..100]`) | narrowed or widened, by the values it admits | as Q6 / Q7 | as Q6 / Q7 |
| Q9 | Order of the items changed | same (not reported) | none | none |
| Q12a | `property` added (from none) | narrowed | potential | potential |
| Q12b | `property` changed (`length` -> `mass`) | changed | potential | potential |
| Q13 | `property` removed | widened | none | none |

Notes:

- **Q5. Two levels.** *Facts*: `units` is the key of an item and a key never "changes" (same principle as
  `node_id`). If an old unit is not in the new list, it is removed (Q2); if the new list has units the old one
  did not, they are added (Q1); there is no modification. The removal is `potential` even when the units are
  convertible (`1 m = 100 cm`): stored data carries the old unit string, which the new list does not accept.
  *Compatibility* is a separate dimension, only for units, that does not change direction, kind or levels. It
  is metadata for the migration plan, computed with `terminology/PropertyUnitData.xml` (properties, units, UCUM
  codes, conversion factors). For each removed unit:
  - `compatible`: the new list has at least one unit of the same property. The item carries the old unit, its
    property, the candidate units of the new list and the conversion factor to each (e.g. `m` -> `cm`, x100).
  - `incompatible`: the unit has a known property and the new list has no unit of that property.
  - `unknown`: the unit is not in `PropertyUnitData.xml`, or has no property there; nothing is assumed.

  In all three cases the levels are those of Q2. Explanation (compatible): "unit `<old>` is no longer accepted
  at `<path>`; units of the same physical property (`<property>`) are accepted, existing values can be converted".
  The diff reports the metadata; choosing a conversion is the job of the migration plan.
- **Q6 and Q7** apply the rows of 6.3 to the magnitude and precision of one item; the path of the item is
  `<path>[units=<u>]`. Precision `-1` ("no limit" in the RM) has no special treatment: it is a value like any other
  of the interval.
- **Q12a and Q12b.** The diff cannot expand a `property` into its units, and it cannot decide how a `property`
  and an explicit list interact, so every addition or change is `potential` and the diff does not guess. This
  holds whatever the unit list is (also when the list is non-empty in both OPTs and unchanged). Whether
  Atomik enforces `property` today is not relevant for the rule (there is a `FIXME` in `CDvQuantity.isValid`).
- **Duplicated `units` in the same list** cannot be paired: reported as removed plus added, and the plan asks a
  human (section 7).
- **RM 1.1.0 extensions (not RM 1.0.2).** `units_system` and `units_display_name` of an item belong to RM
  1.1.0, and the SDK already reads them from the OPT. Until the RM migration they are compared as informative:
  `units_display_name` is a `lexical` change and `units_system` a `changed`, both with data `none` and queries
  `none`. These are the former rows Q10 and Q11; the RM migration (see `rm_bmm_version_gap.md`) has to
  review them.
- **Q12 and the property data.** With `PropertyUnitData.xml` the diff could refine Q12 (units of the list that
  already belong to the `property` make its addition `none`; a list that contradicts the `property` is
  flagged). Not part of the first implementation: every addition or change of `property` is `potential`.

**Explanations** (user-facing). Q1: "unit `<u>` is now accepted at `<path>`". Q2: "unit `<u>` is no longer accepted
at `<path>`; data recorded with it would not be valid under the new template, and queries that filter by that
unit may stop matching". Q3: "`<path>` accepted any units and now only `<list>`". Q4: "`<path>` accepted `<list>`
and now accepts any units". Q12a: "the units at `<path>` are now restricted to units of the physical property
`<name>`; existing data with other units might not be valid". Q12b: "the physical property of the units at
`<path>` changed from `<old>` to `<new>`; existing data with units of the old property might not be valid".
Q13: "the units at `<path>` are no longer restricted to a physical property".

## 7. Pairing of constraint nodes

Archetype roots are paired by model, package, class and concept, then nodes by path (which
includes the `node_id`), then, for id-less nodes, by attribute and RM type.

**Validity precondition.** Under AOM/ADL 1.4 an id-less node is only legal when it has no sibling under
its attribute (AOM 1.4 §Node_id and Paths; ADL 1.4 cADL "Object Node Identification and Paths": node ids
are required for nodes that "would otherwise be ambiguous i.e. has sibling nodes"). Two id-less
alternatives of the same RM type under one attribute are therefore invalid and are not covered by
these rules. For valid OPTs, identity = (path, RM type) is sufficient for id-less nodes.

**Pairing rules.**
1. Archetype roots with the same model, package, class and concept are paired, whatever the
   specialisation and version (an update). Nodes with the same path and RM type (or the same `node_id`) are paired.
2. After that, if exactly one **id-less** node is left unmatched on each side under the same attribute,
   they are paired as an **RM type change** of that path (6.4: data `potential`, queries `potential`).
3. Otherwise the diff **does not pair**. "I cannot pair the node from the source to the target" is a
   valid outcome: the unmatched old nodes are reported as removed and the new ones as added (data
   `potential`, queries `potential`), with an explanation such as "N alternatives in the old version, M
   in the new one, none matches by path and RM type". The diff never guesses.
4. A migration plan turns every unpaired group into a **question with several possible answers** (map
   old X to new Y, keep, accept loss) that a human answers while reviewing the plan.

Fixtures: `src/test/resources/opts/diff_rules/section_7_pairing/` (10 pairs, `README.md` says per
case what is expected). `observe_section_7.sh` regenerates `OBSERVED.md`, the SDK 2.7.7 output for each
pair. Groups: A type changes (one unmatched on each side, beside an unchanged sibling, at different
levels) and the case with two unmatched on each side that cannot be paired; B `node_id` changed (removed plus added) vs
removed plus added; C controls that pair without doubt (quantity items by units, siblings of different
types).

What SDK 2.7.7 does today (observed, not the target):
- An RM type change of an id-less alternative (A2, A4) is reported as `rmTypeName: X -> Y` with **no**
  breaking tag; A1 and A6 add a removed child tagged confirmed breaking. Table 6.4 requires `changed`,
  data `potential`, queries `potential`.
- With two unmatched nodes on each side (A5), it pairs them by position and reports type changes. The
  rules require removed plus added.
- `node_id` change with identical siblings (B1) is reported as removed plus added nodes, as required, but
  with no explanation text.
- Quantity items are paired by units (C1) and alternatives of different types by type (C2), as
  expected.

### 7.1 Open

None at the moment. (A removed node and an added node, even with an identical subtree, are two
independent items; see section 6.4 for the explanations.)

## 8. Out of scope and tech debt

- **Different RM versions.** If the two OPTs use different RM versions, `complete()` and the RM
  defaults differ and attributes may change. An RM diff is needed. One idea: express RM v1 and RM v2
  as OPTs and use them as the base of the OPT diff. Not part of this work. Single <-> multiple
  attribute changes only appear with a different RM version, so they belong here.
- Invariants implemented only in RM code are not in the completed OPT.
- ADL2 / AOM2.
- Archetype and template metadata (the `description` section: author, purpose, keywords, use, misuse,
  copyright, lifecycle state, and its translations). It is not part of the diff for now.
- **Alternatives with RM inheritance (`RmValidator2` tech debt).** Semantics we support (the one Diego Bosca shows in
  the discussion linked below): a node with a `DV_TEXT matches {*}` alternative plus a sibling
  `DV_CODED_TEXT occurrences matches {0} matches {*}` alternative means "this node holds a `DV_TEXT` but not a
  `DV_CODED_TEXT`" (`DV_CODED_TEXT` inherits from `DV_TEXT`). `not matches` is not supported by tools and is not used.
  Alternatives are an OR, but OR is not enough with inheritance: when a data value passes one alternative, the
  validator must also look at the overlapping sibling alternatives (a supertype or subtype of the data's type). If there
  is a sibling constraint for the data's own type, that one must pass too, and if it is `0..0` the data is invalid;
  if there is none, passing the supertype constraint is enough. Diego's reasoning: with a blind OR, "any data type
  would be allowed anywhere" even when constrained to a specific one, and a `DV_CODED_TEXT` instance would not
  validate against the archetype that excludes it. Ian McNicoll's objection (a blind OR, and `DV_TEXT` could be
  re-constrained to `DV_CODED_TEXT` in a specialisation) and Thomas Beale's answer (RM change `DV_PLAIN_TEXT`; ADL2
  `occurrences matches {0}` pattern; a specialisation cannot redefine `DV_TEXT` to `DV_CODED_TEXT` while the parent
  keeps the 0-occurrence alternative) are in the thread, which has no conclusion yet:
  https://discourse.openehr.org/t/constraints-for-not-allowed/17459

  Places where subtype exclusion makes sense:
  1. `DV_TEXT` and `DV_CODED_TEXT`.
  2. `ITEM_STRUCTURE` (abstract) and its subclasses `ITEM_TREE`, `ITEM_LIST`, `ITEM_SINGLE`, `ITEM_TABLE`
     (e.g. `ITEM_STRUCTURE` plus `ITEM_TABLE` 0..0 = any structure except a table).
  3. Maybe the subclasses of `ACTOR` (`PERSON`, `ORGANISATION`, `GROUP`, `AGENT`).

  Fixtures for 1 and 2 are in `src/test/resources/opts/diff_rules/subtype_alternatives/` (3 has none yet).
  Checked 2026-11-20 with `RmValidator2` for case 1 (no source change), current results:

  | OPT alternatives | Data | Expected | `RmValidator2` today |
  |---|---|---|---|
  | `DV_TEXT` | `DV_TEXT` | valid | valid |
  | `DV_TEXT` | `DV_CODED_TEXT` | valid (a subtype passes the supertype constraint) | **invalid** ("type DV_CODED_TEXT is not allowed here"): type match is exact |
  | `DV_TEXT`, `DV_CODED_TEXT` 0..0 | `DV_TEXT` | valid | valid |
  | `DV_TEXT`, `DV_CODED_TEXT` 0..0 | `DV_CODED_TEXT` | **invalid** (subtype prohibited) | **valid**: the `0..0` alternative is counted as an allowed type |
  | `DV_CODED_TEXT`, `DV_TEXT` 0..0 | `DV_TEXT` | **invalid** (plain text prohibited) | **valid** |
  | `DV_CODED_TEXT`, `DV_TEXT` 0..0 | `DV_CODED_TEXT` | valid | valid |

  Causes: `checkAllowedType(AttributeNode, DataValue, ...)` uses the exact class name of the data against the
  alternatives' `rmTypeName`, it does not consider inheritance and it does not exclude alternatives with occurrences
  `0..0` (the comment in `validate_single_attribute` assumes `0..0` nodes never appear in the OPT, which is not true
  for OPTs that carry them). No code was changed; this is recorded as tech debt.

  The OPT diff must classify these changes too (SDK 2.7.7 reports an added `DV_CODED_TEXT` 0..0 sibling as an added
  node with no breaking tag, and a removed one as a removed node with potential breaking; the correct reading is a
  narrowing and a widening of the allowed types). The rules are in table 6.4.
- Not addressable data and indexing (Atomik, test cases pending). In open world an attribute with a single
  alternative (e.g. `value` with only `DV_QUANTITY`, occurrences `0..1`) still accepts other RM types (e.g.
  `DV_TEXT`) when validating, and the data is stored and retrieved with the whole LOCATABLE. But no OPT node
  describes that value, so it is not addressable by path. Atomik must not index data that is not addressable.
  Build test cases for this later (data of an RM-allowed type with no alternative; data of a subtype validated
  against its supertype alternative only when there is no alternative for the exact subtype).
- `attribute_removed` is currently reported with the RM defaults as the new state; the new model
  represents it as a removal that falls back to the RM (section 6.2).

## 9. Test matrix

One fixture pair per row, each a small OPT pair plus a Spock test asserting category, direction,
data and query levels, path and old/new values. The CLI repository adds tests of the report.

| Group | Cases |
|---|---|
| Existence / occurrences | single attribute: existence `0..1 -> 1..1`, `-> 0..0`, widened; alternative occurrences `0..1 -> 1..1` with sole and with several alternatives; occurrences max lowered |
| Cardinality | min raised not covered / covered by children; max lowered; `unique`, `ordered` added; inconsistent definitions in the OPT (not a diff item) |
| Added nodes | optional, mandatory, mandatory subtree (only topmost reported), under optional and under mandatory parent |
| Removed nodes | node with `node_id`, id-less leaf, subtree, alternative of a single attribute, member of a multiple attribute, RM-defined attribute, constraint replaced by `any` |
| Prohibited (`0..0`) | optional / mandatory x single / multiple x alternative / member x top-level / intermediate / leaf x object / attribute |
| Value constraints | range, pattern, precision narrowed and widened; list value dropped and added; constraint added over RM default |
| Types and identity | RM type changed (alternative, member, id-less leaf); `archetype_id` version only (up, down, with only lexical changes below, with data constraint changes below, with nodes added or removed below); `archetype_id` concept changed (with and without version change); `node_id` changed |
| Pairing | RM type change of an id-less alternative, leaf and nested node; one unmatched on each side; two unmatched on each side (cannot pair); `node_id` changed (removed plus added, even with identical content) |
| Quantity (6.5) | unit added / removed with a non-empty list; any -> list and list -> any; units text replaced; item magnitude and precision narrowed and widened, bound inclusivity; order changed; `property` added, changed, removed; duplicated `units`; no constraint written as plain object, empty list and `null` |
| Open expressions | every "no constraint" shown as the most open expression; `null` only for attribute type alternatives (open, normalized by the diff) |

## 10. Differences with SDK 2.7.7

SDK 2.7.7 (current release) follows an earlier draft. When these rules are implemented:

- A removed node is no longer *potential breaking*. It is a widening whose data impact is `affected` or
  `none` and whose query impact is `potential` or `affected` (section 6.2).
- Direction, data impact and query impact replace the single `certain` flag of `BreakingChange`.
- Existence, occurrences and cardinality are compared through the derived values (section 4).
- New: prohibited (`0..0`), `unique` / `ordered`, covered cardinality min raise, effective
  cardinality, `archetype_id` / `node_id` changes, unpaired nodes reported as removed plus added with an explanation.
- The CLI report tags and the JSON `breakingChanges` follow the new model.

Gap analysis of the baseline (`complete()`) and the implementation plan:
[opt_diff_complete_gap_plan.md](opt_diff_complete_gap_plan.md).

## 11. Open points

1. **What `complete()` materializes.** The dividing line in 6.2 (path still addressable or not) follows
   the materialized set of `complete()`: attributes not in the OPT, RM-mandatory attributes, open
   structural attributes, and everything not on the lookup-only list (plan document 9.4). The set is
   agreed. The `name` rule (fallback to the term text of the `node_id`, not to the RM) is written in 6.2.
   The generated RM data (decision D2) is implementation work, not a rule.
2. **Node pairing across specialisation** (`at0010` vs `at0010.2` when one OPT uses the parent archetype and the other the specialised one): rules and fixtures to define, see section 6.4. Not for the first implementation.
3. **Unpaired nodes.** Reporting them as removed plus added with an explanation, and the migration plan
   asking the question (section 7), must be verified with the section 7 fixtures.
4. **Rules not yet written:** archetype slots (includes and excludes changes), terminology and
   value-set bindings, `DV_QUANTITY` assumed value (property, units and precision are in 6.5), `DV_ORDINAL` symbols,
   `DV_DURATION` and date patterns, internal references (`use_node`), `C_CODE_PHRASE` terminology
   changes (`terminologyId`, `terminologyRef`), `DV_ORDINAL` symbol changes, assumed values, primitive
   types the diff does not compare yet (`C_BOOLEAN` is covered, check the rest). A type or attribute
   missing from the RM model of the version is an error of the OPT or of the RM version, reported and
   never guessed (decision D5).
5. **Report format and versions (agreed).** The rules replace `certain` and the old `BreakingChange`
   categories with direction and data/query impact. The API break is accepted, with no deprecated
   fields and no workaround: the OPT diff in the SDK, the report it produces, the CLI that renders it and
   Atomik that consumes it are all refactored. It is an experimental feature, so going back and forth
   is fine; what we keep is the version of each lib or system that works together (SDK, CLI, Atomik).
   Still to define at implementation time, for review: the JSON field names per item (proposal:
   `direction`, `dataImpact`, `queryImpact`, `explanation`) and the CLI text layout.
6. **OPT consistency warnings** (section 4) belong to the parser, to `RmValidator2` and to the diff
   note. They are separate work items, not part of the diff implementation.
7. **`RmValidator2` is closed world** (it reports items not defined in the template). The diff
    follows the open world. Aligning the validator is a separate decision.
