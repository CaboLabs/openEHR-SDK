package com.cabolabs.openehr.opt.diff

import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.AttributeNode
import com.cabolabs.openehr.opt.model.ArchetypeSlot
import com.cabolabs.openehr.opt.model.PrimitiveObjectNode
import com.cabolabs.openehr.opt.model.domain.CCodePhrase
import com.cabolabs.openehr.opt.model.domain.CDvQuantity
import com.cabolabs.openehr.opt.model.domain.CDvOrdinal
import com.cabolabs.openehr.opt.model.primitive.CInteger
import com.cabolabs.openehr.opt.model.primitive.CReal
import com.cabolabs.openehr.opt.model.primitive.CBoolean
import com.cabolabs.openehr.opt.model.primitive.CString
import com.cabolabs.openehr.opt.model.primitive.CDate
import com.cabolabs.openehr.opt.model.primitive.CDateTime
import com.cabolabs.openehr.opt.model.primitive.CTime
import com.cabolabs.openehr.opt.model.primitive.CDuration

/**
 * Detailed semantic OPT diff: traverses both OPTs' object trees in parallel (instead of
 * reconstructing hierarchy from split template path strings, like OperationalTemplateDiffAlgorithm
 * does) and reports field-level constraint changes on each matched node, in addition to
 * added/removed/same/modified status. Attributes are matched by rmAttributeName (an ObjectNode
 * can't constrain the same RM attribute twice). Children under a matched attribute are matched by:
 *  - archetype_id (ignoring its trailing version) for archetype-root nodes, so an archetype
 *    version bump is still recognized as the same slot, while a specialization swap (a different
 *    archetype concept, e.g. "request-lab" replacing "request") is correctly seen as removed+added;
 *  - nodeId otherwise (unique among alternatives under one attribute, and stable across archetype
 *    version changes since plain node paths never reference archetype_id);
 *  - position, as a last resort, when a node has neither (primitives, PATHABLE-nested nodes).
 *
 * This is a separate algorithm from OperationalTemplateDiffAlgorithm, which is left untouched.
 */
class SemanticOperationalTemplateDiffAlgorithm {

   // accumulates every breaking change found during one diff() run; reset at the start of diff()
   private List<BreakingChange> breakingChanges

   SemanticOperationalTemplateDiff diff(OperationalTemplate opt1, OperationalTemplate opt2)
   {
      breakingChanges = []

      def metadataChanges = []
      compareField(metadataChanges, 'templateId', opt1.templateId, opt2.templateId)
      compareField(metadataChanges, 'concept', opt1.concept, opt2.concept)
      compareField(metadataChanges, 'language', opt1.language, opt2.language)
      compareField(metadataChanges, 'purpose', opt1.purpose, opt2.purpose)
      compareField(metadataChanges, 'isControlled', opt1.isControlled, opt2.isControlled)

      def root = compareObjectNodes(opt1.definition, opt2.definition)

      return new SemanticOperationalTemplateDiff(
         compared: opt1,
         to: opt2,
         templateMetadataChanges: metadataChanges,
         root: root,
         breakingChanges: breakingChanges
      )
   }

