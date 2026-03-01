package launch;

import launch.app.config.Config;
import launch.app.helpers.SimpleBuilder;
import launch.app.helpers.SimpleProcess;

public class App {
    
    public static void main(String[] args)  {

        String repo = Config.get("update.repo.url", "");

        if (repo.isEmpty()) {
            System.out.println("[ERROR] Update repo url cannot be empty (please check update.repo.url)!");
            return;
        }

        System.out.println("[INFO] App is running...!");
        
        if (args.length > 0 && args[0].equals("update")) {
            staging(repo);
        }

        if (args.length <= 0 || args[0].equals("help")) {

            System.out.println(
                "update app: java launch/App.java update\n"
                + "app helps: java launch/App.java help\n"
            );

            try {

                Thread.sleep(1000);

            } catch (Exception e) {

                System.out.println("[ERROR] Crashed on running app: " + e.getMessage());
                e.printStackTrace();

            }
        }
    }

    public static void staging(String repo) {

        try {
                
            Thread.sleep(1000);

            try {

                    stagingDownload(repo);
                    
                } catch (Exception e) {

                    System.out.println("[ERROR] Crashed on staging app: " + e.getMessage());
                    e.printStackTrace();
                    return;

                }

        } catch (InterruptedException e) {

            System.out.println("[ERROR] Crashed on staging app");
            e.printStackTrace();
            return;
        }

    }

    public static void stagingDownload(String repo) throws Exception {

        boolean keepConsole = Config.getBoolean("staging.console.debug", false); // cmd /k for debug Staging.java

        SimpleBuilder.runInNewWindow("launch/app/update/Staging.java", "launch.app.update.Staging", keepConsole, repo);

        System.exit(99);
    }
}