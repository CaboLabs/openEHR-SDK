openEHR-OPT
===========

Java/Groovy Support of openEHR Operational Templates, Refernce Model, Data Generators and other tools for www.CaboLabs.com projects.

This will be used in CaboLabs apps like EHRGen, EHRServer, EMRApp and XML Rule Engine.


## Build

The build was tested with [Gradle 6.4.1](https://gradle.org/install/) installed from [SDKMAN!](https://sdkman.io/).

```shell
$ cd openEHR-OPT
$ gradle build
```

### Requires Java 8+ and Groovy 2.5.5+

> - - - - -
> Note: check the opt.sh/opt.bat files to see if the correct path to the groovy dependencies on your machine is set there.
> - - - - -

That will run the tests and build the file ./build/libs/opt.jar

For running tests, there are many options, examples below:

1. Run specific test case from a specific suite
2. Run all tests from a specific suite
3. Run all suites in a package
4. Run all tests

```shell
$ cd openEHR-OPT
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest.testCompleteOPT
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest
$ gradle test --tests com.cabolabs.openehr.opt*
$ gradle test
```

The test report in HTML will be under ./build/reports/tests/test/index.html


## Command Tools (CLI)

### uigen: Generate UI for data input

```shell
$ ./opt.sh uigen path_to_opt dest_folder
```

### ingen: Generate XML instances from OPTs with random data

```shell
$ ./opt.sh ingen path_to_opt dest_folder [amount] [version|composition|version_committer|tagged|json_version|json_composition|json_compo_with_errors] [withParticipations]
```

1. amount: defines how many XML instances will be generated
2. version: generates an instance of a VERSION object
3. composition: generates an instance of a COMPOSITION object
4. version_committer: generates an instance with the format required by the [EHRCommitter] to generate the UI and load data to test the [EHRServer].
5. tagged: generates a version instance with tags instead of data, useful to inject data from your app to commit to the [EHRServer]
6. json_version: openEHR canonical JSON VERSION object
7. json_composition: openEHR canonical JSON COMPOSITION object
8. json_compo_with_errors: canonical JSON COMPOSITION object with violating data elements for cardinality constraints (purpose: data validation testing)


### inval: Validate XML or JSON instances against the schemas

Validate one instance:

```shell
$ ./opt.sh inval path_to_xml_or_json_instance
```

Validate all instances in folder:

```shell
$ ./opt.sh inval path_to_folder_with_xml_or_json_instances
```

> Note: if the folder contains JSON and XML, it will validate both with the correct schema, but the files should have .json or .xml extensions for the mixed validation to work OK.


In both cases, the output is "file IS VALID" or the list of validation errors if the file is not valid against the schemas.


### trans opt: Transform an OPT in it's antive XML form to JSON

```shell
$ ./opt.sh trans opt path_to_opt destination_folder
```

### trans composition: Transform an COMPOSITION instances between canonical XML and JSON formats

To transform a XML COMPOSITION to JSON:

```shell
$ ./opt.sh trans composition path_to_compo.xml destination_folder
```
To transform a JSON COMPOSITION to JSON:

```shell
$ ./opt.sh trans composition path_to_compo.json destination_folder
```

> Note: the transformation of COMPOSITIONS between foramts relies on the file extension, only .xml or .json files are allowed.


## Use as Java/Groovy library

### Parse an openEHR Operational Template

```groovy
OperationalTemplate loadAndParse(String path)
{
   def parser = new OperationalTemplateParser()

   def optFile = new File(getClass().getResource(path).toURI())
   def text = optFile.getText()

   return parser.parse(text)
}
```

To add the atttributes that are in the openEHR Reference Model but are not in the source OPT file, use the method complete():

```groovy
def opt = loadAndParse("vital_signs.opt")
opt.complete()
```

### Parse a JSON COMPOSITION

```groovy
String path = "vital_signs.json"
File file = new File(getClass().getResource(path).toURI())
String json = file.text
def parser = new OpenEhrJsonParser()
Composition c = (Composition)parser.parseJson(json)
```

### Serialize a COMPOSITION to JSON

```groovy
Composition compo = ...
def serializer = new OpenEhrJsonSerializer()
String json = serializer.serialize(compo)
```

### Parse a XML COMPOSITION

```groovy
String path = "vital_signs.xml"
File file = new File(getClass().getResource(path).toURI())
String xml = file.text
def parser = new OpenEhrXmlParser()
Composition c = (Composition)parser.parseXml(xml)
```

### Serialize a COMPOSITION to XML

```groovy
Composition compo = ...
OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
String xml = marshal.serialize(compo)
```

### Validate COMPOSITION against OPT

```groovy
// setup OPT repository
String opt_repo_path = "opts"
OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
OptManager opt_manager = OptManager.getInstance()
opt_manager.init(repo)

// load COMPOSITION
Composition compo = ...

// the validator automatically gets the tempalte from the repo based on the template_id in the COMPOSITION
RmValidator validator = new RmValidator(opt_manager)
RmValidationReport report = validator.dovalidate(compo, OptManager.DEFAULT_NAMESPACE)

report.errors.each { error ->
   println error
}
```


[EHRCommitter]: https://github.com/ppazos/EHRCommitter
[EHRServer]: https://github.com/ppazos/cabolabs-ehrserver