   private SemanticNodeDiff compareObjectNodes(ObjectNode n1, ObjectNode n2)
   {
      if (n1 == null) return buildSubtree(n2, 'added')
      if (n2 == null) return buildSubtree(n1, 'removed')

      def path = n2.templatePath ?: n1.templatePath
      def sizeBefore = breakingChanges.size()

      def fieldChanges = []
      def listChanges = []

      compareField(fieldChanges, 'name', n1.text, n2.text)
      compareField(fieldChanges, 'rmTypeName', n1.rmTypeName, n2.rmTypeName)
      compareField(fieldChanges, 'type', n1.type, n2.type)
      compareIntervalField(fieldChanges, path, 'occurrences', n1.occurrences, n2.occurrences)
      compareField(fieldChanges, 'archetypeId', n1.archetypeId, n2.archetypeId)

      compareTypeSpecific(n1, n2, path, fieldChanges, listChanges)

      def ownBreaking = breakingChanges.subList(sizeBefore, breakingChanges.size()).any { it.certain }

      def attributes = matchAttributes(n1, n2)

      def childrenChanged = attributes.values().any { it.status != 'same' }
      def childrenBreaking = attributes.values().any { it.breaking }

      return new SemanticNodeDiff(
         templatePath: n2.templatePath,
         nodeId:       n2.nodeId ?: n1.nodeId,
         rmTypeName:   n2.rmTypeName,
         type:         n2.type,
         name:         n2.text,
         status:       (fieldChanges || listChanges || childrenChanged) ? 'modified' : 'same',
         fieldChanges: fieldChanges,
         listChanges:  listChanges,
         attributes:   attributes,
         breaking:     ownBreaking || childrenBreaking,
         node1:        n1,
         node2:        n2
      )
   }

   // builds a whole-subtree diff for a node that only exists on one side (status is 'added' or 'removed')
   private SemanticNodeDiff buildSubtree(ObjectNode n, String status)
   {
      // a node newly added whose occurrences don't allow 0 is mandatory: legacy data, which never
      // had this node, can no longer be valid. A removed node isn't breaking for stored data by
      // itself (that data still has the value, it's just not re-validated against it going
      // forward), but it can break a stored query that references this path.
      def mandatory = (status == 'added' && n.occurrences != null && !n.occurrences.has(0))

      if (mandatory)
      {
         addBreaking('node_added_mandatory', n.templatePath, 'occurrences', null, intervalStr(n.occurrences),
            'new node is mandatory (occurrences does not allow 0); legacy data lacks it', true)
      }
      else if (status == 'removed')
      {
         addBreaking('node_removed', n.templatePath, null, n.nodeId, null,
            'node removed; verify no stored queries reference this path', false)
      }

      def attributes = [:]

      n.attributes.each { attr ->
         def children = attr.children.collect { buildSubtree(it, status) }
         def attrMandatory = (status == 'added' && attr.existence != null && !attr.existence.has(0))

         if (attrMandatory)
         {
            addBreaking('attribute_added_mandatory', n.templatePath, attr.rmAttributeName, null, intervalStr(attr.existence),
               'new attribute is mandatory (existence does not allow 0); legacy data lacks it', true)
         }

         attributes[attr.rmAttributeName] = new AttributeDiff(
            rmAttributeName: attr.rmAttributeName,
            status: status,
            children: children,
            breaking: attrMandatory || children.any { it.breaking }
         )
      }

      return new SemanticNodeDiff(
         templatePath: n.templatePath,
         nodeId:       n.nodeId,
         rmTypeName:   n.rmTypeName,
         type:         n.type,
         name:         n.text,
         status:       status,
         attributes:   attributes,
         breaking:     mandatory || attributes.values().any { it.breaking },
         node1:        status == 'removed' ? n : null,
         node2:        status == 'added'   ? n : null
      )
   }

   private Map<String, AttributeDiff> matchAttributes(ObjectNode n1, ObjectNode n2)
   {
      def path = n2.templatePath ?: n1.templatePath
      def names = (n1.attributes*.rmAttributeName + n2.attributes*.rmAttributeName).unique()

      def result = [:]
      names.each { name ->
         def a1 = n1.attributes.find { it.rmAttributeName == name }
         def a2 = n2.attributes.find { it.rmAttributeName == name }
         result[name] = compareAttributeNodes(path, name, a1, a2)
      }
      return result
   }

