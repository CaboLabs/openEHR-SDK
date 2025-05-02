package com.cabolabs.openehr.terminology

import com.cabolabs.openehr.opt.model.CodedTerm
import com.cabolabs.openehr.opt.model.Term
import groovy.util.slurpersupport.GPathResult

// Parser and cache for the openEHR terminology XML files.
@groovy.util.logging.Slf4j
class TerminologyParser {

   private static TerminologyParser instance = null

   // access through the group
   // lang -> group openehr_id -> code, rubric
   Map<String, Map<String, List<CodedRubric>>> codedRubrics = [:]

   // access through the code
   // lang -> code -> code, rubric
   Map<String, Map<String, CodedRubric>> flatCodedRubrics = [:]

   List<String> languages = []

   public static TerminologyParser getInstance()
   {
      if (!instance) instance = new TerminologyParser()
      return instance
   }

   private TerminologyParser()
   {
   }


   void parseTerms(InputStream terminology)
   {
      def trmnlgy = new XmlSlurper().parse( terminology )
      parseTerms(trmnlgy)
   }


   /**
    * Parse from one file, aggregates to current parsed terms without repeating them.
    * @param terminology
    * @return
    */
   void parseTerms(File terminology)
   {
      def trmnlgy = new XmlSlurper().parse( terminology )
      parseTerms(trmnlgy)
   }

   // FIXME: this is returning CodedTerm not LocalizedCodedTerm
   void parseTerms(GPathResult trmnlgy)
   {
      def lang = trmnlgy.@language.text()

      this.languages << lang

      codedRubrics[lang] = [:]
      flatCodedRubrics[lang] = [:]

      trmnlgy.group.each { group ->

         codedRubrics[lang][group.@openehr_id.text()] = []

         group.concept.each { c ->

            codedRubrics[lang][group.@openehr_id.text()] << new CodedRubric(code:c.@id.text(), rubric:c.@rubric.text())

            flatCodedRubrics[lang][c.@id.text()] = new CodedRubric(code:c.@id.text(), rubric:c.@rubric.text())
         }
      }

      // TODO: it doesn't supporting codesets.
   }

   /**
    * Parse from many files.
    * @param terminology
    * @return
    */
   void parseTerms(List<File> terminologies)
   {

      terminologies.each {
         parseTerms(it)
      }
   }

   Map<String, Map<String, CodedRubric>> getTerms()
   {
      return this.flatCodedRubrics.asImmutable()
   }

   String getRubric(String lang, String code)
   {
      if (!this.languages) throw new Exception("openEHR terminologies not loaded")

      if (!this.languages.contains(lang))
      {
         def fb_lang = this.languages[0]

         log.info("openEHR terminology not loaded for '$lang', falling back to '$fb_lang'")

         lang = fb_lang
      }

      if (!this.flatCodedRubrics[lang][code])
      {
         log.error("term for language '$lang' and code '$code' is not defined in the openEHR terminology, can't fall back")
         return
      }

      return this.flatCodedRubrics[lang][code]?.rubric
   }

   // loaded languages
   List<String> getLanguages()
   {
      return this.languages.asImmutable()
   }

   List<CodedRubric> getGroupConcepts(String group, String lang = "en")
   {
      if (!this.languages) throw new Exception("openEHR terminologies not loaded")

      if (!this.languages.contains(lang))
      {
         log.info("openEHR terminology not loaded for '$lang', falling back to '${this.languages[0]}'")
         lang = this.languages[0]
      }

      return this.codedRubrics[lang][group]
   }


}
