openEHR-OPT
===========

Groovy Support of openEHR Operational Templates for CaboLabs Grails/Java projects, with advanced command line tools.

This will be used in CaboLabs apps like EHRGen, EHRServer, EMRApp and XML Rule Engine.

## Commands

Generate UI for data input

> opt uigen path_to_opt dest_folder


Generate XML instances with random data

> opt ingen path_to_opt dest_folder [amount] [version|composition|version_committer|tagged]

1. amount: defines how many XML instances will be generated
2. version: generates an instance of a VERSION object
3. composition: generates an instance of a COMPOSITION object
4. version_committer: generates an instance with the format required by the [EHRCommitter] to generate the UI and load data to test the [EHRServer].
5. tagged: generates a version instance with tags instead of data, useful to inject data from your app to commit to the [EHRServer]

Validate XML instances

Validate one instance:

> opt inval path_to_xml_instance


Validate all instances in folder:

> opt inval path_to_folder_with_xml_instances


[EHRCommitter]: https://github.com/ppazos/EHRCommitter
[EHRServer]: https://github.com/ppazos/cabolabs-ehrserver


## Requires Java 8 and Groovy 2.5.5+

Note: check the opt.sh/opt.bat files to see if the correct path to the groovy dependencies on your machine is set there.
