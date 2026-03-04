import launch.app.config.Config;
import launch.app.helpers.SimpleBuilder;
import launch.app.watchdog.CurrentKiller;
import launch.app.watchdog.ManagedProcess;
import launch.app.watchdog.Watchdog;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Launch {

    public static void main(String[] args) {

        try {
            
            // 0 -> App is stopped!
            // 10 -> App is initialized!
            // ...
            // 80 -> Update is launching...
            // 81 -> Update is relaunching...
            // ...
            // 95 -> Update is error!
            // 96 -> Update is crashed!
            // 99 -> Staging... -> Thông báo 100
            // 100 -> Staging đã tải về, trả 101
            // 101 -> Chuẩn bị Update, đẩy current sang backup, trả 102
            // 102 -> Đẩy staging sang current, trả 103
            // 103 -> Thực hiện cleanup backup (chỉ giữ 2 bản gần nhất và xoá hết staging rác), trả 104
            // 104 -> Thông báo hoàn tất cập nhật & kết thúc quá trình cập nhật
            // ...
            // 280 -> App is launching...
            // 281 -> App is relaunching...
            // ...
            // 295 -> App is error!
            // 296 -> App is crashed!

            System.out.println("[INFO] Launching app...");
            
            runtime();

        } catch (Exception e) {

            System.out.println("[ERROR] Launcher is crashed: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private static void runtime() {

        int exitcode = 10;
        String stderr = "";
        int runtimeDelay = Config.getInt("launch.runtime.delay", 1000);

        List<ManagedProcess> processList = new ArrayList<>();

        int i = 0;

        while (true) {

            String name = Config.get("processes." + i + ".name", null);

            if (name == null) break;

            String cmd = Config.get("processes." + i + ".cmd", "");

            processList.add(new ManagedProcess(name, cmd));

            i++;

        }

        Watchdog watchdog = new Watchdog(processList.toArray(new ManagedProcess[0]));

        while (true) {


            try {

                Thread.sleep(runtimeDelay);

                switch (exitcode) {

                    case 10: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        File current = new File("current");
                        exitcode = current.isDirectory() ? 280 : 80;
                        break;
                    }

                    case 280:
                    case 281: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        watchdog.startAll();

                        // Where launch is controlled by watchdog which is controlled by process also
                        exitcode = watchdog.mainThreadWaitForProcessExitcode();
                        System.out.println("[INFO] Exitcode returned back main Launch thread. Ready for any mission (exitcode from process = " + exitcode + ")");
                        watchdog.stopAll();
                        break;
                    }

                    case 95:
                    case 96:
                    case 295:
                    case 296: {
                        System.out.println("\n[INFO] "
                            + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")"
                            + "\n[ERROR] " + stderr + "\n");
                        stderr = "";
                        exitcode = (exitcode == 295 || exitcode == 296) ? 281 : 81;
                        break;
                    }

                    case 80:
                    case 81: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        exitcode = SimpleBuilder.run("launch/App.java", "launch.App", "update");
                        break;
                    }

                    case 99: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        System.out.println("[INFO] Waiting for staging...");

                        File state = new File("launch/app/update/state/update_staging.json");
                        int stagingTimeout = Config.getInt("update.staging.timeout", 30);
                        int timeout = 0;

                        while (true) {
                            timeout++;
                            if (timeout <= stagingTimeout) {
                                if (state.isFile()) {
                                    String content = new String(Files.readAllBytes(state.toPath()));
                                    if (content.contains("99")) {
                                        System.out.println("[INFO] Staging prepared! ( " + content + ")");
                                        state.delete();
                                        exitcode = 100;
                                        break;
                                    }
                                }
                            } else {
                                stderr = "timeout waiting for staging";
                                exitcode = 81;
                                CurrentKiller.kill();
                                break;
                            }
                            Thread.sleep(1000);
                        }
                        break;
                    }

                    case 100: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        exitcode = 101;
                        break;
                    }

                    case 101: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        try {
                            File[] stagingFolders = new File("staging")
                                .listFiles(f -> f.isDirectory() && f.getName().startsWith("staging_"));
                            if (stagingFolders == null) stagingFolders = new File[0];

                            File stagingLatest = Arrays.stream(stagingFolders)
                                .max(Comparator.comparing(File::getName))
                                .orElse(null);

                            if (stagingFolders.length == 0 || stagingLatest == null) {
                                stderr = "no staging folder was found, trying to relaunch app";
                                exitcode = 81;
                            }

                            if (stagingFolders.length > 0 && stagingLatest != null) {
                                String timestamp = stagingLatest.getName().substring("staging_".length());
                                File current = new File("current");
                                File backup = new File("backup");
                                if (current.isDirectory()) {
                                    CurrentKiller.kill();
                                    backup.mkdir();
                                    File backupTimestamp = new File("backup/backup_" + timestamp);
                                    Files.move(current.toPath(), backupTimestamp.toPath());
                                }
                                exitcode = 102;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            stderr = e.getMessage();
                            exitcode = 96;
                        }
                        break;
                    }

                    case 102: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        try {
                            File[] stagingFolders = new File("staging")
                                .listFiles(f -> f.isDirectory() && f.getName().startsWith("staging_"));
                            if (stagingFolders == null) stagingFolders = new File[0];

                            File stagingLatest = Arrays.stream(stagingFolders)
                                .max(Comparator.comparing(File::getName))
                                .orElse(null);

                            if (stagingFolders.length == 0 || stagingLatest == null) {
                                stderr = "no staging folder was found, trying to relaunch app";
                                exitcode = 81;
                            }

                            if (stagingFolders.length > 0 && stagingLatest != null) {
                                File current = new File("current");
                                Files.move(stagingLatest.toPath(), current.toPath());
                                exitcode = 103;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            stderr = e.getMessage();
                            exitcode = 96;
                        }
                        break;
                    }

                    case 103: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        try {
                            File[] stagingFolders = new File("staging")
                                .listFiles(f -> f.isDirectory() && f.getName().startsWith("staging_"));
                            if (stagingFolders == null) stagingFolders = new File[0];

                            File[] backupFolders = new File("backup")
                                .listFiles(f -> f.isDirectory() && f.getName().startsWith("backup_"));
                            if (backupFolders == null) backupFolders = new File[0];

                            for (File folder : stagingFolders) {
                                Files.walk(folder.toPath())
                                    .sorted(Comparator.reverseOrder())
                                    .map(Path::toFile)
                                    .forEach(f -> {
                                        f.setWritable(true);
                                        boolean deleted = f.delete();
                                        if (!deleted) {
                                            System.out.println("[ERROR] Failed to remove staging folders: " + f.getPath());
                                        }
                                    });
                            }

                            Arrays.sort(backupFolders, Comparator.comparing(File::getName).reversed());
                            int backupMaxCount = Config.getInt("update.backup.maxCount", 2);

                            for (int j = backupMaxCount; j < backupFolders.length; j++) {
                                Files.walk(backupFolders[j].toPath())
                                    .sorted(Comparator.reverseOrder())
                                    .map(Path::toFile)
                                    .forEach(f -> {
                                        f.setWritable(true);
                                        boolean deleted = f.delete();
                                        if (!deleted) {
                                            System.out.println("[ERROR] Failed to remove backup folders: " + f.getPath());
                                        }
                                    });
                            }

                            exitcode = 104;
                        } catch (Exception e) {
                            e.printStackTrace();
                            stderr = e.getMessage();
                            exitcode = 96;
                        }
                        break;
                    }

                    case 104: {
                        System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                        exitcode = 10;
                        break;
                    }

                    default: {
                        System.out.println("[ERROR] Unknown exitcode = " + exitcode + ". Let it be 296 for app restarting.");

                        if (!stderr.isEmpty()) {
                            System.out.println("\n[INFO] "
                                + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")"
                                + "\n[ERROR] " + stderr + "\n");
                            stderr = "";
                        }
                        exitcode = 296;
                    }
                }

            } catch (Exception e) {

                System.out.println("[ERROR] App is crashed: " + e.getMessage());
                e.printStackTrace();
                exitcode = 296;

            }
        }
    }

    private static String exitcodeExplanation(int exitcode) {

        switch (exitcode) {
            case 0:
                return "APP IS STOPPED!";
            case 10:
                return "APP IS INITIALIZED!";
            case 80:
                return "UPDATE IS LAUNCHING...";
            case 81:
                return "UPDATE IS RELAUNCHING...";
            case 95:
                return "UPDATE IS ERROR!";
            case 96:
                return "UPDATE IS CRASHED!";
            case 99:
                return "UPDATE IS DOWNLOADING STAGING...";
            case 100:
                return "STAGING UPDATE IS PREPARED!";
            case 101:
                return "READY TO INSTALL UPDATE!";
            case 102:
                return "CURRENT FOLDER IS BACKUP!";
            case 103:
                return "STAGING FOLDER IS MOVED TO CURRENT!";
            case 104:
                return "UPDATE CLEANUP IS DONE!";
            case 280:
                return "APP IS LAUNCHING...";
            case 281:
                return "APP IS RELAUNCHING...";
            case 295:
                return "APP IS ERROR!";
            case 296:
                return "APP IS CRASHED!";
            default:
                return "APP IS IN AN UNKNOWN STATE.";
        }
    }
}