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

   void testIsUidBasedId() {

      def validTestCases = [
         // Valid cases
         "87284370-2D4B-4e3d-A3F3-F303D2F4F34B::uk.nhs.ehr1::2",                    // UUID::InternetID::trunk
         "87284370-2D4B-4e3d-A3F3-F303D2F4F34B::uk.nhs.ehr1::2.1.5",                // UUID::InternetID::full version
         "1.2.840.113556::2.16.840.1.113883::1",                                     // OID::OID::trunk
         "1.2.840.113556::2.16.840.1.113883::1.2.3",                                 // OID::OID::full version
         "my_system.domain::another-system_v2.org::10",                              // InternetID::InternetID::trunk
         "my_system.domain::another-system_v2.org::10.5.100",                        // InternetID::InternetID::full version
         "12345678-ABCD-1234-EFGH-123456789ABC::1.2.3.4.5::999",                    // UUID::OID::trunk
         "org.example::a1b2c3d4-1234-5678-9abc-def012345678::1.0.1",                // InternetID::UUID::full version
      ]

      def invalidTestCases = [
         "not a uid::uk.nhs.ehr1::2",                                               // Invalid UID
         "87284370-2D4B-4e3d-A3F3-F303D2F4F34B::uk.nhs.ehr1::2.1",                  // Invalid version (only 2 parts)
         "87284370-2D4B-4e3d-A3F3-F303D2F4F34B::uk.nhs.ehr1::abc",                  // Invalid version (non-numeric)
         "87284370-2D4B-4e3d-A3F3-F303D2F4F34B:uk.nhs.ehr1:2",                      // Wrong separator (single colon)
         "87284370-2D4B-4e3d-A3F3-F303D2F4F34B::uk.nhs.ehr1",                       // Missing version
      ]

      validTestCases.each { input ->
         //  def is = UIDBasedId.isUIDBasedId(input)
         // println "${is ? '✓' : '✗'} ${input}"
         assert UIDBasedId.isUIDBasedId(input)
      }

      invalidTestCases.each { input ->
         // def is = UIDBasedId.isObjectVersionId(input)
         // println "${is ? '✓' : '✗'} ${input}"

         // is = UIDBasedId.isHierObjectId(input)
         // println "${is ? '✓' : '✗'} ${input}"
         assertFalse UIDBasedId.isUIDBasedId(input)
      }
   }
}
