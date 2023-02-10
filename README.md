# WASMIZER

# configuration
First set the config.json. WASMIZER will clone the repositories based on the parameters which are defined in the config.json file. 

You can set keywords, period of time, stars, forks and size of repository.

keywords: Repositories are collected based on these keywords.

stars: Repositories are collected based on this number of stars.

forks: Repositories are collected based on this number of forks.

date: Repositories are collected based on this period of time.

size: Repositories are gathered when their size is more than this defined size. The size is based on KB.

numOfSymptoms: This parameter is defined for number of symptoms

Symptoms: Collected repositories are filtered based on these symptoms.

Note: You can add or delete symptomes. Then, edit the numOfSymptoms.

In addition, you should install wat2wasm (https://github.com/WebAssembly/wabt) and emscripten (https://emscripten.org/docs/getting_started/downloads.html). I used docker image.

# How the WASMIZER works?
1- It searches for repositories based on the keywords and save them in the repositories.csv file.

2- It collects repositories that include the wasm symptoms and save them in the WASMrepositories.csv file.

3- It clones the repositories which are in the WASMrepositories.csv.

4- It searches for CMakeLists.txt and Makefile/makefile and run the emscripten.

5- It searches and collects for .wasm and .wat files, then hash their name based on their content and copy them in the wasm-wat-files folder.

	In the wasm-wat-files folder, there are folders based on the name of projects.

	It also collect .wasm and .wat files before compiling in the wasm-wat-files-pre folder.

6- It converts .wat files to .wasm using wat2wasm tool.

