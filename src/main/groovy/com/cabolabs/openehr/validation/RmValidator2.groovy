package com.cabolabs.openehr.validation

import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.validation.*
import com.cabolabs.openehr.rm_1_0_2.ehr.*
import com.cabolabs.openehr.rm_1_0_2.composition.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.navigation.Section
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.*
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.*
import com.cabolabs.openehr.rm_1_0_2.data_types.uri.*
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.demographic.*
import com.cabolabs.openehr.dto_1_0_2.ehr.EhrDto


// TODO: there are no validators for CReal, wichi would work for some values like precision
class RmValidator2 {

   OptManager opt_manager

   RmValidator2(OptManager opt_manager)
   {
      this.opt_manager = opt_manager
   }

   String classToRm(String className)
   {
      // camel case to snake and uppercase
      className.replaceAll( /([A-Z])/, /_$1/ ).toUpperCase().replaceAll( /^_/, '' )
   }

   // TODO: Party subclasses validation

   /**
    * Ehr is validated against the RM constraints. Since it's not Locatable it can't be validated
    * against an OPT (unless we use the OPT as a RM representation)
    */
   RmValidationReport dovalidate(Ehr ehr)
   {
      RmValidationReport report = new RmValidationReport()

      if (!ehr.system_id)
      {
         report.addError("/system_id", "attribute is not present but is required")
      }

      if (!ehr.ehr_id)
      {
         report.addError("/ehr_id", "attribute is not present but is required")
      }

      if (!ehr.ehr_status)
      {
         report.addError("/ehr_status", "attribute is not present but is required")
      }

      if (!ehr.ehr_access)
      {
         report.addError("/ehr_access", "attribute is not present but is required")
      }

      if (!ehr.time_created)
      {
         report.addError("/time_created", "attribute is not present but is required")
      }

      return report
   }

   /**
    * EhrDto is validated against the relaxed RM constraints. Since it's not Locatable it can't be validated
    * against an OPT (unless we use the OPT as a RM representation). Based on the current REST API specification,
    * the EhrDto will be returned in the response, so it will have system_id, ehr_id, time_created and ehr_status
    * (could be the defaullt one). For the API we don't require the ehr_access to be there.
    */
   RmValidationReport dovalidate(EhrDto ehr)
   {
      RmValidationReport report = new RmValidationReport()

      if (!ehr.system_id)
      {
         report.addError("/system_id", "attribute is not present but is required")
      }

      if (!ehr.ehr_id)
      {
         report.addError("/ehr_id", "attribute is not present but is required")
      }

      if (!ehr.ehr_status)
      {
         report.addError("/ehr_status", "attribute is not present but is required")
      }

      if (!ehr.time_created)
      {
         report.addError("/time_created", "attribute is not present but is required")
      }

      return report
   }

   // the namespace is where the OPT is stored/cached, allows to implement multi-tenancy
   RmValidationReport dovalidate(Locatable rm_object, String namespace)
   {
      if (!rm_object.archetype_details)
      {
         return locatable_missing_archetype_details(rm_object)
      }

      String template_id = rm_object.archetype_details.template_id.value

      def opt = this.opt_manager.getOpt(template_id, namespace)

      if (!opt)
      {
         return opt_not_found(template_id)
      }

      // dataPath is needed for error reporting and calculated while the object gets validated
      rm_object.dataPath = '' // avoid using / to avoid all checks, so / could be added at the end

      return validate(rm_object, opt.definition)
   }

   // Checks if the children of the cattr allow the type of the rmObject,
   // if the type is not allowed, it reports the error.
   // If the constraint is open (no children), it allows any type
   private boolean checkAllowedType(AttributeNode cattr, Locatable rm_object, RmValidationReport report)
   {
      def allowed_types = cattr.children*.rmTypeName // [ITEM_TREE, ITEM_LIST]

      if (!allowed_types) return true

      def rm_type = classToRm(rm_object.getClass().getSimpleName()) // ItemTree -> ITEM_TREE

      // Considering abstract types
      if (allowed_types.contains('EVENT'))
      {
         allowed_types.remove('EVENT')
         allowed_types.add('POINT_EVENT')
         allowed_types.add('INTERVAL_EVENT')
      }

      if (allowed_types.contains(rm_type)) return true

      report.addError(
         rm_object.dataPath +'/'+ cattr.rmAttributeName,
         cattr.templatePath,
         "type '${rm_type}' is not allowed here, it should be in ${allowed_types}"
      )
      return false
   }

