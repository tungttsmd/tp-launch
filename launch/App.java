package launch;

import launch.app.helpers.SimpleProcess;

public class App {
    
    public static void main(String[] args)  {

        System.out.println("[INFO] App is running...!");
        
        if (args.length > 0 && args[0].equals("update")) {
            staging("https://github.com/tungttsmd/tp-cloudflared-client-service/archive/refs/heads/dev.zip");
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

        // cmd /k for debug Staging.java
        SimpleProcess.run("cmd", "/c", "start", "cmd", "/c", "java", "launch/app/updater/Staging.java", repo);

        System.exit(99);
    }
}