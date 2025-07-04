package com.cabolabs.openehr.rm_1_0_2.support.identification

abstract class UID {

   // The implementation of the value uses corresponding Java classes
   // That implement UUID and OID.
   //String value

   // Individual regex patterns
   static def UUID_PATTERN = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/
   static def OID_PATTERN = /^[0-9]+(\.[0-9]+)*$/

   // RFC 1034 Internet ID (domain name) pattern
   // Labels: 1-63 chars, alphanumeric + hyphens, no leading/trailing hyphens
   // Domain: dot-separated labels, max 253 chars total
   static def INTERNET_ID_PATTERN = /^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/

   static def UUID_OR_OID_OR_INTERNET_ID_PATTERN = /^([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|[0-9]+(\.[0-9]+)*|[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)$/


   /**
    * Factory method to create a UID instance based on the format of the input string.
    *
    * @param uid The UID string to parse.
    * @return An instance of UID (UUID, OID, or InternetId).
    * @throws IllegalArgumentException if the input does not match any known format.
    */
   static UID build(String uid)
   {
      if (uid == null || uid.isEmpty())
         throw new IllegalArgumentException("UID cannot be null or empty")

      if (isUUID(uid))
      {
         return new UUID(uid)
      }
      else if (isOID(uid))
      {
         return new OID(uid)
      }
      else if (isInternetId(uid))
      {
         return new InternetId(uid)
      }
      else
      {
         throw new IllegalArgumentException("UID '$uid' does not match any known format (UUID, OID, or Internet ID)")
      }
   }

   static boolean isUUID(String input)
   {
      return input?.matches(UUID_PATTERN) ?: false
   }

   static boolean isOID(String input)
   {
      return input?.matches(OID_PATTERN) ?: false
   }

   static boolean isInternetId(String input)
   {
      return input?.matches(INTERNET_ID_PATTERN) ?: false
   }

   // checks the value is any of the three formats: UUID, OID, or Internet ID
   static boolean isUID(String input)
   {
      return input?.matches(UUID_OR_OID_OR_INTERNET_ID_PATTERN) ?: false
   }
}
