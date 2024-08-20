package com.cabolabs.openehr.rm_1_0_2.support.identification

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class ArchetypeId extends ObjectId {

   /* TODO methods
   qualified_rm_entity: String
   rm_originator: String
   rm_name: String
   rm_entity: String
   domain_concept: String
   specialisation: String
   version_id: String
   */

   // Prevents groovy.lang.GroovyRuntimeException: Could not find named-arg compatible constructor with the second constructor
   ArchetypeId()
   {

   }

   ArchetypeId(String value)
   {
      this.value = value
   }

   String getQualifiedRmEntity()
   {
      // TODO
   }

   String getRmOriginator()
   {
      // TODO
   }

   String getRmName()
   {
      // TODO
   }

   String getRmEntity()
   {
      // TODO
   }

   String getDomainConcept()
   {
      // TODO
   }

   // US english variant
   String getSpecialization()
   {
      // TODO
   }

   // Original UK english
   String getSpecialisation()
   {
      // TODO
   }

   String getVersionId()
   {
      // TODO
   }
}