   private boolean checkAllowedType(AttributeNode cattr, DataValue rm_object, RmValidationReport report)
   {
      def allowed_types = cattr.children*.rmTypeName // [ITEM_TREE, ITEM_LIST]

      if (!allowed_types) return true

      def rm_type = classToRm(rm_object.getClass().getSimpleName()) // ItemTree -> ITEM_TREE

      // allowed: [DV_INTERVAL<DV_COUNT>] => [DV_INTERVAL]
      // the RM object type doesn't have the generic type specified
      allowed_types = allowed_types.collect { (it.startsWith('DV_INTERVAL')) ? 'DV_INTERVAL' : it }

      if (allowed_types.contains(rm_type)) return true

      report.addError(
         '', // FIXME: this method needs the parent locatable to obtain the dataPath
         cattr.templatePath,
         "type '${rm_type}' is not allowed here, it should be in ${allowed_types}")
      return false
   }

   private RmValidationReport validate(EhrStatus status, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(status, o)) // validates name

      // NOTE: shouldn't this validate alternatives for the EHR_STATUS.other_details?

      // checking mandatory fields that are not archetyped
      if (!status.subject)
      {
         report.addError(
            "/subject",
            o.templatePath,
            "attribute is not present but is required"
         )
      }

      // checking archetyped fields
      // the attributes that are optional in the opt should be checked by the parent to avoid calling
      // with null because polymorphism can't find the right method. Also if the constraint is null,
      // anything matches.
      def a_other_details = o.getAttr('other_details')
      if (a_other_details)
      {
         if (status.other_details)
         {
            if (checkAllowedType(a_other_details, status.other_details, report)) // only continue if the type is allowed
            {
               status.other_details.dataPath = '/other_details'
               report.append(validate(status.other_details, a_other_details))
            }
         }
         // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         else if (!a_other_details.existence.has(0))
         {
            report.addError(
               "/other_details",
               o.templatePath,
               "attribute is not present but is required"
            )
         }
      }

