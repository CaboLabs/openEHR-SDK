# Wiring OPT diff into openEHR-CLI

Notes for implementing a `diff` command in the [openEHR-CLI] repo (picocli-based,
`cli/services` + `cli/commands` + `MainCli.groovy` layout per `CLI.md`) that exposes
`com.cabolabs.openehr.opt.diff` from openEHR-SDK >= 2.3.0. Written as a briefing an LLM
coding agent can work from directly - it covers the library contract, what's genuinely
undecided (needs a judgement call), and what's a plain implementation detail.

## The library surface

Two algorithms, both in `com.cabolabs.openehr.opt.diff`, both take two parsed
`OperationalTemplate` instances and return a tree - neither mutates its inputs:

```groovy
def opt1 = new OperationalTemplateParser().parse(new File(path1).text)
def opt2 = new OperationalTemplateParser().parse(new File(path2).text)
```

### `OperationalTemplateDiffAlgorithm` (structural)

```groovy
def diff = new OperationalTemplateDiffAlgorithm().diff(opt1, opt2) // OperationalTemplateDiff
```

- `diff.root` is a tree of `NodeDiff { templateDataPath, compareResult: 'same'|'added'|'removed', optNode, attributeDiffs }`.
- `attributeDiffs` is `Map<rmAttributeName, List<NodeDiff>>`.
- `NodeDiff.optNode` is a **live reference into the OPT tree with a circular `parent` pointer** -
  never `JsonOutput.toJson(diff)` it directly, it will blow up or produce garbage. Use
  `OperationalTemplateDiff2JsMindTree.getJsMindTreeString(diff)` for a safe serializable form,
  or write your own walker that only reads the fields you need.
- Only reports `same`/`added`/`removed` at the path level - a node that exists on both sides
  with different internal constraints still comes back `same`. Don't market this mode as
  "shows what changed"; it shows "what moved".

### `SemanticOperationalTemplateDiffAlgorithm` (field-level)

```groovy
def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2) // SemanticOperationalTemplateDiff
```

