import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.manager.*

def path = "C:\\Documents and Settings\\pab\\My Documents\\GitHub\\openEHR-SDK\\resources\\opts"
def path_to_opt = path + "\\Encuentro.opt"

// Carga un OPT usando el parser directamente
def parser = new OperationalTemplateParser()
def optFile = new File( path_to_opt )
def text = optFile.getText()

def opt = parser.parse( text )

// Nodos y rutas del OPT
def nodes = opt.nodes.sort{ it.key }
nodes.each { opt_path, node ->
   println opt_path.padRight(190, '.') +" ref: "+ ((node.archetypeId ?: node.nodeId) ?: node.rmTypeName)
}

// Nodos y rutas de arquetipos
nodes.each { opt_path, node ->
   node.nodes.each { arch_path, subnode ->
      println arch_path.padRight(80, '.') +" ref: "+ ((subnode.archetypeId ?: subnode.nodeId) ?: subnode.rmTypeName)
   }
}


// Carga todos los OPTs de un directorio usando el OptManager
def man = OptManager.getInstance(path)
man.loadAll()

// Todos los OPTs cargados
man.getLoadedOpts().keySet().each { templateId ->
   println templateId
}

// Todos los arquetipos referenciados dsde todos los OPTs cargados
man.getAllReferencedArchetypes().keySet().each { archId ->
   println archId
}

return




