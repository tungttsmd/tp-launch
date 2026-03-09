package launch.app.helpers;

public class SimpleGit {

    // Known install paths (chocolatey custom dir, default choco, system)
    private static final String[] GIT_PATHS = {
        "C:/ProgramData/Microsoft/Windows/WinSxS/Git/bin/git.exe",  // bat installer custom dir
        "C:/ProgramData/Microsoft/Windows/WinSxS/Git/cmd/git.exe",  // alt cmd/ subfolder
        "C:/ProgramData/chocolatey/bin/git.exe",                     // choco shim
        "C:/Program Files/Git/bin/git.exe",                          // default system install
        "C:/Program Files (x86)/Git/bin/git.exe",                    // 32-bit
        System.getProperty("user.home") + "/AppData/Local/Programs/Git/bin/git.exe", // per-user
    };

    // Read fresh PATH from Windows registry (system + user), bypass stale JVM environment
    private static String getFreshPath() {
        try {
            StringBuilder sb = new StringBuilder();
            String[] keys = {
                "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
                "HKCU\\Environment"
            };
            for (String key : keys) {
                Process p = new ProcessBuilder("reg", "query", key, "/v", "PATH")
                    .redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes());
                p.waitFor();
                for (String line : out.split("\\r?\\n")) {
                    if (line.trim().toUpperCase().startsWith("PATH")) {
                        // format: "    PATH    REG_EXPAND_SZ    <value>"
                        int idx = line.indexOf("REG_");
                        if (idx != -1) {
                            idx = line.indexOf("    ", idx);
                            if (idx != -1) sb.append(line.substring(idx).trim()).append(java.io.File.pathSeparator);
                        }
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return System.getenv("PATH");
        }
    }

    public static String findGitExe() {
        // Try with fresh PATH from registry
        try {
            String freshPath = getFreshPath();
            ProcessBuilder pb = new ProcessBuilder("git", "--version").redirectErrorStream(true);
            pb.environment().put("PATH", freshPath);
            Process p = pb.start();
            if (p.waitFor() == 0) return "git";
        } catch (Exception ignored) {}

        // Try known paths
        for (String path : GIT_PATHS) {
            if (new java.io.File(path).exists()) return path;
        }
        return null;
    }

    public static boolean isInstalled() {
        return findGitExe() != null;
    }

    // Clone repo, fallback to SimpleDownloader if git is not installed
    // repoUrl: "https://github.com/user/repo"
    // branch:  "staging"
    public static int cloneOrFallback(String repoUrl, String branch, String destDir) throws Exception {
        String gitExe = findGitExe();
        if (gitExe != null) {
            System.out.println("[INFO] Git found: " + gitExe + ", cloning...");
            return SimpleProcess.run(gitExe, "clone", "--branch", branch, "--single-branch", "--depth", "1", repoUrl, destDir);
        } else {
            System.out.println("[WARN] Git not found, falling back to zip download...");
            String zipUrl = repoUrl + "/archive/refs/heads/" + branch + ".zip";
            return SimpleDownloader.downloadAndExtract(zipUrl, destDir, "download.zip");
        }
    }
}
