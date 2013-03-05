CPP=c++
CC_FLAGS=-std=c++0x -Iinclude -stdlib=libstdc++
LD_FLAGS=-Llib -lclang -lprotobuf

PROJECT_NAME=ObjCIndex

SRC_FILES=$(wildcard *.cc)
OBJ_FILES=$(patsubst %.cc,$(OUT)/%.o,$(SRC_FILES))
OUT=out
DYLIB=$(OUT)/lib$(PROJECT_NAME).dylib

TEST_FILES=$(wildcard tests/*.cc)
TEST_EXE=$(OUT)/a.out



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
	$(CPP) $(CC_FLAGS) $(LD_FLAGS) -I. -L$(OUT) -l$(PROJECT_NAME) $(TEST_FILES) -o $(TEST_EXE)
	$(TEST_EXE)
