# Wasmizer: Curating WebAssembly-driven Projects on GitHub - Artifact
This repository contains the software artifact for the paper "Wasmizer: Curating WebAssembly-driven Projects on GitHub".

## What is Wasmizer?
A tool that regularly mines GitHub projects and makes an up-todate dataset of WebAssembly sources and their binaries publicly available.
Wasmizer has two main parts:

_1. Repository collection_

It uses Github search API to collect repositories based on a configuration. In order to run this tool, you need to input your configuration on config.json file. 

_2. Compilation_

In order to use Wasmizer in compilation phase, the necessary tools for compilation and build tools should be installed. For example, wasmizer is initially built to compile C/C++ to WebAssembly and therefore we use Emscripten. You can use https://emscripten.org/docs/getting_started/downloads.html to install emscripten. We used emscripten docker image. In addition, in order to convert wat files to wasm files, you need to install wat2wasm tool. You can use https://github.com/WebAssembly/wabt for this goal.



## How Wasmizer works?
1- It searches for repositories based on the keywords and save them in the repositories.csv file.

2- It collects repositories that include the wasm symptoms and save them in the WASMrepositories.csv file.

3- It clones the repositories which are in the WASMrepositories.csv.

4- It searches for build and compilation files and run the compiler on them. For instance, we use wasmizer to compile the C/C++ projects to WebAssembly, therefore it searches for CMakeLists.txt and Makefile/makefile, then run the Emscripten.

5- It searches and collects for .wasm and .wat files, then hash their name based on their content and copy them in the output\\wasm-wat-files folder.

	In the wasm-wat-files folder, there are folders based on the name of projects.

	It also collect .wasm and .wat files before compiling in the output\\wasm-wat-files-pre folder.

6- It converts .wat files to .wasm using wat2wasm tool.


## How to configure Wasmizer?

In order to configure the Wasmizer, there is a config.json file in the program. It contains: 

* "keywords" --> Wasmizer collects repositories based on the keywords. It means, if repository has the keywords in the name, topics, description, and readme, it will be collected.

* "date" --> Wasmizer collects repositories based on the date. It means, if repository had been pushed in the period, it will be collected.

* "stars" --> Wasmizer collects repositories which their stars are more than this number of stars.

* "forks" --> Wasmizer collects repositories which their forks are more than this number of forks.

* "size" Wasmizer collects repositories which their size are more than this number (KB).

* "numOfSymptoms" --> Wasmizer gets symptoms in order to filter the repositories. You can define the number of symptoms and the symptoms in this parameter and following parameters, respectively.

* "precompilation_command" and "compilation_command"--> These two parameters are used for compilation commands. As Wasmizer is initially built to compile the C/C++ programs to WebAssembly, we used emcmake cmake, and emmake make command to configure, build, and compile the programs. Therefore, for compilation of the programs in other languages, you should change these commands.



## How to launch Wasmizer?
You can clone the Wasmizer:
1- Open a command line, use this command: git clone https://github.com/arash-mazidi/WASMIZER.git
2- Open the project in Eclipse IDE
3- There is a lib folder in the project that contains libraries that are needed for Wasmizer. You can import all libraries in the classpath of the project. We tested the Wasmizer on the Eclipse IDE.
4- Set up the config.json file (It is explained in the previous section).
5- Run the Wasmizer by running the WASMIZER/blob/main/src/WASMIZER/WASMIZER_class.java.


Furthermore, name and date of all cloned repositories will be stored in the clonedrepo.csv and cloneddate.csv files, respectively.


## How dataset is structured?

Wasmizer will clone the repositories on the repobase folder. In addition, the output of the Wasmizer that are binary files are in the output folder. In the output folder, there are a metadata.csv file and two folders wasm-wat-files and wasm-wat-files-pre.
* metadata.csv contains information about the repositories that generated binary files such as name, url, creation date, last pushed date, stars, forsk, size, commit sha, etc.
* wasm-wat-files contains binary files after comilation for each repository.
* wasm-wat-files-pre contains binary files bedore compilation for each repository.
