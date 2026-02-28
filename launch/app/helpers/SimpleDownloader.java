package launch.app.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SimpleDownloader {

    // Tải zip từ url, giải nén vào destFolder, xoá zip
    public static int downloadAndExtract(String url, String destFolder, String zipName) throws Exception {

        // 0 -> Download is completed!
        // ...
        // 96 -> Download is failed!
        
        File zipFile = new File(destFolder + "/" + zipName);

        int download = download(url, zipFile);
        int extract = extract(zipFile, destFolder);

        zipFile.delete();

        if (download != 0 || extract != 0) {
            return 96;
        }

        return 0;

    }

    private static int download(String url, File dest) throws Exception {

        System.out.println("[INFO] Downloading " + url + "...");

        new File(dest.getParent()).mkdirs();

        var source = URI.create(url).toURL();

        try (InputStream in = source.openStream();
            
            FileOutputStream out = new FileOutputStream(dest)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

        } catch (Exception e) {

            e.printStackTrace();
            return 96;
        }

        System.out.println("[INFO] Download complete.");

        return 0;
    
    }

    private static int extract(File zipFile, String destFolder) throws Exception {

        System.out.println("[INFO] Extracting...");

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {

            String topLevel = null;
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                String name = entry.getName();

                // Detect top-level folder from first entry (GitHub zips wrap everything in one)
                if (topLevel == null) {
                    int slash = name.indexOf('/');
                    topLevel = (slash != -1) ? name.substring(0, slash + 1) : "";
                }

                // Strip the top-level folder prefix
                if (!topLevel.isEmpty() && name.startsWith(topLevel)) {
                    name = name.substring(topLevel.length());
                }

                if (name.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                File outFile = new File(destFolder + "/" + name);

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }

                zis.closeEntry();
            }
        } catch (Exception e) {

            e.printStackTrace();
            return 96;
        }

        System.out.println("[INFO] Extraction complete.");

        return 0;

    }
}
