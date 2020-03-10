public class FileModel {
    private String name;
    private boolean isFile;

    public FileModel(String name, boolean isFile){
        this.name = name;
        this.isFile = isFile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }
}
