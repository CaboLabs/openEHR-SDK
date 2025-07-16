package com.cabolabs.openehr.opt.opt_generator

import org.openehr.am.archetype.Archetype
import org.openehr.am.archetype.constraintmodel.*
import org.openehr.rm.support.basic.Interval
import org.openehr.rm.datatypes.quantity.datetime.DvDuration

import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.model.datatypes.*

// TODO: slots and internal constraint refs
class AdlToOpt {

   def language

   // classes that are not LOCATABLE, so don't have node_id in the path of the C_OBJECT
   def pathables = [
     'EVENT_CONTEXT',
     'ISM_TRANSITION',
     'INSTRUCTION_DETAILS'
   ]

   // for building "data paths" (it's really alternative template paths not "data" pahts)
   def pathCounter = [:]

   AdlToOpt()
   {
      String.metaClass.static.uuid = { ->
         java.util.UUID.randomUUID().toString()
      }
   }

   void useLanguage(String language)
   {
      this.language = language
   }

   OperationalTemplate generateOpt(Archetype archetype)
   {
      // check the archetype is top level
      if (!['COMPOSITION', 'FOLDER', 'EHR_STATUS', 'PERSON', 'ORGANISATION', 'AGENT', 'GROUP', 'ROLE', 'PARTY_RELATIONSHIP'].contains(archetype.definition.rmTypeName))
      {
         throw new Exception("The archetype is not a top level archetype, it is: " + archetype.definition.rmTypeName)
      }

      if (!language)
      {
         language = archetype.getOriginalLanguage().getCodeString()
      }

      println language

      println archetype.getConceptName(language)

      def template = new OperationalTemplate(
         uid:        String.uuid(),
         templateId: archetype.getConceptName(language),
         concept:    archetype.getConcept(), // at0000
         language:   'ISO_639-1::'+ language, // This is how the stored OPT parses the language
         purpose:    archetype.getDescription().getDetails().get(language)?.getPurpose()
      )

      generateOptDefinition(template, archetype)

      return template
   }

   void generateOptDefinition(OperationalTemplate template, Archetype archetype)
   {
      template.definition = processObjectNode(template, archetype, archetype.definition, '/', '/', '/', '/')

      // TODO: process ontolofy.term_definitions into objectNode.termDefinitions
      // NOTE: this is done at the root because the ontology is global to the archetype, later when we process a tree of archetypes resolving the slots, we will need to process this at each archetype root.
      // node.term_definitions.each { tdef ->

      //    obn.termDefinitions << parseCodedTerm(tdef)
      // }
   }

   def processObjectNode(OperationalTemplate template, Archetype archetype, CComplexObject node, String parentPath, String path, String dataPath, String templateDataPath)
   {
      Map paths = calculatePaths(node, parentPath, path, dataPath, templateDataPath)

      def obn = new ObjectNode(
         owner:            template,
         rmTypeName:       node.rmTypeName,
         nodeId:           node.nodeId,
         type:             classToRm(node.getClass().getSimpleName()),
         templatePath:     paths.templatePath,
         path:             paths.path,
         dataPath:         paths.dataPath,
         templateDataPath: paths.templateDataPath,
         occurrences:      adlToOptIntervalInt(node.occurrences),
      )

      // if it's an archetype root, then add the archetype ID
      if (path == '/')
      {
         obn.archetypeId = archetype.getArchetypeId().getValue()
      }

      // List<CAttribute>
      node.attributes.each { cAttribute ->

         obn.attributes << processAttributeNode(
            template,
            archetype,
            obn,
            cAttribute,
            paths.templatePath,
            paths.path,
            paths.dataPath,
            paths.templateDataPath
         )
      }

      return obn
   }

   def processObjectNode(OperationalTemplate template, Archetype archetype, org.openehr.am.openehrprofile.datatypes.text.CCodePhrase node, String parentPath, String path, String dataPath, String templateDataPath)
   {
      Map paths = calculatePaths(node, parentPath, path, dataPath, templateDataPath)

      //println "CODE LIST: "+ node.codeList

      def obn = new com.cabolabs.openehr.opt.model.domain.CCodePhrase(
         owner:            template,
         rmTypeName:       node.rmTypeName,
         nodeId:           node.nodeId,
         type:             classToRm(node.getClass().getSimpleName()),
         archetypeId:      archetype.getArchetypeId().getValue(),
         templatePath:     paths.parentPath,
         path:             paths.path,
         dataPath:         paths.dataPath,
         templateDataPath: paths.templateDataPath,
         occurrences:      adlToOptIntervalInt(node.occurrences),

         terminologyId:    node.terminologyId.value
      )

      if (node.codeList)
      {
         node.codeList.each { code ->

            obn.codeList << code
         }
      }

      println obn

      return obn
   }
   

