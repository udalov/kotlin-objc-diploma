all:
	clang++ -Iinclude -Llib -lclang *.cpp

run: all
	./a.out

clean:
	rm -f a.out
