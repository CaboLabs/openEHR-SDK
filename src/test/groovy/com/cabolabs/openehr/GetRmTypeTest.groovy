package com.cabolabs.openehr

import com.cabolabs.openehr.rm_1_0_2.ehr.*
import com.cabolabs.openehr.rm_1_0_2.demographic.*
import com.cabolabs.openehr.dto_1_0_2.demographic.*

// Tests the method defined in Locatable
class GetRmTypeTest extends GroovyTestCase {

   void testGetRmType()
   {
      def st = new EhrStatus()
      assert st.getRmType() == 'EHR_STATUS'

      def pr = new PartyRelationshipDto()
      assert pr.getRmType() == 'PARTY_RELATIONSHIP'

      def ro = new Role()
      assert ro.getRmType() == 'ROLE'

   }
}