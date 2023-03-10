# Wasmizer: Curating WebAssembly-driven Projects on GitHub - Artifact
This repository contains the software artifact for the paper "Wasmizer: Curating WebAssembly-driven Projects on GitHub".

## What is Wasmizer?
Wasmizer, a tool that regularly collects WebAssembly-driven projects on GitHub, compiles them, and curates an up-to-date and growing dataset of WebAssembly sources and binaries.

Wasmizer has two main phases:

_1. Repository collection_

It identifies C and C++ projects on GitHub using Github search API (https://docs.github.com/en/rest/search?apiVersion=2022-11-28). There are some limitations on the search API such as rate limitation that we make a delay between requests to overcome this limitation. Another limitation is the maximum number of retrieved repositories in the search API that is 1000. We overcome this limitation by defining short period of date to have less than 1000 repositories. In addition, each page includes 100 repositories and we use a loop to have repositories in all pages.
We excluded the forks of another repository to prevent duplicate projects.
We had to select GitHub projects as, practically, we cannot analyse all the C or C++ projects. Therefore, we set up several selection criteria such as WebAssembly-related keywords, minimum number of stars and forks, minimum size of the projects, date of the last pushed, and WebAssembly symptoms in the config.json. It firstly collect projects based on the keywords, then filters the projects based on the symptoms in their code.

_2. Compilation_

In the compilation phase, Wasmizer clones the target repositories as long as they are not already compiled or it is pushed after the previous compilation date. We need to install the necessary tools for compilation and build tools. As Wasmizer is initially built to compile C/C++ projects to WebAssembly and therefore we use Emscripten (https://emscripten.org/docs/getting_started/downloads.html | We used Docker image). In addition, In addition, in order to convert wat files to wasm files, you need to install wat2wasm tool. You can use https://github.com/WebAssembly/wabt for this goal. 
It then searches for precompilation and compilation source files and automatically compiles each project based on the compilation commands. We identified each CMakeLists.txt file, indicating the use of the cmake build system, we run Emscripten’s cmake wrapper (emcmake). Then, for each Makefile, either resulting from the previous step, or standalone, we run Emscripten’s make wrapper (emmake). It should be noted, compilation source files and commands are stored in the config.json file. Therefore, Wasmizer can be adapted to a different language such as rust by replacing the compilation commands in the config.json file. We also use github commit API in order to retrieve the commit sha of the projects. We consider the latest commit, so the first commit in the retrieved list will be the target commit. Then, we get the commit sha and store it in the metadata.csv file. 

After applying the compilation phase to all projects, it looks for files that are named with either a .wasm or a .wat extension, indicating that they are WebAssembly files. We try to convert all .wat files found into their binary version (.wasm) relying on the wat2wasm tool from the WebAssembly Binary Toolkit, configured with the --enable-all flag to enable all available WebAssembly extensions. We then store all .wasm files in a new directory (wasm-wat-files), under a name composed of the SHA-256 sum of their content. This is to ensure that duplicate files are only present once in the dataset. We store a metadata.csv contains information of the binary files such as name, url, forks, stars, size, creation date, last commit date, commit sha etc.
Wasmizer also separates the binary files that were already exist and new generated binary files in two folders (wasm-wat-files-pre and wasm-wat-files). Finally, we share the output (binary files and metadata file) via https://tucloud.tu-clausthal.de/index.php/s/MMRQMEZm66GRGXI with password: wasmizer.


## How to configure Wasmizer?

In order to configure the Wasmizer, there is a config.json file in the project. It contains: 

* "keywords" --> Wasmizer collects repositories based on the keywords. It means, if a repository has the keywords in the name, topics, description, and readme, it will be collected.

* "date" --> Wasmizer collects repositories based on the date. It means, if repository had been pushed in the period, it will be collected.

* "stars" --> Wasmizer collects repositories which their stars are more than this number of stars.

* "forks" --> Wasmizer collects repositories which their forks are more than this number of forks.

* "size" Wasmizer collects repositories which their size are more than this number (KB).

* "numOfSymptoms" --> Wasmizer gets symptoms in order to filter the repositories. You can define the number of symptoms and the symptoms in this parameter and following parameters, respectively.

* "precompilation_command" and "compilation_command"--> These two parameters are used for compilation commands. As Wasmizer is initially built to compile the C/C++ programs to WebAssembly, we used emcmake cmake, and emmake make command to configure, build, and compile the programs. Therefore, for compilation of the programs in other languages, you should change these commands.
Note: If you don't need to precompilation phase (based on the project language), you can make it empty (e.g., precompilation_command = "").

* "precompilation_sourcefile" and "compilation_sourcefile"--> These two parameters are used as compilation source files. As Wasmizer is initially built to compile the C/C++ programs to WebAssembly, we used CMakelist.txt and Makefile for precompilation and compilation source files, respectively.
Note: If you don't need to precompilation phase (based on the project language), you can make it empty (e.g., precompilation_sourcefile = "").


## How Wasmizer works?
1- It searches for repositories based on the keywords and save them in the repositories.csv file.

2- It filters repositories that include the wasm symptoms and save them in the WASMrepositories.csv file.

3- It clones the repositories which are in the WASMrepositories.csv into repobase folder.

4- It searches and collects for .wasm and .wat files before compilation, then hash their name based on their content and copy them in the output\\wasm-wat-files-pre folder.

	In the wasm-wat-files-pre folder, there are folders based on the name of projects.
 
5- It searches for precompilation and compilation source files and run the compiler on them. For instance, we use wasmizer to compile the C/C++ projects to WebAssembly, therefore it searches for CMakeLists.txt and Makefile/makefile, then run the Emscripten.

6- It searches and collects for .wasm and .wat files after compilation, then hash their name based on their content and copy them in the output\\wasm-wat-files folder.

	In the wasm-wat-files folder, there are folders based on the name of projects.

7- It converts .wat files to .wasm using wat2wasm tool.


## How to launch Wasmizer?

You can clone and run the Wasmizer:

1- Clone the project: Open a Command Prompt (cmd.exe), use this command: git clone https://github.com/arash-mazidi/WASMIZER.git

2- Open the project in Eclipse IDE

3- There is a lib folder in the project that contains libraries that are needed to run Wasmizer. You can import all libraries in the classpath of the project. We tested the Wasmizer on the Eclipse IDE.

4- Set up the config.json file (It is explained in the previous sections).

5- Run the Wasmizer by running the WASMIZER/blob/main/src/WASMIZER/WASMIZER_class.java.


Furthermore, name and date of all cloned repositories will be stored in the clonedrepo.csv and cloneddate.csv files, respectively.


## How dataset is structured?

Wasmizer will clone the repositories into repobase folder and compiled projects will be there. 

The output of the Wasmizer that are binary files are in the output folder. In the output folder, there are a metadata.csv file and two folders wasm-wat-files and wasm-wat-files-pre.

* _metadata.csv_ contains information about the repositories that generated binary files such as name, url, creation date, last pushed date, stars, forsk, size, commit sha, etc.

A sample of metadata file header:

Repository ID | Owner-Repository Name | Repository URL | Creation Date | Pushed Date | Stars| Forks| Size| Commit SHA

* _wasm-wat-files_ contains folders with the name of repoOwner-repoName (e.g., RepoOwner: X, and RepoName: Y --> foldername: X-Y). They contain binary files after comilation for the repository.

* _wasm-wat-files-pre_ contains folders with the name of repoOwner-repoName (e.g., RepoOwner: X, and RepoName: Y --> foldername: X-Y). They contain binary files before compilation for each repository.
