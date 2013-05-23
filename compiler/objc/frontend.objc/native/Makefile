# ----------------------------------------------------------------------------
#  KotlinNativeIndexer Makefile
#
#  Usages:
#
#  1. make clean
#  	  Remove previously compiled files (out/ directory)
#
#  2. make proto
#     Launch Protocol Buffers compiler to generate C++ and Java classes
#
#  3. make
#     Compile everything and link to out/libKotlinNativeIndexer.dylib
#
#  4. make test
#     Compile (linking to the .dylib) and run tests
#
#  5. make javah
#     Launch javah on the compiled ObjCResolveFacade class (found in out/)
#     to produce a C header Indexer.h
#
# ----------------------------------------------------------------------------



# ----------------------------------------------------------------------------
#  Variables
# ----------------------------------------------------------------------------

PROJECT_NAME=KotlinNativeIndexer

JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/Current
JAVA_INCLUDE=$(JAVA_HOME)/Headers

CPP=c++
CC_FLAGS=-O2 -Wall -std=c++0x -I$(JAVA_INCLUDE) -Iinclude -stdlib=libstdc++
LD_FLAGS=-lclang -lprotobuf

SRC_FILES=$(wildcard *.cc)
OBJ_FILES=$(patsubst %.cc,$(OUT)/%.o,$(SRC_FILES))
OUT=out
DYLIB=$(OUT)/lib$(PROJECT_NAME).dylib

TEST_FILES=$(wildcard tests/*.cc)
TEST_EXE=$(OUT)/tests


# ----------------------------------------------------------------------------
#  Targets
# ----------------------------------------------------------------------------

.PHONY: all proto mkdir dylib javah clean test


all: proto mkdir dylib

proto:
	@make -C proto all

mkdir:
	@mkdir -p $(OUT)

dylib: $(OBJ_FILES)
	$(CPP) $(LD_FLAGS) -dynamiclib -o $(DYLIB) $^

javah:
	@javah -classpath ../../../../out/production/frontend.objc -o Indexer.h org.jetbrains.jet.lang.resolve.objc.ObjCResolveFacade

$(OUT)/%.o: %.cc
	$(CPP) $(CC_FLAGS) -c $^ -o $@



clean:
	@rm -rf $(OUT)



test: mkdir dylib
	$(CPP) $(CC_FLAGS) $(LD_FLAGS) -I. -L$(OUT) -l$(PROJECT_NAME) $(TEST_FILES) -o $(TEST_EXE)
	$(TEST_EXE)