   private AttributeDiff compareAttributeNodes(String path, String name, AttributeNode a1, AttributeNode a2)
   {
      if (a1 == null)
      {
         def children = a2.children.collect { buildSubtree(it, 'added') }
         def mandatory = (a2.existence != null && !a2.existence.has(0))

         if (mandatory)
         {
            addBreaking('attribute_added_mandatory', path, name, null, intervalStr(a2.existence),
               'new attribute is mandatory (existence does not allow 0); legacy data lacks it', true)
         }

         return new AttributeDiff(
            rmAttributeName: name,
            status: 'added',
            children: children,
            breaking: mandatory || children.any { it.breaking }
         )
      }

      if (a2 == null)
      {
         def children = a1.children.collect { buildSubtree(it, 'removed') }

         addBreaking('attribute_removed', path, name, name, null,
            'attribute removed; verify no stored queries reference this path', false)

         return new AttributeDiff(
            rmAttributeName: name,
            status: 'removed',
            children: children,
            breaking: children.any { it.breaking }
         )
      }

      def sizeBefore = breakingChanges.size()

      def fieldChanges = []
      compareField(fieldChanges, 'cardinality.isOrdered', a1.cardinality?.isOrdered, a2.cardinality?.isOrdered)
      compareField(fieldChanges, 'cardinality.isUnique', a1.cardinality?.isUnique, a2.cardinality?.isUnique)
      compareIntervalField(fieldChanges, path, 'cardinality.interval', a1.cardinality?.interval, a2.cardinality?.interval)
      compareIntervalField(fieldChanges, path, 'existence', a1.existence, a2.existence)

      def ownBreaking = breakingChanges.subList(sizeBefore, breakingChanges.size()).any { it.certain }

      def children = matchChildren(a1.children, a2.children)

      def childrenChanged = children.any { it.status != 'same' }
      def childrenBreaking = children.any { it.breaking }

      return new AttributeDiff(
         rmAttributeName: name,
         status: (fieldChanges || childrenChanged) ? 'modified' : 'same',
         fieldChanges: fieldChanges,
         children: children,
         breaking: ownBreaking || childrenBreaking
      )
   }

   // matches children under one attribute. Archetype-root nodes (archetypeId set) are matched by
   // archetype_id ignoring the trailing version ("openEHR-EHR-OBSERVATION.foo.v1" == "...v2"),
   // since a template update can bump an archetype's version while its root nodeId stays the
   // generic 'at0000' - matching those by nodeId alone would pair up unrelated sibling archetypes
   // under a C_MULTIPLE_ATTRIBUTE (e.g. COMPOSITION.content) that happen to share that root code.
   // A specialized archetype (e.g. "request-lab" replacing "request") has a different concept in
   // its archetype_id, so it naturally falls through as removed+added instead of being matched.
   // Non-root nodes (no archetypeId) keep matching by nodeId, which is unique among alternatives
   // under one attribute and stable across archetype versions since plain node paths never
   // reference archetype_id. Nodes with neither (primitives, PATHABLE-nested nodes) fall back to
   // positional matching against the first remaining node that also has no nodeId.
   private List<SemanticNodeDiff> matchChildren(List c1, List c2)
   {
      def result = []
      def c2Remaining = new ArrayList(c2)

      c1.each { n1 ->
         def match = findChildMatch(n1, c2Remaining)

         if (match)
         {
            c2Remaining.remove(match)
            result << compareObjectNodes(n1, match)
         }
         else
         {
            result << buildSubtree(n1, 'removed')
         }
      }

      c2Remaining.each { n2 ->
         result << buildSubtree(n2, 'added')
      }

      return result
   }

   private ObjectNode findChildMatch(ObjectNode n1, List<ObjectNode> c2Remaining)
   {
      if (n1.archetypeId)
      {
         def key1 = archetypeIdNoVersion(n1.archetypeId)
         return c2Remaining.find { it.archetypeId && archetypeIdNoVersion(it.archetypeId) == key1 }
      }

      return n1.nodeId ?
         c2Remaining.find { it.nodeId == n1.nodeId } :
         c2Remaining.find { !it.nodeId }
   }

