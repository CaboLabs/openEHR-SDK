# AOM 1.4: Attribute Existence vs. Object Occurrences

**Scope:** openEHR Archetype Object Model 1.4, AM Release 2.3.0\
**Purpose:** Implementation-oriented summary for code analysis and
validation.

Specification:
https://specifications.openehr.org/releases/AM/Release-2.3.0/AOM1.4.html

## Core distinction

`C_ATTRIBUTE.existence` and `C_OBJECT.occurrences` constrain different
levels of the archetype structure:

-   **`C_ATTRIBUTE.existence`** constrains whether the attribute's
    target object exists in the reference-model data.
-   **`C_OBJECT.occurrences`** constrains how many times that particular
    constrained object node occurs in the data under its owning
    attribute.

Do not treat these constraints as interchangeable or collapse them into
one multiplicity.

## Model structure

AOM defines an alternating constraint structure:

``` text
C_COMPLEX_OBJECT
  └── C_ATTRIBUTE
        └── children: List<C_OBJECT>
```

`C_ATTRIBUTE` has:

-   `rm_attribute_name`
-   `existence: Interval<Integer>`
-   `children: List<C_OBJECT>`

Each child `C_OBJECT` has its own:

-   `rm_type_name`
-   `occurrences: Interval<Integer>`
-   `node_id`

The owning attribute determines how its child object constraints are
interpreted.

## Attribute existence

`existence` is defined on every `C_ATTRIBUTE`, whether the
reference-model attribute is single-valued or container-valued.

Its interval is constrained by the AOM invariant:

``` text
existence.lower >= 0
existence.upper <= 1
```

Typical meanings:

  Interval   Meaning
  ---------- ---------------------------------
  `{0..0}`   Attribute target must not exist
  `{0..1}`   Attribute target is optional
  `{1..1}`   Attribute target is mandatory

For a container-valued attribute, existence concerns the **container
object itself**, not the number of members inside it.

## Object occurrences

`occurrences` is defined on each `C_OBJECT` and counts occurrences of
that object node under its owning attribute.

Examples:

  Interval   Meaning
  ---------- -------------------------------------------------------
  `{0..1}`   This constrained object may occur zero or one time
  `{1..1}`   This constrained object must occur exactly once
  `{0..*}`   This constrained object may occur any number of times
  `{1..*}`   This constrained object must occur at least once

AOM states that an occurrences upper limit greater than 1 is only
possible when the owning attribute has a cardinality greater than 1.

## Single-valued attributes: children are alternatives

A `C_SINGLE_ATTRIBUTE` represents a single-valued reference-model
attribute.

Its child `C_OBJECT` nodes represent **alternative constraints for the
one attribute value**. They are not multiple co-existing values.

AOM invariant:

``` text
alternatives.for_all(co: C_OBJECT | co.occurrences.upper <= 1)
```

Consequences:

-   The attribute can have at most one value.
-   Multiple child constraints describe alternative allowed forms/values
    for that one value.
-   Do not sum child occurrences as if they were members of a
    collection.
-   The existence constraint still independently says whether the
    attribute target is mandatory or optional.

## Multiple-valued attributes: children are members

A `C_MULTIPLE_ATTRIBUTE` represents a container-valued attribute, such
as `List<T>` or `Set<T>`.

Its child `C_OBJECT` nodes represent constraints on **co-existing member
objects** in the container.

It has an additional `cardinality` constraint:

-   `C_ATTRIBUTE.existence`: whether the container object exists.
-   `C_MULTIPLE_ATTRIBUTE.cardinality.interval`: how many items the
    container must contain.
-   `C_OBJECT.occurrences`: how many times each particular member
    constraint can occur.

`cardinality` also specifies whether the container has ordered and/or
unique membership (`is_ordered`, `is_unique`).

Example conceptual distinction:

``` text
contacts existence = {0..1}
contacts cardinality = {1..5}
Contact A occurrences = {0..2}
Contact B occurrences = {1..3}
```

