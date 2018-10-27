/*
 * $ groovy -cp openEHR_OPT.jar:lib/log4j-1.2.17.jar:lib/staxon-1.3.jar Practica2.OPT.Validacion.groovy
 */

import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*

// WINDOWS
//def path = "C:\\Documents and Settings\\pab\\My Documents\\GitHub\\openEHR-OPT\\resources\\opts"

String PS = System.getProperty("file.separator")
def path = "resources"+ PS +"opts"+ PS


// Carga todos los OPTs de un directorio usando el OptManager
def man = OptManager.getInstance(path)

// usa namespace por defecto "com.cabolabs.openehr_opt.namespaces.default"
man.loadAll()

//println man.referencedArchetypes[man.DEFAULT_NAMESPACE].keySet().sort()

// Restriccion para la presión arterial sistólica (busca el arquetipo en todos los OPTs cargados)
Constraint c = man.getNode('openEHR-EHR-OBSERVATION.blood_pressure.v1', '/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value') // AttributeNode
ObjectNode o = c.children[0]

// --------------------------------------------------------
// Realizamos algunas verificaciones, no es necesario para validar datos

// Es DV_QUANTITY
assert o.rmTypeName == 'DV_QUANTITY'

// Hay una sola restriccion para los atributos de DV_QUANTITY
assert o.list.size() == 1

// Obtenemos la unica restriccion
def constraint = o.list[0]

// Restricción sobre las unidades, verificamos que es milímetros de mercurio (las unidades en las que se mide la presión arterial)
// Si cambiamos el arquetipo en getNode, este assert no sera válido, pero si la validación de abajo que es genérica para cualquier DV_QUANTITY
assert constraint.units == 'mm[Hg]'

// Muestra la restricción del rango para la magnitud de DV_QUANTITY
def range = ((constraint.magnitude.lowerUnbounded ? '*' : constraint.magnitude.lower) +'..'+ (constraint.magnitude.upperUnbounded ? '*' : constraint.magnitude.upper))
assert range == '0.0..1000.0'
println range


// --------------------------------------------------------
// Validador genérico de DV_QUANTITY

// Los datos podrían obtenerse de un formulario, ingresados por un usuario
def datos = [magnitude: 134d, units: "mm[Hg]"] // cambiar el valor o unidad para que el validador de inválido.
def valid = false

// Verifica que las unidades estan en las restricciones, las cuales pueden tener mas de una unidad válida
// Si no están, las unidades en los datos son inválidas
def validador = o.list.find{ it.units == datos.units }
if (validador)
{
   // Si se encuentran unidades válidas, para esas unidades se verifica el rango válido para la magnitud,
   // considera si el rango no tiene cotas.
   // Mejora: puede pasar que no haya un rango definido en el arquetipo por lo que magnitude no tendría retricción y debería devolver valid=true en ese caso
   valid = (validador.magnitude.lowerUnbounded || validador.magnitude.lower <= datos.magnitude) &&
           (validador.magnitude.upperUnbounded || validador.magnitude.upper >= datos.magnitude)
}

println (valid ? "valid" : "invalid")

man.unloadAll()

return
