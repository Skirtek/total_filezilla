package helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesHelper {

    public void openFileWithCmd(String filePath) throws IOException {
        Runtime.getRuntime().exec("cmd /C \"\"" + filePath + "\"\"");
    }

    public boolean deleteFile(String path) {
        try {
            Path fileToDelete = Paths.get(path);

            if (!Files.exists(fileToDelete)) {
                return true;
            }

            Files.delete(fileToDelete);

            return !Files.exists(fileToDelete);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
