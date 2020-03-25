package helpers;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesHelper {

    public void openFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        File fileToOpen = path.toFile();

        if(!Files.exists(path) || !Desktop.isDesktopSupported()){
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        desktop.open(fileToOpen);
    }

    public boolean deleteFile(String path) {
        try {
            Path fileToDelete = Paths.get(path);

            Files.deleteIfExists(fileToDelete);

            return !Files.exists(fileToDelete);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
