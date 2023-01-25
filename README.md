# WASMIZER
# configuration
First set the config.json. WASMIZER will clone the repositories based on the config.json file. 
You can set keywords, period of time, stars, forks and size of repository.
In addition, you should install the emscripten and write the path in this file.

# How the WASMIZER works?
1- It searches for repositories based on the keywords and save them in the repositories.csv file.
2- It collects repositories that include the wasm symptoms and save them in the WASMrepositories.csv file.
3- It clones the repositories which are in the WASMrepositories.csv.
4- It searches for CMakeLists.txt and Makefile/makefile and run the emscripten.
5- It searches and collects for .wasm and .wat files, then hash their name based on their content and copy them in the wasm-wat-files folder.
	In the wasm-wat-files folder, there are folders based on the name of projects.```