- `diff.templateMetadataChanges`: `List<FieldChange>` for `templateId`/`concept`/`language`/`purpose`/`isControlled`.
- `diff.root`: `SemanticNodeDiff`, recursively:
  - `templatePath`, `nodeId`, `rmTypeName`, `type`, `name` - denormalized, no lookups needed to render.
  - `status`: `'same' | 'added' | 'removed' | 'modified'`.
  - `fieldChanges`: `List<FieldChange { field, oldValue, newValue }>` - e.g. `occurrences`, `name`,
    `archetypeId` (shows up when an archetype-root node's version bumped), plus type-specific
    fields (`terminologyId`, `property`, `pattern`, `range`, `trueValid`/`falseValid`, etc.)
  - `listChanges`: `List<ListChange { field, added, removed, modified: List<ListItemChange { item, changes }> }>`
    - `added`/`removed` hold the actual matched items (plain strings for `codeList`;
      `CQuantityItem`/`CDvOrdinalItem` objects for quantity/ordinal lists).
    - `modified` holds one entry per item present on both sides but differing, keyed by its
      matching field (`units` for quantity, `value` for ordinal), each with its own `changes: List<FieldChange>`.
  - `attributes`: `Map<rmAttributeName, AttributeDiff { rmAttributeName, status, fieldChanges, children: List<SemanticNodeDiff> }>`.
  - `node1`/`node2`: raw `ObjectNode` refs, kept for advanced/debug use (e.g. reading a value that
    doesn't get its own `FieldChange` on an `added`/`removed` whole-subtree node, since those
    don't run field comparison - there's nothing to diff against). **Same circular-reference
    caveat as `NodeDiff.optNode` - never serialize these directly.**
- **Node matching** (why two diffs of the "same" node can legitimately show different content):
  attributes match by RM attribute name; children match by archetype_id ignoring its trailing
  version for archetype-root nodes (so `foo.v1` == `foo.v2`, but `foo.v1` != `foo-specialized.v1`
  - specialization swaps correctly come back removed+added), by `nodeId` otherwise, by position
  as a last resort. This matters concretely for a `COMPOSITION.content` with several sibling
  archetypes: their root `nodeId` is conventionally the generic `at0000` for all of them, so
  `nodeId`-only matching would misidentify which sibling is which.

## Suggested command

Following the existing `-s/--source` `-d/--dest` idiom from other commands in `CLI.md`, a diff
needs two sources, so something like:

```bash
java -jar build/libs/opt-x.y.z.jar diff --old path/to/v1.opt --new path/to/v2.opt \
    [--mode semantic|structural] [--format tree|json] [--full] [--no-color]
```

- `--mode` default should be `semantic` - it's a strict superset of what structural reports,
  and the whole point of shipping it is that structural silently misses in-place constraint
  changes. Keep `structural` available for users who explicitly want the cheaper/older behavior
  or want to compare the two outputs.
- `--format json`: **do not** serialize `diff` itself (circular refs, see above). Write a small
  mapper `SemanticNodeDiff -> Map` (or reuse a similar mapper for `NodeDiff`) that walks
  `fieldChanges`/`listChanges`/`attributes` and omits `node1`/`node2`/`optNode`. Sketch:

  ```groovy
  Map toMap(SemanticNodeDiff n) {
     [
        templatePath: n.templatePath, nodeId: n.nodeId, rmTypeName: n.rmTypeName,
        type: n.type, name: n.name, status: n.status,
        fieldChanges: n.fieldChanges.collect { [field: it.field, oldValue: it.oldValue, newValue: it.newValue] },
        listChanges:  n.listChanges.collect { lc -> [
           field: lc.field, added: lc.added, removed: lc.removed,
           modified: lc.modified.collect { [item: it.item, changes: it.changes.collect { c -> [field: c.field, oldValue: c.oldValue, newValue: c.newValue] }] }
        ]},
        attributes: n.attributes.collectEntries { name, a -> [name, [
           rmAttributeName: a.rmAttributeName, status: a.status,
           fieldChanges: a.fieldChanges.collect { [field: it.field, oldValue: it.oldValue, newValue: it.newValue] },
           children: a.children.collect { toMap(it) }
        ]]}
     ]
  }
  ```

## Rendering to a terminal

This is the part with real design freedom - pick what reads best, but a few things are worth
carrying over deliberately:

- **Collapse `same` by default.** A large OPT diffed against a near-identical version will be
  almost entirely `same`; printing it in full buries the actual changes. Default to only
  printing the path from root down to each `modified`/`added`/`removed` node (git-diff-style),
  with a `--full` flag to dump everything including `same` subtrees.
- **Prefix + color, not color alone.** `+`/`-`/`~`/` ` prefixes (added/removed/modified/same)
  keep the output usable with `--no-color` or `NO_COLOR` set, piped to a file, or for
  colorblind users. Color is a bonus, not the only signal.
- **Print `templateMetadataChanges` first**, as a small header block, before the tree - template
  id/concept changes are usually the first thing a reviewer wants confirmed.
- **Print field/list changes indented under their node**, e.g.:
  ```
  ~ .../items[at0004] [modified]
      occurrences: [0..1] -> [1..1]
      name: 'text node 1' -> 'text node 1 changed name'
  ~ .../items[at0009]/value [modified]
      list: +1 -1 ~[cm, mm]
        ~ cm: magnitude: null -> [0.0..*)
  ```
- **End with a summary line** (`3 added, 1 removed, 2 modified, 41 same`) - walk the tree once,
  tally by `status`. Cheap, and it's the first thing worth reading if the tree itself is long.
- **Structural mode's output should say so.** Since `same` under structural diff doesn't mean
  "unchanged", label the report header with which algorithm ran, so a `same`-only structural
  report isn't mistaken for "nothing changed".

## Optional: breaking-change detection

Not part of the library today - a possible follow-up worth flagging as a design question rather
than building blind: an OPT diff in a CI pipeline (e.g. `--fail-on-breaking`, non-zero exit) is a
natural use case, but "breaking" requires product judgement calls the SDK doesn't currently make
(is removing a `codeList` entry breaking? is tightening `existence` from `0..1` to `1..1`
breaking for producers but not consumers?). If this is wanted, treat it as a CLI-side heuristic
layered on top of the semantic diff tree (e.g. flag `removed` nodes, existence/cardinality lower
bound increases, `codeList`/ordinal-list removals, narrowed numeric/date ranges) rather than
something to push back into the SDK - the SDK's job is reporting *what* changed, not judging it.

## Reference implementation / test fixtures

- `openEHR-SDK/src/test/groovy/com/cabolabs/openehr/opt/SemanticOptDiffTest.groovy` - canonical
  usage examples of every field on the result tree, including a multi-sibling-archetype fixture
  (`test_diff_2_v0.opt`/`test_diff_2_v1.opt`) exercising the archetype_id matching behavior.
- `openEHR-SDK/README.md`, "Diff two Operational Templates" section - sample output for both
  algorithms, verified against the passing tests (not fabricated).
- Recommend the CLI's own tests follow the same pattern: small, hand-crafted `.opt` fixture pairs
  with a comment documenting exactly what changed between them, and assertions on the rendered
  terminal output (or the JSON mapper's output), not just "it didn't throw".

[openEHR-CLI]: https://github.com/CaboLabs/openEHR-CLI
