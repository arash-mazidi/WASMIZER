# Wasmizer: Curating WebAssembly-driven Projects on GitHub - Artifact
This repository contains the software artifact for the paper "Wasmizer: Curating WebAssembly-driven Projects on GitHub".

# Two main phases of Wasmizer 
Wasmizer has two main parts:

_1. Repository collection_

_2. Compilation_

### Repository collection
It uses Github search API to collect repositories based on a configuration. In order to run this tool, you need to input your configuration on config.json file.

**Config.json file contains:**

* "keywords" --> WASMIZER collects repositories based on the keywords.

* "date" --> WASMIZER collects repositories based on the date.

* "stars" --> WASMIZER collects repositories which has more than this number of stars.

* "forks" --> WASMIZER collects repositories which has more than this number of forks.

* "size" WASMIZER collects repositories which their size are more than this number (KB).

* "numOfSymptoms" --> WASMIZER gets symptoms in order to filter the repositories. You can define the number of symptoms and the symptoms in this parameter and following parameters, respectively.

* "cmakecommand" and "makecommand" --> WASMIZER compile the c/c++ programs based on these command.

### Compilation
In order to use WASMIZER in compilation phase, you should have installed Emscripten on your system. You can use https://emscripten.org/docs/getting_started/downloads.html to install emscripten. We used emscripten docker image. In addition, in order to convert wat files to wasm files, you need to install wat2wasm tool. You can use https://github.com/WebAssembly/wabt for this goal.

Further, there is a lib folder that contains libraries that are needed for WASMIZER. You can import all libraries in the classpath of the project. We tested the WASMIZER on the Eclipse IDE for JAVA Developers.

Then, you can run the WASMIZER by running the WASMIZER/blob/main/src/WASMIZER/WASMIZER_class.java.

WASMIZER will clone the repositories on the repobase folder. In addition, there are two folders wasm-wat-files and wasm-wat-files-pre that contains wasm and wat files after and before compilation, respectively.

Furthermore, name and date of all cloned repositories will be stored in the clonedrepo.csv and cloneddate.csv files, respectively. After compilation, you will have a meta data such as name, url, stars, and so on in the metadata.csv.


# How the WASMIZER works?
1- It searches for repositories based on the keywords and save them in the repositories.csv file.

2- It collects repositories that include the wasm symptoms and save them in the WASMrepositories.csv file.

3- It clones the repositories which are in the WASMrepositories.csv.

4- It searches for CMakeLists.txt and Makefile/makefile and run the emscripten.

5- It searches and collects for .wasm and .wat files, then hash their name based on their content and copy them in the wasm-wat-files folder.

	In the wasm-wat-files folder, there are folders based on the name of projects.

	It also collect .wasm and .wat files before compiling in the wasm-wat-files-pre folder.

6- It converts .wat files to .wasm using wat2wasm tool.

