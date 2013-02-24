all:
	clang++ -std=c++0x -Iinclude -Llib -lclang -lprotobuf *.cpp *.cc

run: all
	./a.out

clean:
	rm -f a.out
