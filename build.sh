echo "Building..."

start=`date +%s%N`
if [ -d "bin" ]; then
rm -r bin/*
fi

#Compiles the only Java file first
javac -d bin src/net/pempek/unicode/UnicodeBOMInputStream.java

#Compiles rest of groovy code
cd src
find $PWD | grep groovy > ../tmpsources.txt
cd ..

#bin in the CP to let groovy see the compiled java class
groovyc -cp "./lib/*:./bin" -d bin @tmpsources.txt
rm tmpsources.txt

end=`date +%s%N`
tt=$((($end - $start)/1000000))

echo "Done! $tt ms"
