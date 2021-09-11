import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation

def parser = new XmlSlurper(false, false)

def validator = new XmlInstanceValidation('xsd'+ File.separator + 'Version.xsd')
def doc

// Recorre todos los archivos generador en /documents
new File('documents' + File.separator).eachFileMatch(~/.*.xml/) { xml ->

  // Validamos antes de parsear
  if (validator.validate( xml.text ))
  {
     println "Carga: "+ xml.name
     
     doc = parser.parseText( xml.text )
     
     // Acceso a nombre del elemento XML .name()
     if (doc.name() == 'composition')
     {
        println "COMPOSITION"
        
        // Acceso a nodos de texto
        println "Nombre del documento: " + doc.name.value.text()
        
        // Acceso a atributos
        println "Arquetipos del contenido: "
         // se pueden tener varios elementos content dentro de composition
        doc.content.eachWithIndex { content, i ->
          println i +") "+ content.@archetype_node_id
        }
        
        // Combiene crear funciones para procesar cada tipo del IM
        // y jugar con la programacion dinamica en los nombres de
        // esas funciones, ej. parseOBSERVATION / parseEVALUATION
        // se pueden invocar sin hacer un IF por el tipo.
     }
     else if (doc.name() == 'version')
     {
        println "VERSION"
        
        // TODO
     }
     
     println "-----"
  }
  else
    println xml.name +" NO PROCESADO"
}