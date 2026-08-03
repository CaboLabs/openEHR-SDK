package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.model.datatypes.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.diff.*

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

      def path2 = PS +"test_opt_diff"+ PS +"test diff v1.opt"
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

      def nameChange = at0004.fieldChanges.find { it.field == 'name' }
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
      assert cmMagnitude.oldValue == null
      assert cmMagnitude.newValue == '[0.0..*)'

      def mmChange = listChange.modified.find { it.item == 'mm' }
      assert mmChange.changes.find { it.field == 'magnitude' }
   }

   // breaking changes: test diff v1.opt -> test diff v2.opt (same COMPOSITION/OBSERVATION archetypes bumped v1->v2)
   //  - at0004 (DV_TEXT "text node 1 changed name") removed entirely
   //  - at0011 (DV_TEXT "text node 2 new") added
   //  - at0005 (DV_CODED_TEXT): nested CCodePhrase codeList lost code 'at0007'
   //  - at0009 (DV_QUANTITY) untouched between v1 and v2, stays 'same'
   void testSemanticDiffBreakingChangesV1toV2()
   {
      def path1 = PS +"test_opt_diff"+ PS +"test diff v1.opt"
      def opt1 = loadAndParse(path1)

      def path2 = PS +"test_opt_diff"+ PS +"test diff v2.opt"
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
      assert at0011.name == 'text node 2 new'

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
      assert typeAttr.status == 'added'
      assert typeAttr.children[0].node2.item.list == [2]

      def precisionAttr = proportionNode.attributes['precision']
      assert precisionAttr
      assert precisionAttr.status == 'added'
      def precisionRange = precisionAttr.children[0].node2.item.range
      assert precisionRange.lower == 1
      assert precisionRange.upper == 1

      // at0003: DV_DATE_TIME gained a whole new 'value' constraint (fully-specified pattern)
      def datetimeNode = findNode(diff, "${adminPrefix}[at0003]/value")
      assert datetimeNode
      assert datetimeNode.status == 'modified'
      def datetimeValueAttr = datetimeNode.attributes['value']
      assert datetimeValueAttr
      assert datetimeValueAttr.status == 'added'
      assert datetimeValueAttr.children[0].node2.item.pattern == 'yyyy-mm-ddThh:mm:ss'

      // at0004: DV_COUNT gained a whole new 'magnitude' constraint (>= 0)
      def countNode = findNode(diff, "${adminPrefix}[at0004]/value")
      assert countNode
      assert countNode.status == 'modified'
      def countMagnitudeAttr = countNode.attributes['magnitude']
      assert countMagnitudeAttr
      assert countMagnitudeAttr.status == 'added'
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
      assert ordinalNode.name == 'ordinal node 1'

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
      def nameChange = at0004.fieldChanges.find { it.field == 'name' }
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