   def processObjectNode(OperationalTemplate template, Archetype archetype, org.openehr.am.openehrprofile.datatypes.quantity.CDvOrdinal node, String parentPath, String path, String dataPath, String templateDataPath)
   {
      Map paths = calculatePaths(node, parentPath, path, dataPath, templateDataPath)

      //println "CODE LIST: "+ node.codeList

      def obn = new com.cabolabs.openehr.opt.model.domain.CDvOrdinal(
         owner:            template,
         rmTypeName:       node.rmTypeName,
         nodeId:           node.nodeId,
         type:             classToRm(node.getClass().getSimpleName()),
         archetypeId:      archetype.getArchetypeId().getValue(),
         templatePath:     paths.parentPath,
         path:             paths.path,
         dataPath:         paths.dataPath,
         templateDataPath: paths.templateDataPath,
         occurrences:      adlToOptIntervalInt(node.occurrences)
      )

      if (node.list) // List<CDvOrdinalItem>
      {
         def cdvo
         node.list.each { ordinal -> // int value, codephrase symbol

            cdvo = new com.cabolabs.openehr.opt.model.domain.CDvOrdinalItem(
               value: ordinal.value,
               symbol: new com.cabolabs.openehr.opt.model.datatypes.CodePhrase(
                  codeString:    ordinal.symbol.codeString,
                  terminologyId: ordinal.symbol.terminologyId.value
               )
            )

            obn.list << cdvo
         }
      }

      println obn

      return obn
   }



   def processObjectNode(OperationalTemplate template, Archetype archetype, org.openehr.am.openehrprofile.datatypes.quantity.CDvQuantity node, String parentPath, String path, String dataPath, String templateDataPath)
   {
      Map paths = calculatePaths(node, parentPath, path, dataPath, templateDataPath)

      //println "CODE LIST: "+ node.codeList

      def obn = new com.cabolabs.openehr.opt.model.domain.CDvQuantity(
         owner:            template,
         rmTypeName:       node.rmTypeName,
         nodeId:           node.nodeId,
         type:             classToRm(node.getClass().getSimpleName()),
         archetypeId:      archetype.getArchetypeId().getValue(),
         templatePath:     paths.parentPath,
         path:             paths.path,
         dataPath:         paths.dataPath,
         templateDataPath: paths.templateDataPath,
         occurrences:      adlToOptIntervalInt(node.occurrences),
      )

      if (node.property) // CodePhrase
      {
         obn.property = new com.cabolabs.openehr.opt.model.datatypes.CodePhrase(
            codeString:    node.property.codeString,
            terminologyId: node.property.terminologyId.value
         )
      }

      if (node.list)
      {
         def cqtyItem
         node.list.each { qtyItem -> // CQuantityItem

            // .magnitude // Interval<Double>
            // .precision // Interval<Integer>
            // .units     // String

            cqtyItem = new com.cabolabs.openehr.opt.model.domain.CQuantityItem(
               units: qtyItem.units,
               magnitude: adlToOptIntervalDouble(qtyItem.magnitude),
               precision: adlToOptIntervalInt(qtyItem.precision)
            )

            obn.list << cqtyItem
         }
      }

      println obn

      return obn
   }

