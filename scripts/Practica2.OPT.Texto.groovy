import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.manager.*

def path = "C:\\Documents and Settings\\pab\\My Documents\\GitHub\\openEHR-OPT\\resources\\opts"
def man = OptManager.getInstance(path)
man.loadAll()
      
def opt = man.getOpt('Encuentro')

// Combina recorrida y mostrar los nombres de los nodos que tienen nodId en el idioma del OPT.
traverse( opt.definition, 0, opt.definition.archetypeId, opt )

def traverse(ObjectNode o, int pad, String parentArchetype, OperationalTemplate opt)
{
   // necesitamos saber el arquetipo padre del nodo que procesamos
   if (o.archetypeId) parentArchetype = o.archetypeId
   if (o.type == 'ARCHETYPE_SLOT') return // evita procesar slots
   
   println " ".multiply(pad) + o.rmTypeName.padRight(35-pad, '.') + (o.archetypeId ?: o.path).padRight(70, '.') +
           (o.nodeId ? opt.getTerm(parentArchetype, o.nodeId) : '') // Muestra el nombre del nodo
   
   pad++
   o.attributes.each{
      traverse(it, pad, parentArchetype, opt)
   }
}

def traverse(AttributeNode a, int pad, String parentArchetype, OperationalTemplate opt)
{
   println " ".multiply(pad) + a.rmAttributeName
   pad++
   a.children.each{
      traverse(it, pad, parentArchetype, opt)
   }
}

return
