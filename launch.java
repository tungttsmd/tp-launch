import launch.app.helpers.SimpleProcess;
import launch.app.watchdog.CurrentKiller;
import launch.app.watchdog.ManagedProcess;
import launch.app.watchdog.Watchdog;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

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
        Watchdog watchdog = new Watchdog(
            new ManagedProcess("libreHwMonitor",             "current\\libreHwMonitor.exe"),
            new ManagedProcess("client-mqtt-service",        "current\\client-mqtt-service.exe"),
            new ManagedProcess("client-cloudflared-service", "current\\client-cloudflared-service.exe"),
            new ManagedProcess("tp-agent",                   "current\\tp-agent.exe")
        );

        while (true) {

            try {

                if (exitcode == 10) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");

                    File current = new File("current");

                    if (!current.isDirectory()) {
                        
                        exitcode = 80;

                    } else {

                        exitcode = 280;

                    }
                }

                if (exitcode == 280 || exitcode == 281) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");

                    watchdog.startAll();

                    stderr = watchdog.waitForFatal();

                    watchdog.stopAll();

                    exitcode = 296;
                }

                if (exitcode == 95 || exitcode == 96 || exitcode == 295 || exitcode == 296 || !stderr.isEmpty()) {

                    System.out.println("\n[INFO] "
                        + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")"
                        + "\n[ERROR] " + stderr + "\n");
                    stderr = "";
                    
                    exitcode = 81;
                }

                if (exitcode ==  80 || exitcode == 81) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");

                    exitcode = SimpleProcess.run("java", "launch/App.java", "update");
                }
                
                if (exitcode == 99) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");

                    System.out.println("[INFO] Waiting for staging...");

                    File state = new File("launch/app/updater/state/update_staging.json");

                    int timeout = 0;

                    while (true) {

                        timeout++;

                        if (timeout <= 30) {

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
                            break;
                        }
                        
                        Thread.sleep(1000);
                    }
                }
                
                if (exitcode == 100) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");

                    exitcode = 101;

                }

                if (exitcode == 101) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                    
                    try {

                        File[] stagingFolders = new File("staging")
                            .listFiles(
                                f -> f.isDirectory() && f.getName().startsWith("staging_")
                            );

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
                }

                if (exitcode == 102) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                    
                    try {

                        File[] stagingFolders = new File("staging")
                            .listFiles(
                                f -> f.isDirectory() && f.getName().startsWith("staging_")
                            );

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
                }

                if (exitcode == 103) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                    
                    try {
                        File[] stagingFolders = new File("staging")
                            .listFiles(
                                f -> f.isDirectory() && f.getName().startsWith("staging_")
                            );

                        if (stagingFolders == null) stagingFolders = new File[0];

                        File[] backupFolders = new File("backup")
                            .listFiles(
                                f -> f.isDirectory() && f.getName().startsWith("backup_")
                            );

                        if (backupFolders == null) backupFolders = new File[0];

                        for (File folder: stagingFolders) {
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

                        Arrays.sort(
                            backupFolders,
                            Comparator.comparing(File::getName).reversed()
                        );

                        for (int i = 2; i < backupFolders.length; i++) {
                            Files.walk(backupFolders[i].toPath())
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
                }

                if (exitcode == 104) {

                    System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");

                    exitcode = 10;
                }

                System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
                
                Thread.sleep(1000);

            } catch (Exception e) {
                System.out.println("[ERROR] App is crashed: " + e.getMessage());
                e.printStackTrace();
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