   def processObjectNode(OperationalTemplate template, Archetype archetype, ConstraintRef node, String parentPath, String path, String dataPath, String templateDataPath)
   {
      Map paths = calculatePaths(node, parentPath, path, dataPath, templateDataPath)

      String reference = node.reference // ac0004

      //println "ref ${reference}"

      /*
      // List<OntologyBinding>
      archetype.getOntology().getConstraintBindingList().each { obind ->

         println obind.getTerminology() // name

         // println obind.getBindingList()

         // List<OntologyBindingItem < QueryBindingItem>
         obind.getBindingList().each { obitem ->

            println " - "+ obitem.getCode() // ac0001
            println " - "+ obitem.getQuery().getUrl() // terminology:minsal.cl/norma820/regiones
         }
      }
      */

      // Get URL by reference (code)

      String terminologyRef
      def obitem
      for (def obind: archetype.ontology.constraintBindingList)
      {
         obitem = obind.bindingList.find{ it.code == reference } // QueryBindingItem > OntologyBindingItem
         if (obitem)
         {
            terminologyRef = obitem.query.url
            break
         }
      }

      println "terminologyRef ${terminologyRef}"

      // List<OntologyDefinitions>
      // archetype.getOntology().getConstraintDefinitionsList().each { ondef ->

      //    println ondef.getLanguage()
      // }

      return new CCodePhrase(
         owner:            template,
         rmTypeName:       node.rmTypeName,
         nodeId:           node.nodeId,
         type:             classToRm(node.getClass().getSimpleName()),
         archetypeId:      archetype.getArchetypeId().getValue(),
         templatePath:     paths.parentPath,
         path:             paths.path,
         dataPath:         paths.dataPath,
         templateDataPath: paths.templateDataPath,
         occurrences:      adlToOptIntervalInt(node.occurrences),

         terminologyRef:   terminologyRef,
         reference:        reference
      ) // TODO: terminologyRef (URL), reference, codeList, terminologyId

      //    <children xsi:type="C_CODE_REFERENCE">
      //      <rm_type_name>CODE_PHRASE</rm_type_name>
      //      <occurrences>
      //          <lower_included>true</lower_included>
      //          <upper_included>true</upper_included>
      //          <lower_unbounded>false</lower_unbounded>
      //          <upper_unbounded>false</upper_unbounded>
      //          <lower>0</lower>
      //          <upper>1</upper>
      //      </occurrences>
      //      <node_id></node_id>
      //      <referenceSetUri>terminology:iso.org/3166-2</referenceSetUri>
      //  </children>
   }

   Map calculatePaths(CObject node, String parentPath, String path, String dataPath, String templateDataPath)
   {
      def templatePath = parentPath

      if (templatePath != '/')
      {
         if (node.nodeId)
         {
            // NOTE: this case shouldn't happen when processing a single archetype.
            // archetype Id
            if (node.nodeId.startsWith('openEHR'))
            {
               templatePath     += '[archetype_id='+ node.nodeId +']' // slot in the path instead of node_id
               templateDataPath += '[archetype_id='+ node.nodeId +']'

               // We already know the archetype.definition is the root and there are no other archetype roots since this process is for a single archetype.
               // if (node.'@xsi:type'.text() == "C_ARCHETYPE_ROOT")
               // {
               //    path     = '/' // archetype root found
               //    dataPath = '/' // reset data path when path is root
               // }
            }
            // at node Id
            else
            {
               // avoids adding the node_id for PATHABLE nodes
               // if node is a LOCATABLE add the node_id to the dataPath
               if (!pathables.contains(node.rmTypeName))
               {
                  templatePath     += '['+ node.nodeId + ']'
                  path             += '['+ node.nodeId + ']'
                  templateDataPath += '['+ node.nodeId + ']'
                  dataPath         += '['+ node.nodeId + ']'
               }
            }
         }

         // only for non root nodes and nodes with node_id
         if (!pathCounter[templatePath])
         {
            pathCounter[templatePath] = 1
         }
         else
         {
            pathCounter[templatePath] ++
         }

         templateDataPath += '('+ pathCounter[templatePath] +')'
      }

      [
         templatePath:     templatePath,
         templateDataPath: templateDataPath,
         path:             path,
         dataPath:         dataPath
      ]
   }

