import groovy.xml.MarkupBuilder

// setup
def writer = new StringWriter()
def xml = new MarkupBuilder(writer)
xml.setDoubleQuotes(true)

// composition
xml.composition(xmlns: 'http://schemas.openehr.org/v1',
                'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                archetype_node_id: 'openEHR-EHR-COMPOSITION.encounter.v1') {

  // campos heredados de LOCATABLE
  name() {
    value('Registro de atencion ambulatoria')
  }
  archetype_details() { // ARCHETYPED
    archetype_id() { // ARCHETYPE_ID
      value('openEHR-EHR-COMPOSITION.encounter.v1')
    }
    template_id() { // TEMPLATE_ID
      value('Encounter')
    }
    rm_version('1.0.2')
  }


  // campos de COMPOSITION
  language() {
    terminology_id() {
      value('ISO_639-1')
    }
    code_string('es')
  }
  territory() {
    terminology_id() {
      value('ISO_3166-1')
    }
    code_string('UY')
  }
  category() {
    value('event') // event o persistent
    defining_code() {
      terminology_id() {
        value('openehr')
      }
      code_string(433) // 433=event, 431=persistent, ver https://github.com/ppazos/openEHR-OPT/blob/master/resources/terminology/openehr_terminology_en.xml
    }
  }

  composer('xsi:type':'PARTY_IDENTIFIED') {
    external_ref {
      id('xsi:type': 'HIER_OBJECT_ID') {
        value('cc193f71-f5fe-438a-87f9-81ecb302eede') // identificador interno, unico, global
      }
      namespace('DEMOGRAPHIC')
      type('PERSON')
    }
    name('Dr. House')
    // identifiers DV_IDENTIFIER // se pueden poner varios identificadores externos
  }

  context() {
    start_time() {
      value('20160823T145000-0300')
    }
    setting() {
      value('Atención Médica Primaria') // depende del idioma
      defining_code() {
        terminology_id() {
          value('openehr')
        }
        code_string(228) // Atencion Medica Primaria
      }
    }
  }

  // Contenido de la COMPOSITION
}


// Excribe el XML en un archivo
String xml_text = writer.toString()
new File( "documents" + File.separator + new java.text.SimpleDateFormat("yyyyMMddhhmmss'.xml'").format(new Date()) ) << xml_text
