package launch.app.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleBuilder {

    // Dev  = file .java còn tồn tại bên cạnh source
    // Prod = chạy từ jpackage exe, không có .java, chỉ có JAR
    private static boolean isDev(String sourceFile) {
        return new File(sourceFile).exists();
    }

    private static String getJarPath() {
        try {
            return new File(SimpleBuilder.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            return "app/launch.jar";
        }
    }

    private static String getJavaExe() {
        File jar = new File(getJarPath());
        System.out.println("[INFO] Java.exe? jar path: " + jar.getAbsolutePath());
        File bundledJava = new File(jar.getParentFile().getParentFile(), "runtime/bin/java.exe");
        System.out.println("[INFO] Java.exe? bundled java: " + bundledJava.getAbsolutePath() + " exists=" + bundledJava.exists());
        if (bundledJava.exists()) {
            return bundledJava.getAbsolutePath();
        }
        return "java";
    }

    private static String[] buildCmd(String sourceFile, String className, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(getJavaExe());
        if (isDev(sourceFile)) {
            cmd.add(sourceFile);                                      // Dev: java launch/App.java
        } else {
            cmd.add("-cp");
            cmd.add(getJarPath());
            cmd.add(className);                                       // Prod: java -cp /abs/path/launch.jar launch.App
        }
        cmd.addAll(Arrays.asList(args));
        return cmd.toArray(new String[0]);
    }

    // Blocking — dùng cho App.java (update)
    public static int run(String sourceFile, String className, String... args) throws Exception {
        return SimpleProcess.run(buildCmd(sourceFile, className, args));
    }

    private static String getWorkDir() {
        // Derive working dir from JAR location: <root>/app/WinAtSvcL.jar -> <root>
        try {
            File jar = new File(SimpleBuilder.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsoluteFile();
            return jar.getParentFile().getParentFile().getAbsolutePath();
        } catch (Exception e) {
            return System.getProperty("user.dir");
        }
    }

    // Non-blocking (staging update có xài waitFor nên cảm giác tưởng bị block) new window — dùng cho Staging.java
    public static void runInNewWindow(String sourceFile, String className, boolean isKeepConsole, String... args) throws Exception {

        String workDir = getWorkDir();

        if (isKeepConsole) {

            List<String> cmd = new ArrayList<>(Arrays.asList("cmd", "/c", "start", "/d", workDir, "", "cmd", "/k"));
            cmd.addAll(Arrays.asList(buildCmd(sourceFile, className, args)));
            SimpleProcess.run(cmd.toArray(new String[0]));

        } else {
            ProcessBuilder pb = new ProcessBuilder(buildCmd(sourceFile, className, args));
            pb.inheritIO();
            pb.directory(new File(workDir));
            pb.start();
        }
    }
}
