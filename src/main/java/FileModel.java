public class FileModel implements Comparable<FileModel>{
    private String name;
    private String absolutePath;
    private boolean isFile;

    public FileModel(String name, String absolutePath, boolean isFile){
        this.name = name;
        this.absolutePath = absolutePath;
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

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }


    @Override
    public int compareTo(FileModel o) {
        if((!this.isFile && !o.isFile) || (this.isFile && o.isFile)) {
            return 0;
        }

        return this.isFile ? 1 : -1;
    }
}
