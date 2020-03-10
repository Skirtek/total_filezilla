import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.text.DecimalFormat;

public class ViewController {

    public TableView<FileModel> left_table;
    public TableView<FileModel> right_table;

    public TableColumn<FileModel, String> left_table_list;
    public TableColumn<FileModel, String> right_table_list;
    public Accordion disks_accordion;

    private ObservableList<FileModel> left_files;
    private ObservableList<FileModel> right_files;

    private DecimalFormat gigabytesTextFormat = new DecimalFormat("#.##");

    @FXML
    public void initialize() {
        try {
            left_files = FXCollections.observableArrayList();
            right_files = FXCollections.observableArrayList();

            getDirectoryContent("C:\\", left_files);
            getDirectoryContent("D:\\", right_files);
            initializeTables();
            populateDiskPanels();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void populateDiskPanels(){
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            try {
                FileStore store = Files.getFileStore(root);
                long usableSpace = store.getUsableSpace();
                long totalSpace = store.getTotalSpace();

                disks_accordion.getPanes().add(
                        new TitledPane(root.toString(),
                                new VBox(
                                        getDiskNameLabel(root.toFile()),
                                        new ProgressBar(getUsedDiskSpaceInPercents(usableSpace, totalSpace)),
                                        getSpaceDescLabel(usableSpace, totalSpace))));
            } catch (IOException e) {
                System.out.println("error querying space: " + e.toString());
            }
        }
    }

    private Label getDiskNameLabel(File disk) {
        String diskName = FileSystemView.getFileSystemView().getSystemDisplayName(disk);
        return new Label(diskName);
    }

    private Label getSpaceDescLabel(long freeSpace, long totalSpace) {
        return new Label(formatSpaceToGigabytes(freeSpace) + " GB wolnych z " + formatSpaceToGigabytes(totalSpace) + " GB");
    }

    private String formatSpaceToGigabytes(long bytesCount) {
        return gigabytesTextFormat.format(bytesCount / (Math.pow(1024, 3)));
    }

    private double getUsedDiskSpaceInPercents(long freeSpace, long totalSpace) {
        return ((totalSpace - freeSpace) / (double) totalSpace);
    }

    private void initializeTables() {
        left_table_list.setCellValueFactory(new PropertyValueFactory<>("name"));
        right_table_list.setCellValueFactory(new PropertyValueFactory<>("name"));

        left_table.setItems(left_files);
        right_table.setItems(right_files);

        left_table.setRowFactory(tableView -> configureRow(Position.LEFT_TABLE));
        right_table.setRowFactory(tableView -> configureRow(Position.RIGHT_TABLE));
    }

    private TableRow<FileModel> configureRow(Position position) {
        final TableRow<FileModel> row = new TableRow<>();

        createContextMenu(row, position);
        row.setOnMouseClicked(event -> setOnClickAction(event, row, position));

        return row;
    }

    private void createContextMenu(TableRow<FileModel> row, Position position) {
        final ContextMenu contextMenu = new ContextMenu();
        TableView<FileModel> table = position == Position.LEFT_TABLE ? left_table : right_table;

        final MenuItem openFileMenu = createContextMenuItem("Otwórz", event -> onOpenAction(row.getItem(), position));
        final MenuItem removeMenuItem = createContextMenuItem("Usuń", event -> table.getItems().remove(row.getItem()));

        contextMenu.getItems().addAll(openFileMenu, removeMenuItem);
        row.contextMenuProperty().bind(
                Bindings.when(row.emptyProperty())
                        .then((ContextMenu) null)
                        .otherwise(contextMenu)
        );
    }

    private void setOnClickAction(MouseEvent event, TableRow<FileModel> row, Position position) {
        if (event.getClickCount() != 2 || row.isEmpty()) {
            return;
        }

        onOpenAction(row.getItem(), position);
    }

    private void onOpenAction(FileModel rowData, Position position) {
        if (rowData.isFile()) {
            openFileWithCmd(rowData.getName());
            return;
        }

        getDirectoryContent(rowData.getName(), position == Position.LEFT_TABLE ? left_files : right_files);
    }

    //TODO Fix and use this
    private void openFile() throws IOException {
        String inputFile = "path/youtfile.ext";
        Path tempOutput = Files.createTempFile("TempManual", ".ext");
        tempOutput.toFile().deleteOnExit();
        System.out.println("tempOutput: " + tempOutput);
        try (InputStream is = ViewController.class.getClassLoader().getResourceAsStream(inputFile)) {
            Files.copy(is, tempOutput, StandardCopyOption.REPLACE_EXISTING);
        }
        Desktop.getDesktop().open(tempOutput.toFile());
    }

    private void openFileWithCmd(String fileName) {
        try {
            Runtime.getRuntime().exec("cmd /C \"\"" + fileName + "\"\"");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getDirectoryContent(String path, ObservableList<FileModel> collection) {
        try {
            collection.clear();
            Files.walk(Paths.get(path), 1).forEach(x -> {
                collection.add(new FileModel(x.toAbsolutePath().toString(), x.toFile().isFile()));
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private MenuItem createContextMenuItem(String text, EventHandler<ActionEvent> value) {
        final MenuItem item = new MenuItem(text);
        item.setOnAction(value);

        return item;
    }
}
