# Kotlin Programming Language

## Objective-C interop

This is a fork of [Kotlin](https://github.com/JetBrains/Kotlin) programming language project, adding support of interoperability of Kotlin-JVM with Objective-C.
Basically it allows to call functions of compiled Objective-C libraries from Kotlin under JVM.
The project is maintained by [Alexander Udalov](https://github.com/udalov) and was presented as a graduation thesis to Saint Petersburg State University, Mathematics & Mechanics Faculty, Department of Software Engineering in 2013 under supervision of [Andrey Breslav](https://github.com/abreslav).

## Building and testing

1. Download the project and launch **Make** in IDEA

2. Install **protobuf 2.4.1** as a global library.
   A possible way to do this via [Homebrew](https://github.com/mxcl/homebrew):

   ```
   cd /usr/local
   git checkout 544209f /usr/local/Library/Formula/protobuf.rb
   brew install protobuf
   ```

3. Make **libKotlinNativeIndexer.dylib**:
   ```
   cd compiler/objc/frontend.objc/native
   make
   ```

   Optionally, you can run Native Indexer tests:
   ```
   make tests
   ```

4. Make **libKotlinNative.dylib**:
   ```
   cd runtime/native
   ./ffi_make  # this is needed only the first time to download and build libffi
   ./make_native
   ```

5. Run `ant dist`

Now you can run tests. They are found under `compiler/tests/org/jetbrains/jet/objc/`. If you have trouble launching them, copy the needed environment variables and java arguments from the **Obj-C Tests** run configuration.
