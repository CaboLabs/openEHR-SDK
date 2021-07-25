package com.cabolabs.openehr.validation

import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.rm_1_0_2.composition.*
import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.*
import com.cabolabs.openehr.opt.model.validation.*
import com.cabolabs.openehr.opt.model.domain.* 
class RmValidator {

   def opt_manager

   RmValidator(String opt_repo_path)
   {
      // TODO: add support for S3 repo
      def repo = new OptRepositoryFSImpl(opt_repo_path)
      this.opt_manager = OptManager.getInstance(repo)
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

      report.append(validate(c.context, o.getAttr('context')))

      report.append(validate(c.content, o.getAttr('content')))

      return report
   }

   private RmValidationReport validate(DvCodedText category, AttributeNode a)
   {
      RmValidationReport report = new RmValidationReport()

      // existence
      if (a.existence)
      {
         def existence = (category ? 1 : 0)
         if (!a.existence.has(existence))
         {
            // existence error
            // TODO: not sure if this path is the right one, I guess should be calculated from the instance...
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(category, a.children))

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

      def attr_defining_code = o.getAttr('defining_code')

      report.append(validate(ct.defining_code, attr_defining_code))

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

      report.append(context.other_context, o.getAttr('other_context'))

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

      // TODO: attributes

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
            report.addError("Node '${a.templateDataPath}' doesn't match existence")
         }
      }

      report.append(validate_alternatives(is, a.children))

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

      if (e.value)
      {
         report.append(validate(e.value, o.getAttr('value')))
      }
      else
      {
         // TODO: the existence of the attribute when it's null should be checked by the parent
         // because polymorphism can't choose the right method to call with a null value
      }

      if (e.null_flavour)
      {
         report.append(validate(e.null_flavour, o.getAttr('null_flavour')))
      }
      else
      {
         // TODO: the existence of the attribute when it's null should be checked by the parent
         // because polymorphism can't choose the right method to call with a null value
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