package launch.app.update;

import launch.app.config.Config;
import launch.app.helpers.SimpleProcess;
import launch.app.helpers.SimpleDownloader;
import java.io.File;
import java.io.FileWriter;

public class Staging {


    public static void main(String[] args) {
        
        // 0 -> App is initialized!
        // ...
        // 80 -> Staging is started! (downloading...)
        // ...
        // 95 -> Staging is error!
        // 96 -> Staging is crashed!
        // 99 -> Staging is completed!
        // ...
        // 796 -> Staging state file writing/reading is crashed!

        writeStatus(80, "Staging is started!");

        int exitcode = repoClone(args[0]);

        if (exitcode != 95 && exitcode != 96) {
            System.out.println("[INFO] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
        } else {
            System.out.println("[ERROR] " + exitcodeExplanation(exitcode) + " (exitcode: " + exitcode + ")");
        }

        // rename staging -> current
        // start app
    }

    private static int repoClone(String repo) {

        try {
            String stagingDir = "staging_" + timestamp();

            File staging = new File("staging/" + stagingDir);

            if (staging.isDirectory()) {

                writeStatus(95, "Staging with a current timestamp folder was existed!");
                return 95;
            }

            new File("staging").mkdirs();

            System.out.println("[INFO] Cloning update repo...");
            
            int exitcode = SimpleDownloader.downloadAndExtract(repo, "staging/" + stagingDir, "download.zip");
            
            if (exitcode != 0) {

                writeStatus(exitcode, "Staging failed to clone update repo!");
                return exitcode;

            } else {

                writeStatus(99, "Staging is completed!");
                return 99;
            }

        } catch (Exception e) {

            writeStatus(96, "Staging crashed on cloning update repo!");
            e.printStackTrace();
            return 96;
        }
    }

    private static String timestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    private static void writeStatus(int exitcode, String stderr) {

        new File("launch/app/update/state").mkdirs();

        try (FileWriter fw = new FileWriter("launch/app/update/state/update_staging.json")) {
            
            fw.write("{\"exitcode\": " + exitcode + ", \"stderr\": \"" + stderr + "\"}");

        } catch (Exception e) {

            exitcode = 796;
            e.printStackTrace();
        }
    }

    private static String exitcodeExplanation(int exitcode) {
        switch (exitcode) {
            case 0:
                return "STAGING IS INITIALIZED!";
            case 80:
                return "STAGING IS STARTED!";
            case 95:
                return "STAGING IS ERROR! (read update_staging.json for more details)";
            case 96:
                return "STAGING IS CRASHED! (read update_staging.json for more details)";
            case 99:
                return "STAGING IS COMPLETED!";
            case 796:
                return "STAGING STATE FILE WRITING/READING IS CRASHED!";
            default:
                return "STAGING IS IN AN UNKNOWN STATE.";
        }
    }
}