   def processObjectNode(OperationalTemplate template, Archetype archetype, CPrimitiveObject node, String parentPath, String path, String dataPath, String templateDataPath)
   {
      Map paths = calculatePaths(node, parentPath, path, dataPath, templateDataPath)

      def obn = new PrimitiveObjectNode(
         owner:            template,
         rmTypeName:       node.rmTypeName,
         nodeId:           node.nodeId,
         type:             classToRm(node.getClass().getSimpleName()),
         archetypeId:      archetype.getArchetypeId().getValue(),
         templatePath:     paths.parentPath,
         path:             paths.path,
         dataPath:         paths.dataPath,
         templateDataPath: paths.templateDataPath,
         occurrences:      adlToOptIntervalInt(node.occurrences)
      )

      org.openehr.am.archetype.constraintmodel.primitive.CPrimitive primitive = node.getItem()

      if (!primitive)
      {
         throw new Exception("Invalid template: missing required primitive.item at "+ this.template.templateId +" "+ path)
      }

      // FIXME: create individual methods
      if (primitive instanceof org.openehr.am.archetype.constraintmodel.primitive.CInteger)
      {
         obn.item = new com.cabolabs.openehr.opt.model.primitive.CInteger()

         if (!primitive.interval) // NOTE: in the java ref impl it's interval but in the spec it's range
         {
            obn.item.range = adlToOptIntervalInt(primitive.interval)
         }
         else
         {
            primitive.list.each {
               obn.item.list << Integer.parseInt(it)
            }
         }
      }
      else if (primitive instanceof org.openehr.am.archetype.constraintmodel.primitive.CDateTime)
      {
         obn.item = new com.cabolabs.openehr.opt.model.primitive.CDateTime()
         obn.item.pattern = primitive.pattern // TODO: has pattern, interval, list
      }
      else if (primitive instanceof org.openehr.am.archetype.constraintmodel.primitive.CDate)
      {
         obn.item = new com.cabolabs.openehr.opt.model.primitive.CDate()
         obn.item.pattern = primitive.pattern // TODO: has pattern, interval, list
      }
      // TODO: CTime
      else if (primitive instanceof org.openehr.am.archetype.constraintmodel.primitive.CBoolean)
      {
         obn.item = new com.cabolabs.openehr.opt.model.primitive.CBoolean(
            trueValid: primitive.isTrueValid(),
            falseValid: primitive.isFalseValid()
         )
         /*
         <item xsi:type="C_BOOLEAN">
         <true_valid>true</true_valid>
         <false_valid>true</false_valid>
         </item>
         */
      }
      else if (primitive instanceof org.openehr.am.archetype.constraintmodel.primitive.CDuration)
      {
         obn.item = new com.cabolabs.openehr.opt.model.primitive.CDuration()

         // Interval<DvDuration>
         if (primitive.interval)
         {
            // NOTE: the java ref impl calls interval but in the specs is range
            // NOTE: in the java ref imple it's an Interval<DvDuration> but in the spec it's Interval<Duration>
            obn.item.range = adlToOptIntervalDuration(primitive.interval)
         }
         else
         {
            try
            {
               obn.item.pattern = primitive.pattern // throws exception if value is invalid
            }
            catch (Exception e)
            {
               throw new Exception("There was a problem parsing the C_DURATION.pattern: "+ e.message, e)
            }
         }
      }
      else if (primitive instanceof org.openehr.am.archetype.constraintmodel.primitive.CReal)
      {
         obn.item = new com.cabolabs.openehr.opt.model.primitive.CReal()
         obn.item.range = parseIntervalBigDecimal(primitive.range)
      }
      else if (primitive instanceof org.openehr.am.archetype.constraintmodel.primitive.CString)
      {
         obn.item = new com.cabolabs.openehr.opt.model.primitive.CString()

         if (primitive.pattern)
            obn.item.pattern = primitive.pattern
         else
         {
            primitive.list.each { li ->
               // there are OPTs with empty elements this avoids to load them as items on the list
               // <item xsi:type="C_STRING">
               //    <list />
               //  </item>
               if (li) obn.item.list << li
            }
         }
      }
      else
      {
         throw new Exception("primitive '"+ primitive.getClass().getSimpleName() +"' not supported, check "+ path)
      }

      return obn
   }

