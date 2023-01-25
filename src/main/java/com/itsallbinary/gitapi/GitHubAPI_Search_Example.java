package com.itsallbinary.gitapi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.poi.ss.formula.functions.Replace;
import java.io.BufferedWriter;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GitHubAPI_Search_Example {

	private static Gson gson;
	public static String s1;
	public static int i = 0, j = 0, k = 0, co = -1, count;
	public static String repokeywords;
	public static String contentkeywords;
	public static String date;
	public static String stars;
	public static String forks;
	public static String size;
	public static ArrayList<String> repolist = new ArrayList<String>();
	public static ArrayList<String> repolistBasedcode = new ArrayList<String>();
	public static List<String> submodules;
	public static List<String> watfiles = new ArrayList<String>();
	public static List<String> watfilesindex = new ArrayList<String>();
	public static List<String> wasmfiles = new ArrayList<String>();
	public static List<String> wasmfilesindex = new ArrayList<String>();
	public static int cmakelistcount = 0, makefilecount = 0, cmakeruncount = 0, makefileruncount = 0;
	public static int wasmcount = 0, watcount = 0;
	public static String reponame;
	public static String repolink;
	public static String path;
	public static String prepath;
	public static String MakefileName = "Makefile";
	public static String makefilename = "makefile";
	public static String CMakeListName = "CMakeLists.txt";
	public static String newFileName;
	public static ArrayList<String> CMakeListpaths = new ArrayList<String>();
	private static String GITHUB_API_BASE_URL = "https://api.github.com/";
	private static String GITHUB_API_SEARCH_CODE_PATH = "search/code?q=";
	private static String symptom1 = "%22include%20%3Cemscripten.h%3E%22";
	private static String symptom2 = "%22include%20%3Chtml5.h%3E%22";
	private static String symptom3 = "%22EM_ASM(%22";
	private static String symptom4 = "%22EM_JS(%22";
	private static String symptom5 = "%22emcc%22";
	private static String symptom6 = "%22emsdk%22";
	private static String symptom7 = "%22-target%20cheerp-wasm%22";
	private static String symptom8 = "%22--target%3Dwasm32%22";
	public static File f1;
	public static String path_of_emmake = null;
	public static String path_of_emcmake = null;

	public static void main(String[] args) throws IOException, URISyntaxException {

		// Using GSON to parse or print response JSON.
		gson = new GsonBuilder().setPrettyPrinting().create();
		// Read features of query and paths from config.json file
		readConfig();
		// Collecting the repositories based on the keywords and symptoms
		searchCodeByContent();
		// Clone the repository, compile it, count the .wasm and .wat files
		cloneAndCompile();

		System.out.println("The End ... ");
		System.out.println("Number of CMakeList.txt :" + cmakelistcount);
		System.out.println("\nNumber of Makefile/makefile :" + makefilecount);
		System.out.println("\n\nNumber of .wasm files :" + wasmfiles.size());
		System.out.println("\nNumber of .wat files :" + watfiles.size());

	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for collecting the repositories
	private static void searchCodeByContent() throws ClientProtocolException, IOException {

		double totalcount, totalcount2;
		i = 0;
		do {
			i++;
			// Query for collecting repositories
			String repoContentQuery = repokeywords + "+in:name,topics,description,readme+pushed:" + date
					+ "+language:c+language:cpp+size:%3E=" + size + "%20forks:%3E=" + forks + "%20stars:%3E=" + stars
					+ "%20&page=" + i + "&per_page=100";
			Map repocontentSearchResult = makeRESTCall(
					GITHUB_API_BASE_URL + "search/repositories?q=" + repoContentQuery,
					"application/vnd.github.v3.text-match+json");
			totalcount = Double.parseDouble(repocontentSearchResult.get("total_count").toString());
			System.out.println("Total number or results = " + repocontentSearchResult.get("total_count"));
			System.out.println("incomplete_results = " + repocontentSearchResult.get("incomplete_results"));

			gson.toJsonTree(repocontentSearchResult).getAsJsonObject().get("items").getAsJsonArray().forEach(r -> {
				repolist.add(r.getAsJsonObject().get("html_url").toString());
			});
		} while (i * 100 < totalcount);

		System.out.println(repolist.size());

		// Save the repositories in the file
		File file = new File("repositories.csv");
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("Repository");
		bw.newLine();
		for (int i = 0; i < repolist.size(); i++) {
			bw.write(repolist.get(i));
			bw.newLine();
		}
		bw.close();
		fw.close();
		System.out.println("Repositories are saved in the file!");

		String codeContentQuery;
		Map contentSearchResult;
		// Filter the repositories based on the symptoms
		for (j = 0; j < repolist.size(); j++) {
			System.out.println("****" + j + "*****");
			String repos = repolist.get(j).replace("https://github.com/", "");
			codeContentQuery = symptom1 + "+in:file+repo:" + repos;
			codeContentQuery = codeContentQuery.replace("\"", "");
			contentSearchResult = makeRESTCall(GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
					"application/vnd.github.v3.text-match+json");
			totalcount2 = Double.parseDouble(contentSearchResult.get("total_count").toString());
			if (totalcount2 >= 1) {
				gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items").getAsJsonArray().forEach(r -> {
					s1 = "Repo: " + r.getAsJsonObject().get("repository").getAsJsonObject().get("html_url");
				});
				s1 = s1.replace("Repo: ", "");
				repolistBasedcode.add(s1);
			} else {
				codeContentQuery = symptom2 + "+in:file+repo:" + repos;
				codeContentQuery = codeContentQuery.replace("\"", "");
				stopForSecond(15);
				contentSearchResult = makeRESTCall(GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
						"application/vnd.github.v3.text-match+json");
				totalcount2 = Double.parseDouble(contentSearchResult.get("total_count").toString());
				if (totalcount2 >= 1) {
					gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items").getAsJsonArray().forEach(r -> {
						s1 = "Repo: " + r.getAsJsonObject().get("repository").getAsJsonObject().get("html_url");
					});
					s1 = s1.replace("Repo: ", "");
					repolistBasedcode.add(s1);
				} else {
					codeContentQuery = symptom3 + "+in:file+repo:" + repos;
					codeContentQuery = codeContentQuery.replace("\"", "");
					stopForSecond(15);
					contentSearchResult = makeRESTCall(
							GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
							"application/vnd.github.v3.text-match+json");
					totalcount2 = Double.parseDouble(contentSearchResult.get("total_count").toString());
					if (totalcount2 >= 1) {
						gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items").getAsJsonArray()
								.forEach(r -> {
									s1 = "Repo: "
											+ r.getAsJsonObject().get("repository").getAsJsonObject().get("html_url");
								});
						s1 = s1.replace("Repo: ", "");
						repolistBasedcode.add(s1);
					} else {
						codeContentQuery = symptom4 + "+in:file+repo:" + repos;
						codeContentQuery = codeContentQuery.replace("\"", "");
						stopForSecond(15);
						contentSearchResult = makeRESTCall(
								GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
								"application/vnd.github.v3.text-match+json");
						totalcount2 = Double.parseDouble(contentSearchResult.get("total_count").toString());
						if (totalcount2 >= 1) {
							gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items").getAsJsonArray()
									.forEach(r -> {
										s1 = "Repo: " + r.getAsJsonObject().get("repository").getAsJsonObject()
												.get("html_url");
									});
							s1 = s1.replace("Repo: ", "");
							repolistBasedcode.add(s1);
						} else {
							codeContentQuery = symptom5 + "+in:file+repo:" + repos;
							codeContentQuery = codeContentQuery.replace("\"", "");
							stopForSecond(15);
							contentSearchResult = makeRESTCall(
									GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
									"application/vnd.github.v3.text-match+json");
							totalcount2 = Double.parseDouble(contentSearchResult.get("total_count").toString());
							if (totalcount2 >= 1) {
								gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items").getAsJsonArray()
										.forEach(r -> {
											s1 = "Repo: " + r.getAsJsonObject().get("repository").getAsJsonObject()
													.get("html_url");
										});
								s1 = s1.replace("Repo: ", "");
								repolistBasedcode.add(s1);
							} else {
								codeContentQuery = symptom6 + "+in:file+repo:" + repos;
								codeContentQuery = codeContentQuery.replace("\"", "");
								stopForSecond(15);
								contentSearchResult = makeRESTCall(
										GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
										"application/vnd.github.v3.text-match+json");
								totalcount2 = Double.parseDouble(contentSearchResult.get("total_count").toString());
								if (totalcount2 >= 1) {
									gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items").getAsJsonArray()
											.forEach(r -> {
												s1 = "Repo: " + r.getAsJsonObject().get("repository").getAsJsonObject()
														.get("html_url");
											});
									s1 = s1.replace("Repo: ", "");
									repolistBasedcode.add(s1);
								} else {
									codeContentQuery = symptom7 + "+in:file+repo:" + repos;
									codeContentQuery = codeContentQuery.replace("\"", "");
									stopForSecond(15);
									contentSearchResult = makeRESTCall(
											GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
											"application/vnd.github.v3.text-match+json");
									totalcount2 = Double.parseDouble(contentSearchResult.get("total_count").toString());
									if (totalcount2 >= 1) {
										gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items")
												.getAsJsonArray().forEach(r -> {
													s1 = "Repo: " + r.getAsJsonObject().get("repository")
															.getAsJsonObject().get("html_url");
												});
										s1 = s1.replace("Repo: ", "");
										repolistBasedcode.add(s1);
									} else {
										codeContentQuery = symptom8 + "+in:file+repo:" + repos;
										codeContentQuery = codeContentQuery.replace("\"", "");
										stopForSecond(15);
										contentSearchResult = makeRESTCall(
												GITHUB_API_BASE_URL + GITHUB_API_SEARCH_CODE_PATH + codeContentQuery,
												"application/vnd.github.v3.text-match+json");
										totalcount2 = Double
												.parseDouble(contentSearchResult.get("total_count").toString());
										if (totalcount2 >= 1) {
											gson.toJsonTree(contentSearchResult).getAsJsonObject().get("items")
													.getAsJsonArray().forEach(r -> {
														s1 = "Repo: " + r.getAsJsonObject().get("repository")
																.getAsJsonObject().get("html_url");
													});
											s1 = s1.replace("Repo: ", "");
											repolistBasedcode.add(s1);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// Save target repositories in the file
		File file2 = new File("WASMrepositories.csv");
		FileWriter fw2 = new FileWriter(file2);
		BufferedWriter bw2 = new BufferedWriter(fw2);
		bw2.write("WASM Repository");
		bw2.newLine();
		for (int k = 0; k < repolistBasedcode.size(); k++) {
			bw2.write(repolistBasedcode.get(k));
			bw2.newLine();
		}
		bw2.close();
		fw2.close();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for cloning and compiling the repositories
	public static void cloneAndCompile() throws IOException {
		for (count = 0; count < repolistBasedcode.size(); count++) {
			repolink = repolistBasedcode.get(count);
			path = clone(repolink);
			reponame = path;

			// create folders with the project name for .wasm and .wat files in the
			// wasm-wat-files folder
			String command2 = "cmd.exe /c cd wasm-wat-files\\wasm-files && md " + reponame + "";
			String command3 = "cmd.exe /c cd wasm-wat-files\\wat-files && md " + reponame + "";
			Process pro = Runtime.getRuntime().exec(command2);
			try {
				pro.waitFor();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			Process pro2 = Runtime.getRuntime().exec(command3);
			try {
				pro2.waitFor();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			String rootDir = path + "\\";

			f1 = new File(rootDir);
			if (path == "")
				continue;

			while (true) {
				if (f1.exists())
					break;
				System.out.println("\nCloning ...");
				stopForSecond(5);
			}
			stopForSecond(20);
			System.out.println("\n\nRepository is cloned!");

			// Find submodules in a repository
			submodules = new ArrayList<String>();
			RepositoryBuilder builder = new RepositoryBuilder();
			Repository repo = builder.setGitDir(new File(rootDir + ".git")).readEnvironment() // scan environment GIT_*
					.findGitDir() // scan up the file system tree
					.build();

			SubmoduleWalk walk = SubmoduleWalk.forIndex(repo);
			while (walk.next()) {
				try {
					submodules.add(rootDir + walk.getModuleName());
					int xxx = 0;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ConfigInvalidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			File root1 = new File(rootDir);
			// Search for CMakeLists.txt files
			searchForCMakefile(root1, CMakeListName);
			stopForSecond(15);

			File root2 = new File(rootDir);
			// Search for Makefile and makefile files
			searchForMakefile(root2, makefilename, MakefileName);
			stopForSecond(15);
			File root3 = new File(rootDir);
			// Search for .wasm and .wat
			searchForWasmAndWat(root3);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for cloning repositories
	public static String clone(String url) {
		// Clone a git repository and return the path where it was cloned
		String path = url.split("/")[url.split("/").length - 2] + "-" + url.split("/")[url.split("/").length - 1];
		path = path.replace("\"", "");
		System.out.println("trying to clone " + url);
		File file = new File(path);
		if (!file.exists()) {
			try {
				Process process = Runtime.getRuntime().exec(
						"cmd.exe /c git clone --depth 1 --recurse-submodules --shallow-submodules " + url + " " + path);
				return path;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ""; // Skip the ones that we already cloned
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for searching CMakeList.txt files and running cmake
	public static void searchForCMakefile(File root, String makefileName) {
		File[] files = root.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {

					String cmakefiledir = file.getAbsolutePath().toString();
					String parentdir = cmakefiledir;
					String xx = file.getName().toString();
					parentdir = parentdir.replace(xx, "");
					if (xx.equals(makefileName)) {
						if (!(is_insubmodule(cmakefiledir))) {
							cmakelistcount++;
							try {
								cmakeruncount++;
								String[] command = { path_of_emcmake + " cmake.exe", "-S " + parentdir,
										"-B " + parentdir + "\\NewBuild" };

								Process p = Runtime.getRuntime().exec(command);
								int xv = 0;
							} catch (IOException e) {
								e.printStackTrace();
							}
							stopForSecond(5);

						}
					}
					int xcxc = 0;
				} else if (file.isDirectory())
					searchForCMakefile(file, makefileName);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for checking submodules
	public static boolean is_insubmodule(String cmaketemp) {
		for (int h = 0; h < submodules.size(); h++) {
			if (cmaketemp.contains(submodules.get(h).toString())) {
				return true;
			}
		}
		return false;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for searching the Makefile/makefile files and running make
	public static void searchForMakefile(File root, String makefileName, String MakefileName) {
		File[] files = root.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					String dirr = file.getAbsolutePath().toString();
					String parentdir = dirr;
					String xx = file.getName().toString();
					parentdir = parentdir.replace(xx, "");
					if (xx.equals(makefileName) || xx.equals(MakefileName)) {
						makefilecount++;
						try {
							makefileruncount++;
							Process p = Runtime.getRuntime()
									.exec("cmd.exe /c cd " + parentdir + " && " + path_of_emmake + " make");
							p.waitFor();
							int xv = 0;
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						stopForSecond(5);
					}
				} else if (file.isDirectory())
					searchForMakefile(file, makefileName, MakefileName);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for searching .wasm and .wat files
	public static void searchForWasmAndWat(File root) throws IOException {
		File[] files = root.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					String dirr = file.getAbsolutePath().toString();
					String parentdir = dirr;
					String tmp = file.getName().toString();
					parentdir = parentdir.replace(tmp, "");
					String xx = file.getName().toString();
					Path source = Paths.get(parentdir + "/" + file.getName().toString());
					if (xx.endsWith(".wat")) {
						watfiles.add(file.getAbsolutePath());
						watfilesindex.add(file.getName());
						Path destination = Paths.get("wasm-wat-files\\wat-files\\" + reponame);
						String newFileName = hash256(source);
						newFileName = newFileName + ".wat";
						File f2 = new File(destination + "\\" + newFileName);
						Path newPath = Paths.get(f2.toString());
						if (!f2.exists())
							Files.copy(source, newPath);
						else
							System.out.println("File exists");

					} else if (xx.endsWith(".wasm")) {
						wasmfiles.add(file.getAbsolutePath());
						wasmfilesindex.add(file.getName());
						Path destination = Paths.get("wasm-wat-files\\wasm-files\\" + reponame);
						String newFileName = hash256(source);
						newFileName = newFileName + ".wasm";
						File f2 = new File(destination + "\\" + newFileName);
						Path newPath = Paths.get(f2.toString());
						if (!f2.exists())
							Files.copy(source, newPath);
						else
							System.out.println("File exists");
					}
				} else if (file.isDirectory())
					searchForWasmAndWat(file);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for hashing
	public static String hash256(Path source) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] fileBytes = Files.readAllBytes(Paths.get(source.toString()));
			digest.update(fileBytes);
			byte[] hash = digest.digest();
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			newFileName = hexString.toString();
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}

		return newFileName;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for sleeping
	public static void stopForSecond(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for reading the config.json file
	public static void readConfig() {
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(new FileReader("config.json"));
			JSONObject jsonObject = (JSONObject) obj;
			repokeywords = (String) jsonObject.get("keywords");
			date = (String) jsonObject.get("date");
			stars = (String) jsonObject.get("stars");
			forks = (String) jsonObject.get("forks");
			size = (String) jsonObject.get("size");
			path_of_emmake = (String) jsonObject.get("emmakepath");
			path_of_emcmake = (String) jsonObject.get("emcmakepath");

		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for making a REST GET call for this URL using Apache http client
	private static Map makeRESTCall(String restUrl, String acceptHeaderValue)
			throws ClientProtocolException, IOException {
		Request request = Request.Get(restUrl);

		if (acceptHeaderValue != null && !acceptHeaderValue.isBlank()) {
			request.addHeader("Accept", acceptHeaderValue);
		}

		Content content = request.execute().returnContent();
		String jsonString = content.asString();

		// To print response JSON, using GSON. Any other JSON parser can be used here.
		Map jsonMap = gson.fromJson(jsonString, Map.class);
		return jsonMap;
	}

	private static Map makeRESTCall(String restUrl) throws ClientProtocolException, IOException {
		return makeRESTCall(restUrl, null);
	}
}
