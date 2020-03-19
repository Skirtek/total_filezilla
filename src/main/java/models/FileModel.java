package models;

public class FileModel implements Comparable<FileModel>{
    private String name;
    private String absolutePath;
    private boolean isFile;
    private boolean isDisk;

    public FileModel(String name, String absolutePath, boolean isFile, boolean isDisk){
        this.name = name;
        this.absolutePath = absolutePath;
        this.isFile = isFile;
        this.isDisk = isDisk;
    }

    public String getName() {
        return name;
    }

    public boolean isFile() {
        return isFile;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public boolean isDisk() {
        return isDisk;
    }

    @Override
    public int compareTo(FileModel o) {
        if((!this.isFile && !o.isFile) || (this.isFile && o.isFile)) {
            return 0;
        }

        return this.isFile ? 1 : -1;
    }
}
