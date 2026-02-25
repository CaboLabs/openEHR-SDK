package com.cabolabs.openehr.opt.cli.services

import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.OperationalTemplate

class UiGeneratorService {

   private static String PS = System.getProperty("file.separator")

   static File generateUi(OperationalTemplate opt, String destPath, int bootstrap, boolean fullPage) {
      def gen = new OptUiGenerator(fullPage, bootstrap)
      def ui = gen.generate(opt)

      def fname = destPath + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date())
      def file = new File(fname)
      file << ui
      
      return file
   }
}
