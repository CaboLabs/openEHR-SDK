import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*

def path = "C:\\Documents and Settings\\pab\\My Documents\\GitHub\\openEHR-OPT\\resources\\opts"

// Carga todos los OPTs de un directorio usando el OptManager
def man = OptManager.getInstance(path)
man.loadAll()

// Obtener un OPT por su templateId
def opt = man.getOpt('Encuentro')

// Recorrida
traverse(opt.definition, 0)

def traverse(ObjectNode o, int pad)
{
   println " ".multiply(pad) + o.rmTypeName.padRight(35-pad, '.') + (o.archetypeId ?: o.path)
   
   pad++
   o.attributes.each{
      traverse(it, pad)
   }
}

def traverse(AttributeNode a, int pad)
{
   println " ".multiply(pad) + a.rmAttributeName
   
   pad++
   a.children.each{
      traverse(it, pad)
   }
}

man.unloadAll()

return