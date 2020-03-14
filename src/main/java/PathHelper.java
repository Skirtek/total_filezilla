import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class PathHelper {
    public Path getFileName(String parent, String currentPath, String newFileName){
        Optional<String> extension = getFileExtension(currentPath);
        return extension.map(s -> Paths.get(parent.concat(newFileName).concat("."+s))).orElseGet(() -> Paths.get(parent.concat(newFileName)));
    }

    public Path getDirectoryName(String parent, String newDirectoryName){
        return Paths.get(parent, newDirectoryName);
    }

    private Optional<String> getFileExtension(String path){
        int i = path.lastIndexOf('.');
        if (i > 0) {
            return Optional.of(path.substring(i+1));
        }

        return Optional.empty();
    }
}
