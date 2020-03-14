import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ViewController {

    public TableView<FileModel> left_table;
    public TableView<FileModel> right_table;

    public TableColumn<FileModel, String> left_table_list;
    public TableColumn<FileModel, String> right_table_list;

    public Accordion disks_accordion;

    private Label left_header = new Label();
    private Label right_header = new Label();

    private ObservableList<FileModel> left_files;
    private ObservableList<FileModel> right_files;

    private DiskHelper diskHelper;
    private PathHelper pathHelper;

    private static Path copiedFile;

    private Stage stage;

    void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        try {
            left_files = FXCollections.observableArrayList();
            right_files = FXCollections.observableArrayList();

            diskHelper = new DiskHelper();
            pathHelper = new PathHelper();

            left_table_list.setGraphic(getHeaderLayout(left_header, Position.LEFT_TABLE));
            right_table_list.setGraphic(getHeaderLayout(right_header, Position.RIGHT_TABLE));
            getDirectoryContent("C:\\", Position.LEFT_TABLE);
            getDirectoryContent("D:\\", Position.RIGHT_TABLE);
            setCurrentPathField("C:\\", Position.LEFT_TABLE);
            setCurrentPathField("D:\\", Position.RIGHT_TABLE);
            initializeTables();
            populateDiskPanels();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private HBox getHeaderLayout(Label headerLabel, Position position) {
        Hyperlink arrow = new Hyperlink("⬆");
        arrow.setStyle("-fx-underline: false");
        arrow.setAlignment(Pos.CENTER_RIGHT);
        arrow.setOnMouseClicked(event -> goToParentDirectory(headerLabel.getText(), position));

        Region region1 = new Region();
        HBox.setHgrow(region1, Priority.ALWAYS);

        HBox box = new HBox(headerLabel, region1, arrow);
        box.setPadding(new Insets(8));

        return box;
    }

    private void populateDiskPanels() {
        for (Path root : getDisksCollection()) {
            try {
                FileStore store = Files.getFileStore(root);
                long usableSpace = store.getUsableSpace();
                long totalSpace = store.getTotalSpace();

                disks_accordion.getPanes().add(
                        new TitledPane(root.toString(),
                                new VBox(
                                        diskHelper.getDiskNameLabel(root.toFile()),
                                        new ProgressBar(diskHelper.getUsedDiskSpaceInPercents(usableSpace, totalSpace)),
                                        diskHelper.getSpaceDescLabel(usableSpace, totalSpace))));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private ArrayList<Path> getDisksCollection() {
        ArrayList<Path> result = new ArrayList<>();

        try {
            FileSystems.getDefault().getRootDirectories().forEach(result::add);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return result;
        }
    }

    private void setCurrentPathField(String text, Position position) {
        if (position == Position.RIGHT_TABLE) {
            right_header.setText(text);
            return;
        }

        left_header.setText(text);
    }

    private void initializeTables() {
        left_table_list.setCellValueFactory(new PropertyValueFactory<>("name"));
        right_table_list.setCellValueFactory(new PropertyValueFactory<>("name"));

        left_table.setItems(left_files);
        right_table.setItems(right_files);

        left_table.setRowFactory(tableView -> configureRow(Position.LEFT_TABLE));
        right_table.setRowFactory(tableView -> configureRow(Position.RIGHT_TABLE));

        left_table.setPlaceholder(new Label("Ten folder jest pusty"));
        right_table.setPlaceholder(new Label("Ten folder jest pusty"));
    }

    private TableRow<FileModel> configureRow(Position position) {
        final TableRow<FileModel> row = new TableRow<>();
        row.setOnDragDetected(event -> {
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            db.setDragView(row.snapshot(null, null));
            System.out.println(row.getItem().getName());
            event.consume();
        });

        createContextMenu(row, position);
        row.setOnMouseClicked(event -> setOnClickAction(event, row, position));

        return row;
    }

    private void createContextMenu(TableRow<FileModel> row, Position position) {
        final ContextMenu contextMenu = new ContextMenu();

        final MenuItem openFileMenuItem = createContextMenuItem("Otwórz", event -> onOpenAction(row.getItem(), position));

        final MenuItem copyFileMenuItem = createContextMenuItem("Kopiuj", event -> onCopyAction(row.getItem()));
        final MenuItem pasteFileMenuItem = createContextMenuItem("Wklej", event -> onPasteAction(position));
        final MenuItem moveFileMenuItem = createContextMenuItem("Przenieś do...", event -> onMoveAction(row.getItem(), position));
        final MenuItem newDirectoryMenuItem = createContextMenuItem("Nowy folder", event -> onNewDirectoryAction(position, row.getItem()));
        final MenuItem renameMenuItem = createContextMenuItem("Zmień nazwę", event -> onRenameAction(position, row.getItem()));
        final MenuItem removeMenuItem = createContextMenuItem("Usuń", event -> onRemoveAction(position, row.getItem()));
        contextMenu.getItems().addAll(openFileMenuItem, copyFileMenuItem, pasteFileMenuItem, moveFileMenuItem, newDirectoryMenuItem, renameMenuItem, removeMenuItem);

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
    
    //TODO Dodać drag and drop pomiędzy tabelami
    //TODO klasa z constami na teksty
    private void onRenameAction(Position position, FileModel model) {
        try {

            if(model.isDisk()){
                showAlert(Alert.AlertType.ERROR, "Niedozwolona akcja", "Nie jest możliwa zmiana nazwy dysku.");
                return;
            }

            String type = model.isFile() ? "pliku" : "katalogu";
            Optional<String> result = showInputDialog("Zmiana nazwy", "Wpisz nową nazwę " + type + ":");

            if (!result.isPresent() || !Utils.isDirectoryNameValid(result.get())) {
                showAlert(Alert.AlertType.WARNING, "Zła nazwa " + type, "Nazwa " + type + " jest błędna lub zawiera niedozwolony znak.");
                return;
            }

            Optional<String> parent = getParentAbsolutePath(model.getAbsolutePath());

            if (parent.isPresent()) {
                Path newPath = model.isFile()
                        ? pathHelper.getFileName(parent.get(), model.getAbsolutePath(), result.get())
                        : pathHelper.getDirectoryName(parent.get(), result.get());

                Files.move(Paths.get(model.getAbsolutePath()), newPath.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                setCurrentPathField(parent.get(), position);
                getDirectoryContent(parent.get(), position);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Zmiana nazwy " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodła się z powodu krytycznego błędu.");
        }
    }

    private void onCopyAction(FileModel model) {
        try {
            if(model.isDisk()){
                showAlert(Alert.AlertType.ERROR, "Niedozwolona akcja", "Nie jest możliwa skopiowanie całego dysku.");
                return;
            }

            if (Utils.isNullOrWhitespace(model.getAbsolutePath()) || !Files.exists(Paths.get(model.getAbsolutePath()))) {
                return;
            }

            copiedFile = Paths.get(model.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Skopiowanie " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private void onMoveAction(FileModel model, Position position) {
        try {
            if(model.isDisk()){
                showAlert(Alert.AlertType.ERROR, "Niedozwolona akcja", "Nie jest możliwe przeniesienie całego dysku.");
                return;
            }

            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Wybierz folder docelowy");
            directoryChooser.setInitialDirectory(Paths.get(model.getAbsolutePath()).getParent().toFile());
            File file = directoryChooser.showDialog(stage);

            if (file != null) {
                Files.move(Paths.get(model.getAbsolutePath()), Paths.get(file.getAbsolutePath(), model.getName()), StandardCopyOption.REPLACE_EXISTING);
                setCurrentPathField(file.getAbsolutePath(), position);
                getDirectoryContent(file.getAbsolutePath(), position);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Przeniesienie " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private void onPasteAction(Position position) {
        try {

            String targetPath = position == Position.LEFT_TABLE ? left_header.getText() : right_header.getText();

            if (!copiedFile.toFile().exists() || Utils.isNullOrWhitespace(targetPath)) {
                showAlert(Alert.AlertType.ERROR, "Niedozwolona akcja", "Wklejenie pliku w tym miejscu nie jest możliwe.");
                return;
            }

            if (copiedFile.toFile().isDirectory()) {
                onDirectoryPaste(targetPath, position);
                return;
            }

            Path targetFile = Files.copy(copiedFile, Paths.get(targetPath, copiedFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);

            if (Files.exists(targetFile)) {
                getDirectoryContent(targetPath, position);
            }
        } catch (Exception ex) {
            if (ex instanceof AccessDeniedException) {
                showAlert(Alert.AlertType.ERROR, "Brak uprawnień", "Brak uprawnień do umieszczenia pliku/katalogu w wybranym katalogu");
                return;
            }

            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Wklejenie nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private void onDirectoryPaste(String targetPath, Position position) throws IOException {
        Path targetDirectory = Paths.get(targetPath, copiedFile.getFileName().toString());

        if (!Files.exists(targetDirectory)) {
            Files.createDirectory(targetDirectory);
        }

        Files.walk(copiedFile)
                .forEach(source -> {
                    try {
                        Path target = targetDirectory.resolve(copiedFile.relativize(source));
                        if (!Files.exists(target)) {
                            Files.copy(source, target);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Wklejenie nie powiodło się z powodu krytycznego błędu.");
                    }
                });

        if (Files.exists(targetDirectory)) {
            getDirectoryContent(targetPath, position);
        }
    }

    private void onNewDirectoryAction(Position position, FileModel model) {
        try {
            if (model.isDisk()) {
                showAlert(Alert.AlertType.ERROR, "Niedozwolona akcja", "Utworzenie nowego katalogu w tym miejscu jest niedozwolone.");
                return;
            }

            Optional<String> result = showInputDialog("Tworzenie katalogu", "Wpisz nazwę katalogu:");

            if (!result.isPresent() || !Utils.isDirectoryNameValid(result.get())) {
                showAlert(Alert.AlertType.WARNING, "Zła nazwa katalogu", "Nazwa katalogu jest błędna lub zawiera niedozwolony znak.");
                return;
            }

            Optional<String> parent = getParentAbsolutePath(model.getAbsolutePath());

            if (!parent.isPresent()) {
                return;
            }

            Path resultPath = Files.createDirectory(Paths.get(parent.get(), result.get()));

            if (!Files.exists(resultPath)) {
                showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Utworzenie nowego katalogu nie powiodło się.");
                return;
            }

            getDirectoryContent(resultPath.getParent().toString(), position);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Utworzenie nowego katalogu nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private Optional<String> getParentAbsolutePath(String childPath) {
        Path parent = Paths.get(childPath).getParent();

        return parent != null ? Optional.of(parent.toString()) : Optional.empty();
    }

    private void onRemoveAction(Position position, FileModel model) {
        try {
            if (model.isDisk()) {
                showAlert(Alert.AlertType.ERROR, "Niedozwolona akcja", "Usunięcie partycji jest niedozwolone.");
                return;
            }

            if (!getFeedbackFromConfirmationAlert("Usunięcie pliku", "Czy na pewno chcesz usunąć " + model.getName() + "?")) {
                return;
            }

            if (position == Position.LEFT_TABLE) {
                if (removeSelectedElement(model)) {
                    left_files.remove(model);
                }
                return;
            }

            if (removeSelectedElement(model)) {
                right_files.remove(model);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Usunięcie " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private Optional<String> showInputDialog(String header, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(header);
        dialog.setHeaderText(null);
        dialog.setContentText(message);

        return dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(header);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private boolean getFeedbackFromConfirmationAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(header);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void onOpenAction(FileModel rowData, Position position) {
        try {
            if (rowData.isFile()) {
                openFileWithCmd(rowData.getAbsolutePath());
                return;
            }

            getDirectoryContent(rowData.getAbsolutePath(), position);
            setCurrentPathField(rowData.getAbsolutePath(), position);
        } catch (Exception ex) {
            if (ex instanceof AccessDeniedException) {
                showAlert(Alert.AlertType.ERROR, "Brak uprawnień", "Otwarcie " + (rowData.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu braku wymaganych uprawnień.");
                return;
            }

            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Wystąpił krytyczny błąd", "Otwarcie " + (rowData.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private boolean removeSelectedElement(FileModel model) {
        if (model.isFile()) {
            return deleteFile(model.getAbsolutePath());
        }

        return deleteDirectory(model.getAbsolutePath());
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

    private void openFileWithCmd(String filePath) throws IOException {
        Runtime.getRuntime().exec("cmd /C \"\"" + filePath + "\"\"");
    }

    private void getDirectoryContent(String directoryPath, Position position) throws IOException {
        if (position == Position.LEFT_TABLE) {
            left_files.setAll(getCurrentDirectoryContent(directoryPath));
            return;
        }

        right_files.setAll(getCurrentDirectoryContent(directoryPath));
    }

    private void goToParentDirectory(String currentPath, Position position) {
        try {
            Path parentPath = Paths.get(currentPath).getParent();
            if (parentPath == null || Utils.isNullOrWhitespace(parentPath.toString())) {
                showDiskList(position);
                return;
            }

            String parentAbsolutePath = parentPath.toString();

            setCurrentPathField(parentAbsolutePath, position);
            getDirectoryContent(parentAbsolutePath, position);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showDiskList(Position position) {
        List<FileModel> disks = getDisksCollection()
                .stream()
                .map(disk -> new FileModel(disk.toString(), disk.toString(), false, true))
                .collect(Collectors.toList());

        if (position == Position.LEFT_TABLE) {
            left_header.setText("");
            left_files.setAll(disks);
            return;
        }

        right_header.setText("");
        right_files.setAll(disks);
    }

    private List<FileModel> getCurrentDirectoryContent(String directoryPath) throws IOException {
        return Files.walk(Paths.get(directoryPath), 1)
                .filter(x -> Files.isReadable(x) && !x.toAbsolutePath().equals(Paths.get(directoryPath).toAbsolutePath()))
                .map(path -> new FileModel(path.toFile().getName(), path.toAbsolutePath().toString(), path.toFile().isFile(), false))
                .sorted()
                .collect(Collectors.toList());
    }

    private MenuItem createContextMenuItem(String text, EventHandler<ActionEvent> value) {
        final MenuItem item = new MenuItem(text);
        item.setOnAction(value);

        return item;
    }

    private boolean deleteFile(String path) {
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

    private boolean deleteDirectory(String path) {
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
}
