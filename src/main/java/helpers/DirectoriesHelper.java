package helpers;

import models.FileModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DirectoriesHelper {

    public boolean deleteDirectory(String path) {
        try {
            Path directoryToDelete = Paths.get(path);

            if (!Files.exists(directoryToDelete)) {
                return true;
            }

            Files.walk(directoryToDelete)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            return !Files.exists(directoryToDelete);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<FileModel> getCurrentDirectoryContent(String directoryPath) throws IOException {
        return Files.walk(Paths.get(directoryPath), 1)
                .filter(x -> Files.isReadable(x) && !x.toAbsolutePath().equals(Paths.get(directoryPath).toAbsolutePath()))
                .map(path -> new FileModel(path.toFile().getName(), path.toAbsolutePath().toString(), path.toFile().isFile(), false))
                .sorted()
                .collect(Collectors.toList());
    }
}
