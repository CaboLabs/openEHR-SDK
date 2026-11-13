package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvProportion
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.validation.RmValidator2

// Invariants of DV_PROPORTION in the RM (BMM / data types specification): Type_validity, Precision_validity,
// Is_integral_validity, Fraction_validity, Unitary_validity, Percent_validity and Valid_denominator
class DvProportionInvariantsTest extends GroovyTestCase {

   private List<String> errorPaths(Integer type, BigDecimal numerator, BigDecimal denominator, Integer precision = -1)
   {
      def d = new DvProportion(type: type, numerator: numerator, denominator: denominator, precision: precision)
      def parent = new Element()
      parent.dataPath = '/content[0]/items[0]'
      def o = new ObjectNode(templatePath: '/tp', rmTypeName: 'DV_PROPORTION')

      def report = new RmValidator2().validate(parent, d, o, '/value')
      return report.errors*.dataPath
   }

   void testValid()
   {
      assert errorPaths(0, 3.5, 2.0).isEmpty()      // ratio
      assert errorPaths(1, 0.5, 1).isEmpty()        // unitary
      assert errorPaths(2, 15.5, 100).isEmpty()     // percent
      assert errorPaths(3, 1, 2).isEmpty()          // fraction
      assert errorPaths(4, 3, 2).isEmpty()          // integer fraction
      assert errorPaths(0, 3, 2, 0).isEmpty()       // precision 0 and integral
   }

   void testTypeValidity()
   {
      assert errorPaths(5, 1, 2) == ['/content[0]/items[0]/value/type']
   }

   void testUnitaryAndPercentDenominators()
   {
      assert errorPaths(1, 1, 2) == ['/content[0]/items[0]/value/denominator']   // unitary => denominator 1
      assert errorPaths(2, 1, 50) == ['/content[0]/items[0]/value/denominator']  // percent => denominator 100
   }

   void testValidDenominator()
   {
      assert errorPaths(0, 1, 0) == ['/content[0]/items[0]/value/denominator']
      assert errorPaths(3, 1, 0) == ['/content[0]/items[0]/value/denominator']   // fraction
      assert errorPaths(4, 1, 0) == ['/content[0]/items[0]/value/denominator']   // integer fraction
   }

   void testFractionIsIntegral()
   {
      assert errorPaths(3, 1.5, 2).contains('/content[0]/items[0]/value/numerator')
      assert errorPaths(4, 1, 2.5).contains('/content[0]/items[0]/value/denominator')
   }

   void testPrecisionZeroImpliesIntegral()
   {
      assert errorPaths(0, 1.5, 2, 0) == ['/content[0]/items[0]/value/numerator']
      assert errorPaths(0, 1, 2.5, 0) == ['/content[0]/items[0]/value/denominator']
      assert errorPaths(0, 1.5, 2.5, 1).isEmpty() // precision 1 allows decimals
      assert errorPaths(0, 1.5, 2.5, -1).isEmpty() // no limit
   }
}