   private String archetypeIdNoVersion(String archetypeId)
   {
      archetypeId.replaceAll(/\.v\d+$/, '')
   }

   // ---- type-specific comparators ----

   private void compareTypeSpecific(ObjectNode n1, ObjectNode n2, String path, List fieldChanges, List listChanges)
   {
      if (n1 instanceof CCodePhrase && n2 instanceof CCodePhrase)
      {
         compareCCodePhrase(n1, n2, path, fieldChanges, listChanges)
      }
      else if (n1 instanceof CDvQuantity && n2 instanceof CDvQuantity)
      {
         compareCDvQuantity(n1, n2, path, fieldChanges, listChanges)
      }
      else if (n1 instanceof CDvOrdinal && n2 instanceof CDvOrdinal)
      {
         compareCDvOrdinal(n1, n2, path, fieldChanges, listChanges)
      }
      else if (n1 instanceof ArchetypeSlot && n2 instanceof ArchetypeSlot)
      {
         compareArchetypeSlot(n1, n2, fieldChanges)
      }
      else if (n1 instanceof PrimitiveObjectNode && n2 instanceof PrimitiveObjectNode)
      {
         comparePrimitiveObjectNode(n1, n2, path, fieldChanges, listChanges)
      }
      // else: generic ObjectNode, or the two sides are different constraint subtypes -
      // the 'type'/'rmTypeName' FieldChanges already added above capture that the
      // constraint kind itself changed; there's nothing meaningful to structurally
      // diff across two different constraint kinds.
   }

   private void compareCCodePhrase(CCodePhrase n1, CCodePhrase n2, String path, List fieldChanges, List listChanges)
   {
      compareField(fieldChanges, 'terminologyId', n1.terminologyId, n2.terminologyId)
      compareField(fieldChanges, 'terminologyRef', n1.terminologyRef, n2.terminologyRef)
      compareField(fieldChanges, 'reference', n1.reference, n2.reference)

      def added = n2.codeList - n1.codeList
      def removed = n1.codeList - n2.codeList

      if (added || removed)
      {
         listChanges << new ListChange(field: 'codeList', added: added, removed: removed)

         if (removed)
         {
            addBreaking('value_removed', path, 'codeList', removed, null,
               "code(s) ${removed} removed from the valid set; legacy data using them would fail validation", true)
         }
      }
   }

   private void compareCDvQuantity(CDvQuantity n1, CDvQuantity n2, String path, List fieldChanges, List listChanges)
   {
      compareField(fieldChanges, 'property', codePhraseStr(n1.property), codePhraseStr(n2.property))

      def list1 = n1.list ?: []
      def list2 = n2.list ?: []

      def units1 = list1*.units
      def units2 = list2*.units

      def added = list2.findAll { !(it.units in units1) }
      def removed = list1.findAll { !(it.units in units2) }
      def modified = []

      units1.intersect(units2).each { u ->
         def i1 = list1.find { it.units == u }
         def i2 = list2.find { it.units == u }
         def itemChanges = []
         compareIntervalField(itemChanges, "${path}[units=${u}]", 'magnitude', i1.magnitude, i2.magnitude)
         compareIntervalField(itemChanges, "${path}[units=${u}]", 'precision', i1.precision, i2.precision)
         if (itemChanges) modified << new ListItemChange(item: u, changes: itemChanges)
      }

      if (added || removed || modified)
      {
         listChanges << new ListChange(field: 'list', added: added, removed: removed, modified: modified)

         if (removed)
         {
            addBreaking('value_removed', path, 'list', removed*.units, null,
               "unit(s) ${removed*.units} removed from the valid set; legacy data using them would fail validation", true)
         }
      }
   }

