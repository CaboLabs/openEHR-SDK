echo "Building..."

start=`date +%s%N`
if [ -d "bin" ]; then
rm -r bin/*
fi

cd src
find $PWD | grep groovy > ../tmpsources.txt
cd ..
groovyc -cp "./lib/*" -d bin @tmpsources.txt
rm tmpsources.txt

end=`date +%s%N`
tt=$((($end - $start)/1000000))

echo "Done! $tt ms"
