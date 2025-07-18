package com.cabolabs.openehr.rm_1_0_2.support.identification

import groovy.util.GroovyTestCase
import org.ietf.jgss.GSSException

class UIDGroovyTestCase extends GroovyTestCase {

   void testUUIDCreationRandom() {
      def uuid = new UUID()
      assertNotNull uuid.value
      assertTrue UID.isUUID(uuid.value.toString())
   }

   void testUUIDCreationFromString() {
      def validUUID = "550e8400-e29b-41d4-a716-446655440000"
      def uuid = new UUID(validUUID)
      assertEquals validUUID, uuid.value.toString()
   }

   void testUUIDCreationFromInvalidString() {
      shouldFail(IllegalArgumentException) {
         new UUID("invalid-uuid-string")
      }
   }

   void testOIDCreationFromString() {
      def validOID = "2.16.840.1.113883.3"
      def oid = new OID(validOID)
      assertEquals validOID, oid.value.toString()
   }

   void testOIDCreationFromInvalidString() {
      shouldFail(GSSException) {
         new OID("invalid.oid..string")
      }
   }

   void testInternetIdCreationFromString() {
      def validDomain = "example.org"
      def internetId = new InternetId(validDomain)
      assertEquals validDomain, internetId.value.toString()
   }

   // void testInternetIdCreationFromInvalidString() {
   //    shouldFail(IllegalArgumentException) {
   //       new InternetId("invalid           domain") // missing scheme
   //    }
   // }

   void testUIDBuildUUID() {
      def input = "550e8400-e29b-41d4-a716-446655440000"
      def uid = UID.build(input)
      assertTrue uid instanceof UUID
      assertEquals input, uid.value.toString()
   }

   void testUIDBuildOID() {
      def input = "2.16.840.1.113883.3"
      def uid = UID.build(input)
      assertTrue uid instanceof OID
      assertEquals input, uid.value.toString()
   }

   void testUIDBuildInternetId() {
      def input = "openehr.org"
      def uid = UID.build(input)
      assertTrue uid instanceof InternetId
      assertEquals input, uid.value.toString()
   }

   void testUIDBuildInvalid()
   {
      [null, "", "not.a.valid..oid", "-invalid.domain", "550e8400-e29b-41d4-a716-INVALID"].each { input ->
         println shouldFail(IllegalArgumentException) {
            UID.build(input)
         }
      }
   }

   void testIsUUID() {
      assertTrue UID.isUUID("550e8400-e29b-41d4-a716-446655440000")
      assertFalse UID.isUUID("550e8400-e29b-41d4-a716")
      assertFalse UID.isUUID("2.16.840.1.113883.3")
      assertFalse UID.isUUID("openehr.org")
      assertFalse UID.isUUID(null)
      assertFalse UID.isUUID("")
   }

   void testIsOID() {
      assertTrue UID.isOID("2.16.840.1.113883.3")
      assertTrue UID.isOID("1")
      assertFalse UID.isOID("2..16.840")
      assertFalse UID.isOID("550e8400-e29b-41d4-a716-446655440000")
      assertFalse UID.isOID("openehr.org")
      assertFalse UID.isOID(null) // cant pass null to assertFalse
      assertFalse UID.isOID("")
   }

   void testIsInternetId() {
      assertTrue UID.isInternetId("openehr.org")
      assertTrue UID.isInternetId("open-ehr.org")
      assertTrue UID.isInternetId("localhost")
      assertTrue UID.isInternetId("localhost-something")
      assertTrue UID.isInternetId("a.b.c")
      assertTrue UID.isInternetId("2.16.840.1.113883.3") // fails, this is actually valid for internet ID

      assertFalse UID.isInternetId("-invalid.org")
      //assertTrue UID.isUID("not-a-valid-id")
      //assertFalse UID.isInternetId("550e8400-e29b-41d4-a716-446655440000") // fails
      assertFalse UID.isInternetId(null)
      assertFalse UID.isInternetId("")
   }

   void testIsUID() {
      assertTrue UID.isUID("550e8400-e29b-41d4-a716-446655440000")
      assertTrue UID.isUID("2.16.840.1.113883.3")
      assertTrue UID.isUID("openehr.org")
      // assertFalse UID.isUID("not-a-valid-id")
      assertFalse UID.isUID(null)
      assertFalse UID.isUID("")
   }
}
