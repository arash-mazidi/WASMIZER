package WASMIZER;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.poi.ss.formula.functions.Replace;
import java.io.BufferedReader;
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

public class WASMIZER_class {

	private static Gson gson;
	public static List<String> repolist = new ArrayList<String>();
	public static List<String> clonedrepo = new ArrayList<String>();
	public static List<String> cloneddate = new ArrayList<String>();
	public static List<String> repolistBasedcode = new ArrayList<String>();
	public static List<String> datelist = new ArrayList<String>();
	public static List<String> CMakeListpaths = new ArrayList<String>();
	public static List<String> submodules;
	public static List<String> watfiles = new ArrayList<String>();
	public static List<String> wasmfiles = new ArrayList<String>();
	public static List<String> symptopms = new ArrayList<String>();
	public static int wasmcount = 0, watcount = 0, count;
	public static String reponame, repolink, path, prepath, newFileName, s1;
	public static String repokeywords, date, stars, forks, size, numOfSymptoms;

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
		System.out.println("\n\nNumber of .wasm files :" + wasmfiles.size());
		System.out.println("\nNumber of .wat files :" + watfiles.size());

		// Save .wasm and .wat files directories in the .csv files
		saveArraylistFile(wasmfiles, "wasmdirectories");
		saveArraylistFile(watfiles, "watdirectories");
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for collecting the repositories
	private static void searchCodeByContent() throws ClientProtocolException, IOException {

		String incomplete;
		Map repocontentSearchResult;
		double totalcount, totalcount2;
		clonedrepo = readFile("clonedrepo");
		cloneddate = readFile("cloneddate");
		String GITHUB_API_BASE_URL = "https://api.github.com/", GITHUB_API_SEARCH_CODE_PATH = "search/code?q=";
		int i = 0;
		do {
			i++;
			// Query for collecting repositories
			String repoContentQuery = repokeywords + "+in:name,topics,description,readme+pushed:" + date
					+ "+language:c+language:cpp+size:%3E=" + size + "%20forks:%3E=" + forks + "%20stars:%3E=" + stars
					+ "%20&page=" + i + "&per_page=100";

			//Check the results of github API
			do {
				repocontentSearchResult = makeRESTCall(
						GITHUB_API_BASE_URL + "search/repositories?q=" + repoContentQuery,
						"application/vnd.github.v3.text-match+json");
				totalcount = Double.parseDouble(repocontentSearchResult.get("total_count").toString());
				System.out.println("Total number or results = " + repocontentSearchResult.get("total_count"));
				incomplete = repocontentSearchResult.get("incomplete_results").toString();
				System.out.println("incomplete_results = " + incomplete);
				stopForSecond(20);
			} while (incomplete.equals("true"));

			gson.toJsonTree(repocontentSearchResult).getAsJsonObject().get("items").getAsJsonArray().forEach(r -> {
				// Ignore forked repositories
				String fork = r.getAsJsonObject().get("fork").toString();
				String repodate = r.getAsJsonObject().get("pushed_at").toString();
				String repourl = r.getAsJsonObject().get("html_url").toString();
				if (fork.equals("false")) {
					// if repository is not cloned, add to list for cloning
					if (!(clonedrepo.contains(repourl))) {
						repolist.add(repourl);
						datelist.add(repodate);
					}
					// if repository is cloned but pushed after previous clone
					else if (!(cloneddate.get(clonedrepo.indexOf(repourl)).equals(repodate))) {
						int index = clonedrepo.indexOf(repourl);
						clonedrepo.remove(index);
						cloneddate.remove(index);
						String folder = repourl.split("/")[repourl.split("/").length - 2] + "-"
								+ repourl.split("/")[repourl.split("/").length - 1];
						folder = folder.replace("\"", "");

						// Repository cloned before but has pushed after cloning, we delete existing
						// files
						String comm0 = "cmd.exe /c rd /s /q " + folder + "";
						runcommand(comm0, 120);
						String comm1 = "cmd.exe /c cd wasm-wat-files-pre\\wat-files && rd /s /q " + folder + "";
						runcommand(comm1, 60);
						String comm2 = "cmd.exe /c cd wasm-wat-files-pre\\wasm-files && rd /s /q " + folder + "";
						runcommand(comm2, 60);
						String comm3 = "cmd.exe /c cd wasm-wat-files\\wat-files && rd /s /q " + folder + "";
						runcommand(comm3, 60);
						String comm4 = "cmd.exe /c cd wasm-wat-files\\wasm-files && rd /s /q " + folder + "";
						runcommand(comm4, 60);

						repolist.add(repourl);
						datelist.add(repodate);
					}
				}
			});
		} while (i * 100 < totalcount);

		System.out.println(repolist.size());

		// Save the repositories in the file
		saveArraylistFile(repolist, "repositories");
		saveArraylistFile(datelist, "datelist");

		System.out.println("Repositories are saved in the file!");

		String codeContentQuery;
		Map contentSearchResult;
		// Filter the repositories based on the symptoms
		for (int j = 0; j < repolist.size(); j++) {
			System.out.println("****" + j + "*****");
			String repos = repolist.get(j).replace("https://github.com/", "");

			for (int num = 0; num < symptopms.size(); num++) {
				stopForSecond(20);
				codeContentQuery = symptopms.get(num) + "+in:file+repo:" + repos;
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
					break;
				}
			}
		}
		// Save target repositories in the file
		saveArraylistFile(repolistBasedcode, "WASMrepositories");
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for cloning and compiling the repositories
	public static void cloneAndCompile() throws IOException {

		for (count = 0; count < repolistBasedcode.size(); count++) {
			repolink = repolistBasedcode.get(count);
			// Checking Internet connection
			while (!(isInternetAvailable("www.google.com", 80))) {
				System.out.println("\nYou are not connected to Internet! Wait to connect ...");
				stopForSecond(5);
			}
			path = clone(repolink);
			stopForSecond(5);
			System.out.println("\n\nRepository is cloned!");
			reponame = path;

			// when repository is already cloned, path will be null
			if (path == "")
				continue;
			// create folders with the project name for .wasm and .wat files in the
			// wasm-wat-files, and wasm-wat-files-pre folders
			String command2 = "cmd.exe /c cd wasm-wat-files\\wasm-files && md " + reponame + "";
			runcommand(command2, 60);
			String command3 = "cmd.exe /c cd wasm-wat-files\\wat-files && md " + reponame + "";
			runcommand(command3, 60);
			String command4 = "cmd.exe /c cd wasm-wat-files-pre\\wasm-files && md " + reponame + "";
			runcommand(command4, 60);
			String command5 = "cmd.exe /c cd wasm-wat-files-pre\\wat-files && md " + reponame + "";
			runcommand(command5, 60);

			String rootDir = path + "\\";

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
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ConfigInvalidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			File root1 = new File(rootDir);
			// Search for .wasm and .wat before compile
			searchForWasmAndWat(root1, "pre");
			// Search for CMakeLists.txt files
			searchForCMakefile(root1);
			stopForSecond(15);
			// Make a new file in order to ensure all added file exist
			File root2 = new File(rootDir);
			// Search for Makefile and makefile files
			searchForMakefile(root2);
			stopForSecond(15);
			// Make a new file in order to ensure all added file exist
			File root3 = new File(rootDir);
			// Search for .wasm and .wat
			searchForWasmAndWat(root3, "post");
		}
		saveArraylistFile(clonedrepo, "clonedrepo");
		saveArraylistFile(cloneddate, "cloneddate");
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for cloning repositories
	public static String clone(String url) {

		long timeout = 240 * 1000; // 240 seconds
		// Clone a git repository and return the path where it was cloned
		String path = url.split("/")[url.split("/").length - 2] + "-" + url.split("/")[url.split("/").length - 1];
		path = path.replace("\"", "");
		System.out.println("trying to clone " + url);
		File file = new File(path);
		if (!file.exists()) {
			try {
				Process process = Runtime.getRuntime().exec(
						"cmd.exe /c git clone --depth 1 --recurse-submodules --shallow-submodules " + url + " " + path);
				try {
					process.waitFor(timeout, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				clonedrepo.add(url);
				cloneddate.add(datelist.get(repolist.indexOf(url)));
				stopForSecond(20);
				return path;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ""; // Skip the ones that we already cloned
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for searching CMakeList.txt files and running cmake
	public static void searchForCMakefile(File root) {
		File[] files = root.listFiles();
		long timeout = 180 * 1000;
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					String cmakefiledir = file.getAbsolutePath().toString();
					String parentdir = cmakefiledir;
					String xx = file.getName().toString();
					parentdir = parentdir.replace(xx, "");
					if (xx.equals("CMakeLists.txt")) {
						if (!(is_insubmodule(cmakefiledir))) {
							try {
								String command = "cmd.exe /c docker run --rm -v " + parentdir
										+ ":/src  emscripten/emsdk emcmake cmake";
								Process p = Runtime.getRuntime().exec(command);
								p.waitFor(timeout, TimeUnit.MILLISECONDS);
							} catch (IOException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							stopForSecond(5);
							System.out.println("\nCMake command is running ...");
						}
					}
				} else if (file.isDirectory())
					searchForCMakefile(file);
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
	public static void searchForMakefile(File root) {
		File[] files = root.listFiles();
		long timeout = 180 * 1000;
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					String dirr = file.getAbsolutePath().toString();
					String parentdir = dirr;
					String xx = file.getName().toString();
					parentdir = parentdir.replace(xx, "");
					if (xx.equals("makefile") || xx.equals("Makefile")) {
						try {
							String command = "cmd.exe /c docker run --rm -v " + parentdir
									+ ":/src  emscripten/emsdk emmake make";
							Process p = Runtime.getRuntime().exec(command);
							p.waitFor(timeout, TimeUnit.MILLISECONDS);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						stopForSecond(5);
						System.out.println("\nMake command is running ...");
					}
				} else if (file.isDirectory())
					searchForMakefile(file);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for searching .wasm and .wat files
	public static void searchForWasmAndWat(File root, String prepost) throws IOException {
		File[] files = root.listFiles();
		Path destination;
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
						if (prepost.equals("pre"))
							destination = Paths.get("wasm-wat-files-pre\\wat-files\\" + reponame);
						else
							destination = Paths.get("wasm-wat-files\\wat-files\\" + reponame);
						String newFileName = hash256(source);
						newFileName = newFileName + ".wat";

						File f2 = new File(destination + "\\" + newFileName);
						Path newPath = Paths.get(f2.toString());
						if (!f2.exists()) {
							Files.copy(source, newPath);
							String command = "cmd.exe /c cd " + destination + " && wat2wasm " + newFileName;
							runcommand(command, 60);
							saveMetafiles(destination, newFileName, dirr);
						} else
							System.out.println("File exists");
					} else if (xx.endsWith(".wasm")) {
						wasmfiles.add(file.getAbsolutePath());
						if (prepost.equals("pre"))
							destination = Paths.get("wasm-wat-files-pre\\wasm-files\\" + reponame);
						else
							destination = Paths.get("wasm-wat-files\\wasm-files\\" + reponame);
						String newFileName = hash256(source);
						newFileName = newFileName + ".wasm";
						File f2 = new File(destination + "\\" + newFileName);
						Path newPath = Paths.get(f2.toString());
						if (!f2.exists()) {
							Files.copy(source, newPath);
							saveMetafiles(destination, newFileName, dirr);
						} else
							System.out.println("File exists");
					}
				} else if (file.isDirectory())
					searchForWasmAndWat(file, prepost);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for saving meta file (directory of .wasm and .wat files
	public static void saveMetafiles(Path metadestination, String newname, String metacontent) {
		try {
			File file = new File(metadestination + "\\" + newname + ".meta");
			FileWriter writer = new FileWriter(file);
			writer.write(metacontent);
			writer.close();
			System.out.println("File created successfully");
		} catch (IOException e) {
			System.out.println("An error occurred while creating the file");
			e.printStackTrace();
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
		int numofsym;
		try {
			Object obj = parser.parse(new FileReader("config.json"));
			JSONObject jsonObject = (JSONObject) obj;
			repokeywords = (String) jsonObject.get("keywords");
			date = (String) jsonObject.get("date");
			stars = (String) jsonObject.get("stars");
			forks = (String) jsonObject.get("forks");
			size = (String) jsonObject.get("size");
			numOfSymptoms = (String) jsonObject.get("numOfSymptoms");
			numofsym = Integer.parseInt(numOfSymptoms);
			for (int g = 0; g < numofsym; g++) {
				String symp = "symptom" + g;
				symptopms.add((String) jsonObject.get(symp));
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for saving in .csv file
	public static void saveArraylistFile(List<String> list, String name) throws IOException {
		File file = new File(name + ".csv");
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		for (int k = 0; k < list.size(); k++) {
			bw.write(list.get(k));
			bw.newLine();
		}
		bw.close();
		fw.close();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for reading from .csv file
	public static List<String> readFile(String name) {
		List<String> data = new ArrayList<String>();
		List<String> list = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(name + ".csv"));
			String line;
			while ((line = br.readLine()) != null) {
				data.add(line);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for removing existing folders
	public static void runcommand(String command, long timeout) {
		timeout = timeout * 1000;
		Process process;
		try {
			process = Runtime.getRuntime().exec(command);
			try {
				process.waitFor(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for checking Internet connection before cloning the repository
	public static boolean isInternetAvailable(String host, int port) {
		try (Socket socket = new Socket()) {
			SocketAddress address = new InetSocketAddress(host, port);
			socket.connect(address, 2000);
			return true;
		} catch (IOException e) {
			return false;
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