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

    private static String[] buildCmd(String sourceFile, String className, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
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

    // Non-blocking new window — dùng cho Staging.java
    public static void runInNewWindow(String sourceFile, String className, boolean isKeepConsole, String... args) throws Exception {

        String keepType = (isKeepConsole) ? "/k" : "/c";

        List<String> cmd = new ArrayList<>(Arrays.asList("cmd", "/c", "start", "cmd", keepType));
        cmd.addAll(Arrays.asList(buildCmd(sourceFile, className, args)));
        SimpleProcess.run(cmd.toArray(new String[0]));
    }
}
