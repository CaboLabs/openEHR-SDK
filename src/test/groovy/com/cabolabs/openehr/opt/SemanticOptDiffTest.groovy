package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.model.datatypes.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.diff.*
import com.cabolabs.openehr.rm_1_0_2.Model

class SemanticOptDiffTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   OperationalTemplate loadAndParse(String path)
   {
      def parser = new OperationalTemplateParser()

      def optFile = new File(getClass().getResource(path).toURI())
      def text = optFile.getText()

      assertNotNull(text)
      assert text != ''

      return parser.parse(text)
   }

   // structural-level check reusing the existing diff_test.opt / diff_test_v2.opt fixtures:
   // v2 removes the at0003 element and changes the template_id, at0002/at0004 stay the same.
   void testSemanticDiffStructural()
   {
      def path1 = PS +"opts"+ PS + 'diff' + PS +"diff_test.opt"
      def opt1 = loadAndParse(path1)

      def path2 = PS +"opts"+ PS + 'diff' + PS +"diff_test_v2.opt"
      def opt2 = loadAndParse(path2)

      def diffal = new SemanticOperationalTemplateDiffAlgorithm()
      def diff = diffal.diff(opt1, opt2)

      def templateIdChange = diff.templateMetadataChanges.find { it.field == 'templateId' }
      assert templateIdChange
      assert templateIdChange.oldValue == 'diff test'
      assert templateIdChange.newValue == 'diff test v2'

      // a removal deep in the tree bubbles the 'modified' status all the way up to root
      assert diff.root.status == 'modified'

      def removedNode = findNode(diff, '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test.v1]/data[at0001]/items[at0003]')
      assert removedNode
      assert removedNode.status == 'removed'

      def sameNode = findNode(diff, '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test.v1]/data[at0001]/items[at0002]')
      assert sameNode
      assert sameNode.status == 'same'
   }

   // field-level: CCodePhrase.codeList / terminologyId
   void testSemanticDiffCCodePhrase()
   {
      def cp1 = new CCodePhrase(
         templatePath: '/test', nodeId: 'at0001', rmTypeName: 'CODE_PHRASE', type: 'C_CODE_PHRASE',
         text: 'Code', terminologyId: 'openehr', codeList: ['a', 'b']
      )
      def cp2 = new CCodePhrase(
         templatePath: '/test', nodeId: 'at0001', rmTypeName: 'CODE_PHRASE', type: 'C_CODE_PHRASE',
         text: 'Code', terminologyId: 'local', codeList: ['b', 'c']
      )

      def opt1 = new OperationalTemplate(templateId: 't1', definition: cp1)
      def opt2 = new OperationalTemplate(templateId: 't1', definition: cp2)

      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)
      def node = diff.root

      assert node.status == 'modified'

      def terminologyIdChange = node.fieldChanges.find { it.field == 'terminologyId' }
      assert terminologyIdChange
      assert terminologyIdChange.oldValue == 'openehr'
      assert terminologyIdChange.newValue == 'local'

      def codeListChange = node.listChanges.find { it.field == 'codeList' }
      assert codeListChange
      assert codeListChange.added == ['c']
      assert codeListChange.removed == ['a']
   }

   // field-level: CDvOrdinal.list items added/removed/modified (matched by value)
   void testSemanticDiffCDvOrdinal()
   {
      def list1 = [
         new CDvOrdinalItem(value: 1, symbol: new CodePhrase(terminologyId: 'local', codeString: '1')),
         new CDvOrdinalItem(value: 2, symbol: new CodePhrase(terminologyId: 'local', codeString: '2'))
      ]
      def list2 = [
         new CDvOrdinalItem(value: 1, symbol: new CodePhrase(terminologyId: 'local', codeString: '1-changed')),
         new CDvOrdinalItem(value: 3, symbol: new CodePhrase(terminologyId: 'local', codeString: '3'))
      ]

      def ord1 = new CDvOrdinal(templatePath: '/test', nodeId: 'at0002', rmTypeName: 'DV_ORDINAL', type: 'C_DV_ORDINAL', text: 'Ordinal', list: list1)
      def ord2 = new CDvOrdinal(templatePath: '/test', nodeId: 'at0002', rmTypeName: 'DV_ORDINAL', type: 'C_DV_ORDINAL', text: 'Ordinal', list: list2)

      def opt1 = new OperationalTemplate(templateId: 't1', definition: ord1)
      def opt2 = new OperationalTemplate(templateId: 't1', definition: ord2)

      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)
      def node = diff.root

      assert node.status == 'modified'

      def listChange = node.listChanges.find { it.field == 'list' }
      assert listChange
      assert listChange.removed*.value == [2]
      assert listChange.added*.value == [3]
      assert listChange.modified.size() == 1
      assert listChange.modified[0].item == 1
      assert listChange.modified[0].changes.find { it.field == 'symbol' }
   }

   // field-level: AttributeNode cardinality/existence + nested PrimitiveObjectNode (CString) pattern change
   void testSemanticDiffAttributeAndPrimitive()
   {
      def pon1 = new PrimitiveObjectNode(
         templatePath: '/root/value', rmTypeName: 'DV_TEXT', type: 'C_PRIMITIVE_OBJECT',
         text: 'Value', item: new CString(pattern: 'a.*')
      )
      def atn1 = new AttributeNode(
         rmAttributeName: 'value', type: 'C_SINGLE_ATTRIBUTE',
         existence: new IntervalInt(lower: 1, upper: 1, lowerIncluded: true, upperIncluded: true, lowerUnbounded: false, upperUnbounded: false),
         children: [pon1]
      )
      def root1 = new ObjectNode(templatePath: '/root', nodeId: 'at0001', rmTypeName: 'ELEMENT', type: 'C_COMPLEX_OBJECT', text: 'Root', attributes: [atn1])

      def pon2 = new PrimitiveObjectNode(
         templatePath: '/root/value', rmTypeName: 'DV_TEXT', type: 'C_PRIMITIVE_OBJECT',
         text: 'Value', item: new CString(pattern: 'b.*')
      )
      def atn2 = new AttributeNode(
         rmAttributeName: 'value', type: 'C_SINGLE_ATTRIBUTE',
         existence: new IntervalInt(lower: 0, upper: 1, lowerIncluded: true, upperIncluded: true, lowerUnbounded: false, upperUnbounded: false),
         children: [pon2]
      )
      def root2 = new ObjectNode(templatePath: '/root', nodeId: 'at0001', rmTypeName: 'ELEMENT', type: 'C_COMPLEX_OBJECT', text: 'Root', attributes: [atn2])

      def opt1 = new OperationalTemplate(templateId: 't1', definition: root1)
      def opt2 = new OperationalTemplate(templateId: 't1', definition: root2)

      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)
      def node = diff.root

      assert node.status == 'modified'

      def attrDiff = node.attributes['value']
      assert attrDiff.status == 'modified'
      assert attrDiff.fieldChanges.find { it.field == 'existence' }

      def childNode = attrDiff.children[0]
      def patternChange = childNode.fieldChanges.find { it.field == 'pattern' }
      assert patternChange
      assert patternChange.oldValue == 'a.*'
      assert patternChange.newValue == 'b.*'
   }

   // user-provided fixtures (src/test/resources/test_opt_diff): a COMPOSITION including an
   // OBSERVATION with 3 ELEMENTs under data/events/data/items:
   //  - at0004 (DV_TEXT): occurrences 0..1 -> 1..1, name changed
   //  - at0005 (DV_CODED_TEXT): nested CCodePhrase codeList gained 'at0010'
   //  - at0009 (DV_QUANTITY): list gained a '[in_i]' item, and 'cm'/'mm' items each gained a magnitude >= 0 constraint
   void testSemanticDiffUserProvidedFixtures()
   {
      def path1 = PS +"test_opt_diff"+ PS +"test_diff_v0.opt"
      def opt1 = loadAndParse(path1)

      def path2 = PS +"test_opt_diff"+ PS +"test_diff_v1.opt"
      def opt2 = loadAndParse(path2)

      def diffal = new SemanticOperationalTemplateDiffAlgorithm()
      def diff = diffal.diff(opt1, opt2)

      // templatePath on a matched node is taken from the 'to' (opt2) side, and the archetype_id
      // in this fixture pair was bumped from .v0 to .v1 along with the other changes
      def prefix = "/content[archetype_id=openEHR-EHR-OBSERVATION.diff_observation_test.v1]/data[at0001]/events[at0002]/data[at0003]/items"

      // at0004: occurrences 0..1 -> 1..1, name changed
      def at0004 = findNode(diff, "${prefix}[at0004]")
      assert at0004
      assert at0004.status == 'modified'

      def occChange = at0004.fieldChanges.find { it.field == 'occurrences' }
      assert occChange
      assert occChange.oldValue == '[0..1]'
      assert occChange.newValue == '[1..1]'

      def nameChange = at0004.fieldChanges.find { it.field == 'text' }
      assert nameChange
      assert nameChange.oldValue == 'text node 1'
      assert nameChange.newValue == 'text node 1 changed name'

      // at0005: nested CCodePhrase gained code 'at0010'
      def codePhraseNode = findNode(diff, "${prefix}[at0005]/value/defining_code")
      assert codePhraseNode
      assert codePhraseNode.status == 'modified'

      def codeListChange = codePhraseNode.listChanges.find { it.field == 'codeList' }
      assert codeListChange
      assert codeListChange.added == ['at0010']
      assert codeListChange.removed == []

      // at0009: DV_QUANTITY gained '[in_i]' item, and 'cm'/'mm' items each gained a magnitude constraint
      def qtyNode = findNode(diff, "${prefix}[at0009]/value")
      assert qtyNode
      assert qtyNode.status == 'modified'

      def listChange = qtyNode.listChanges.find { it.field == 'list' }
      assert listChange
      assert listChange.added*.units == ['[in_i]']
      assert listChange.modified*.item.sort() == ['cm', 'mm']

      def cmChange = listChange.modified.find { it.item == 'cm' }
      def cmMagnitude = cmChange.changes.find { it.field == 'magnitude' }
      assert cmMagnitude
      assert cmMagnitude.oldValue == '(*..*)' // no constraint is the most open interval, not null
      assert cmMagnitude.newValue == '[0.0..*)'

      def mmChange = listChange.modified.find { it.item == 'mm' }
      assert mmChange.changes.find { it.field == 'magnitude' }

      // breaking-change detection: occurrences 0..1 -> 1..1 is a narrower constraint (a legacy
      // instance with 0 occurrences of at0004 is no longer valid)
      assert at0004.breaking
      def occBreak = diff.breakingChanges.find { it.category == 'occurrences_narrowed' && it.templatePath == at0004.templatePath }
      assert occBreak
      assert occBreak.certain
      assert occBreak.oldValue == '[0..1]'
      assert occBreak.newValue == '[1..1]'

      // codeList gaining 'at0010' only widens the valid set, not breaking
      assert !diff.breakingChanges.any { it.templatePath == codePhraseNode.templatePath }

      // a magnitude constraint appearing where there was none before narrows what's valid, but only for the data
      // with values outside of it: a potential breaking change, not a certain one
      def magBreaks = diff.breakingChanges.findAll { it.category == 'range_narrowed' && it.field == 'magnitude' }
      assert magBreaks.size() == 2 // cm and mm
      assert magBreaks.every { !it.certain }
      assert !qtyNode.breaking // 'breaking' is for certain breaking changes

      assert diff.root.breaking
   }

   // breaking changes: test_diff_v1.opt -> test_diff_v2.opt (same COMPOSITION/OBSERVATION archetypes bumped v1->v2)
   //  - at0004 (DV_TEXT "text node 1 changed name") removed entirely
   //  - at0011 (DV_TEXT "text node 2 new") added
   //  - at0005 (DV_CODED_TEXT): nested CCodePhrase codeList lost code 'at0007'
   //  - at0009 (DV_QUANTITY) untouched between v1 and v2, stays 'same'
   void testSemanticDiffBreakingChangesV1toV2()
   {
      def path1 = PS +"test_opt_diff"+ PS +"test_diff_v1.opt"
      def opt1 = loadAndParse(path1)

      def path2 = PS +"test_opt_diff"+ PS +"test_diff_v2.opt"
      def opt2 = loadAndParse(path2)

      def diffal = new SemanticOperationalTemplateDiffAlgorithm()
      def diff = diffal.diff(opt1, opt2)

      assert diff.root.status == 'modified'

      // removed subtrees keep the templatePath from the 'compared' (opt1/v1) side
      def prefix1 = "/content[archetype_id=openEHR-EHR-OBSERVATION.diff_observation_test.v1]/data[at0001]/events[at0002]/data[at0003]/items"
      // added/matched subtrees carry the templatePath from the 'to' (opt2/v2) side
      def prefix2 = "/content[archetype_id=openEHR-EHR-OBSERVATION.diff_observation_test.v2]/data[at0001]/events[at0002]/data[at0003]/items"

      // at0004 removed completely
      def at0004 = findNode(diff, "${prefix1}[at0004]")
      assert at0004
      assert at0004.status == 'removed'

      // at0011 added completely
      def at0011 = findNode(diff, "${prefix2}[at0011]")
      assert at0011
      assert at0011.status == 'added'
      assert at0011.text == 'text node 2 new'

      // at0005: nested CCodePhrase lost code 'at0007'
      def codePhraseNode = findNode(diff, "${prefix2}[at0005]/value/defining_code")
      assert codePhraseNode
      assert codePhraseNode.status == 'modified'

      def codeListChange = codePhraseNode.listChanges.find { it.field == 'codeList' }
      assert codeListChange
      assert codeListChange.added == []
      assert codeListChange.removed == ['at0007']

      // at0009 unchanged between v1 and v2
      def at0009 = findNode(diff, "${prefix2}[at0009]")
      assert at0009
      assert at0009.status == 'same'
      assert !at0009.breaking

      // at0004 was mandatory (1..1) and it is removed: all the legacy data has it, so it is a confirmed breaking change
      assert at0004.breaking
      def removedBreak = diff.breakingChanges.find { it.category == 'node_removed' && it.templatePath == at0004.templatePath }
      assert removedBreak
      assert removedBreak.certain

      // losing code 'at0007' from the valid set is certain breaking: legacy data using it would
      // fail validation under v2
      assert codePhraseNode.breaking
      def valueRemoved = diff.breakingChanges.find { it.category == 'value_removed' && it.templatePath == codePhraseNode.templatePath }
      assert valueRemoved
      assert valueRemoved.certain
      assert valueRemoved.oldValue == ['at0007']

      // the certain breaking change deep under at0005 bubbles .breaking up to root
      assert diff.root.breaking
   }

   // test_diff_2_v0.opt -> test_diff_2_v1.opt: a COMPOSITION with 2 sibling archetypes under
   // content - ADMIN_ENTRY (root nodeId at0000) and OBSERVATION (root nodeId at0000, SAME nodeId
   // as the ADMIN_ENTRY root). This exercises archetype-root matching by archetype_id rather than
   // nodeId (see findChildMatch): matching by nodeId alone would be ambiguous here since both
   // roots share 'at0000'.
   //  - ADMIN_ENTRY bumped v0 -> v1, and:
   //     - at0002 (DV_PROPORTION) gained 'type' (list constrained to [2], i.e. PERCENT) and
   //       'precision' (range constrained to [1..1]) constraints
   //     - at0003 (DV_DATE_TIME) gained a 'value' constraint with a fully-specified pattern
   //     - at0004 (DV_COUNT) gained a 'magnitude' constraint >= 0
   //     - at0005 (DV_IDENTIFIER) unchanged
   //     - at0006 (DV_ORDINAL) added entirely
   //  - OBSERVATION bumped v0 -> v1 with the same changes as the existing test_diff_v0/v1
   //    fixtures: at0004 occurrences+name change, at0005 codeList gains 'at0010', at0009 gains
   //    a '[in_i]' item and magnitude constraints on 'cm'/'mm'
   void testSemanticDiffMultiArchetypeCompositionV0toV1()
   {
      def path1 = PS +"test_opt_diff"+ PS +"test_diff_2_v0.opt"
      def opt1 = loadAndParse(path1)

      def path2 = PS +"test_opt_diff"+ PS +"test_diff_2_v1.opt"
      def opt2 = loadAndParse(path2)

      def diffal = new SemanticOperationalTemplateDiffAlgorithm()
      def diff = diffal.diff(opt1, opt2)

      assert diff.root.status == 'modified'

      // both archetype roots matched correctly despite sharing nodeId 'at0000'
      def contentAttr = diff.root.attributes['content']
      assert contentAttr
      assert contentAttr.children.size() == 2
      assert contentAttr.children*.rmTypeName.sort() == ['ADMIN_ENTRY', 'OBSERVATION']

      def adminPrefix = "/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]/items"
      def obsPrefix    = "/content[archetype_id=openEHR-EHR-OBSERVATION.diff_observation_test.v1]/data[at0001]/events[at0002]/data[at0003]/items"

      def adminRoot = findNode(diff, "/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]")
      assert adminRoot
      assert adminRoot.status == 'modified'
      def adminArchetypeIdChange = adminRoot.fieldChanges.find { it.field == 'archetypeId' }
      assert adminArchetypeIdChange
      assert adminArchetypeIdChange.oldValue == 'openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v0'
      assert adminArchetypeIdChange.newValue == 'openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1'

      // at0002: DV_PROPORTION had no attributes at all in v0, so 'type' (list -> [2]) and
      // 'precision' (range -> [1..1]) are whole new constraints, not modifications of existing ones
      def proportionNode = findNode(diff, "${adminPrefix}[at0002]/value")
      assert proportionNode
      assert proportionNode.status == 'modified'

      def typeAttr = proportionNode.attributes['type']
      assert typeAttr
      assert typeAttr.status == 'modified' // the RM defines the attribute, the OPT constrains it
      assert typeAttr.children[0].node2.item.list == [2]

      def precisionAttr = proportionNode.attributes['precision']
      assert precisionAttr
      assert precisionAttr.status == 'modified'
      def precisionRange = precisionAttr.children[0].node2.item.range
      assert precisionRange.lower == 1
      assert precisionRange.upper == 1

      // at0003: DV_DATE_TIME gained a whole new 'value' constraint (fully-specified pattern)
      def datetimeNode = findNode(diff, "${adminPrefix}[at0003]/value")
      assert datetimeNode
      assert datetimeNode.status == 'modified'
      def datetimeValueAttr = datetimeNode.attributes['value']
      assert datetimeValueAttr
      assert datetimeValueAttr.status == 'modified'
      assert datetimeValueAttr.children[0].node2.item.pattern == 'yyyy-mm-ddThh:mm:ss'

      // at0004: DV_COUNT gained a whole new 'magnitude' constraint (>= 0)
      def countNode = findNode(diff, "${adminPrefix}[at0004]/value")
      assert countNode
      assert countNode.status == 'modified'
      def countMagnitudeAttr = countNode.attributes['magnitude']
      assert countMagnitudeAttr
      assert countMagnitudeAttr.status == 'modified'
      def magnitudeRange = countMagnitudeAttr.children[0].node2.item.range
      assert magnitudeRange.lower == 0
      assert magnitudeRange.lowerIncluded == true
      assert magnitudeRange.upperUnbounded == true

      // at0005: DV_IDENTIFIER unchanged
      def identifierNode = findNode(diff, "${adminPrefix}[at0005]")
      assert identifierNode
      assert identifierNode.status == 'same'

      // at0006: DV_ORDINAL added entirely
      def ordinalNode = findNode(diff, "${adminPrefix}[at0006]")
      assert ordinalNode
      assert ordinalNode.status == 'added'
      assert ordinalNode.text == 'ordinal node 1'

      // OBSERVATION side mirrors the existing test_diff_v0/v1 fixture changes
      def obsRoot = findNode(diff, "/content[archetype_id=openEHR-EHR-OBSERVATION.diff_observation_test.v1]")
      assert obsRoot
      assert obsRoot.status == 'modified'

      def at0004 = findNode(diff, "${obsPrefix}[at0004]")
      assert at0004
      assert at0004.status == 'modified'
      def occChange = at0004.fieldChanges.find { it.field == 'occurrences' }
      assert occChange
      assert occChange.oldValue == '[0..1]'
      assert occChange.newValue == '[1..1]'
      def nameChange = at0004.fieldChanges.find { it.field == 'text' }
      assert nameChange
      assert nameChange.oldValue == 'text node 1'
      assert nameChange.newValue == 'text node 1 changed name'

      def codePhraseNode = findNode(diff, "${obsPrefix}[at0005]/value/defining_code")
      assert codePhraseNode
      assert codePhraseNode.status == 'modified'
      def codeListChange = codePhraseNode.listChanges.find { it.field == 'codeList' }
      assert codeListChange
      assert codeListChange.added == ['at0010']
      assert codeListChange.removed == []

      def qtyNode = findNode(diff, "${obsPrefix}[at0009]/value")
      assert qtyNode
      assert qtyNode.status == 'modified'
      def qtyListChange = qtyNode.listChanges.find { it.field == 'list' }
      assert qtyListChange
      assert qtyListChange.added*.units == ['[in_i]']
      assert qtyListChange.modified*.item.sort() == ['cm', 'mm']
   }

   // Nodes without node_id (e.g. /category, a DV_CODED_TEXT under a C_SINGLE_ATTRIBUTE)
   // get a synthetic text derived from the root's text (the template concept). Renaming
   // the template concept must not mark those unchanged nodes as modified.
   void testSemanticDiffNoNodeIdSyntheticTextIgnored()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")

      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      // the root changed (concept / archetypeId)
      assert diff.root.status == 'modified'
      assert diff.root.fieldChanges.find { it.field == 'archetypeId' }

      // /category and /category/defining_code have no node_id and are unchanged
      def category = findNode(diff, '/category')
      assert category
      assert !category.nodeId
      assert category.status == 'same'
      assert !category.fieldChanges.find { it.field == 'text' }

      def definingCode = findNode(diff, '/category/defining_code')
      assert definingCode
      assert definingCode.status == 'same'
      assert !definingCode.fieldChanges.find { it.field == 'text' }
   }

   // Synthetic text for nodes without node_id is the dotted attribute path, without the
   // template concept as prefix.
   void testSyntheticTextHasNoConceptPrefix()
   {
      def opt = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2.opt")

      def category = opt.definition.getAttr('category').children[0]
      assert category.text == 'category'

      def definingCode = category.getAttr('defining_code').children[0]
      assert definingCode.text == null // attributes of a DV_XXX have no text
      assert definingCode.description == null

      opt.nodes.each { path, node ->
         if (!node.nodeId && node.parent) assert !node.text?.startsWith(opt.concept)
      }

      // the children of DV_PROPORTION attributes have no text nor description, also after complete()
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")
      opt2.complete()
      def proportion = opt2.nodes.values().flatten().find { it.rmTypeName == 'DV_PROPORTION' }
      assert proportion.text
      assert proportion.attributes.size() == 4
      proportion.attributes.each { a ->
         a.children.each { c ->
            assert c.text == null, a.rmAttributeName
            assert c.description == null, a.rmAttributeName
         }
      }
   }

   // The RM gives existence/occurrences to DV_PROPORTION attributes that the old OPT doesn't
   // constrain: type is mandatory (1..1), precision is optional (0..1). Constraining them
   // explicitly is a change only if it differs from what the RM already implies.
   void testSemanticDiffImpliedRmExistenceAndOccurrences()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")

      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      def prop = '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]/items[at0002]/value'

      // type: mandatory in the RM, so explicit 1..1 is not a change nor a breaking change
      def type = findNode(diff, prop +'/type')
      assert type
      assert type.status == 'modified' // the C_INTEGER list constrains the RM default [0..4]
      assert !type.fieldChanges.find { it.field == 'occurrences' }
      assert !diff.breakingChanges.find { it.templatePath == prop +'/type' && it.certain } // only the potential one for the list [2]
      assert !diff.breakingChanges.find { it.templatePath == prop +'/type' && it.category == 'node_added_mandatory' }
      assert !diff.breakingChanges.find { it.templatePath == prop && it.field == 'type' }

      // precision: optional in the RM (0..1), explicit occurrences 1..1 is a (narrowing) change
      def precision = findNode(diff, prop +'/precision')
      assert precision
      def occ = precision.fieldChanges.find { it.field == 'occurrences' }
      assert occ
      assert occ.oldValue == '[0..1]'
      assert occ.newValue == '[1..1]'
      assert diff.breakingChanges.find { it.templatePath == prop +'/precision' && it.category == 'occurrences_narrowed' }
      assert !diff.breakingChanges.find { it.templatePath == prop +'/precision' && it.category == 'node_added_mandatory' }
   }

   // complete() adds the attributes that are mandatory in the RM and are not in the OPT,
   // and doesn't touch the ones that are already there.
   void testCompleteAddsRmMandatoryAttributes()
   {
      // old OPT: DV_PROPORTION without attribute constraints
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2.opt")
      def p1 = opt1.nodes.values().flatten().find { it.rmTypeName == 'DV_PROPORTION' }
      assert p1.rmTypeName == 'DV_PROPORTION'
      assert p1.attributes.size() == 0
      opt1.complete()

      ['numerator', 'denominator', 'type'].each { name ->
         def attr = p1.getAttr(name)
         assert attr, name
         assert attr.existence.lower == 1 && attr.existence.upper == 1
         assert attr.children.size() == 1
         assert attr.children[0].occurrences.lower == 1 && attr.children[0].occurrences.upper == 1
      }
      // injected primitives use the RM types, same as the parsed ones
      assert p1.getAttr('numerator').children[0].rmTypeName == 'Real'
      assert p1.getAttr('type').children[0].rmTypeName == 'Integer'
      // DV_PROPORTION.type is a PROPORTION_KIND enumeration: 0..4
      assert p1.getAttr('type').children[0].item.list == [0, 1, 2, 3, 4]
      assert p1.getAttr('numerator').children[0].item instanceof CReal
      assert p1.getAttr('type').children[0].item instanceof CInteger
      assert p1.getAttr('precision') == null // optional in the RM, not added

      // new OPT: type and precision are in the OPT, they should not be duplicated or changed
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")
      def p2 = opt2.nodes.values().flatten().find { it.rmTypeName == 'DV_PROPORTION' }
      opt2.complete()

      assert p2.attributes.findAll { it.rmAttributeName == 'type' }.size() == 1
      assert p2.getAttr('type').children[0].item.list == [2]
      assert p2.getAttr('precision').existence.lower == 0
      assert p2.getAttr('numerator') && p2.getAttr('denominator')
   }

   // OPTs spell primitive types INTEGER, the RM (BMM) Integer
   void testPrimitiveTypeSpellings()
   {
      assert Model.sameType('INTEGER', 'Integer')
      assert Model.sameType('Integer64', 'INTEGER')
      assert Model.sameType('DOUBLE', 'Real')
      assert Model.sameType('BigDecimal', 'REAL')
      assert Model.sameType('STRING', 'String')
      assert Model.sameType('DV_TEXT', 'DV_TEXT')
      assert !Model.sameType('INTEGER', 'REAL')
      assert !Model.sameType('INTEGER', 'STRING')
      assert !Model.sameType('DV_TEXT', 'dv_text') // only primitives are spelled in different ways
      assert Model.normalizePrimitiveType('INTEGER') == 'Integer'
      assert Model.normalizePrimitiveType('DV_TEXT') == 'DV_TEXT'
      assert Model.normalizePrimitiveType(null) == null
   }

   // The OPT has INTEGER, the model uses the RM type (Integer), and it is written back as INTEGER.
   void testPrimitiveTypesAreNormalizedOnParseAndSerialize()
   {
      def opt = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")
      def proportion = opt.nodes.values().flatten().find { it.rmTypeName == 'DV_PROPORTION' }

      assert proportion.getAttr('type').children[0].rmTypeName == 'Integer'
      assert proportion.getAttr('precision').children[0].rmTypeName == 'Integer'

      String xml = new com.cabolabs.openehr.opt.serializer.OptXmlSerializer(true).serialize(opt)
      assert xml.contains('<rm_type_name>INTEGER</rm_type_name>')
      assert !xml.contains('<rm_type_name>Integer</rm_type_name>')

      // parse and serialize again gives the same
      def opt2 = new OperationalTemplateParser().parse(xml)
      assert new com.cabolabs.openehr.opt.serializer.OptXmlSerializer(true).serialize(opt2) == xml
   }

   // Only the root of an added subtree can be mandatory for the legacy data: ADMIN_ENTRY items[at0006] is a new optional
   // ELEMENT (0..1) with a mandatory value (1..1), that is not a breaking change.
   void testNothingMandatoryBelowAnOptionalAddedNode()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")
      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      def element = '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]/items[at0006]'
      assert findNode(diff, element).status == 'added'
      assert !diff.breakingChanges.find { it.templatePath.startsWith(element) }
   }

   // test_diff_2_v2.opt is test_diff_2_v1.opt with the ELEMENT items[at0006] required (occurrences 1..1)
   //
   // v0 -> v2: the node is new and mandatory, legacy data can't have it, so it is a confirmed breaking change. Only the root
   // of the added subtree counts, its value (1..1) and the rest of the subtree add nothing.
   void testAddedRequiredNodeIsConfirmedBreaking()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v2.opt")
      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      def element = '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]/items[at0006]'

      def node = findNode(diff, element)
      assert node.status == 'added'
      assert node.breaking

      def underElement = diff.breakingChanges.findAll { it.templatePath.startsWith(element) }
      assert underElement.size() == 1

      def mandatory = underElement[0]
      assert mandatory.templatePath == element
      assert mandatory.category == 'node_added_mandatory'
      assert mandatory.certain
      assert mandatory.newValue == '[1..1]'

      // the value below it is part of the new subtree, not a change on its own
      assert !findNode(diff, element +'/value').breaking
   }

   // v1 -> v2: the node existed as optional (0..1) and is now required (1..1), occurrences narrowed (confirmed breaking),
   // and the same constraints are not reported again
   void testOptionalNodeMadeRequiredIsConfirmedBreaking()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v2.opt")
      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      def element = '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]/items[at0006]'

      def node = findNode(diff, element)
      assert node.status == 'modified'
      assert node.breaking
      assert node.fieldChanges.find { it.field == 'occurrences' && it.oldValue == '[0..1]' && it.newValue == '[1..1]' }

      assert diff.breakingChanges.size() == 1
      def bc = diff.breakingChanges[0]
      assert bc.templatePath == element
      assert bc.category == 'occurrences_narrowed'
      assert bc.certain
   }

   // v2 (items[at0006] required, 1..1) -> v3_removed_mandatory (items[at0006] removed): all the legacy data has the node, so
   // removing it is a confirmed breaking change, reported only at the removed node, not below it
   void testRemovedMandatoryNodeIsConfirmedBreaking()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v2.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v3_removed_mandatory.opt")
      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      def element = '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]/items[at0006]'

      def node = findNode(diff, element)
      assert node.status == 'removed'
      assert node.breaking
      assert !findNode(diff, element +'/value').breaking

      assert diff.breakingChanges.size() == 1
      def bc = diff.breakingChanges[0]
      assert bc.templatePath == element
      assert bc.category == 'node_removed'
      assert bc.certain
   }

   // v1 (items[at0006] optional, 0..1) -> v3_removed_optional (items[at0006] removed): only the data that has the node is
   // affected, a potential breaking change to check against the data, reported only at the removed node
   void testRemovedOptionalNodeIsPotentialBreaking()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v3_removed_optional.opt")
      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      def element = '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]/items[at0006]'

      def node = findNode(diff, element)
      assert node.status == 'removed'
      assert !node.breaking // no confirmed breaking change, the potential one needs the data
      assert !findNode(diff, element +'/value').breaking

      assert diff.breakingChanges.size() == 1
      def bc = diff.breakingChanges[0]
      assert bc.templatePath == element
      assert bc.category == 'node_removed'
      assert !bc.certain
   }

   // A constraint over a value that the old OPT didn't constrain is a potential breaking change (certain = false), it
   // breaks only the legacy data with values outside of the new constraint. The RM default is the baseline.
   void testConstraintsOverRmDefaultsArePotentialBreakingChanges()
   {
      def opt1 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2.opt")
      def opt2 = loadAndParse(PS +"opts"+ PS +"diff"+ PS +"test_diff_2_v1.opt")
      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(opt1, opt2)

      def admin = '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.diff_test_admin.v1]/data[at0001]'
      def bc = { String path, String category -> diff.breakingChanges.find { it.templatePath == path && it.category == category } }

      // DV_PROPORTION.type: from the RM enumeration 0..4 to 2
      def type = bc(admin +'/items[at0002]/value/type', 'constraint_added')
      assert type && !type.certain
      assert type.oldValue == [0, 1, 2, 3, 4]
      assert type.newValue == [2]
      def typeChange = findNode(diff, admin +'/items[at0002]/value/type').fieldChanges.find { it.field == 'list' }
      assert typeChange.oldValue == [0, 1, 2, 3, 4] && typeChange.newValue == [2]

      // DV_PROPORTION.precision: the range of the value is a new constraint, the occurrences narrowing is certain
      def precision = bc(admin +'/items[at0002]/value/precision', 'constraint_added')
      assert precision && !precision.certain
      assert precision.newValue == '[1..1]'
      assert bc(admin +'/items[at0002]/value/precision', 'occurrences_narrowed').certain

      // DV_COUNT.magnitude (Integer: any value) to [0..*)
      def magnitude = bc(admin +'/items[at0004]/value/magnitude', 'constraint_added')
      assert magnitude && !magnitude.certain
      assert magnitude.newValue == '[0..*)'

      // DV_DATE_TIME.value to a full date time
      def pattern = bc(admin +'/items[at0003]/value/value', 'constraint_added')
      assert pattern && !pattern.certain
      assert pattern.newValue == 'yyyy-mm-ddThh:mm:ss'

      // DV_QUANTITY cm and mm had no range for the magnitude: potential too
      def obs = '/content[archetype_id=openEHR-EHR-OBSERVATION.diff_observation_test.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0009]/value'
      ['cm', 'mm'].each { unit ->
         def r = bc(obs +"[units=${unit}]", 'range_narrowed')
         assert r && !r.certain
      }

      // structural narrowing stays certain: ELEMENT optional to mandatory
      def element = diff.breakingChanges.find { it.category == 'occurrences_narrowed' && it.templatePath.endsWith('items[at0004]') }
      assert element.certain
   }

   def findNode(SemanticOperationalTemplateDiff diff, String templatePath)
   {
      findNodeRecursive(diff.root, templatePath)
   }

   def findNodeRecursive(SemanticNodeDiff node, String templatePath)
   {
      if (node.templatePath == templatePath) return node

      def found
      node.attributes.each { name, attrDiff ->
         attrDiff.children.each { child ->
            if (!found) found = findNodeRecursive(child, templatePath)
         }
      }
      return found
   }
}
