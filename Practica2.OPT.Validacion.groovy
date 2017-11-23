import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*

def path = "C:\\Documents and Settings\\pab\\My Documents\\GitHub\\openEHR-OPT\\resources\\opts"

// Carga todos los OPTs de un directorio usando el OptManager
def man = OptManager.getInstance(path)

// usa namespace por defecto "com.cabolabs.openehr_opt.namespaces.default"
man.loadAll()

//println man.referencedArchetypes[man.DEFAULT_NAMESPACE].keySet().sort()

// Restriccion para la presi�n arterial sist�lica (busca el arquetipo en todos los OPTs cargados)
ObjectNode o = man.getNode('openEHR-EHR-OBSERVATION.blood_pressure.v1', '/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value')

// Muestra restricci�n del objeto XML
//println groovy.xml.XmlUtil.serialize( o.xmlNode )


// --------------------------------------------------------
// Realizamos algunas verificaciones, no es necesario para validar datos

// Es DV_QUANTITY
assert o.xmlNode.rm_type_name.text() == 'DV_QUANTITY'

// Hay una sola restriccion para los atributos de DV_QUANTITY
assert o.xmlNode.list.size() == 1

// Obtenemos la unica restriccion
def constraint = o.xmlNode.list[0]

// Restricci�n sobre las unidades, verificamos que es mil�metros de mercurio (las unidades en las que se mide la presi�n arterial)
// Si cambiamos el arquetipo en getNode, este assert no sera v�lido, pero si la validaci�n de abajo que es gen�rica para cualquier DV_QUANTITY
assert constraint.units.text() == 'mm[Hg]'

// Muestra la restricci�n del rango para la magnitud de DV_QUANTITY
def range = ((constraint.magnitude.lower_unbounded.text().toBoolean() ? '*' : constraint.magnitude.lower.text()) +'..'+ (constraint.magnitude.upper_unbounded.text().toBoolean() ? '*' : constraint.magnitude.upper.text()))
assert range == '0..1000'
println range


// --------------------------------------------------------
// Validador gen�rico de DV_QUANTITY

// Los datos podr�an obtenerse de un formulario, ingresados por un usuario
def datos = [magnitude: 134d, units: "mm[Hg]"] // cambiar el valor o unidad para que el validador de inv�lido.
def valid = false

// Verifica que las unidades estan en las restricciones, las cuales pueden tener mas de una unidad v�lida
// Si no est�n, las unidades en los datos son inv�lidas
def validador = o.xmlNode.list.find{ it.units.text() == datos.units }
if (validador)
{
   // Si se encuentran unidades v�lidas, para esas unidades se verifica el rango v�lido para la magnitud,
   // considera si el rango no tiene cotas.
   // Mejora: puede pasar que no haya un rango definido en el arquetipo por lo que magnitude no tendr�a retricci�n y deber�a devolver valid=true en ese caso
   valid = (validador.magnitude.lower_unbounded.text().toBoolean() || validador.magnitude.lower.text().toDouble() <= datos.magnitude) &&
           (validador.magnitude.upper_unbounded.text().toBoolean() || validador.magnitude.upper.text().toDouble() >= datos.magnitude) 
}

println (valid ? "valid" : "invalid")

man.unloadAll()

return