all:
	clang++ -Iinclude -Llib -lclang -lprotobuf *.cpp *.cc

run: all
	./a.out

clean:
	rm -f a.out