   private void compareCDvOrdinal(CDvOrdinal n1, CDvOrdinal n2, String path, List fieldChanges, List listChanges)
   {
      def list1 = n1.list ?: []
      def list2 = n2.list ?: []

      def values1 = list1*.value
      def values2 = list2*.value

      def added = list2.findAll { !(it.value in values1) }
      def removed = list1.findAll { !(it.value in values2) }
      def modified = []

      values1.intersect(values2).each { v ->
         def i1 = list1.find { it.value == v }
         def i2 = list2.find { it.value == v }
         def itemChanges = []
         compareField(itemChanges, 'symbol', codePhraseStr(i1.symbol), codePhraseStr(i2.symbol))
         if (itemChanges) modified << new ListItemChange(item: v, changes: itemChanges)
      }

      if (added || removed || modified)
      {
         listChanges << new ListChange(field: 'list', added: added, removed: removed, modified: modified)

         if (removed)
         {
            addBreaking('value_removed', path, 'list', removed*.value, null,
               "ordinal value(s) ${removed*.value} removed from the valid set; legacy data using them would fail validation", true)
         }
      }
   }

   private void compareArchetypeSlot(ArchetypeSlot n1, ArchetypeSlot n2, List fieldChanges)
   {
      compareField(fieldChanges, 'includes', n1.includes, n2.includes)
      compareField(fieldChanges, 'excludes', n1.excludes, n2.excludes)
   }

   private void comparePrimitiveObjectNode(PrimitiveObjectNode n1, PrimitiveObjectNode n2, String path, List fieldChanges, List listChanges)
   {
      def i1 = n1.item
      def i2 = n2.item

      if (i1 == null || i2 == null || i1.class != i2.class)
      {
         compareField(fieldChanges, 'primitiveType', i1?.class?.simpleName, i2?.class?.simpleName)
         return
      }

      if (i1 instanceof CInteger)
      {
         compareIntervalField(fieldChanges, path, 'range', i1.range, i2.range)
         def added = i2.list - i1.list
         def removed = i1.list - i2.list
         if (added || removed)
         {
            listChanges << new ListChange(field: 'list', added: added, removed: removed)
            if (removed)
            {
               addBreaking('value_removed', path, 'list', removed, null,
                  "value(s) ${removed} removed from the valid set; legacy data using them would fail validation", true)
            }
         }
      }
      else if (i1 instanceof CReal)
      {
         compareIntervalField(fieldChanges, path, 'range', i1.range, i2.range)
      }
      else if (i1 instanceof CBoolean)
      {
         compareField(fieldChanges, 'trueValid', i1.trueValid, i2.trueValid)
         compareField(fieldChanges, 'falseValid', i1.falseValid, i2.falseValid)

         if (i1.trueValid && !i2.trueValid)
         {
            addBreaking('value_removed', path, 'trueValid', true, false,
               "'true' no longer a valid value; legacy data with value true would fail validation", true)
         }
         if (i1.falseValid && !i2.falseValid)
         {
            addBreaking('value_removed', path, 'falseValid', true, false,
               "'false' no longer a valid value; legacy data with value false would fail validation", true)
         }
      }
      else if (i1 instanceof CString)
      {
         compareField(fieldChanges, 'pattern', i1.pattern, i2.pattern)
         def added = i2.list - i1.list
         def removed = i1.list - i2.list
         if (added || removed)
         {
            listChanges << new ListChange(field: 'list', added: added, removed: removed)
            if (removed)
            {
               addBreaking('value_removed', path, 'list', removed, null,
                  "value(s) ${removed} removed from the valid set; legacy data using them would fail validation", true)
            }
         }
      }
      else if (i1 instanceof CDuration)
      {
         compareField(fieldChanges, 'pattern', i1.pattern, i2.pattern)
         compareIntervalField(fieldChanges, path, 'range', i1.range, i2.range)
      }
      else if (i1 instanceof CDate || i1 instanceof CDateTime || i1 instanceof CTime)
      {
         compareField(fieldChanges, 'pattern', i1.pattern, i2.pattern)
      }
   }

   // ---- helpers ----

