openEHR-SDK
===========

Java/Groovy library for openEHR Operational Templates, Reference Model, Data Generators and other tools for www.CaboLabs.com projects.

This library is used in CaboLabs apps like EHRGen, EHRServer, EMRApp and XML Rule Engine.


## Build

The build was tested with [Gradle 6.4.1](https://gradle.org/install/) installed from [SDKMAN!](https://sdkman.io/).

```shell
$ cd openEHR-SDK
$ gradle clean build
```

Build without running the tests (faster):

```shell
$ cd openEHR-SDK
$ gradle build -x test
```

### Requires Java 8+ and Groovy 2.5.5+

Also tested with JDK 11.0.x and Groovy 3.0.x.

For running tests, there are many options, examples below:

1. Run specific test case from a specific suite
2. Run all tests from a specific suite
3. Run all suites in a package
4. Run all tests

```shell
$ cd openEHR-SDK
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest.testCompleteOPT
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest
$ gradle test --tests com.cabolabs.openehr.opt*
$ gradle test
```

The test report in HTML will be under ./build/reports/tests/test/index.html


## Usage as Java/Groovy Library

### Common Imports

```groovy
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepository
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.validation.RmValidator2
import com.cabolabs.openehr.validation.RmValidationReport
import com.cabolabs.openehr.opt.instance_generator.RmInstanceGenerator
```

### OptManager

`OptManager` is a process-wide singleton that caches parsed OPTs (and their referenced
archetypes) per namespace, so callers don't reparse the same template on every request. It's
used internally by `RmValidator2` and the instance generators, and can be used directly to
load/inspect OPTs.

```groovy
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepository
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl

OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("opts").toURI())
OptManager opt_manager = OptManager.getInstance()
opt_manager.init(repo)

// look up by templateId within a namespace (cache hit, or search + cache on miss)
def opt = opt_manager.getOpt('my_template.v1', OptManager.DEFAULT_NAMESPACE)

// load a specific file/location directly, bypassing the templateId search - an alternative
// to the namespace+templateId lookup above, not a variant of it
def opt2 = opt_manager.loadFromLocation('/path/to/some.opt', OptManager.DEFAULT_NAMESPACE)
```

**Thread safety.** `OptManager`'s internal caches are backed by `ConcurrentHashMap` /
`CopyOnWriteArrayList`, and the compound-mutation methods (`loadAll`, `load`, `getOpt`,
`unloadAll`, `removeOpt`, `cleanCache`, `cleanAll`, `getInstance`) are synchronized. Reads
(`getLoadedOpts`, `getNodes`, `getText`, `isLoaded`, ...) don't need external locking.

**`complete()` runs by default.** `loadAll()`, `load()`, `loadFromLocation()` and `getOpt()`
all call `OperationalTemplate.complete()` by default now, so RM-mandatory
attributes not explicit in the archetype/OPT get materialized before validation - including
`getOpt()`'s implicit lazy-load path used by `RmValidator2`. Pass `complete: false` to any of
them to opt out and get the raw, uncompleted OPT:

```groovy
def opt = opt_manager.getOpt('my_template.v1', OptManager.DEFAULT_NAMESPACE, false)
```

Since 2.7.0, `getOpt()` with `complete` true (the default) also completes an OPT that is
already cached uncompleted (e.g. loaded earlier with `load(..., false)`), in place, using
`OperationalTemplate.isCompleted`. The result no longer depends on which caller loaded the
OPT first. Because the cached instance is shared, every caller sees the completed OPT from
then on; use `getOpt(..., false)` to read the cached OPT as it is without completing it.

**Errors.** `getOpt()`, `load()` and `loadFromLocation()` throw an `Exception` when the OPT is
not found or can't be read or parsed, they never return `null`. The message includes the
reason from the repo (`There was a problem reading the template from the repo: <reason>`) and
the original exception is the cause. Since 2.7.0, `loadAll()` also skips a file that fails to
parse instead of re-caching the template parsed from the previous file.

**Configurable default namespace.** `OptManager.DEFAULT_NAMESPACE` is the namespace every
method falls back to when none is passed explicitly. Set it once at startup instead of
passing a namespace on every call:

```groovy
// either at init() time, matching the folder name that holds your OPTs in the repo
opt_manager.init(repo, 1800, 'my_org_templates')

// or explicitly, any time
OptManager.setDefaultNamespace('my_org_templates')
```

**Resetting.** `OptManager.reset()` drops the singleton (and, with it, all cached state) -
the next `getInstance()` starts from a clean slate. Useful between test runs or when
switching repos entirely; `unloadAll(namespace)` clears just one namespace instead.

### Validate an openEHR Operational Template against its schema

```groovy
import com.cabolabs.openehr.opt.instance_validation.XmlValidation

def inputStream = this.getClass().getResourceAsStream('/xsd/OperationalTemplateExtra.xsd')
def validator = new XmlValidation(inputStream)

def path = "someopt.opt"
def f = new File(path)
if (!f.exists())
{
   println path +" doesn't exist"
   System.exit(0)
}

validateXML(validator, f)

static boolean validateXML(validator, file)
{
   boolean isValid = true
   if (!validator.validate( file.text ))
   {
      println file.name +' NOT VALID'
      println '====================================='
      validator.errors.each {
         println it
      }
      println '====================================='
      isValid = false
   }
   else
   {
      println file.name +' VALID'
   }

   println ""
   return isValid
}
```

### Parse an openEHR Operational Template

```groovy
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.OperationalTemplate

OperationalTemplate loadAndParse(String path)
{
   def parser = new OperationalTemplateParser()

   def optFile = new File(getClass().getResource(path).toURI())
   def text = optFile.getText()

   return parser.parse(text)
}
```

To add the attributes that are in the openEHR Reference Model but are not in the source OPT file, use the method complete():

```groovy
def opt = loadAndParse("vital_signs.opt")
opt.complete()
```

#### Parser notes

**C_DURATION: range or pattern.** A `C_DURATION` constrains a duration expression in one of two ways: with a *pattern*, the expression has to follow a format (`PYMD` allows years, months and days), or with a *range*, the expression has to be between two durations (`PT0M..PT100M` means between 0 and 100 minutes). The schema allows both in the same constraint, but in practice they are alternatives, so the parser treats them that way: if the OPT has a range, `CDuration.range` is set and the pattern is not read (`CDuration.pattern` stays `null`); the pattern is only read when there is no range.

**Description metadata.** The `<description>` of the OPT (RESOURCE_DESCRIPTION) is parsed into `OperationalTemplate` and written back by `OptXmlSerializer` and `JsonSerializer`:

- `originalAuthor`: map of `id -> value` from the `original_author` elements (e.g. `name`, `organisation`, `email`, `date`), in document order.
- `otherContributors`: list from `other_contributors`.
- `lifecycleState`, `resourcePackageUri`: `null` when empty or not present.
- `otherDetails`: map of `id -> value` from the `other_details` of the description (e.g. `licence`, `custodian_organisation`, `MD5-CAM-1.0.1`). Ids with an empty value are kept with `''`.
- From the first `<details>` item: `purpose`, `keywords` (list), `use`, `misuse`, `copyright`, `originalResourceUri` and `detailsOtherDetails` (maps of `id -> value`).

Only the first `<details>` item is read, so the descriptions in other languages are not kept. When an OPT has no `original_author`, the XML serializer writes `Not Specified`, since the schema requires it.

### Parse a JSON COMPOSITION

```groovy
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String path = "vital_signs.json"
File file = new File(getClass().getResource(path).toURI())
String json = file.text
def parser = new OpenEhrJsonParser()
Composition c = (Composition)parser.parseJson(json)
```

### Serialize a COMPOSITION to JSON

```groovy
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

Composition compo = ...
def serializer = new OpenEhrJsonSerializer()
String json = serializer.serialize(compo)
```

### Parse an XML COMPOSITION

```groovy
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String path = "vital_signs.xml"
File file = new File(getClass().getResource(path).toURI())
String xml = file.text
def parser = new OpenEhrXmlParser()
Composition c = (Composition)parser.parseLocatable(xml)
```

### Serialize a COMPOSITION to XML

```groovy
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

Composition compo = ...
OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
String xml = marshal.serialize(compo)
```

### Validate COMPOSITION against OPT

> **Note:** the original `RmValidator` has been decommissioned and removed. `RmValidator2` is
> the only supported RM validator; use it for all new code.

```groovy
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepository
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl
import com.cabolabs.openehr.validation.RmValidator2
import com.cabolabs.openehr.validation.RmValidationReport
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

// setup OPT repository
String opt_repo_path = "opts"
OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
OptManager opt_manager = OptManager.getInstance()
opt_manager.init(repo)

// load COMPOSITION
Composition compo = ...

// the validator automatically gets the template from the repo based on the template_id in the COMPOSITION
RmValidator2 validator = new RmValidator2(opt_manager)
RmValidationReport report = validator.dovalidate(compo, OptManager.DEFAULT_NAMESPACE)

report.errors.each { error ->
   println error
}
```

The OPT for the COMPOSITION's `template_id` must exist: that is a precondition of the validation,
not a data validation result. If the OPT can't be found or loaded, `dovalidate()` throws the
`Exception` from `OptManager.getOpt()` (with the reason in its message) instead of returning a
report. Since 2.7.0, the public `RmValidator2.opt_not_found(String)` method was removed, it was
never reachable.

### Transform XML COMPOSITION to JSON

```groovy
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String xml = ...
def parser = new OpenEhrXmlParser()
Composition c = (Composition)parser.parseLocatable(xml)
def serializer = new OpenEhrJsonSerializer()
String json = serializer.serialize(c)
```

### Transform JSON COMPOSITION to XML

```groovy
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String json = ...
def parser = new OpenEhrJsonParser()
Composition c = (Composition)parser.parseJson(json)
def serializer = new OpenEhrXmlSerializer()
String xml = serializer.serialize(c)
```

### Generate JSON COMPOSITION from OPT

```groovy
import com.cabolabs.openehr.opt.instance_generator.JsonInstanceCanonicalGenerator2
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('vital_signs.opt')
def igen = new JsonInstanceCanonicalGenerator2()
String json = igen.generateJSONVersionStringFromOPT(opt, true, true)
```

### Generate XML COMPOSITION from OPT

```groovy
import com.cabolabs.openehr.opt.instance_generator.XmlInstanceGenerator
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('vital_signs.opt')
def igen = new XmlInstanceGenerator()
String xml = igen.generateXMLCompositionStringFromOPT(opt, true)
```

### Generate Instances with RmInstanceGenerator

The `RmInstanceGenerator` can generate instances for various openEHR types:

```groovy
import com.cabolabs.openehr.opt.instance_generator.RmInstanceGenerator
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.demographic.Person

def generator = new RmInstanceGenerator()
def opt = loadAndParse('template.opt')

// Generate COMPOSITION
Composition composition = generator.generateCompositionFromOPT(opt, false)

// Generate COMPOSITION with participations
Composition compositionWithPart = generator.generateCompositionFromOPT(opt, true)

// Generate VERSION wrapper
def version = generator.generateVersionFromOPT(opt, false, 'rm')

// Generate EHR_STATUS
EhrStatus ehrStatus = generator.generateEhrStatusFromOPT(opt)

// Generate FOLDER
Folder folder = generator.generateFolderFromOPT(opt)

// Generate PERSON (demographic)
Person person = generator.generatePersonFromOPT(opt)

// Generate PERSON DTO (API flavor with resolved references)
def personDto = generator.generatePersonDtoFromOPT(opt)

// Other demographic types
def organization = generator.generateOrganizationFromOPT(opt)
def agent = generator.generateAgentFromOPT(opt)
def group = generator.generateGroupFromOPT(opt)
def role = generator.generateRoleFromOPT(opt)
def relationship = generator.generateRelationshipFromOPT(opt)
```

### Transform OPT to JSON

```groovy
import com.cabolabs.openehr.opt.serializer.JsonSerializer
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('template.opt')
def serializer = new JsonSerializer()
serializer.serialize(opt)
String json = serializer.get(true) // true for pretty print
```

### Generate OPT from ADL Archetype

```groovy
import com.cabolabs.openehr.opt.opt_generator.AdlToOpt
import com.cabolabs.openehr.opt.serializer.OptXmlSerializer
import se.acode.openehr.parser.ADLParser

def adlFile = new File('archetype.adl')
def parser = new ADLParser(adlFile)
def archetype = parser.archetype()

def adlToOpt = new AdlToOpt()
def opt = adlToOpt.generateOpt(archetype)

def serializer = new OptXmlSerializer(true)
String optXml = serializer.serialize(opt)
```

### Generate UI from OPT

```groovy
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('template.opt')

// Generate full HTML page with Bootstrap 5
def generator = new OptUiGenerator(true, 5)
String html = generator.generate(opt)

// Generate form only with Bootstrap 4
def formGenerator = new OptUiGenerator(false, 4)
String formHtml = formGenerator.generate(opt)
```

### Diff two Operational Templates

Two complementary diff algorithms live under `com.cabolabs.openehr.opt.diff`:

- **`OperationalTemplateDiffAlgorithm`** — structural diff. Classifies every node as `same`, `added` or `removed` by comparing template paths between the two OPTs. Fast, and good for spotting structural changes, but doesn't look inside a node: if a node exists in both OPTs at the same path but its constraints changed (name, code list, cardinality, etc.), it's still reported as `same`.
- **`SemanticOperationalTemplateDiffAlgorithm`** — field-level diff. Traverses both OPTs' object trees in parallel (attributes matched by RM attribute name; children matched by archetype_id ignoring its trailing version for archetype-root nodes, by `nodeId` otherwise, falling back to position when neither is present) and additionally reports `modified` nodes together with the specific fields/constraints that changed: `text` (the OPT node text), RM type, occurrences, `archetypeId` (so a version bump on an archetype slot shows up explicitly), cardinality, existence, code lists, terminology, quantity units/magnitude/precision, ordinal values/symbols, primitive patterns/ranges, archetype slot includes/excludes, etc. Matching an archetype root by archetype_id rather than `nodeId` matters because the root's `nodeId` is conventionally the generic `at0000` for every archetype - under a `C_MULTIPLE_ATTRIBUTE` slot holding several sibling archetypes (e.g. `COMPOSITION.content`), `nodeId`-only matching would pair up unrelated archetypes; a specialized archetype replacing a generic one (different archetype_id concept, e.g. `request-lab` for `request`) is correctly reported as removed+added rather than matched. It also flags which of those changes are *breaking* - would invalidate data that was valid under `opt1` once validated against `opt2` (see [Breaking-change detection](#breaking-change-detection)).

Both return a tree (`OperationalTemplateDiff` / `SemanticOperationalTemplateDiff`) rooted at the OPT's `definition`; neither algorithm mutates the input OPTs, so the same `OperationalTemplate` instances can be diffed against several others.

#### Structural diff

```groovy
import com.cabolabs.openehr.opt.diff.OperationalTemplateDiffAlgorithm
import com.cabolabs.openehr.opt.diff.OperationalTemplateDiff2JsMindTree

def opt1 = loadAndParse('template_v1.opt')
def opt2 = loadAndParse('template_v2.opt')

def diffal = new OperationalTemplateDiffAlgorithm()
def diff = diffal.diff(opt1, opt2) // OperationalTemplateDiff

// diff.root is a tree of NodeDiff: { templateDataPath, compareResult: same|added|removed, optNode, attributeDiffs }
// attributeDiffs is Map<rmAttributeName, List<NodeDiff>>, since an attribute can hold repeated/alternative nodes

// NodeDiff.optNode holds a live reference into the OPT tree (with a circular parent pointer),
// so don't JsonOutput.toJson(diff) directly - use the jsMind serializer instead:
def jsmind = new OperationalTemplateDiff2JsMindTree()
println jsmind.getJsMindTreeString(diff)
```

Given a template where `at0004`, `at0005` and `at0009` all got their internal constraints edited but no node was added or removed, `diff.root` looks like this (printed manually by walking `attributeDiffs`):

```
/ [same]
  @content [same]
    /content[archetype_id=...diff_observation_test.v1](1) [same]
      @data [same]
        .../data[at0001](1) [same]
          @events [same]
            .../events[at0002](1) [same]
              @data [same]
                .../data[at0003](1) [same]
                  @items [same]
                    .../items[at0004](1) [same]   <- name/occurrences changed, NOT detected
                    .../items[at0005](1) [same]   <- code list changed, NOT detected
                    .../items[at0009](1) [same]   <- quantity list changed, NOT detected
```

Since nothing was added or removed, the whole tree reports `same` - the structural diff can't see the changes inside those three nodes.

#### Semantic diff

```groovy
import com.cabolabs.openehr.opt.diff.SemanticOperationalTemplateDiffAlgorithm

def opt1 = loadAndParse('template_v1.opt')
def opt2 = loadAndParse('template_v2.opt')

def diffal = new SemanticOperationalTemplateDiffAlgorithm()
def diff = diffal.diff(opt1, opt2) // SemanticOperationalTemplateDiff

println diff.templateMetadataChanges // template_id/concept/language/purpose/isControlled changes

def dump
dump = { node, indent ->
   println "${indent}${node.templatePath} [${node.status}]"
   node.fieldChanges.each { println "${indent}  * ${it.field}: '${it.oldValue}' -> '${it.newValue}'" }
   node.listChanges.each  { println "${indent}  * ${it.field}: added=${it.added.size()} removed=${it.removed.size()} modified=${it.modified*.item}" }
   node.attributes.each { name, attrDiff -> attrDiff.children.each { child -> dump(child, indent + '  ') } }
}
dump(diff.root, '')
```

For the same at0004/at0005/at0009 changes (occurrences `0..1`→`1..1` and a name change on at0004, one code added to at0005's local code list, one quantity unit added plus a `magnitude >= 0` constraint added to the existing units on at0009 - this exact scenario is covered by `SemanticOptDiffTest.testSemanticDiffUserProvidedFixtures`), the output looks like this:

```
/ [modified]
  ...
  .../items[at0004] [modified]
    * occurrences: '[0..1]' -> '[1..1]'
    * text: 'text node 1' -> 'text node 1 changed name'
  .../items[at0005] [modified]
    .../items[at0005]/value [modified]
      .../items[at0005]/value/defining_code [modified]
        * codeList: added=1 removed=0 modified=[]
  .../items[at0009] [modified]
    .../items[at0009]/value [modified]
      * list: added=1 removed=0 modified=[cm, mm]
```

(`added`/`removed` on a `list`/`codeList` `ListChange` hold the actual matched items - plain strings for `codeList`, `CQuantityItem`/`CDvOrdinalItem` objects for quantity/ordinal lists; `modified` holds one `ListItemChange` per item that exists on both sides but differs, keyed by the item's matching field - `units` for quantity, `value` for ordinal - each carrying its own `FieldChange` list, e.g. `magnitude: '(*..*)' -> '[0.0..*)'` for `cm`/`mm` above.)

Every `modified` status bubbles up to all ancestors, so `diff.root.status == 'modified'` even though the actual changes are deep in the tree - and every node already carries `templatePath`, `nodeId`, `rmTypeName`, `type`, `text`, `status`, `fieldChanges` and `listChanges`, so a tree UI can render straight off this structure without any further lookups into the OPTs.

#### Breaking-change detection

On top of `fieldChanges`/`listChanges`, the semantic diff also flags which changes can invalidate data that was valid under `opt1` once it's validated against `opt2`. Every such change is collected into `diff.breakingChanges` (flat `List<BreakingChange>`, each with `templatePath`, `category`, `field`, `oldValue`, `newValue`, `reason`, `certain`). Every `SemanticNodeDiff`/`AttributeDiff` also carries a `breaking` boolean rolled up from its own changes and all its descendants, so a tree UI can flag a path without walking the flat list. `breaking` is true only for *confirmed* changes (`certain = true`): a node with only potential changes is not `breaking`, filter `diff.breakingChanges` by `certain` to get those.

##### Confirmed vs potential

- **Confirmed** (`certain = true`): the change alone makes data that was valid under `opt1` invalid, no need to look at the data.
- **Potential** (`certain = false`): the change breaks only the data that has certain values, so it needs a check against the data that can be automated (e.g. query the repository for values outside of the new constraint). If no data has those values, nothing breaks.

##### The RM defaults are the baseline

An OPT only states what it constrains. What it doesn't constrain is ruled by the RM (the BMM `openehr_rm_1.2.0`): any value of the attribute's type, the attribute's mandatory/optional existence, or the values of an RM enumeration. The diff compares against that baseline, so an attribute that the old OPT didn't mention is not "nothing":

- Attributes that are mandatory in the RM (e.g. `DV_PROPORTION.numerator/denominator/type`, `DV_COUNT.magnitude`, `DV_QUANTITY.magnitude/units`, `DV_CODED_TEXT.defining_code`, `DV_DATE_TIME.value`) have existence `1..1` and occurrences `1..1`, the optional ones (e.g. `DV_PROPORTION.precision`) `0..1`. Writing the same `1..1` in the new OPT is not a change.
- When the OPT constrains an attribute that the RM defines, it is reported as `modified` (not `added`), going from the RM default to the new constraint, e.g. `DV_COUNT.magnitude` `(*..*)` -> `[0..*)`.
- The RM doesn't say a magnitude is positive: `Integer64` allows negatives, so `(*..*)` is the default and any range on it is a narrowing.
- RM enumerations are the baseline too: `DV_PROPORTION.type` is `[0, 1, 2, 3, 4]`, so `[2]` is a narrowing of that list.
- A constraint that isn't there is shown as the most open expression of its type, never as `null`: `(*..*)` for intervals, `any` for patterns and string lists, `[true, false]` for booleans.

##### Categories

Confirmed (`certain = true`):

| Category | When |
|----------|------|
| `occurrences_narrowed`, `existence_narrowed`, `cardinality_narrowed`, `range_narrowed` | An interval the old OPT stated explicitly got tighter: a higher lower bound, a lower upper bound, or a bound that flips from inclusive to exclusive. |
| `value_removed` | A value dropped out of a constrained set: a code removed from a `CODE_PHRASE` `codeList`, an item removed from an ordinal/string/integer `list`, a quantity unit removed, `CBoolean.trueValid`/`falseValid` flipping to invalid. |
| `node_added_mandatory`, `attribute_added_mandatory` | A node or attribute that didn't exist in `opt1` is now required (occurrences/existence doesn't allow `0`): legacy data never had it. |
| `node_removed` (mandatory node) | A node that was mandatory in `opt1` (occurrences doesn't allow `0`) was removed. All the legacy data has it, and it is not valid for `opt2`. |

Potential (`certain = false`):

| Category | When |
|----------|------|
| `constraint_added` | A constraint over a value the old OPT didn't constrain (so the RM default applied): a list or range on an integer/real/duration, a pattern, a string list, a boolean restricted to one value, an RM enumeration restricted (`DV_PROPORTION.type` `[0..4]` -> `[2]`). Breaks only the data with values outside the new constraint. |
| `range_narrowed` | The same case for a range, when the old OPT had a range that was fully unbounded or none. |
| `node_removed` (optional node) | An optional node was removed. Breaks only the data that has the node, and any stored query that references the path. |
| `attribute_removed` | An attribute was removed, check the data and the stored queries that reference the path. |

##### Added and removed subtrees

Only the root of an added or removed subtree is evaluated, never what is below it:

- **Added:** an optional new node (`occurrences` `0..1`) whose `value` is `1..1` is not breaking: there is no legacy data under a node that didn't exist. Only a new node that is itself required is breaking (`node_added_mandatory`).
- **Removed:** one `node_removed` for the removed node, confirmed or potential depending on its occurrences. Its children are not reported.

Widening changes (a bound relaxed, a value added to a set, a constraint dropped entirely) are never reported as breaking.

##### Example

For `test_diff_2.opt` -> `test_diff_2_v1.opt` (see `SemanticOptDiffTest`):

```groovy
diff.breakingChanges.each { println "${it.templatePath} [${it.category}] ${it.field}: '${it.oldValue}' -> '${it.newValue}' (certain=${it.certain})" }
```

```
.../items[at0002]/value/precision [occurrences_narrowed] occurrences: '[0..1]' -> '[1..1]' (certain=true)
.../items[at0002]/value/precision [constraint_added] range: '(*..*)' -> '[1..1]' (certain=false)
.../items[at0002]/value/type [constraint_added] list: '[0, 1, 2, 3, 4]' -> '[2]' (certain=false)
.../items[at0003]/value/value [constraint_added] pattern: 'any' -> 'yyyy-mm-ddThh:mm:ss' (certain=false)
.../items[at0004]/value/magnitude [constraint_added] range: '(*..*)' -> '[0..*)' (certain=false)
.../items[at0009]/value[units=cm] [range_narrowed] magnitude: '(*..*)' -> '[0.0..*)' (certain=false)
```

(plus the same for `mm`, and a confirmed `occurrences_narrowed` for `items[at0004]` of the observation). The new optional `items[at0006]` is `added` and appears nowhere in the list. In `test_diff_2_v2.opt`, `at0006` is required (`1..1`), so it is reported as a confirmed `node_added_mandatory`, and the removal of a required node (`test_diff_2_v3_removed_mandatory.opt`) as a confirmed `node_removed`.

[EHRCommitter]: https://github.com/ppazos/EHRCommitter
[EHRServer]: https://github.com/ppazos/cabolabs-ehrserver