   def processAttributeNode(OperationalTemplate template, Archetype archetype, ObjectNode parent, CAttribute cAttribute, String parentPath, String path, String dataPath, String templateDataPath)
   {
      // Path calculation
      def templatePath = parentPath
      if (templatePath == '/')
      {
         templatePath     += cAttribute.rmAttributeName // Avoids to repeat '/'
         templateDataPath += cAttribute.rmAttributeName
      }
      else
      {
         templatePath     += '/'+ cAttribute.rmAttributeName
         templateDataPath += '/'+ cAttribute.rmAttributeName
      }

      def nextArchPath
      if (path == '/')
      {
         nextArchPath = '/' + cAttribute.rmAttributeName
         dataPath     = '/' + cAttribute.rmAttributeName
      }
      else
      {
         nextArchPath =     path +'/'+ cAttribute.rmAttributeName
         dataPath     = dataPath +'/'+ cAttribute.rmAttributeName
      }

      def atn = new AttributeNode(
         rmAttributeName:  cAttribute.rmAttributeName,
         type:             classToRm(cAttribute.getClass().getSimpleName()),
         parent:           parent,
         path:             nextArchPath,
         dataPath:         dataPath,
         templatePath:     templatePath,
         templateDataPath: templateDataPath,
         existence:        adlToOptExistence(cAttribute.existence)
      )

      if (cAttribute instanceof CMultipleAttribute)
      {
         atn.cardinality = adlToOptCardinality(cAttribute.cardinality)
      }

      def obn
      cAttribute.children.each { cObject ->

         println cObject.getClass().getSimpleName()

         obn = processObjectNode(
            template,
            archetype,
            cObject,
            templatePath,
            nextArchPath,
            dataPath,
            templateDataPath
         )

         obn.parent = atn
         atn.children << obn
      }

      return atn
   }


   String classToRm(String className)
   {
      // camel case to snake and uppercase
      className.replaceAll( /([A-Z])/, /_$1/ ).toUpperCase().replaceAll( /^_/, '' )
   }

   IntervalInt adlToOptIntervalInt(Interval<Integer> interval)
   {
      if (!interval) return null

      def itv = new IntervalInt(
         upperIncluded:  interval.isUpperIncluded(),
         lowerIncluded:  interval.isLowerIncluded(),
         upperUnbounded: interval.isUpperUnbounded(),
         lowerUnbounded: interval.isLowerUnbounded()
      )

      if (!itv.lowerUnbounded)
      {
         itv.lower = interval.lower
      }
      if (!itv.upperUnbounded)
      {
         itv.upper = interval.upper
      }

      return itv
   }

   IntervalBigDecimal adlToOptIntervalDouble(Interval<Double> interval)
   {
      if (!interval) return null

      def itv = new IntervalBigDecimal(
         upperIncluded:  interval.isUpperIncluded(),
         lowerIncluded:  interval.isLowerIncluded(),
         upperUnbounded: interval.isUpperUnbounded(),
         lowerUnbounded: interval.isLowerUnbounded()
      )

      if (!itv.lowerUnbounded)
      {
         itv.lower = interval.lower
      }
      if (!itv.upperUnbounded)
      {
         itv.upper = interval.upper
      }

      return itv
   }

   IntervalDuration adlToOptIntervalDuration(Interval<DvDuration> interval)
   {
      if (!interval) return null

      def itv = new IntervalDuration(
         upperIncluded:  interval.isUpperIncluded(),
         lowerIncluded:  interval.isLowerIncluded(),
         upperUnbounded: interval.isUpperUnbounded(),
         lowerUnbounded: interval.isLowerUnbounded()
      )

      if (!itv.lowerUnbounded)
      {
         itv.lower = interval.lower
      }
      if (!itv.upperUnbounded)
      {
         itv.upper = interval.upper
      }

      return itv
   }

   IntervalInt adlToOptExistence(CAttribute.Existence existence)
   {
      switch (existence)
      {
         case CAttribute.Existence.REQUIRED:
            return new IntervalInt(lower: 1, upper: 1, lowerIncluded: true, upperIncluded: true, lowerUnbounded: false, upperUnbounded: false)
         case CAttribute.Existence.OPTIONAL:
            return new IntervalInt(lower: 0, upper: 1, lowerIncluded: true, upperIncluded: true, lowerUnbounded: false, upperUnbounded: false)
         case CAttribute.Existence.NOT_ALLOWED:
            return new IntervalInt(lower: 0, upper: 0, lowerIncluded: true, upperIncluded: true, lowerUnbounded: false, upperUnbounded: false)
      }
   }

   com.cabolabs.openehr.opt.model.Cardinality adlToOptCardinality(org.openehr.am.archetype.constraintmodel.Cardinality cardinality)
   {
      if (!cardinality) return null

      def optCardinality = new com.cabolabs.openehr.opt.model.Cardinality(
         isOrdered: cardinality.isOrdered(),
         isUnique:  cardinality.isUnique(),
         interval:  adlToOptIntervalInt(cardinality.getInterval())
      )

      return optCardinality
   }

}