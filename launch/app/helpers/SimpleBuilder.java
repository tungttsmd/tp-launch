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

    private static String getAppDir() {
        return ProcessHandle.current().info().command()
            .map(cmd -> new File(cmd).getParent())
            .orElse(".");
    }

    private static String[] buildCmd(String sourceFile, String className, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if (isDev(sourceFile)) {
            cmd.add(sourceFile);                                      // Dev: java launch/App.java
        } else {
            cmd.add("-cp");
            cmd.add(getAppDir() + "/app/launch.jar");
            cmd.add(className);                                       // Prod: java -cp app/launch.jar launch.App
        }
        cmd.addAll(Arrays.asList(args));
        return cmd.toArray(new String[0]);
    }

    // Blocking — dùng cho App.java (update)
    public static int run(String sourceFile, String className, String... args) throws Exception {
        return SimpleProcess.run(buildCmd(sourceFile, className, args));
    }

    // Non-blocking new window — dùng cho Staging.java
    public static void runInNewWindow(String sourceFile, String className, String... args) throws Exception {
        List<String> cmd = new ArrayList<>(Arrays.asList("cmd", "/c", "start", "cmd", "/c"));
        cmd.addAll(Arrays.asList(buildCmd(sourceFile, className, args)));
        SimpleProcess.run(cmd.toArray(new String[0]));
    }
}