   private void compareField(List fieldChanges, String field, def v1, def v2)
   {
      if (v1 != v2)
      {
         fieldChanges << new FieldChange(field: field, oldValue: v1, newValue: v2)
      }
   }

   // compares two Interval-like constraints (IntervalInt / IntervalBigDecimal / IntervalDuration,
   // duck-typed on lower/upper/lowerIncluded/upperIncluded/lowerUnbounded/upperUnbounded).
   // Records a FieldChange when they differ, and a BreakingChange when the new interval is
   // narrower than the old one - the case where legacy data conforming to the old OPT can be
   // invalid under the new OPT.
   private void compareIntervalField(List fieldChanges, String path, String field, def i1, def i2)
   {
      def s1 = intervalStr(i1)
      def s2 = intervalStr(i2)

      if (s1 == s2) return

      fieldChanges << new FieldChange(field: field, oldValue: s1, newValue: s2)

      if (isNarrower(i1, i2))
      {
         addBreaking(narrowedCategory(field), path, field, s1, s2,
            'constraint narrowed; values previously valid under the old OPT may now be invalid', true)
      }
   }

   private String narrowedCategory(String field)
   {
      switch (field)
      {
         case 'occurrences':          return 'occurrences_narrowed'
         case 'existence':            return 'existence_narrowed'
         case 'cardinality.interval': return 'cardinality_narrowed'
         default:                     return 'range_narrowed' // range, magnitude, precision
      }
   }

   // true when i2 (new) accepts a strict subset of what i1 (old) accepted: a tighter lower
   // bound, a tighter upper bound, or a bound flipping from included to excluded at the same
   // value. A brand-new constraint where none existed before (i1 == null) counts as narrowing
   // too, since previously-unconstrained values are now checked against it. A constraint that's
   // dropped entirely (i2 == null) is a widening, never breaking.
   private boolean isNarrower(def i1, def i2)
   {
      if (i2 == null) return false
      if (i1 == null) return true

      boolean lowerTighter = false
      if (!i2.lowerUnbounded)
      {
         if (i1.lowerUnbounded) lowerTighter = true
         else
         {
            def cmp = i2.lower <=> i1.lower
            if (cmp > 0) lowerTighter = true
            else if (cmp == 0 && i1.lowerIncluded && !i2.lowerIncluded) lowerTighter = true
         }
      }

      boolean upperTighter = false
      if (!i2.upperUnbounded)
      {
         if (i1.upperUnbounded) upperTighter = true
         else
         {
            def cmp = i2.upper <=> i1.upper
            if (cmp < 0) upperTighter = true
            else if (cmp == 0 && i1.upperIncluded && !i2.upperIncluded) upperTighter = true
         }
      }

      return lowerTighter || upperTighter
   }

   private void addBreaking(String category, String path, String field, def oldValue, def newValue, String reason, boolean certain)
   {
      breakingChanges << new BreakingChange(
         templatePath: path,
         category: category,
         field: field,
         oldValue: oldValue,
         newValue: newValue,
         reason: reason,
         certain: certain
      )
   }

   // works for IntervalInt, IntervalBigDecimal and IntervalDuration alike (duck typing on the
   // shared lowerIncluded/upperIncluded/lowerUnbounded/upperUnbounded/lower/upper fields);
   // built by hand instead of relying on each Interval*.toString() since those omit the
   // included/unbounded flags, which would hide a real constraint change (e.g. 0..1 vs (0..1)).
   private String intervalStr(def interval)
   {
      if (interval == null) return null

      def lo = interval.lowerUnbounded ? '*' : interval.lower
      def hi = interval.upperUnbounded ? '*' : interval.upper
      def lb = interval.lowerIncluded ? '[' : '('
      def ub = interval.upperIncluded ? ']' : ')'

      return "${lb}${lo}..${hi}${ub}"
   }

   private String codePhraseStr(def cp)
   {
      cp ? "${cp.terminologyId}::${cp.codeString}" : null
   }
}
