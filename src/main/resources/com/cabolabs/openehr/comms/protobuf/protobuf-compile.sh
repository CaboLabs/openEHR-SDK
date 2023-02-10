cd $(dirname $0)
SRC_DIR=.
# src/main/groovy
DST_DIR=../../../../../../java
DST_DIR_1=$(builtin cd $DST_DIR; pwd)
echo $DST_DIR_1
protoc -I=$SRC_DIR --java_out=$DST_DIR_1 $SRC_DIR/OpenEhrProtobufMessage.proto