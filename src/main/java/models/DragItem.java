package models;

public class DragItem {
    private Position position;
    private FileModel item;

    public DragItem(Position position, FileModel item) {
        this.position = position;
        this.item = item;
    }

    public Position getPosition() {
        return position;
    }

    public FileModel getItem() {
        return item;
    }
}
