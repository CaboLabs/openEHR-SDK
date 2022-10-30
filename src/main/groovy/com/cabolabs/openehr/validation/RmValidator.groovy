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
class RmValidator {

   OptManager opt_manager

   RmValidator(OptManager opt_manager)
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

      // pathable path and dataPath are loaded only from parsing,
      // not from creating RM instances programatically, but are used
      // to report errors so we need to calculate them here
      if (!rm_object.path)
      {
         rm_object.fillPathable(null, null)
      }

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

      report.addError(cattr.templateDataPath, "type '${rm_type}' is not allowed here, it should be in ${allowed_types}")
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

      report.addError(cattr.templateDataPath, "type '${rm_type}' is not allowed here, it should be in ${allowed_types}")
      return false
   }

   private RmValidationReport validate(EhrStatus e, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(e, o)) // validates name

      // FIXME: shouldn't this validate alternatives for the EHR_STATUS.other_details?

      // the attributes that are optional in the opt should be checked by the parent to avoid calling
      // with null because polymorphism can't find the right method. Also if the constraint is null,
      // anything matches.
      def a_other_details = o.getAttr('other_details')
      if (a_other_details)
      {
         if (e.other_details)
         {
            // Validate the type in the instance is allowed by the template
            // FIXME: the type validation should be implemented on the rest of the validators!
            //def allowed_types = a_other_details.children*.rmTypeName
            //if (!allowed_types.contains(classToRm(e.other_details.getClass().getSimpleName())))
            if (checkAllowedType(a_other_details, e.other_details, report)) // only continue if the type is allowed
            {
               report.append(validate(e.other_details, a_other_details))
            }
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_other_details.existence.has(0))
            {
               report.addError("/other_details", "attribute is not present but is required")
            }
         }
      }

      return report
   }

   // Person, Org, Group, Agent validator
   private RmValidationReport validate(Actor p, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(p, o)) // validates name

      // the attributes that are optional in the opt should be checked by the parent to avoid calling
      // with null because polymorphism can't find the right method. Also if the constraint is null,
      // anything matches.
      def a_details = o.getAttr('details')
      if (a_details)
      {
         if (p.details)
         {
            // Validate the type in the instance is allowed by the template
            // FIXME: the type validation should be implemented on the rest of the validators!
            //def allowed_types = a_other_details.children*.rmTypeName
            //if (!allowed_types.contains(classToRm(e.other_details.getClass().getSimpleName())))
            if (checkAllowedType(a_details, p.details, report)) // only continue if the type is allowed
            {
               report.append(validate(p.details, a_details))
            }
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_details.existence.has(0))
            {
               report.addError("/details", "attribute is not present but is required")
            }
         }
      }

      // NOTE: identities is 1..* in the RM
      def a_identities = o.getAttr('identities')
      if (a_identities)
      {
         if (p.identities != null)
         {
            report.append(validate(p.identities, a_identities)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_identities.existence.has(0))
            {
               report.addError("/identities", "attribute is not present but is required")
            }
         }
      }

      def a_contacts = o.getAttr('contacts')
      if (a_contacts)
      {
         if (p.contacts != null)
         {
            report.append(validate(p.contacts, a_contacts)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_contacts.existence.has(0))
            {
               report.addError("/contacts", "attribute is not present but is required")
            }
         }
      }

      // TODO: languages
      // TODO: roles

      return report
   }

   private RmValidationReport validate(Role role, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(role, o)) // validates name

      // the attributes that are optional in the opt should be checked by the parent to avoid calling
      // with null because polymorphism can't find the right method. Also if the constraint is null,
      // anything matches.
      def a_details = o.getAttr('details')
      if (a_details)
      {
         if (role.details)
         {
            // Validate the type in the instance is allowed by the template
            // FIXME: the type validation should be implemented on the rest of the validators!
            //def allowed_types = a_other_details.children*.rmTypeName
            //if (!allowed_types.contains(classToRm(e.other_details.getClass().getSimpleName())))
            if (checkAllowedType(a_details, role.details, report)) // only continue if the type is allowed
            {
               report.append(validate(role.details, a_details))
            }
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_details.existence.has(0))
            {
               report.addError("/details", "attribute is not present but is required")
            }
         }
      }

      // NOTE: identities is 1..* in the RM
      def a_identities = o.getAttr('identities')
      if (a_identities)
      {
         if (role.identities != null)
         {
            report.append(validate(role.identities, a_identities)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_identities.existence.has(0))
            {
               report.addError("/identities", "attribute is not present but is required")
            }
         }
      }

      def a_contacts = o.getAttr('contacts')
      if (a_contacts)
      {
         if (role.contacts != null)
         {
            report.append(validate(role.contacts, a_contacts)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_contacts.existence.has(0))
            {
               report.addError("/contacts", "attribute is not present but is required")
            }
         }
      }

      // TODO: time_validity
      // TODO: performer

      return report
   }

   private RmValidationReport validate(PartyIdentity pi, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(pi, o)) // validates name

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (pi ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_details = o.getAttr('details') // item structure
      if (a_details) // if the attribute node is null, all objects validate
      {
         if (pi.details)
         {
            if (checkAllowedType(a_details, pi.details, report)) // only continue if the type is allowed
            {
               report.append(validate(pi.details, a_details))
            }
         }
         else
         {
            if (!a_details.existence.has(0))
            {
               report.addError(o.templateDataPath, "/details is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Composition c, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(c, o)) // validates name

      report.append(validate(c, c.category, o.getAttr('category'), "/category"))

      // the attributes that are optional in the opt should be checked by the parent to avoid calling
      // with null because polymorphism can't find the right method. Also if the constraint is null,
      // anything matches.
      def a_context = o.getAttr('context')
      if (a_context)
      {
         if (c.context)
         {
            report.append(validate(c.context, a_context))
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_context.existence.has(0))
            {
               report.addError("/context", "attribute is not present but is required")
            }
         }
      }

      def a_content = o.getAttr('content')
      if (a_content)
      {
         if (c.content != null)
         {
            //println "composition content "+ c.content
            //println a_content
            //println a_content.children*.rmTypeName
            report.append(validate(c.content, a_content)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_content.existence.has(0))
            {
               report.addError("/content", "attribute is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Folder f, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(f, o)) // validates name

      def a_items = o.getAttr('items')
      if (a_items)
      {
         if (f.items)
         {
            if (checkAllowedType(a_items, f.items, report)) // only continue if the type is allowed
            {
               report.append(validate(f.items, a_items))
            }
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_items.existence.has(0))
            {
               report.addError("/items", "attribute is not present but is required")
            }
         }
      }

      def a_folders = o.getAttr('folders')
      if (a_folders)
      {
         if (f.folders)
         {
            // FIXME: folders can only contain FOLDER so this check is unneded
            if (checkAllowedType(a_folders, f.folders, report)) // only continue if the type is allowed
            {
               // NOTE: not sure if a subfolder could comply with a totally different OPT (so the validator
               // might not be correct here) or if the main OPT should contain all possible archetypes for subfolders.
               // This is validating like the second option.
               report.append(validate(f.folders, a_folders))
            }
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_folders.existence.has(0))
            {
               report.addError("/folders", "attribute is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Section s, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(s, o)) // validates name

      def a_content = o.getAttr('items')
      if (a_content)
      {
         if (s.items != null)
         {
            report.append(validate(s.items, a_content)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the attr
         {
            if (!a_content.existence.has(0))
            {
               // TODO: fix path to add parents
               report.addError(o.templateDataPath, "/items attribute is not present but is required")
            }
         }
      }

      return report
   }

   // all container attributes will get here
   private RmValidationReport validate(List container, AttributeNode c_multiple_attribute)
   {
      //println "validate container attribute: "+ c_multiple_attribute.rmAttributeName
      //println c_multiple_attribute.templatePath
      //println c_multiple_attribute.templateDataPath
      //println c_multiple_attribute.dataPath
      
      RmValidationReport report = new RmValidationReport()

      //println c_multiple_attribute.cardinality.interval
      //println c_multiple_attribute.dataPath +" size: "+ container.size()

      if (!c_multiple_attribute.cardinality.interval.has(container.size()))
      {
         // cardinality error
         // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
         report.addError(c_multiple_attribute.templateDataPath, "Number of objects in container ${container.size()} doesn't match cardinality constraint "+ c_multiple_attribute.cardinality.interval.toString())
      }

      // existence
      if (c_multiple_attribute.existence)
      {
         def existence = (container != null ? 1 : 0)
         if (!c_multiple_attribute.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            // It should be the instance path, which needs the parent, to now the data path of the owner of the container, and name of the current attribute
            // or at least the path from the parent and the name of the container attribute
            report.addError(c_multiple_attribute.templateDataPath, "Node doesn't match existence")
         }
      }

      // validate each item in the container if any
      // and if c_multiple_attribute has children (if not, any object is valid)
      if (c_multiple_attribute.children)
      {
         // println "mattr children "+ c_multiple_attribute.children*.templateDataPath
         // println "container node_ids "+ container*.archetype_node_id
         container.each { item ->

            // println "item "+ item.archetype_node_id
            // println c_multiple_attribute.children*.archetypeId
            // println c_multiple_attribute.children*.nodeId

            // FIXME: this is unreliable, there could be many child objects for the CATTR that are
            //        C_ARCHETYPE_ROOT with the same archetypeId. The name is added to differentiate.
            //
            // each item in the collection should validate against the child object with the same node_id
            //
            // NOTE: the code below is safer since it checks if the name is not there when there are multiple
            //       alternatives with the same archetype_node_id.
            def alternative_objs = c_multiple_attribute.children.findAll{
               //println it.nodeId
               if (it.type == 'C_ARCHETYPE_ROOT')
                  it.archetypeId == item.archetype_node_id
               else
                  it.nodeId == item.archetype_node_id
            }

            def obj

            // println "item "+ item.archetype_node_id
            // println "alternative_objs "+ alternative_objs
            // println "alternative node_ids "+ alternative_objs*.nodeId

            // When there is only one alternative, that is matched by archetype_id or node_id only
            if (alternative_objs.size() == 1)
            {
               obj = alternative_objs[0]

               if (!obj)
               {
                  report.addError(c_multiple_attribute.templateDataPath, "No object found with node_id ${item.archetype_node_id}")
               }
            }
            else // When there are multiple alternatives, the specicic C_OBJECT should be matched by name too (in OPT 1.4 there is no way around)
            {
               // FIXME: when the container has a generated object to comply with the existence and the alternatives in the attribute are only slots,
               // then the object will match all the alternatives by name since for slots the validation returns true

               // if there is a constraint for the name, we should try to use the constrained name first, then the text associated to the node by the node_id
               def error_report, name_constraint
               obj = alternative_objs.find { // it = alt_obj

                  name_constraint = it.getAttr('name')
               
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
               if (!obj)
               {
                  // match by the node text
                  obj = alternative_objs.find{
                     it.text == item.name.value
                  }
               }

               //println obj

               // if none matches it means a. there is a constraint validation issue (none matches the OPT) or b. altenrative nodes are not uniquely named
               if (!obj)
               {
                  //println c_multiple_attribute.templateDataPath
                  report.addError(c_multiple_attribute.templateDataPath, "Multiple alternative constraint objects found for archetype_node_id '${item.archetype_node_id}' at ${c_multiple_attribute.templatePath}, none matches the constraints for the name or the current node text '${item.name.value}' in the OPT")
               }

               //println ""
            }

            
            // println "------"
            // println c_multiple_attribute.children
            // println obj
            // println item
            // println item.archetype_node_id
            
            //println obj.type
            // println "RM TYPE: "+ item.getClass().getSimpleName()
            // println "RM PATH: "+ item.dataPath
            // println "RM NAME: "+ item.name.value

            // println "CATTR RM ATTR NAME: "+ c_multiple_attribute.rmAttributeName
            // println "CATTR PATH: "+ c_multiple_attribute.dataPath
            // println "CATTR TPATH: "+ c_multiple_attribute.templateDataPath
            // println "CATTR CHILDREN TEXT:"+ c_multiple_attribute.children*.text
            // println "CATTR CHILDREN RM TYPE: "+ c_multiple_attribute.children*.rmTypeName
            // println ""

            if (obj && checkAllowedType(c_multiple_attribute, item, report)) // only continue if the type is allowed
            {
               report.append(validate(item, obj))
            }
         }
      }

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

      report.append(_validate_locatable(ob, o)) // validates name

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ob ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // history

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ob.data) // this is mandatory by the RM
         {
            report.append(validate(ob.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      def a_state = o.getAttr('state') // history

      if (a_state) // if the attribute node is null, all objects validate
      {
         if (ob.state) // this is mandatory by the RM
         {
            report.append(validate(ob.state, a_state))
         }
         else
         {
            if (!a_state.existence.has(0))
            {
               report.addError(o.templateDataPath, "/state is not present but is required")
            }
         }
      }


      // TODO: protocol

      return report
   }

   private RmValidationReport validate(History h, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (h ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // TODO: not validating occurrences?

      def a_events = o.getAttr('events')
      if (a_events)
      {
         if (h.events != null)
         {
            report.append(validate(h.events, a_events)) // validate container
         }
         else
         {
            // if the container attribute is null the existence is validated in the parent object
            if (!a_events.existence.has(0))
            {
               report.addError(h.dataPath + "/events", "is not present but is required")
            }
         }
      }

      // TODO: summary

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item_structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (e.data) // this is mandatory by the RM
         {
            if (checkAllowedType(a_data, e.data, report)) // only continue if the type is allowed
            {
               report.append(validate(e.data, a_data))
            }
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      // TODO: state

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item_structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (e.data) // this is mandatory by the RM
         {
            if (checkAllowedType(a_data, e.data, report)) // only continue if the type is allowed
            {
               report.append(validate(e.data, a_data))
            }
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      // TODO: state

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

      report.append(_validate_locatable(ev, o)) // validates name

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ev ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ev.data)
         {
            if (checkAllowedType(a_data, ev.data, report)) // only continue if the type is allowed
            {
               report.append(validate(ev.data, a_data))
            }
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }


      // TODO: protocol

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

      report.append(_validate_locatable(ins, o)) // validates name

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ins ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }
      
      def a_activities = o.getAttr('activities') // List<Activity>
      if (a_activities)
      {
         if (ins.activities != null)
         {
            report.append(validate(ins.activities, a_activities)) // validate container
         }
         else
         {
            // if the container attribute is null the existence is validated in the parent object
            if (!a_activities.existence.has(0))
            {
               report.addError(ins.dataPath + "/activities", "is not present but is required")
            }
         }
      }

      // TODO: protocol

      return report
   }

   private RmValidationReport validate(Activity act, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(_validate_locatable(act, o)) // validates name

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (act ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_description = o.getAttr('description') // item structure

      if (a_description) // if the attribute node is null, all objects validate
      {
         if (act.description)
         {
            if (checkAllowedType(a_description, act.description, report)) // only continue if the type is allowed
            {
               report.append(validate(act.description, a_description))
            }
         }
         else
         {
            // TODO: should validate existence if the attribute node is not null
            if (!a_description.existence.has(0))
            {
               report.addError(o.templateDataPath, "/description is not present but is required")
            }
         }
      }

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

      report.append(_validate_locatable(ac, o)) // validates name

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ac ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_description = o.getAttr('description') // item structure

      if (a_description) // if the attribute node is null, all objects validate
      {
         if (ac.description)
         {
            if (checkAllowedType(a_description, ac.description, report)) // only continue if the type is allowed
            {
               report.append(validate(ac.description, a_description))
            }
         }
         else
         {
            // TODO: should validate existence if the attribute node is not null
            if (!a_description.existence.has(0))
            {
               report.addError(o.templateDataPath, "/description is not present but is required")
            }
         }
      }

      // TODO: protocol

      return report
   }

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

      report.append(_validate_locatable(ae, o)) // validates name

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ae ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ae.data)
         {
            if (checkAllowedType(a_data, ae.data, report)) // only continue if the type is allowed
            {
               report.append(validate(ae.data, a_data))
            }
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError(o.templateDataPath, "/data is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(EventContext context, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (context ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      if (context.other_context)
      {
         report.append(validate(context.other_context, o.getAttr('other_context')))
      }
      else
      {
         // TODO: the existence of the attribute when it's null should be checked by the parent
         // because polymorphism can't choose the right method to call with a null value
      }

      // TODO: is 'participations' archetypable?

      return report
   }
 
   private RmValidationReport validate(ItemTree is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(is.dataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_items = o.getAttr('items')
      if (a_items)
      {
         if (is.items != null)
         {
            report.append(validate(is.items, a_items)) // validate container
         }
         else
         {
            if (!a_items.existence.has(0))
            {
               report.addError(is.dataPath + "/items", "is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(ItemList is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            report.addError(is.dataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_items = o.getAttr('items')
      if (a_items)
      {
         if (is.items != null)
         {
            report.append(validate(is.items, a_items)) // validate container
         }
         else
         {
            if (!a_items.existence.has(0))
            {
               report.addError(is.dataPath + "/items", "is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(ItemTable is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(is.dataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_rows = o.getAttr('rows')
      if (a_rows)
      {
         if (is.rows != null)
         {
            report.append(validate(is.rows, a_rows)) // validate container
         }
         else
         {
            if (!a_rows.existence.has(0))
            {
               report.addError(is.dataPath + "/rows", "is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(ItemSingle is, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(is.dataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      report.append(validate(is.item, o.getAttr('item')))

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (cl ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_items = o.getAttr('items')
      if (a_items)
      {
         if (cl.items != null)
         {
            report.append(validate(cl.items, a_items)) // validate container
         }
         else
         {
            if (!a_items.existence.has(0))
            {
               report.addError(cl.dataPath + "/items", "is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Element e, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (e ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: need to pass the path to the DV* validators so the error reporting can
      // include the path, but should be the data path!

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (e.value)
         {
            if (checkAllowedType(a_value, e.value, report)) // only continue if the type is allowed
            {
               report.append(validate(e, e.value, a_value, "/value"))
            }
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError(e.dataPath + "/value", "is not present but is required")
            }
         }
      }

      def a_null_flavour = o.getAttr('null_flavour')
      if (a_null_flavour)
      {
         if (e.null_flavour)
         {
            report.append(validate(e, e.null_flavour, a_null_flavour, "/null_flavour"))
         }
         else
         {
            // FIXME: existence is null for this attribute, check the OPTs and the parser,
            // since existence should be mandatory or have a default value from the RM
            // added the check for existence
            if (a_null_flavour.existence && !a_null_flavour.existence.has(0))
            {
               report.addError(e.dataPath + "/null_flavour", "is not present but is required")
            }
         }
      }


      return report
   }

   private RmValidationReport validate(Pathable parent, DvCodedText ct, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (ct ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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
         if (!report.hasErrors())
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ct ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_defining_code = o.getAttr('defining_code')
      if (a_defining_code)
      {
         if (ct.defining_code)
         {
            report.append(validate(parent, ct.defining_code, a_defining_code, dv_path + "/defining_code"))
         }
         else
         {
            if (!a_defining_code.existence.has(0))
            {
               report.addError("/defining_code", "is not present but is required")
            }
         }
      }

      // TODO: mappings

      return report
   }

   private RmValidationReport validate(Pathable parent, CodePhrase cp, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (cp ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (cp ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // specific type constraint validation
      ValidationResult valid = o.isValid(cp)

      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }

   private RmValidationReport validate(Pathable parent, DvText te, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (te ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this dv_path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (te ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: validate value string if there is any constrain
      def oa = o.getAttr('value')
      if (oa)
      {
         report.append(validate(parent, te.value, oa, dv_path +"/value"))
      }

      return report
   }

   private RmValidationReport validate(Pathable parent, DvProportion d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: validate numerator and denominator if there is any constrain, and against the type

      return report
   }


   private RmValidationReport validate(Pathable parent, DvQuantity d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // validate magnitude, precision and units
      ValidationResult valid = o.isValid(d)
      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvCount d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // TODO: validate magnitude
      def a_magnitude = o.getAttr('magnitude')
      if (a_magnitude)
      {
         if (d.magnitude != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.magnitude, a_magnitude, dv_path +'/magnitude'))
         }
         else
         {
            if (!a_magnitude.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /magnitude is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Pathable parent, Integer d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d != null ? 1 : 0) // != null is used to avpod the falsy 0
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }



   private RmValidationReport validate(Pathable parent, DvDateTime d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvDate d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvTime d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, String d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvOrdinal d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // def a_value = o.getAttr('value')
      // if (a_value)
      // {
      //    if (d.value != null) // compare to null to avoid 0 as false
      //    {
      //       report.append(validate(parent, d.value, a_value, dv_path +'/value'))
      //    }
      //    else
      //    {
      //       if (!a_value.existence.has(0))
      //       {
      //          report.addError("'${o.templateDataPath}' /value is not present but is required")
      //       }
      //    }
      // }

      ValidationResult valid = o.isValid(d)
      if (!valid)
      {
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvBoolean d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, Boolean d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d != null ? 1 : 0) // != null is used to differentiate from the valid false value
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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
         report.addError(parent.dataPath + dv_path, valid.message)
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvDuration d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvInterval d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      // in the OPT the interval definition is a C_COMPLEX_OBJECT
      // it contains C_COMPLEX_OBJECT for the upper and lower constraints
      // each constraint could be a range for the magnitude, so we could have:
      // lower(a..b) .. upper(c..d) in the OPT
      // then in the data we have an interval (a1..b1) that is lower and higher
      // so we know a1 should be in a..b
      // and b2 should be in c..d

      def a_lower = o.getAttr('lower')
      if (a_lower)
      {
         if (d.lower != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.lower, a_lower, dv_path +'/lower'))
         }
         else
         {
            if (!a_lower.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /lower is not present but is required")
            }
         }
      }

      def a_upper = o.getAttr('upper')
      if (a_upper)
      {
         if (d.upper != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.upper, a_upper, dv_path +'/upper'))
         }
         else
         {
            if (!a_upper.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /upper is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvMultimedia d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            report.addError(parent.dataPath, "Node doesn't match occurrences")
         }
      }
      
      // media_type is mandatory by the RM
      if (!d.media_type)
      {
         report.addError(parent.dataPath + dv_path + "/media_type", "is not present but is required")
      }

      // size is mandatory by the RM
      if (!d.size)
      {
         report.addError(parent.dataPath + dv_path + "/size", "is not present but is required")
      }

      def a_charset = o.getAttr('charset')
      if (a_charset)
      {
         if (d.charset)
         {
            report.append(validate(parent, d.charset, a_charset, dv_path +'/charset'))
         }
         else
         {
            if (!a_charset.existence.has(0))
            {
               report.addError(parent.dataPath + dv_path +"/charset", "is not present but is required")
            }
         }
      }
      
      def a_language = o.getAttr('language')
      if (a_language)
      {
         if (d.language)
         {
            report.append(validate(parent, d.language, a_language, dv_path +'/language'))
         }
         else
         {
            if (!a_language.existence.has(0))
            {
               report.addError(parent.dataPath + dv_path +"/language", "is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(Pathable parent, DvParsable d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            report.addError(parent.dataPath, "Node doesn't match occurrences")
         }
      }

      // value is mandatory by the RM
      if (!d.value)
      {
         report.addError(parent.dataPath + dv_path + "/value", "is not present but is required")
      }

      // formalism is mandatory by the RM
      if (!d.formalism)
      {
         report.addError(parent.dataPath + dv_path + "/formalism", "is not present but is required")
      }

      def a_charset = o.getAttr('charset')
      if (a_charset)
      {
         if (d.charset)
         {
            report.append(validate(parent, d.charset, a_charset, dv_path +'/charset'))
         }
         else
         {
            if (!a_charset.existence.has(0))
            {
               report.addError(parent.dataPath + dv_path +"/charset", "is not present but is required")
            }
         }
      }
      
      def a_language = o.getAttr('language')
      if (a_language)
      {
         if (d.language)
         {
            report.append(validate(parent, d.language, a_language, dv_path +'/language'))
         }
         else
         {
            if (!a_language.existence.has(0))
            {
               report.addError(parent.dataPath + dv_path +"/language", "is not present but is required")
            }
         }
      }

      return report
   }


   // Same validators are used for DvEhrUri
   private RmValidationReport validate(Pathable parent, DvUri d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (d.value != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.value, a_value, dv_path +'/value'))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /value is not present but is required")
            }
         }
      }
      
      return report
   }


   private RmValidationReport validate(Pathable parent, DvIdentifier d, AttributeNode a, String dv_path)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(a.templateDataPath, "Node doesn't match existence")
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (d ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError(o.templateDataPath, "Node doesn't match occurrences")
         }
      }

      def a_issuer = o.getAttr('issuer')
      if (a_issuer)
      {
         if (d.issuer != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.issuer, a_issuer, dv_path + '/issuer'))
         }
         else
         {
            if (!a_issuer.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /issuer is not present but is required")
            }
         }
      }

      def a_type = o.getAttr('type')
      if (a_type)
      {
         if (d.type != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.type, a_type, dv_path +'/type'))
         }
         else
         {
            if (!a_type.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /type is not present but is required")
            }
         }
      }

      def a_assigned = o.getAttr('assigner')
      if (a_assigned)
      {
         if (d.assigner != null) // compare to null to avoid 0 as false
         {
            report.append(validate(parent, d.assigner, a_assigned, dv_path +'/assigned'))
         }
         else
         {
            if (!a_assigned.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /assigner is not present but is required")
            }
         }
      }
      
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
               o.templateDataPath == '/' ? "/name" : o.templateDataPath + "/name",
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
               o.templateDataPath == '/' ? "/name" : o.templateDataPath + "/name",
               "expected name is '${o.text}' and actual name is '${locatable.name.value}'"
            )
         }
      }

      return report
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