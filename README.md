openEHR-SDK
===========

Java/Groovy library for openEHR Operational Templates, Reference Model, Data Generators and other tools for www.CaboLabs.com projects.

This library is used in CaboLabs apps like EHRGen, EHRServer, EMRApp and XML Rule Engine.


## Build

The build was tested with [Gradle 6.4.1](https://gradle.org/install/) installed from [SDKMAN!](https://sdkman.io/).

```shell
$ cd openEHR-SDK
$ gradle clean build
```

Build without running the tests (faster):

```shell
$ cd openEHR-SDK
$ gradle build -x test
```

### Requires Java 8+ and Groovy 2.5.5+

Also tested with JDK 11.0.x and Groovy 3.0.x.

For running tests, there are many options, examples below:

1. Run specific test case from a specific suite
2. Run all tests from a specific suite
3. Run all suites in a package
4. Run all tests

```shell
$ cd openEHR-SDK
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest.testCompleteOPT
$ gradle test --tests com.cabolabs.openehr.opt.OPTParserTest
$ gradle test --tests com.cabolabs.openehr.opt*
$ gradle test
```

The test report in HTML will be under ./build/reports/tests/test/index.html


## Usage as Java/Groovy Library

### Common Imports

```groovy
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepository
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.validation.RmValidator2
import com.cabolabs.openehr.validation.RmValidationReport
import com.cabolabs.openehr.opt.instance_generator.RmInstanceGenerator
```

### Validate an openEHR Operational Template against its schema

```groovy
import com.cabolabs.openehr.opt.instance_validation.XmlValidation

def inputStream = this.getClass().getResourceAsStream('/xsd/OperationalTemplateExtra.xsd')
def validator = new XmlValidation(inputStream)

def path = "someopt.opt"
def f = new File(path)
if (!f.exists())
{
   println path +" doesn't exist"
   System.exit(0)
}

validateXML(validator, f)

static boolean validateXML(validator, file)
{
   boolean isValid = true
   if (!validator.validate( file.text ))
   {
      println file.name +' NOT VALID'
      println '====================================='
      validator.errors.each {
         println it
      }
      println '====================================='
      isValid = false
   }
   else
   {
      println file.name +' VALID'
   }

   println ""
   return isValid
}
```

### Parse an openEHR Operational Template

```groovy
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.OperationalTemplate

OperationalTemplate loadAndParse(String path)
{
   def parser = new OperationalTemplateParser()

   def optFile = new File(getClass().getResource(path).toURI())
   def text = optFile.getText()

   return parser.parse(text)
}
```

To add the attributes that are in the openEHR Reference Model but are not in the source OPT file, use the method complete():

```groovy
def opt = loadAndParse("vital_signs.opt")
opt.complete()
```

### Parse a JSON COMPOSITION

```groovy
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String path = "vital_signs.json"
File file = new File(getClass().getResource(path).toURI())
String json = file.text
def parser = new OpenEhrJsonParser()
Composition c = (Composition)parser.parseJson(json)
```

### Serialize a COMPOSITION to JSON

```groovy
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

Composition compo = ...
def serializer = new OpenEhrJsonSerializer()
String json = serializer.serialize(compo)
```

### Parse an XML COMPOSITION

```groovy
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String path = "vital_signs.xml"
File file = new File(getClass().getResource(path).toURI())
String xml = file.text
def parser = new OpenEhrXmlParser()
Composition c = (Composition)parser.parseLocatable(xml)
```

### Serialize a COMPOSITION to XML

```groovy
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

Composition compo = ...
OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
String xml = marshal.serialize(compo)
```

### Validate COMPOSITION against OPT

```groovy
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepository
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl
import com.cabolabs.openehr.validation.RmValidator2
import com.cabolabs.openehr.validation.RmValidationReport
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

// setup OPT repository
String opt_repo_path = "opts"
OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
OptManager opt_manager = OptManager.getInstance()
opt_manager.init(repo)

// load COMPOSITION
Composition compo = ...

// the validator automatically gets the template from the repo based on the template_id in the COMPOSITION
RmValidator2 validator = new RmValidator2(opt_manager)
RmValidationReport report = validator.dovalidate(compo, OptManager.DEFAULT_NAMESPACE)

report.errors.each { error ->
   println error
}
```

### Transform XML COMPOSITION to JSON

```groovy
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String xml = ...
def parser = new OpenEhrXmlParser()
Composition c = (Composition)parser.parseLocatable(xml)
def serializer = new OpenEhrJsonSerializer()
String json = serializer.serialize(c)
```

### Transform JSON COMPOSITION to XML

```groovy
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition

String json = ...
def parser = new OpenEhrJsonParser()
Composition c = (Composition)parser.parseJson(json)
def serializer = new OpenEhrXmlSerializer()
String xml = serializer.serialize(c)
```

### Generate JSON COMPOSITION from OPT

```groovy
import com.cabolabs.openehr.opt.instance_generator.JsonInstanceCanonicalGenerator2
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('vital_signs.opt')
def igen = new JsonInstanceCanonicalGenerator2()
String json = igen.generateJSONVersionStringFromOPT(opt, true, true)
```

### Generate XML COMPOSITION from OPT

```groovy
import com.cabolabs.openehr.opt.instance_generator.XmlInstanceGenerator
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('vital_signs.opt')
def igen = new XmlInstanceGenerator()
String xml = igen.generateXMLCompositionStringFromOPT(opt, true)
```

### Generate Instances with RmInstanceGenerator

The `RmInstanceGenerator` can generate instances for various openEHR types:

```groovy
import com.cabolabs.openehr.opt.instance_generator.RmInstanceGenerator
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.demographic.Person

def generator = new RmInstanceGenerator()
def opt = loadAndParse('template.opt')

// Generate COMPOSITION
Composition composition = generator.generateCompositionFromOPT(opt, false)

// Generate COMPOSITION with participations
Composition compositionWithPart = generator.generateCompositionFromOPT(opt, true)

// Generate VERSION wrapper
def version = generator.generateVersionFromOPT(opt, false, 'rm')

// Generate EHR_STATUS
EhrStatus ehrStatus = generator.generateEhrStatusFromOPT(opt)

// Generate FOLDER
Folder folder = generator.generateFolderFromOPT(opt)

// Generate PERSON (demographic)
Person person = generator.generatePersonFromOPT(opt)

// Generate PERSON DTO (API flavor with resolved references)
def personDto = generator.generatePersonDtoFromOPT(opt)

// Other demographic types
def organization = generator.generateOrganizationFromOPT(opt)
def agent = generator.generateAgentFromOPT(opt)
def group = generator.generateGroupFromOPT(opt)
def role = generator.generateRoleFromOPT(opt)
def relationship = generator.generateRelationshipFromOPT(opt)
```

### Transform OPT to JSON

```groovy
import com.cabolabs.openehr.opt.serializer.JsonSerializer
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('template.opt')
def serializer = new JsonSerializer()
serializer.serialize(opt)
String json = serializer.get(true) // true for pretty print
```

### Generate OPT from ADL Archetype

```groovy
import com.cabolabs.openehr.opt.opt_generator.AdlToOpt
import com.cabolabs.openehr.opt.serializer.OptXmlSerializer
import se.acode.openehr.parser.ADLParser

def adlFile = new File('archetype.adl')
def parser = new ADLParser(adlFile)
def archetype = parser.archetype()

def adlToOpt = new AdlToOpt()
def opt = adlToOpt.generateOpt(archetype)

def serializer = new OptXmlSerializer(true)
String optXml = serializer.serialize(opt)
```

### Generate UI from OPT

```groovy
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.OperationalTemplate

def opt = loadAndParse('template.opt')

// Generate full HTML page with Bootstrap 5
def generator = new OptUiGenerator(true, 5)
String html = generator.generate(opt)

// Generate form only with Bootstrap 4
def formGenerator = new OptUiGenerator(false, 4)
String formHtml = formGenerator.generate(opt)
```

[EHRCommitter]: https://github.com/ppazos/EHRCommitter
[EHRServer]: https://github.com/ppazos/cabolabs-ehrserver
