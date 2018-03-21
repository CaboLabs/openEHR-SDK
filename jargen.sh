@REM jar -cfv openEHR_OPT.jar -C bin .
jar -cvmf MANIFEST.txt openEHR_OPT.jar -C bin . -C .. xsd/* -C .. resources/terminology/*
