rm -r bin/*
cd src
find $PWD | grep groovy > ../tmpsources.txt
cd ..
groovyc -cp "./lib/*" -d bin @tmpsources.txt
rm tmpsources.txt
