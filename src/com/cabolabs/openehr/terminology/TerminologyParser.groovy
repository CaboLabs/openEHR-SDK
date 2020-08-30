package com.cabolabs.openehr.terminology

import com.cabolabs.openehr.opt.model.CodedTerm
import com.cabolabs.openehr.opt.model.Term
import groovy.util.slurpersupport.GPathResult
import org.apache.log4j.Logger

class TerminologyParser {

   private static TerminologyParser instance = null
   private Logger log = Logger.getLogger(getClass())

   // TODO: now just read everything, we need to consider the language!!!
   Map<String, LocalizedCodedTerm> terms = [:]

   List<String> languages = []

   public static TerminologyParser getInstance()
   {
      if (!instance) instance = new TerminologyParser()
      return instance
   }

   private TerminologyParser()
   {
   }


   Map<String, LocalizedCodedTerm> parseTerms(InputStream terminology)
   {
      def trmnlgy = new XmlSlurper().parse( terminology )
      return parseTerms(trmnlgy)
   }


   /**
    * Parse from one file, aggregates to current parsed terms without repeating them.
    * @param terminology
    * @return
    */
   Map<String, LocalizedCodedTerm> parseTerms(File terminology)
   {
      def trmnlgy = new XmlSlurper().parse( terminology )
      return parseTerms(trmnlgy)
   }

   // FIXME: this is returning CodedTerm not LocalizedCodedTerm
   Map<String, LocalizedCodedTerm> parseTerms(GPathResult trmnlgy)
   {
      def res = [:]

      /* LIST OF CODESET OR GROUP, CODESET is a list of values, group is a list of concepts id/rubric
       * The language is on the root element.
       * <terminology name="openehr" language="en">
            <codeset issuer="openehr" openehr_id="compression algorithms" external_id="openehr_compression_algorithms">
               <code value="compress"/>
               <code value="deflate"/>
               ...
            </codeset>
            <group name="term mapping purpose">
               <concept id="669" rubric="public health"/>
               <concept id="670" rubric="reimbursement"/>
               <concept id="671" rubric="research study"/>
            </group>
            ...
       */

      def lang = trmnlgy.@language.text()

      this.languages << lang

      // TODO> NOT supporting codesets for the moment

      trmnlgy.group.each { g ->
        //println "loading group: "+ g.@name.text()
        g.concept.each { c ->

           res << [ (lang +'_'+ c.@id.text()): new CodedTerm(code:c.@id.text(), term: new Term(text:c.@rubric.text())) ]
        }
      }

      this.terms << res

      return res
   }

   /**
    * Parse from many files.
    * @param terminology
    * @return
    */
   Map<String, LocalizedCodedTerm> parseTerms(List<File> terminologies)
   {
      def res = [:]
      terminologies.each {
         res << parseTerms(it)
      }
      return res
   }

   Map<String, LocalizedCodedTerm> getTerms()
   {
      return this.terms.asImmutable()
   }

   String getRubric(String lang, String code)
   {
      if (this.languages) throw new Exception("openEHR terminologies not loaded")

      def fb_lang = this.languages[0]

      if (!this.languages.contains(lang))
      {
         log.info("openEHR terminology not loaded for '$lang', falling back to '$fb_lang'")

         if (!this.terms[fb_lang +'_'+ code])
         {
            log.error("term for language '$fb_lang' and code '$code' is not defined in the openEHR terminology, can't fall back") // if the language is not supported will fall here
            return
         }
         return this.terms[fb_lang +'_'+ code]?.term.text  
      }

      if (!this.terms[lang +'_'+ code])
      {   
         log.info("term for language '$lang' and code '$code' is not defined in the openEHR terminology, falling back to '$fb_lang'") // if the language is not supported will fall here

         if (!this.terms[fb_lang +'_'+ code])
         {
            log.error("term for language '$fb_lang' and code '$code' is not defined in the openEHR terminology, can't fall back") // if the language is not supported will fall here
            return
         }
         return this.terms[fb_lang +'_'+ code]?.term.text   
      }
      return this.terms[lang +'_'+ code]?.term.text
   }

   // loaded languages
   List<String> getLanguages()
   {
      return this.languages.asImmutable()
   }
}