This means the contacts container is optional; if present, its
membership is constrained by the cardinality interval, and the
individual member constraints govern occurrences of their respective
constrained object nodes. The example is illustrative: the combined
constraints must be checked for consistency.

## Implementation guidance

1.  Keep attribute existence and object occurrences as separate
    properties in the internal model and validation logic.
2.  Interpret child `C_OBJECT` nodes according to the owning attribute
    type:
    -   `C_SINGLE_ATTRIBUTE`: alternatives for one value.
    -   `C_MULTIPLE_ATTRIBUTE`: member constraints for a container.
3.  Apply `cardinality` only to `C_MULTIPLE_ATTRIBUTE`; it constrains
    the container's membership, not whether the container exists.
4.  Do not infer attribute existence from child occurrences, or infer
    child occurrences from attribute existence.
5.  Validate the combined constraints for consistency. Independent
    properties do not mean their semantics can be evaluated in
    isolation.
6.  Respect the AOM invariant that a single-valued attribute's
    alternative child occurrences have an upper bound no greater than 1.
7.  For a `C_OBJECT` under a multiply-valued attribute, an occurrences
    upper bound greater than 1 requires an owning attribute cardinality
    upper bound greater than 1.

## Key references in AOM 1.4

-   **§4.2.2 -- Attribute Node Types:** explains single-valued
    alternatives, multiple-valued co-existing members, and the
    distinction between existence and cardinality.
-   **§4.2.3 -- Object Node Types:** describes the object/attribute
    constraint hierarchy.
-   **§4.3.2 -- `C_ATTRIBUTE`:** defines `existence` and `children`.
-   **§4.3.3 -- `C_SINGLE_ATTRIBUTE`:** defines children as alternatives
    and gives the `occurrences.upper <= 1` invariant.
-   **§4.3.4 -- `C_MULTIPLE_ATTRIBUTE`:** defines member constraints and
    `cardinality`.
-   **§4.3.5 -- `CARDINALITY`:** defines container membership interval,
    ordering, and uniqueness.
-   **§4.3.6 -- `C_OBJECT`:** defines `occurrences` under the owning
    attribute.

## Short version

``` text
C_ATTRIBUTE.existence
    = Does the attribute target (the value/container) exist?

C_OBJECT.occurrences
    = How many times does this particular constrained object node occur
      under its owning attribute?

C_MULTIPLE_ATTRIBUTE.cardinality
    = How many members may/must the container contain, and what are
      its ordering/uniqueness semantics?
```

These are distinct constraints at distinct levels. Preserve all three in
parsing, model representation, and validation.

## Semantics summary: single vs multiple attributes

A null or absent value counts as 0 occurrences of every child definition.

| Attribute kind | Existence | Children | Occurrences | Cardinality |
|---|---|---|---|---|
| Single | Whether the value is present (null vs not). | Alternatives for one value. | How many times that alternative occurs, 0 or 1. | n/a |
| Multiple | Whether the container exists (null vs not). | Members. | How many times each member definition occurs. | How many items the container holds (`[]` is size 0). |

### Derived values (used to compare two versions of a constraint)

-   **Single attribute**
    -   The value is **required** if existence min >= 1 or any
        alternative has occurrences min >= 1 (a null has 0 occurrences of
        that alternative, so it fails).
    -   The value is **prohibited** if existence max = 0 or every
        alternative has occurrences max = 0.
-   **Multiple attribute**
    -   The **effective size range** is
        `[max(card.min, sum(child occ.min)) .. min(card.max, sum(child occ.max))]`.
    -   The container is **required** (non-null) if existence min >= 1 or
        the effective min >= 1 (a null container is size 0, which breaks
        the minimum).
    -   The definitions are **inconsistent** if `sum(child occ.max) < card.min`
        or `sum(child occ.min) > card.max`. This is an error in the OPT
        itself, independent of any data.
