package com.cabolabs.openehr.opt.cli.commands

import picocli.CommandLine.Command
import java.util.concurrent.Callable

@Command(name = "trans", 
         description = "Transform OPT or Locatable between formats",
         subcommands = [TransformOptCommand, TransformLocatableCommand])
class TransformCommand implements Callable<Integer> {

   @Override
   Integer call() {
      println "Usage: trans [opt|locatable] -s <source> -d <dest>"
      println "Use 'trans opt' to transform OPT from XML to JSON"
      println "Use 'trans locatable' to transform Locatable between XML and JSON"
      return 0
   }
}
