#jar -cvmf MANIFEST.txt openEHR_OPT.jar -C bin . -C .. xsd/* -C .. resources/terminology/* -C .. resources/images/*
jar -cvmf MANIFEST.txt openEHR_OPT.jar -C bin . -C .. xsd/* xsd/openEHR_RMtoHTML.xsl resources/terminology/* resources/images/*
