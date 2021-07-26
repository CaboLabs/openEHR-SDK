package com.cabolabs.openehr.validation

import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.rm_1_0_2.composition.*
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*
import com.cabolabs.openehr.opt.model.validation.*
import com.cabolabs.openehr.opt.model.domain.*

class RmValidator {

   OptManager opt_manager

   RmValidator(OptManager opt_manager)
   {
      this.opt_manager = opt_manager
   }

   RmValidationReport dovalidate(Composition c)
   {
      String template_id = c.archetype_details.template_id.value

      def opt = this.opt_manager.getOpt(template_id)

      if (!opt) return opt_not_found(template_id)

      return validate(c, opt.definition)
   }

   private RmValidationReport validate(Composition c, ObjectNode o)
   {
      RmValidationReport report = new RmValidationReport()

      report.append(validate(c.category, o.getAttr('category')))

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
         else // parent validates the existence if the attribute is null: should validate existence 0 of the context
         {
            if (!a_context.existence.has(0))
            {
               report.addError("/context is not present but is required")
            }
         }
      }

      def a_content = o.getAttr('content')
      if (a_content)
      {
         if (c.content != null)
         {
            report.append(validate(c.content, a_content)) // validate container
         }
         else // parent validates the existence if the attribute is null: should validate existence 0 of the context
         {
            if (!a_content.existence.has(0))
            {
               report.addError("/content is not present but is required")
            }
         }
      }