      return report
   }


   /**
    * Parent is the object wit ha container attribute (e.g. ItemTree)
    * Container is the value of the container attribute (e.g. ItemTree.items)
    * cma is the C_MULTIPLE_ATTRIBUTE AOM constraint for the container attribute
    */
   // all container attributes will get here
   private RmValidationReport validate(Locatable parent, List container, AttributeNode cma)
   {
      //println "validate container attribute: "+ cma.rmAttributeName
      //println cma.templatePath
      //println cma.templateDataPath
      //println cma.dataPath

      RmValidationReport report = new RmValidationReport()

      //println cma.cardinality.interval
      //println cma.dataPath +" size: "+ container.size()

      if (!cma.cardinality.interval.has(container.size()))
      {
         report.addError(
            parent.dataPath +'/'+ cma.rmAttributeName,
            cma.templatePath,
            "Number of objects in container (${container.size()}) violates the cardinality constraint "+ cma.cardinality.interval.toString()
         )
      }



      // nothing else to validate?
      if (!cma.children) return report



      // Validate occurrences of items in the container: for each c_object in cma.children,
      //   1. count all items in container with the same c_object.node_id
      //   2. the count should be in c_object.occurrences
      // So the occurrences are validated in the parent attribute, not in the same node validation!
      def sibling_count
      cma.children.each { c_object ->

         sibling_count = container.count { it.archetype_node_id == c_object.nodeId }

         if (!c_object.occurrences?.has(sibling_count))
         {
            // println c_object.templatePath
            // println c_object.templateDataPath
            report.addError(
               parent.dataPath +'/'+ cma.rmAttributeName,
               c_object.templatePath,
               "Children with archetype_node_id=${c_object.nodeId} occurs ${sibling_count} times, violates occurrences constraint ${c_object.occurrences.toString()}"
            )
         }
      }



      // validate each item in the container

      // println "mattr children "+ cma.children*.templateDataPath
      // println "container node_ids "+ container*.archetype_node_id
      def alternative_cobjs, cobj, error_report, name_constraint
      container.eachWithIndex { item, i ->

         item.dataPath = parent.dataPath +'/'+ cma.rmAttributeName +'('+ i +')'

         // println "item "+ item.archetype_node_id
         // println cma.children*.archetypeId
         // println cma.children*.nodeId

         // FIXME: this is unreliable, there could be many child objects for the CATTR that are
         //        C_ARCHETYPE_ROOT with the same archetypeId. The name is added to differentiate.
         //
         // each item in the collection should validate against the child object with the same node_id
         //
         // NOTE: the code below is safer since it checks if the name is not there when there are multiple
         //       alternatives with the same archetype_node_id.
         alternative_cobjs = cma.children.findAll {
            //println it.nodeId
            if (it.type == 'C_ARCHETYPE_ROOT')
               it.archetypeId == item.archetype_node_id
            else
               it.nodeId == item.archetype_node_id
         }


         // there is an object in the data that is not defined in the template
         if (!alternative_cobjs)
         {
            report.addError(
               item.dataPath,
               "No c_object found with archetype_node_id ${item.archetype_node_id}, the RM object contains an item that is not defined in the template"
            )
         }


         // println "item "+ item.archetype_node_id
         // println "alternative_cobjs "+ alternative_cobjs
         // println "alternative node_ids "+ alternative_cobjs*.nodeId

         // When there is only one alternative, that is matched by archetype_id or node_id only
         if (alternative_cobjs.size() == 1)
         {
            cobj = alternative_cobjs[0]
         }
         else // When there are multiple alternatives, the specific C_OBJECT should be matched by name too (in OPT 1.4 there is no way around)
         {
            // FIXME: when the container has a generated object to comply with the existence and the alternatives in the attribute are only slots,
            // then the object will match all the alternatives by name since for slots the validation returns true

            // if there is a constraint for the name, we should try to use the constrained name first, then the text associated to the node by the node_id
            cobj = alternative_cobjs.find { // it = alt_obj

               name_constraint = it.getAttr('name')

               // if one of the name constraints validates, that is th cobj that will be used to validate the item
               if (name_constraint)
               {
                  // println "validate data name: "+ item.name.value
                  // println "against "+ name_constraint.children[0].getAttr('value')?.children?.getAt(0)?.item?.list
                  error_report = validate(item, item.name, name_constraint, '/name')

                  //println error_report.errors

                  // if there are no validation errors, then the data matches this alt_cobj
                  return !error_report.hasErrors()
               }

               return false
            }

            //println obj

            // if there are no constraints for the name or none matches the value for the name,
            // try finding by the AOM node text
            if (!cobj)
            {
               // match by the node text
               cobj = alternative_cobjs.find{
                  it.text == item.name.value
               }
            }

            //println obj

            // if none matches it means:
            // a. there is a constraint validation issue (none matches the OPT) or
            // b. altenrative nodes are not uniquely named (this could be tested when the OPT is loaded)
            if (!cobj)
            {
               //println cma.templateDataPath
               report.addError(
                  cma.templateDataPath,
                  "Multiple alternative constraint objects found for archetype_node_id '${item.archetype_node_id}' at ${item.dataPath}, none matches the constraints for the name or the current node text '${item.name.value}' in the OPT"
               )
            }

            //println ""
         }


         // println "------"
         // println cma.children
         // println obj
         // println item
         // println item.archetype_node_id

         //println obj.type
         // println "RM TYPE: "+ item.getClass().getSimpleName()
         // println "RM PATH: "+ item.dataPath
         // println "RM NAME: "+ item.name.value

         // println "CATTR RM ATTR NAME: "+ cma.rmAttributeName
         // println "CATTR PATH: "+ cma.dataPath
         // println "CATTR TPATH: "+ cma.templateDataPath
         // println "CATTR CHILDREN TEXT:"+ cma.children*.text
         // println "CATTR CHILDREN RM TYPE: "+ cma.children*.rmTypeName
         // println ""

         if (cobj && checkAllowedType(cma, item, report)) // only continue if the type is allowed
         {
            report.append(validate(item, cobj))
         }
      }

      return report
   }


   // Person, Org, Group, Agent validator
   private RmValidationReport validate(Actor p, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_party(p, o))

      // TODO: languages
      // TODO: roles

      return report
   }

   private RmValidationReport validate(Role role, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_party(role, o))

      // TODO: time_validity
      // TODO: performer

      return report
   }

   private RmValidationReport validate(PartyIdentity pi, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(pi, o)) // validates name

      validate_single_attribute(pi, o, 'details', report)

      return report
   }

   private RmValidationReport validate(Composition c, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(c, o)) // validates name

      report.append(validate(c, c.category, o.getAttr('category'), "/category"))

      validate_single_attribute(c, o, 'context', report)

      validate_multiple_attribute(c, o, 'content', report)

      return report
   }

   private RmValidationReport validate(Folder f, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(f, o)) // validates name

      validate_multiple_attribute(f, o, 'items', report)

      validate_multiple_attribute(f, o, 'folders', report)

      return report
   }

   private RmValidationReport validate(Section s, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(s, o)) // validates name

      validate_multiple_attribute(s, o, 'items', report)

      return report
   }


   private RmValidationReport validate_alternatives(Observation ob, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ob, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Observation ob, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_care_entry(ob, o)) // validates protocol, name

      validate_single_attribute(ob, o, 'data', report)

      validate_single_attribute(ob, o, 'state', report)

      return report
   }

   private RmValidationReport validate(History h, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(h, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(History h, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(h, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(History h, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(h, o)) // validates name

      validate_multiple_attribute(h, o, 'events', report)

      validate_single_attribute(h, o, 'summary', report)

      return report
   }

   private RmValidationReport validate_alternatives(PointEvent e, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(e, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(PointEvent e, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(e, o)) // validates name

      validate_single_attribute(e, o, 'data', report) // validates existence, occurrences and continues recursion

      validate_single_attribute(e, o, 'state', report)

      return report
   }

   private RmValidationReport validate_alternatives(IntervalEvent e, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(e, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(IntervalEvent e, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(e, o)) // validates name

      validate_single_attribute(e, o, 'data', report) // validates existence, occurrences and continues recursion

      validate_single_attribute(e, o, 'state', report) // validates existence, occurrences and continues recursion

      return report
   }


   private RmValidationReport validate_alternatives(Evaluation ev, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ev, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Evaluation ev, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_care_entry(ev, o)) // validates protocol, name

      validate_single_attribute(ev, o, 'data', report) // validates existence, occurrences and continues recursion

      return report
   }

   private RmValidationReport validate_alternatives(Instruction ins, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ins, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Instruction ins, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_care_entry(ins, o)) // validates protocol, name

      validate_multiple_attribute(ins, o, 'activities', report)

      return report
   }

   private RmValidationReport validate(Activity act, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(act, o)) // validates name

      validate_single_attribute(act, o, 'description', report) // validates existence, occurrences and continues recursion

      // TODO: timing, action_archetype_id

      return report
   }

   private RmValidationReport validate_alternatives(Action ac, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ac, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Action ac, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_care_entry(ac, o)) // validates protocol, name

      validate_single_attribute(ac, o, 'description', report) // validates existence, occurrences and continues recursion

      return report
   }

   // NOTE: entries don't have an attribute method because the only attribute that can
   //       hold an entry is multiple, and all multiple attributes use the same validation
   //       method: validate(Locatable parent, List container, AttributeNode cma)
   private RmValidationReport validate_alternatives(AdminEntry ae, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ae, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(AdminEntry ae, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_entry(ae, o))

      validate_single_attribute(ae, o, 'data', report) // validates existence, occurrences and continues recursion

      return report
   }

   private RmValidationReport validate(EventContext context, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(context, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(EventContext context, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(context, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(EventContext context, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute(context, o, 'other_context', report) // validates existence, occurrences and continues recursion

      // TODO: is 'participations' archetypable?

      return report
   }

   private RmValidationReport validate(ItemTree is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemTree is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemTree is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(is, o)) // validates name

      validate_multiple_attribute(is, o, 'items', report)

      return report
   }

   private RmValidationReport validate(ItemList is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemList is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemList is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(is, o)) // validates name

      // validates existens and continues recursion
      validate_multiple_attribute(is, o, 'items', report)

      return report
   }

   private RmValidationReport validate(ItemTable is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemTable is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemTable is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(is, o)) // validates name

      validate_multiple_attribute(is, o, 'rows', report)

      return report
   }

   private RmValidationReport validate(ItemSingle is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(is, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(ItemSingle is, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(is, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(ItemSingle is, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(is, o)) // validates name

      validate_single_attribute(is, o, 'item', report) // validates existence, occurrences and continues recursion

      return report
   }


   private RmValidationReport validate_alternatives(Cluster cl, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(cl, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Cluster cl, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(cl, o)) // validates name

      validate_multiple_attribute(cl, o, 'items', report)

      return report
   }

   private RmValidationReport validate(Element e, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(e, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(Element e, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(e, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Element e, ObjectNode o)
   {
      /*
      println 'element path '+ e.path
      println 'element data path '+ e.dataPath
      println 'object path '+ o.path
      println 'object template path '+ o.templatePath
      */

      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(e, o)) // validates name

      //validate_single_attribute_dv(e, e.value, '/value', o, 'value', report)
      validate_single_attribute(e, o, 'value', report)

      //validate_single_attribute_dv(e, e.null_flavour, '/null_flavour', o, 'null_flavour', report)
      validate_single_attribute(e, o, 'null_flavour', report)


      return report
   }

   private RmValidationReport validate(Pathable parent, DvCodedText ct, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence should be already verified in the parent

      report.append(validate_alternatives(parent, ct, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvCodedText ct, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, ct, o, dv_path)
         if (!report.hasErrors()) // if there is one alternative that validates the data, then it passes the validation
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvCodedText ct, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // FIXME: this is trying to access the attr name directly from the parent locatable but since it's a structured DV
      //        the value should be taken from the DV not from the LOCATABLE
      validate_single_attribute_dv(parent, ct, dv_path, o, 'defining_code', report)

      // custom cross-field validation for the coded text:
      // if the terminology is 'local' => the name should the the text of the at code
      // in defininig_code.code_string from the OPT
      //
      // NOTE: opt.getTerm() only works if the code_string is in the list in the template, if it's not there, that
      //       will return null, and null is always different than the value, making the validation to fail, so
      //       the value is validated ONLY if the defining_code validates OK.
      //
      if (ct.defining_code.terminology_id.value == 'local' && !report.hasErrors())
      {
         def valid_coded_value = o.owner.getTerm(findRootRecursive(o).archetypeId, ct.defining_code.code_string)

         if (valid_coded_value != ct.value)
         {
            report.addError(
               parent.dataPath + dv_path +'/value',
               o.templatePath,
               "Value '${ct.value}' doesn't match value from template '${valid_coded_value}'"
            )
         }
      }

      // TODO: mappings

      return report
   }


   // ========================================================
   // TEST: find root up
   // TODO: these operations should be part of ObjectNode
   // Search for archetype root traversing the nodes up
   private ObjectNode findRootRecursive(ObjectNode obj)
   {
      if (obj.path == '/' || obj.type == 'C_ARCHETYPE_ROOT')
      {
         return obj
      }

      findRootRecursive(obj.parent)
   }

   private ObjectNode findRootRecursive(AttributeNode attr)
   {
      findRootRecursive(attr.parent)
   }
   // ========================================================




   private RmValidationReport validate(Pathable parent, CodePhrase cp, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, cp, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, CodePhrase cp, List<CCodePhrase> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, cp, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, CodePhrase cp, CCodePhrase o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()


      // TODO:
      // custom validation if the terminology is 'openehr', verify the code against
      // the openehr terminology files


      // specific type constraint validation
      ValidationResult valid = o.isValid(cp)

      if (!valid)
      {
         report.addError(
            parent.dataPath + dv_path,
            o.templatePath,
            valid.message
         )
      }

      return report
   }

   private RmValidationReport validate(Pathable parent, DvText te, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, te, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvText te, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, te, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvText te, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // def oa = o.getAttr('value')
      // if (oa)
      // {
      //    report.append(validate(parent, te.value, oa, dv_path +"/value"))
      // }

      validate_single_attribute_dv(parent, te, dv_path +'/value', o, 'value', report)

      return report
   }

   private RmValidationReport validate(Pathable parent, DvProportion d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvProportion d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvProportion d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // TODO: validate numerator and denominator if there is any constrain, and against the type

      return report
   }


   private RmValidationReport validate(Pathable parent, DvQuantity d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvQuantity d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         //println o.type +" "+ o.rmTypeName

         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return new RmValidationReport() //report
   }

   private RmValidationReport validate(Pathable parent, DvQuantity d, CDvQuantity o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // validate magnitude, precision and units
      ValidationResult valid = o.isValid(d)
      if (!valid)
      {
         report.addError(
            parent.dataPath + dv_path,
            o.templatePath,
            valid.message
         )
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvCount d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvCount d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvCount d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path +'/magnitude', o, 'magnitude', report)

      return report
   }

   private RmValidationReport validate(Pathable parent, Integer d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, Integer d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, Integer d, PrimitiveObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      ValidationResult valid = o.item.isValid(d) // item is CInteger

      if (!valid)
      {
         report.addError(
            parent.dataPath + dv_path,
            o.templatePath,
            valid.message
         )
      }

      return report
   }



   private RmValidationReport validate(Pathable parent, DvDateTime d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvDateTime d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvDateTime d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path, o, 'value', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, DvDate d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvDate d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvDate d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path, o, 'value', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, DvTime d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvTime d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvTime d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path, o, 'value', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, String d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, String d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, String d, PrimitiveObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      ValidationResult valid = o.item.isValid(d) // item is could be CDateTime, CString, CDate, CDuration, etc.

      if (!valid)
      {
         report.addError(
            parent.dataPath + dv_path,
            o.templatePath,
            valid.message
         )
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvOrdinal d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvOrdinal d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvOrdinal d, CDvOrdinal o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      ValidationResult valid = o.isValid(d)
      if (!valid)
      {
         report.addError(
            parent.dataPath + dv_path,
            o.templatePath,
            valid.message
         )
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvBoolean d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvBoolean d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvBoolean d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path, o, 'value', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, Boolean d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   private RmValidationReport validate_alternatives(Pathable parent, Boolean d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, Boolean d, PrimitiveObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      ValidationResult valid = o.item.isValid(d) // item is could be CBoolean

      if (!valid)
      {
         report.addError(
            parent.dataPath + dv_path,
            o.templatePath,
            valid.message
         )
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvDuration d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvDuration d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvDuration d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path, o, 'value', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, DvInterval d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvInterval d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvInterval d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // in the OPT the interval definition is a C_COMPLEX_OBJECT
      // it contains C_COMPLEX_OBJECT for the upper and lower constraints
      // each constraint could be a range for the magnitude, so we could have:
      // lower(a..b) .. upper(c..d) in the OPT
      // then in the data we have an interval (a1..b1) that is lower and higher
      // so we know a1 should be in a..b
      // and b2 should be in c..d

      validate_single_attribute_dv(parent, d, dv_path, o, 'lower', report)
      validate_single_attribute_dv(parent, d, dv_path, o, 'upper', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, DvMultimedia d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvMultimedia d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvMultimedia d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // media_type is mandatory by the RM
      if (!d.media_type)
      {
         report.addError(
            parent.dataPath + dv_path + "/media_type",
            o.templatePath,
            "is not present but is required"
         )
      }

      // size is mandatory by the RM
      if (!d.size)
      {
         report.addError(
            parent.dataPath + dv_path + "/size",
            o.templatePath,
            "is not present but is required"
         )
      }

      validate_single_attribute_dv(parent, d, dv_path, o, 'charset', report)
      validate_single_attribute_dv(parent, d, dv_path, o, 'language', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, DvParsable d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvParsable d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvParsable d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // value is mandatory by the RM
      if (!d.value)
      {
         report.addError(
            parent.dataPath + dv_path + "/value",
            o.templatePath,
            "is not present but is required"
         )
      }

      // formalism is mandatory by the RM
      if (!d.formalism)
      {
         report.addError(
            parent.dataPath + dv_path + "/formalism",
            o.templatePath,
            "is not present but is required"
         )
      }

      validate_single_attribute_dv(parent, d, dv_path, o, 'charset', report)
      validate_single_attribute_dv(parent, d, dv_path, o, 'language', report)

      return report
   }


   // Same validators are used for DvEhrUri
   private RmValidationReport validate(Pathable parent, DvUri d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvUri d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvUri d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path, o, 'value', report)

      return report
   }


   private RmValidationReport validate(Pathable parent, DvIdentifier d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate_alternatives(parent, d, a.children, dv_path))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(Pathable parent, DvIdentifier d, List<ObjectNode> os, String dv_path)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(parent, d, o, dv_path)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(Pathable parent, DvIdentifier d, ObjectNode o, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      validate_single_attribute_dv(parent, d, dv_path, o, 'issuer', report)
      validate_single_attribute_dv(parent, d, dv_path, o, 'type', report)
      validate_single_attribute_dv(parent, d, dv_path, o, 'assigner', report)
      validate_single_attribute_dv(parent, d, dv_path, o, 'id', report)

      return report
   }

   // validates name
   private RmValidationReport _validate_locatable(Locatable locatable, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      def a_name = o.getAttr('name')
      if (a_name) // should comply with the constraint for the DvText/DvCodedText
      {
         if (locatable.name)
         {
            // Validate the type in the instance is allowed by the template
            if (checkAllowedType(a_name, locatable.name, report)) // only continue if the type is allowed
            {
               //report.append(validate(locatable.name, a_name))
               report.append(validate(locatable, locatable.name, a_name, "/name"))
            }
         }
         else
         {
            // no checks on the name because it's mandatory in the MR
            report.addError(
               locatable.dataPath == '/' ? "/name" : locatable.dataPath + "/name",
               o.templatePath == '/' ? "/name" : o.templatePath + "/name",
               "attribute is not present but is required"
            )
         }
      }
      else // should be a DvText which value is equal to the current node's text in the OPT
      {
         if (o.text != locatable.name.value)
         {
            report.addError(
               locatable.dataPath == '/' ? "/name" : locatable.dataPath + "/name",
               o.templatePath == '/' ? "/name" : o.templatePath + "/name",
               "expected name is '${o.text}' and actual name is '${locatable.name.value}'"
            )
         }
      }

      return report
   }

   private RmValidationReport _validate_care_entry(CareEntry ce, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_entry(ce, o))

      validate_single_attribute(ce, o, 'protocol', report)

      return report
   }

   private RmValidationReport _validate_entry(Entry entry, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      // TODO: should validate subject type is valid?

      report.append(_validate_locatable(entry, o)) // validates name

      return report
   }

   private RmValidationReport _validate_party(Party party, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(party, o)) // validates name

      validate_single_attribute(party, o, 'details', report)

      validate_multiple_attribute(party, o, 'identities', report)

      validate_multiple_attribute(party, o, 'contacts', report)

      validate_multiple_attribute(party, o, 'relationships', report)

      return report
   }

   private void validate_multiple_attribute(Locatable object, ObjectNode o, String attribute_name, RmValidationReport report)
   {
      def c_attr = o.getAttr(attribute_name)
      if (c_attr)
      {
         if (object."$attribute_name" != null)
         {
            report.append(validate(object, object."$attribute_name", c_attr)) // validate container
         }
         else
         {
            if (!c_attr.existence.has(0))
            {
               report.addError(
                  object.dataPath +'/'+ attribute_name,
                  o.templatePath,
                  "attribute not present but is required"
               )
            }
         }
      }
   }

   // This refactors common code of many validators into a generic function
   private void validate_single_attribute(Locatable object, ObjectNode o, String attribute_name, RmValidationReport report)
   {
      def c_attr = o.getAttr(attribute_name)

      // if c_attribute is null, all objects validate
      if (c_attr)
      {
         if (object."$attribute_name") // data is not null
         {
            // only continue if the type is allowed
            // this also does the error reporting
            if (checkAllowedType(c_attr, object."$attribute_name", report))
            {
               // =============================================================================================
               // if object."$attribute_name" is not locatable, it should call to a DV validation method
               // NOTE: CODE_PHRASE is not a DV!
               // 1. or we allow DV and CODE_PHRASE here,
               // 2. or we let coded text to valdiate CODE_PHRASE internally
               // 3. or we create a method specifically for CODE_PHRASE
               // validate(Pathable parent, DV dv, AttributeNode a, String dv_path)
               if (object."$attribute_name" instanceof DataValue)
               {
                  report.append(validate(object, object."$attribute_name", c_attr, '/'+ attribute_name))
               }
               else
               {
                  // set child dataPath only if child is not null
                  object."$attribute_name".dataPath = object.dataPath +'/'+ attribute_name
                  report.append(validate(object."$attribute_name", c_attr))
               }

            }

            // occurrences

            // The only way occurrences can fail for a single attribute is if it's 0..0, and if it's 0..0 the node
            // won't appear in the OPT. If there is just one alternative, this fails if occurrences is 0..0 since
            // data is not null in this case, but if occurrences is 0..0 the c_object won't be on the opt. If the
            // c_attribute has multiple alternatives, if one alternative contains 1 (0..1 or 1..1) then it validates,
            // the only case occurrences won't validate is if ALL alternatives have occurrences 0..0, but in that case
            // the nodes won't appear in the OPT. So neither of these cases will happen in a correctly built OPT.
         }
         else // data is null
         {
            // existence: the only way of violating existence is with 1..1 so
            // it's checked only if the value is null
            if (!c_attr.existence.has(0))
            {
               report.addError(
                  object.dataPath +'/'+ attribute_name,
                  o.templatePath,
                  "/$attribute_name is not present but is required by the existence constraint"
               )
            }

            // occurrences
            // For a null value, the occurrences constraints should be checked on the parent object,
            // because with a null value, the dataPath can't be retrieved to set it on the error report.
            // With a not null value, any existence will pass and occurrences 0..0 will fail.

            if (c_attr.children().size() == 1 && !c_attr.children[0].occurrences.has(0))
            {
               // here we need the parent for the dataPath since the data is nul
               report.addError(
                  object.dataPath +'/'+ attribute_name,
                  o.templatePath,
                  "Node doesn't match occurrences"
               )
            }
            // for multiple alternatives, a null value will always validate since all occurrences.lower == 0,
            // when there are multiple alternatives for a single attribute, since lower == 1 would make that
            // node the only alternative, and there are many, so there is no need for an else here.
         }
      }
   }

   // The attribute_name should be read from the parent_dv not from the parent locatable!
   private void validate_single_attribute_dv(Locatable parent, DataValue parent_dv, String dv_path, ObjectNode o, String attribute_name, RmValidationReport report)
   {
      def c_attr = o.getAttr(attribute_name)
      if (c_attr)
      {
         if (parent_dv."$attribute_name")
         {
            report.append(validate(parent, parent_dv."$attribute_name", c_attr, dv_path +'/'+ attribute_name))
         }
         else
         {
            // FIXME: existence is null for this attribute, check the OPTs and the parser,
            // since existence should be mandatory or have a default value from the RM
            // added the check for existence, though the spec says all attributes should have
            // existence, also note for all missing existence, we can get the default existence
            // from the RM, so we can complete that when parsing OPTs with missing existence.
            if (c_attr.existence && !c_attr.existence.has(0))
            {
               // NOTE: dv_path can contain multiple levels for structured Dvs
               report.addError(parent.dataPath + dv_path, "is not present but is required by existence")
            }
         }
      }
   }

   // ================================================================
   // General errors

   RmValidationReport opt_not_found(String template_id)
   {
      def report = new RmValidationReport()
      report.addError("Template '${template_id}' was not found")
      return report
   }

   RmValidationReport locatable_missing_archetype_details(Locatable rm_object)
   {
      def report = new RmValidationReport()
      report.addError("/archetype_details", "${rm_object.getClass().getSimpleName()} doesn't have 'archetype_details' which is required for validating RM instances")
      return report
   }
}