package WASMIZER;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.poi.ss.formula.functions.Replace;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WASMIZER_class {

	private static Gson gson;
	public static List<String> metadata = new ArrayList<String>();
	public static List<String> commitSHAlist = new ArrayList<String>();
	public static List<String> repolist = new ArrayList<String>();
	public static List<String> clonedrepo = new ArrayList<String>();
	public static List<String> starscountlist = new ArrayList<String>();
	public static List<String> forkscountlist = new ArrayList<String>();
	public static List<String> sizelist = new ArrayList<String>();
	public static List<String> licenselist = new ArrayList<String>();
	public static List<String> branchlist = new ArrayList<String>();
	public static List<String> createddatelist = new ArrayList<String>();
	public static List<Long> idlist = new ArrayList<Long>();
	public static List<String> foldernamelist = new ArrayList<String>();
	public static List<String> cloneddate = new ArrayList<String>();
	public static List<String> repolistBasedcode = new ArrayList<String>();
	public static List<String> pusheddatelist = new ArrayList<String>();
	public static List<String> CMakeListpaths = new ArrayList<String>();
	public static List<String> submodules;
	public static List<String> prewatfiles = new ArrayList<String>();
	public static List<String> prewasmfiles = new ArrayList<String>();
	public static List<String> pretemp = new ArrayList<String>();
	public static List<String> posttemp = new ArrayList<String>();
	public static List<String> postwatfiles = new ArrayList<String>();
	public static List<String> postwasmfiles = new ArrayList<String>();
	public static List<String> symptopms = new ArrayList<String>();
	public static List<String> newfiles = new ArrayList<String>();
	public static List<String> newfilessource = new ArrayList<String>();
	public static int wasmcount = 0, watcount = 0, count, prewasmcount = 0, prewatcount = 0, fcount = 0, r = 0,
			presourcecount = 0, compilesourcecount = 0, totalwat, totalwasm;
	public static String reponame, repolink, path, prepath, newFileName, s1;
	public static String repokeywords, date, stars, forks, size, numOfSymptoms, precompilation_command,
			compilation_command, precompilation_sourcefile, compilation_sourcefile, formattedstartdatetime, token;
	public static File mainroot;
	public static LocalDateTime startdatetime, enddatetime;

	public static void main(String[] args) throws IOException, URISyntaxException, JSONException {

		// Using GSON to parse or print response JSON.
		gson = new GsonBuilder().setPrettyPrinting().create();
		// Read features of query and paths from config.json file
		readConfig();
		// Collecting the repositories based on the keywords and symptoms
		searchCodeByContent();
		// Clone the repository, compile it, count the .wasm and .wat files
		cloneAndCompile();

		System.out.println("The End ... ");
		System.out.println("\n\nNumber of .wasm files before compile :" + prewasmfiles.size());
		System.out.println("\nNumber of .wat files before compile :" + prewatfiles.size());
		System.out.println("\n\nNumber of .wasm files after compile :" + postwasmfiles.size());
		System.out.println("\nNumber of .wat files after compile :" + postwatfiles.size());

		// Save .wasm and .wat files directories in the .csv files
		saveArraylistFile(prewasmfiles, "prewasmdirectories");
		saveArraylistFile(prewatfiles, "prewatdirectories");
		saveArraylistFile(postwasmfiles, "postwasmdirectories");
		saveArraylistFile(postwatfiles, "postwatdirectories");
		saveArraylistFile(newfiles, "newfiles");
		saveArraylistFile(newfilessource, "newfilesources");
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for collecting the repositories
	private static void searchCodeByContent() throws ClientProtocolException, IOException {

		String incomplete;
		Map repocontentSearchResult;
		double totalcount, totalcount2;
		// Read the cloned data
		clonedrepo = readFile("clonedrepo");
		cloneddate = readFile("cloneddate");
		metadata = readmetadata("metadata");
		if (clonedrepo.size() > 0) {
			clonedrepo.remove(0);
			cloneddate.remove(0);
			metadata.remove(0);
		}
		// Read the .wasm and .wat directories of cloned repositories
		prewasmfiles = readFile("prewasmdirectories");
		prewatfiles = readFile("prewatdirectories");
		postwasmfiles = readFile("postwasmdirectories");
		postwatfiles = readFile("postwatdirectories");
		if (prewasmfiles.size() > 0)
			prewasmfiles.remove(0);
		if (prewatfiles.size() > 0)
			prewatfiles.remove(0);
		if (postwasmfiles.size() > 0)
			postwasmfiles.remove(0);
		if (postwatfiles.size() > 0)
			postwatfiles.remove(0);

		String GITHUB_API_BASE_URL = "https://api.github.com/", GITHUB_API_SEARCH_CODE_PATH = "search/code?q=";
		int i = 0;
		do {
			i++;
			// Query for collecting repositories
			String repoContentQuery = repokeywords + "+in:name,topics,description,readme+pushed:" + date
					+ "+language:c+language:cpp+size:%3E=" + size + "%20forks:%3E=" + forks + "%20stars:%3E=" + stars
					+ "%20&page=" + i + "&per_page=100";

			// Check the results of github API
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
				long id = (r.getAsJsonObject().get("id").getAsLong());
				String repourl = r.getAsJsonObject().get("html_url").toString();
				String foldername = repourl.split("/")[repourl.split("/").length - 2] + "-"
						+ repourl.split("/")[repourl.split("/").length - 1];
				foldername = foldername.replace("\"", "");
				String fork = r.getAsJsonObject().get("fork").toString();
				String createddate = r.getAsJsonObject().get("created_at").toString();
				String pusheddate = r.getAsJsonObject().get("pushed_at").toString();
				String starscount = r.getAsJsonObject().get("stargazers_count").toString();
				String size = r.getAsJsonObject().get("size").toString();
				String forkscount = r.getAsJsonObject().get("forks_count").toString();
				String branch = r.getAsJsonObject().get("default_branch").toString();
				branch = branch.replace("\"", "");
				String licensename;
				try {
					String license = r.getAsJsonObject().get("license").toString();
					String license1 = license.split(",")[1];
					licensename = license1.split(":")[1];
					licensename = licensename.replace("\"", "");
				} catch (Exception e) {
					licensename = "unknown";
				}
				String commit = "";
				try {
					commit = commitSHA(repourl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (fork.equals("false")) {
					// if repository is not cloned, add to list for cloning
					if (!(clonedrepo.contains(repourl))) {
						idlist.add(id);
						foldernamelist.add(foldername);
						repolist.add(repourl);
						createddatelist.add(createddate);
						pusheddatelist.add(pusheddate);
						starscountlist.add(starscount);
						forkscountlist.add(forkscount);
						sizelist.add(size);
						branchlist.add(branch);
						licenselist.add(licensename);
						commitSHAlist.add(commit);
					}
					// if repository is cloned but pushed after previous clone
					else if (!(cloneddate.get(clonedrepo.indexOf(repourl)).equals(pusheddate))) {
						int index = clonedrepo.indexOf(repourl);
						clonedrepo.remove(index);
						cloneddate.remove(index);
						metadata.remove(index);
						String folder = foldername;

						// Repository cloned before but has pushed after cloning, we delete existing
						// files
						String comm0 = "cmd.exe /c cd repobase && rd /s /q " + folder + "";
						runcommand(comm0, 180);
						String comm1 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wat-files && rd /s /q " + folder + "";
						runcommand(comm1, 60);
						String comm2 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wasm-files && rd /s /q " + folder
								+ "";
						runcommand(comm2, 60);
						String comm3 = "cmd.exe /c cd output\\wasm-wat-files\\wat-files && rd /s /q " + folder + "";
						runcommand(comm3, 60);
						String comm4 = "cmd.exe /c cd output\\wasm-wat-files\\wasm-files && rd /s /q " + folder + "";
						runcommand(comm4, 60);

						idlist.add(id);
						foldernamelist.add(foldername);
						repolist.add(repourl);
						createddatelist.add(createddate);
						pusheddatelist.add(pusheddate);
						starscountlist.add(starscount);
						forkscountlist.add(forkscount);
						sizelist.add(size);
						branchlist.add(branch);
						licenselist.add(licensename);
						commitSHAlist.add(commit);
					}
				}
			});
		} while (i * 100 < totalcount && i < 10);

		System.out.println(repolist.size());

		// Save the repositories in the file
		saveArraylistFile(repolist, "repositories");
		saveArraylistFile(pusheddatelist, "pusheddatelist");

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
	public static void cloneAndCompile() throws IOException, JSONException {

//		repolistBasedcode.add("https://github.com/goldvideo/decoder_wasm");
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
			String command2 = "cmd.exe /c cd output\\wasm-wat-files\\wasm-files && md " + reponame + "";
			runcommand(command2, 60);
			String command3 = "cmd.exe /c cd output\\wasm-wat-files\\wat-files && md " + reponame + "";
			runcommand(command3, 60);
			String command4 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wasm-files && md " + reponame + "";
			runcommand(command4, 60);
			String command5 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wat-files && md " + reponame + "";
			runcommand(command5, 60);

			String rootDir = "repobase\\" + path + "\\";

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

			mainroot = new File(rootDir);
			// Search for .wasm and .wat before compile
			prewasmcount = 0;
			prewatcount = 0;
			searchForWasmAndWat(mainroot, "pre", repolink);
			// Search for precompilation files and run them
			startdatetime = LocalDateTime.now();
			formattedstartdatetime = startdatetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			presourcecount = 0;
			if (precompilation_command.length() != 0 || precompilation_sourcefile.length() != 0) {
				r = 0;
				preCompilation(mainroot);
			}
			stopForSecond(15);
			// Search for compilation files and compile them
			compilesourcecount = 0;
			if (compilation_command.length() != 0 || compilation_sourcefile.length() != 0) {
				r = 0;
				Compilation(mainroot);
			}
			stopForSecond(20);
			// Search for .wasm and .wat
			wasmcount = 0;
			watcount = 0;
			enddatetime = LocalDateTime.now();

			searchForWasmAndWat(mainroot, "post", repolink);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for cloning repositories
	public static String clone(String url) throws JSONException {

		long timeout = 240 * 1000; // 240 seconds
		// Clone a git repository and return the path where it was cloned
		String path = url.split("/")[url.split("/").length - 2] + "-" + url.split("/")[url.split("/").length - 1];
		path = path.replace("\"", "");
		System.out.println("trying to clone " + url);
		File file = new File(path);
		if (!file.exists()) {
			try {
				Process process = Runtime.getRuntime()
						.exec("cmd.exe /c git clone --depth 1 --recurse-submodules --shallow-submodules " + url + " "
								+ "repobase\\" + path);
				try {
					process.waitFor(timeout, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				clonedrepo.add(url);
				int index = repolist.indexOf(url);
				cloneddate.add(pusheddatelist.get(index));
				stopForSecond(20);
				return path;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ""; // Skip the ones that we already cloned
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for searching preCompilation files and running precomilation command
	public static void preCompilation(File root) throws IOException {
		File[] files = root.listFiles();
		int cc1 = 0, cc2 = 0;
		long timeout = 200 * 1000;
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					String cmakefiledir = file.getAbsolutePath().toString();
					String parentdir = cmakefiledir;
					String xx = file.getName().toString();
					parentdir = parentdir.replace(xx, "");
					if (xx.equals(precompilation_sourcefile)) {
						if (!(is_insubmodule(cmakefiledir))) {
							presourcecount++;
							pretemp.clear();
							posttemp.clear();
							fcount = 0;
							cc1 = countwasmwatfiles(mainroot, "pre");
							String command;
							try {
								command = "cmd.exe /c docker run --rm -v " + parentdir + ":/src  "
										+ precompilation_command;
								Process p = Runtime.getRuntime().exec(command);
								p.waitFor(timeout, TimeUnit.MILLISECONDS);

							} catch (IOException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							stopForSecond(15);
							r++;
							System.out.println("\nCMake command is running " + r + " ...");
							// counting the files after command
							fcount = 0;
							cc2 = countwasmwatfiles(mainroot, "post");
							if (cc1 != cc2) {
								posttemp.removeAll(pretemp);
								for (String entity : posttemp) {
									if (entity.endsWith(".wasm")) {
										String hashed = hash256((Paths.get(entity)));
										hashed = hashed + ".wasm";
										newfiles.add(hashed);
										newfilessource.add(cmakefiledir);
									} else if (entity.endsWith(".wat")) {
										String hashed = hash256((Paths.get(entity)));
										hashed = hashed + ".wat";
										newfiles.add(hashed);
										newfilessource.add(cmakefiledir);
									}
								}
							}

						}
					}
				} else if (file.isDirectory())
					preCompilation(file);
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
	// Function for searching the Compilation files and running compilation command
	public static void Compilation(File root) throws IOException {
		File[] files = root.listFiles();
		int cc1 = 0, cc2 = 0;
		long timeout = 240 * 1000;
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					String dirr = file.getAbsolutePath().toString();
					String parentdir = dirr;
					String xx = file.getName().toString();
					parentdir = parentdir.replace(xx, "");
					if (xx.equals(compilation_sourcefile) || xx.equals(compilation_sourcefile.toLowerCase())) {
						// counting the files before command
						compilesourcecount++;
						pretemp.clear();
						posttemp.clear();
						fcount = 0;
						cc1 = countwasmwatfiles(mainroot, "pre");
						try {
							String command = "cmd.exe /c docker run --rm -v " + parentdir + ":/src  "
									+ compilation_command;
							Process p = Runtime.getRuntime().exec(command);
							p.waitFor(timeout, TimeUnit.MILLISECONDS);

						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						stopForSecond(20);
						r++;
						System.out.println("\nMake command is running " + r + " ...");
						// counting the files after command
						fcount = 0;
						cc2 = countwasmwatfiles(mainroot, "post");
						if (cc1 != cc2) {
							posttemp.removeAll(pretemp);
							for (String entity : posttemp) {
								if (entity.endsWith(".wasm")) {
									String hashed = hash256((Paths.get(entity)));
									hashed = hashed + ".wasm";
									newfiles.add(hashed);
									newfilessource.add(dirr);
								} else if (entity.endsWith(".wat")) {
									String hashed = hash256((Paths.get(entity)));
									hashed = hashed + ".wat";
									newfiles.add(hashed);
									newfilessource.add(dirr);
								}
							}
						}
					}
				} else if (file.isDirectory())
					Compilation(file);
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for searching .wasm and .wat files
	public static void searchForWasmAndWat(File root, String prepost, String url) throws IOException {
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
						if (prepost.equals("pre")) {
							prewatfiles.add(file.getAbsolutePath());
							destination = Paths.get("output\\wasm-wat-files-pre\\wat-files\\" + reponame);
							prewatcount++;
							totalwat++;
						} else {
							postwatfiles.add(file.getAbsolutePath());
							destination = Paths.get("output\\wasm-wat-files\\wat-files\\" + reponame);
							watcount++;
							totalwat++;
						}
						String newFileName = hash256(source);
						newFileName = newFileName + ".wat";
						File f2 = new File(destination + "\\" + newFileName);
						Path newPath = Paths.get(f2.toString());
						if (!f2.exists()) {
							Files.copy(source, newPath);
							stopForSecond(5);
							if (prepost.equals("pre")) {
								String command = "cmd.exe /c cd output\\wasm-wat-files-pre\\wat-files\\" + reponame
										+ "\\ && C:\\wabt\\bin\\wat2wasm --enable-all \"" + newFileName + "\"";
								runcommand(command, 60);
							} else {
								String command = "cmd.exe /c cd output\\wasm-wat-files\\wat-files\\" + reponame
										+ "\\ && C:\\wabt\\bin\\wat2wasm --enable-all \"" + newFileName + "\"";
								runcommand(command, 60);
							}
							saveMetafiles(destination, newFileName, dirr);
						} else
							System.out.println("File exists");
					} else if (xx.endsWith(".wasm")) {

						if (prepost.equals("pre")) {
							prewasmfiles.add(file.getAbsolutePath());
							destination = Paths.get("output\\wasm-wat-files-pre\\wasm-files\\" + reponame);
							prewasmcount++;
							totalwasm++;
						} else {
							postwasmfiles.add(file.getAbsolutePath());
							destination = Paths.get("output\\wasm-wat-files\\wasm-files\\" + reponame);
							wasmcount++;
							totalwasm++;
						}
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
					searchForWasmAndWat(file, prepost, url);
			}
		}
		int total = wasmcount + watcount + prewatcount + prewasmcount;
		String meta = "";
		long durationseconds = 0;
		if (prepost.equals("post") && total != 0) {
			Duration duration = Duration.between(startdatetime, enddatetime);
			durationseconds = duration.getSeconds();
			int index = repolist.indexOf(url);
			meta = idlist.get(index) + "," + foldernamelist.get(index) + "," + repolist.get(index) + ","
					+ createddatelist.get(index) + "," + pusheddatelist.get(index) + "," + starscountlist.get(index)
					+ "," + forkscountlist.get(index) + "," + sizelist.get(index) + "," + branchlist.get(index) + ","
					+ commitSHAlist.get(index) + "," + licenselist.get(index) + "," + prewatcount + "," + prewasmcount
					+ "," + watcount + "," + wasmcount + "," + presourcecount + "," + compilesourcecount + ","
					+ formattedstartdatetime + "," + durationseconds;
		}

		if (prepost.equals("post") && total == 0) {
			String comm0 = "cmd.exe /c cd repobase && rd /s /q " + reponame + "";
			runcommand(comm0, 180);
			String comm1 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wat-files && rd /s /q " + reponame + "";
			runcommand(comm1, 60);
			String comm2 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wasm-files && rd /s /q " + reponame + "";
			runcommand(comm2, 60);
			String comm3 = "cmd.exe /c cd output\\wasm-wat-files\\wat-files && rd /s /q " + reponame + "";
			runcommand(comm3, 60);
			String comm4 = "cmd.exe /c cd output\\wasm-wat-files\\wasm-files && rd /s /q " + reponame + "";
			runcommand(comm4, 60);

		} else if (prepost.equals("post") && watcount == 0 && wasmcount != 0) {
			String comm3 = "cmd.exe /c cd output\\wasm-wat-files\\wat-files && rd /s /q " + reponame + "";
			runcommand(comm3, 60);

			File file = new File("output\\wasm-wat-files\\wasm-files\\" + reponame + "\\metadata.csv");
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(
					"Repository ID , Owner-Repository Name , Repository URL , Creation Date , Pushed date , Stars , Forks , Size , Branch Name , Commit SHA , License Name , # wat file before compilation , # wasm file before compilation , # wat file after compilation , # wasm file after compilation , # Pre compilation source , # compilation source , Date/Time of compilation , Compilation time (ms)");
			bw.newLine();
			bw.write(meta);
			bw.close();
			fw.close();
			metadata.add(meta);
			savemetadata(metadata);
		} else if (prepost.equals("post") && wasmcount == 0 && watcount != 0) {
			String comm4 = "cmd.exe /c cd output\\wasm-wat-files\\wasm-files && rd /s /q " + reponame + "";
			runcommand(comm4, 60);

			File file = new File("output\\wasm-wat-files\\wat-files\\" + reponame + "\\metadata.csv");
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(
					"Repository ID , Owner-Repository Name , Repository URL , Creation Date , Pushed date , Stars , Forks , Size , Branch Name , Commit SHA , License Name , # wat file before compilation , # wasm file before compilation , # wat file after compilation , # wasm file after compilation , # Pre compilation source , # compilation source , Date/Time of compilation , Compilation time (ms)");
			bw.newLine();
			bw.write(meta);
			bw.close();
			fw.close();
			metadata.add(meta);
			savemetadata(metadata);
		} else if (prepost.equals("post") && prewatcount == 0) {
			String comm4 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wat-files && rd /s /q " + reponame + "";
			runcommand(comm4, 60);
			metadata.add(meta);
			savemetadata(metadata);
		} else if (prepost.equals("post") && prewasmcount == 0) {
			String comm4 = "cmd.exe /c cd output\\wasm-wat-files-pre\\wasm-files && rd /s /q " + reponame + "";
			runcommand(comm4, 60);
			metadata.add(meta);
			savemetadata(metadata);
		}
		// Save total statistic of wat and wasm files
		File file = new File("output\\statistics.csv");
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("Total wat files");
		bw.newLine();
		String tmp1 = Integer.toString(totalwat);
		bw.write(tmp1);
		bw.newLine();
		bw.newLine();
		bw.write("Total wasm files");
		bw.newLine();
		String tmp2 = Integer.toString(totalwasm);
		bw.write(tmp2);
		bw.close();
		fw.close();

		saveArraylistFile(clonedrepo, "clonedrepo");
		saveArraylistFile(cloneddate, "cloneddate");
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
			precompilation_command = (String) jsonObject.get("precompilation_command");
			compilation_command = (String) jsonObject.get("compilation_command");
			precompilation_sourcefile = (String) jsonObject.get("precompilation_sourcefile");
			compilation_sourcefile = (String) jsonObject.get("compilation_sourcefile");
			token = (String) jsonObject.get("token");
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader("output\\statistics.csv"));
			br.readLine();
			totalwat = Integer.parseInt(br.readLine());
			br.readLine();
			br.readLine();
			totalwasm = Integer.parseInt(br.readLine());
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for saving in .csv file
	public static void saveArraylistFile(List<String> list, String name) throws IOException {
		File file = new File(name + ".csv");
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(name);
		bw.newLine();
		for (int k = 0; k < list.size(); k++) {
			bw.write(list.get(k));
			bw.newLine();
		}
		bw.close();
		fw.close();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Function for saving in .csv file
	public static void savemetadata(List<String> list) throws IOException {
		File file = new File("output\\metadata.csv");
		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(
				"Repository ID , Owner-Repository Name , Repository URL , Creation Date , Pushed date , Stars , Forks , Size , Branch Name , Commit SHA , License Name , # wat file before compilation , # wasm file before compilation , # wat file after compilation , # wasm file after compilation , # Pre compilation source , # compilation source");
		bw.newLine();
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
	// Function for reading from .csv file
	public static List<String> readmetadata(String name) {
		List<String> data = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("output\\" + name + ".csv"));
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
		stopForSecond(30);
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
// Function for checking Internet connection before cloning the repository
	public static int countwasmwatfiles(File root, String pp) throws IOException {
		File[] files = root.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					if (file.getName().toString().endsWith(".wasm")) {
						fcount++;
						if (pp.equals("pre"))
							pretemp.add(file.getAbsolutePath());
						else
							posttemp.add(file.getAbsolutePath());
					}
				} else if (file.isDirectory())
					countwasmwatfiles(file, pp);
			}
		}
		return fcount;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Finding the commit SHA
	public static String commitSHA(String url) throws IOException, JSONException, InterruptedException {
		stopForSecond(20);
		String commitSHA = "";
		int retrycount = 0;
		while (retrycount < 5) {
			try {
				String url2 = url;
				url2 = url2.replace("https://github.com/", "");
				url2 = url2.replace("\"", "");
				url2 = "https://api.github.com/repos/" + url2 + "/commits";

				URL urll = new URL(url2);
				HttpURLConnection con = (HttpURLConnection) urll.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("Accept", "application/vnd.github.v3+json");
				con.setRequestProperty("Authorization", "token " + token);

				// Read the API response and parse the JSON data
				int status = con.getResponseCode();
				if (status == 200) {
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					StringBuilder response = new StringBuilder();
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
					JSONArray jsonArray2 = new JSONArray(response.toString());
					org.json.JSONObject commit = jsonArray2.getJSONObject(0);
					commitSHA = commit.getString("sha");
					break;
				} else if (status == 403) {
					String resetheader = con.getHeaderField("X-RateLimit-Reset");
					if (resetheader != null) {
						long resettime = Long.parseLong(resetheader) * 1000;
						long waittime = resettime - System.currentTimeMillis();
						Thread.sleep(waittime);
						stopForSecond(20);
					}
				}
			} catch (IOException e) {
				commitSHA = "Rate limit";
			}
			retrycount++;
		}
		return commitSHA;
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