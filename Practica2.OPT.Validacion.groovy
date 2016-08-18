import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*

def path = "C:\\Documents and Settings\\pab\\My Documents\\GitHub\\openEHR-OPT\\resources\\opts"

// Carga todos los OPTs de un directorio usando el OptManager
def man = OptManager.getInstance(path)
man.loadAll()

// Restriccion para la presion arterial sistolica (busca el arquetipo en todos los OPTs cargados)
ObjectNode o = man.getNode('openEHR-EHR-OBSERVATION.blood_pressure.v1', '/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value')

// Muestra restriccion del objeto XML
//println groovy.xml.XmlUtil.serialize( o.xmlNode )

// Es DV_QUANTITY
assert o.xmlNode.rm_type_name.text() == 'DV_QUANTITY'

// Hay una sola restriccion para los atributos de DV_QUANTITY
assert o.xmlNode.list.size() == 1

def constraint = o.xmlNode.list[0]

// Restriccion sobre las unidades
assert constraint.units.text() == 'mm[Hg]'

def range = ((constraint.magnitude.lower_unbounded.text().toBoolean() ? '*' : constraint.magnitude.lower.text()) +'..'+ (constraint.magnitude.upper_unbounded.text().toBoolean() ? '*' : constraint.magnitude.upper.text()))
assert range == '0..1000'
println range

// Validador generico de DV_QUANTITY
def datos = [magnitude: 134d, units: "mm[Hg]"] // cambiar el valor o unidad para que el validador de invalido.
def valid = false
def validador = o.xmlNode.list.find{ it.units.text() == datos.units } // soporta multiples unidades
if (validador)
{
   valid = (constraint.magnitude.lower_unbounded.text().toBoolean() || constraint.magnitude.lower.text().toDouble() <= datos.magnitude) &&
           (constraint.magnitude.upper_unbounded.text().toBoolean() || constraint.magnitude.upper.text().toDouble() >= datos.magnitude) 
}

println (valid ? "valid" : "invalid")

man.unloadAll()

return