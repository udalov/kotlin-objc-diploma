JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/Current
JAVA_INCLUDE=$(JAVA_HOME)/Headers

CC=c++
CC_FLAGS=-I$(JAVA_INCLUDE)
LD_FLAGS=-Llib -lObjCIndex

SRC_FILES=jni/*.cc
DYLIB=lib/libObjCDescriptorResolver.dylib


all:
	$(CC) $(CC_FLAGS) $(LD_FLAGS) -dynamiclib $(SRC_FILES) -o $(DYLIB)

clean:
	@rm -rf $(DYLIB)
