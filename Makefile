CPP=c++
CC_FLAGS=-std=c++0x -Iinclude -stdlib=libstdc++
LD_FLAGS=-Llib -lclang -lprotobuf

SRC_FILES=$(wildcard *.cc)
OBJ_FILES=$(patsubst %.cc,$(OUT)/%.o,$(SRC_FILES))
OUT=out
DYLIB=$(OUT)/libObjCIndex.dylib
EXE=$(OUT)/a.out



all: mkdir dylib

clean:
	@rm -rf $(OUT)

mkdir:
	@mkdir -p $(OUT)

dylib: $(OBJ_FILES)
	$(CPP) $(LD_FLAGS) -dynamiclib -o $(DYLIB) $^

$(OUT)/%.o: %.cc
	$(CPP) $(CC_FLAGS) -c $^ -o $@



test: mkdir dylib
	$(CPP) $(CC_FLAGS) $(LD_FLAGS) -I. -Lout -lObjCIndex $(wildcard tests/*.cc) -o $(EXE)
	$(EXE)
