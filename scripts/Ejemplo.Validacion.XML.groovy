import com.cabolabs.openehr.opt.instance_validation.XmlValidation

def validator = new XmlValidation('xsd'+ File.separator + 'Version.xsd')

// Recorre todos los archivos generador en /documents
new File('documents' + File.separator).eachFileMatch(~/.*.xml/) { xml ->

  if (!validator.validate( xml.text ))
  {
     println xml.name +' NO VALIDA'
     println '====================================='
     validator.errors.each {
        println it
     }
     println '====================================='
  }
  else
     println xml.name +' VALIDA'
}