      return report
   }

   // all container attributes will get here
   private RmValidationReport validate(List container, AttributeNode c_multiple_attribute)
   {
      println "validate container attribute "+ c_multiple_attribute.templatePath
      RmValidationReport report = new RmValidationReport()

      if (!c_multiple_attribute.cardinality.interval.has(container.size()))
      {
         // cardinality error
         // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
         report.addError("Node '${o.templateDataPath}' doesn't match cardinality")
      }

      // existence
      if (c_multiple_attribute.existence)
      {
         def existence = (container ? 1 : 0) // FIXME: all attribute existence should be validated in the parent object, since if it's null, it can't call the method
         if (!c_multiple_attribute.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${c_multiple_attribute.templateDataPath}' doesn't match existence")
         }
      }

      // validate each item in the container if any
      // and if c_multiple_attribute has children (if not, any object is valid)
      if (c_multiple_attribute.children)
      {
         //println container*.value
         //println container*.archetype_node_id
         container.each { item ->

            println "item "+ item.archetype_node_id
         
            // each item in the collection should validate against the child object with the same node_id
            def obj = c_multiple_attribute.children.find{ it.nodeId == item.archetype_node_id }
            if (obj)
            {
               report.append(validate(item, obj))
            }
            else
            {
              println "No object node with node_id ${item.archetype_node_id}"
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ob ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
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
               report.addError("'${o.templateDataPath}' /data is not present but is required")
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
               report.addError("'${o.templateDataPath}' /state is not present but is required")
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
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
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

      def a_events = o.getAttr('events')
      if (a_events)
      {
         if (h.events != null)
         {
            report.append(validate(h.events, a_events)) // validate container
         }
         else
         {
            if (!a_events.existence.has(0))
            {
               report.addError("/events is not present but is required")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item_structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (e.data) // this is mandatory by the RM
         {
            report.append(validate(e.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /data is not present but is required")
            }
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item_structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (e.data) // this is mandatory by the RM
         {
            report.append(validate(e.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /data is not present but is required")
            }
         }
      }

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ev ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ev.data)
         {
            report.append(validate(ev.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /data is not present but is required")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ins ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      /*
      def activities = o.getAttr('activities') // List<Activity>

      if (ins.activities)
      {
         if (activities) // if the attribute node is null, all objects validate
         {
            report.append(validate(ins.activities, activities)) // validate container
         }
      }
      else
      {
         // TODO: should validate existence if the attribute node is not null
      }
      */


      // TODO: protocol

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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ac ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      def a_description = o.getAttr('description') // item structure

      if (a_description) // if the attribute node is null, all objects validate
      {
         if (ac.description)
         {
            report.append(validate(ac.description, a_description))
         }
         else
         {
            // TODO: should validate existence if the attribute node is not null
            if (!a_description.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /description is not present but is required")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (ae ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      def a_data = o.getAttr('data') // item structure

      if (a_data) // if the attribute node is null, all objects validate
      {
         if (ae.data)
         {
            report.append(validate(ae.data, a_data))
         }
         else
         {
            if (!a_data.existence.has(0))
            {
               report.addError("'${o.templateDataPath}' /data is not present but is required")
            }
         }
      }

      return report
   }


   private RmValidationReport validate(DvCodedText ct, AttributeNode a)
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
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(ct, a.children))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(DvCodedText ct, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(ct, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(DvCodedText ct, ObjectNode o)
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
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      def a_defining_code = o.getAttr('defining_code')
      if (a_defining_code)
      {
         if (ct.defining_code)
         {
            report.append(validate(ct.defining_code, a_defining_code))
         }
         else
         {
            if (!a_defining_code.existence.has(0))
            {
               report.addError("/defining_code is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(CodePhrase cp, AttributeNode a)
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
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(cp, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(CodePhrase cp, List<CCodePhrase> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(cp, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(CodePhrase cp, CCodePhrase o)
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
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      // specific type constraint validation
      ValidationResult valid = o.isValid(cp.code_string, cp.terminology_id.value)

      if (!valid)
      {
         report.addError(r.message)
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
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
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
         report = validate(cp, o)
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
         report.append(context.other_context, o.getAttr('other_context'))
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
      println "validate ItemTree attribute "+ a.templatePath
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
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
               report.addError("/items is not present but is required")
            }
         }
      }

      return report
   }



   private RmValidationReport validate(ItemList is, AttributeNode a)
   {
      println "validate ItemList attribute "+ a.templatePath
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      // TODO: attributes

      return report
   }



   private RmValidationReport validate(ItemTable is, AttributeNode a)
   {
      println "validate ItemTable attribute "+ a.templatePath
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      // TODO: attributes

      return report
   }



   private RmValidationReport validate(ItemSingle is, AttributeNode a)
   {
      println "validate ItemSingle attribute "+ a.templatePath
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (is ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (is ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
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

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (cl ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
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
               report.addError("/items is not present but is required")
            }
         }
      }

      return report
   }

   private RmValidationReport validate(Element e, AttributeNode a)
   {
      println "validate Element attribute"
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (e ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(e, a.children))

      return report
   }

   private RmValidationReport validate_alternatives(Element e, List<ObjectNode> os)
   {
      println "validate_alternatives Element"
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
      println "validate Element object "+ o.templatePath
      RmValidationReport report = new RmValidationReport()

      // occurrences
      if (o.occurrences)
      {
         def occurrences = (e ? 1 : 0)
         if (!o.occurrences.has(occurrences))
         {
            // occurrences error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      def a_value = o.getAttr('value')
      if (a_value)
      {
         if (e.value)
         {
            report.append(validate(e.value, a_value))
         }
         else
         {
            if (!a_value.existence.has(0))
            {
               report.addError("/value is not present but is required")
            }
         }
      }

      /*
      if (e.null_flavour)
      {
         report.append(validate(e.null_flavour, o.getAttr('null_flavour')))
      }
      else
      {
         // TODO: the existence of the attribute when it's null should be checked by the parent
         // because polymorphism can't choose the right method to call with a null value
      }
      */

      return report
   }


   private RmValidationReport validate(DvText te, AttributeNode a)
   {
      println "validate DvText attribute "+ a.templatePath
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (te ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(te, a.children))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(DvText te, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(te, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(DvText te, ObjectNode o)
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
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      // TODO: validate value string if there is any constrain

      return report
   }



   private RmValidationReport validate(DvProportion d, AttributeNode a)
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
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(d, a.children))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(DvProportion d, List<ObjectNode> os)
   {
      RmValidationReport report
      for (o in os)
      {
         report = validate(d, o)
         if (!report.hasErrors())
         {
            return report
         }
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return report
   }

   private RmValidationReport validate(DvProportion d, ObjectNode o)
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
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      // TODO: validate numerator and denominator if there is any constrain, and against the type

      return report
   }


   private RmValidationReport validate(DvQuantity d, AttributeNode a)
   {
      println "validate DvQuantity attribute "+ a.templatePath
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (d ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(d, a.children))

      return report
   }

   // validates against the children of a CSingleAttribute
   // should check all the constraints and if one validates, the whole thing validates
   private RmValidationReport validate_alternatives(DvQuantity d, List<ObjectNode> os)
   {
      println "validate_alternatives DvQuantity"

      RmValidationReport report
      for (o in os)
      {
         println o.type +" "+ o.rmTypeName
         /*
         report = validate(d, o)
         if (!report.hasErrors())
         {
            return report
         }
*/
      }

      // this will return the last failed validation
      // we can also add an error saying the data doesn't validates against any alternative
      return new RmValidationReport() //report
   }

   private RmValidationReport validate(DvQuantity d, CDvQuantity o)
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
            report.addError("Node '${o.templateDataPath}' doesn't match occurrences")
         }
      }

      // TODO: validate magnitude, precision and units
      ValidationResult valid = o.isValid(d.units, d.magnitude)
      if (!valid)
      {
         report.addError(valid.message)
      }

      return report
   }


   RmValidationReport opt_not_found(String template_id)
   {
      def report = new RmValidationReport()
      report.addError("Template '${template_id}' was not found")
      return report
   }
}