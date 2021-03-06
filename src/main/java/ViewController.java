import com.google.gson.Gson;
import helpers.*;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import models.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
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
    private DialogsHelper dialogsHelper;
    private DirectoriesHelper directoriesHelper;
    private FilesHelper filesHelper;
    private Gson gsonInstance;

    private static Path copiedFile;

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

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
            dialogsHelper = new DialogsHelper();
            directoriesHelper = new DirectoriesHelper();
            filesHelper = new FilesHelper();
            gsonInstance = new Gson();

            left_table_list.setGraphic(getHeaderLayout(left_header, Position.LEFT_TABLE));
            right_table_list.setGraphic(getHeaderLayout(right_header, Position.RIGHT_TABLE));
            initializeFilesViews();
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

        left_table.setPlaceholder(new Label(GuiResources.EmptyDirectory));
        right_table.setPlaceholder(new Label(GuiResources.EmptyDirectory));
    }

    private TableRow<FileModel> configureRow(Position position) {
        final TableRow<FileModel> row = new TableRow<>();

        addDragDropEvents(position, row);
        createContextMenu(row, position);
        row.setOnMouseClicked(event -> setOnClickAction(event, row, position));

        return row;
    }

    private void addDragDropEvents(Position position, TableRow<FileModel> row) {
        final String targetPath = position == Position.LEFT_TABLE ? left_header.getText() : right_header.getText();

        row.setOnDragDetected(event -> {
            if (!row.isEmpty()) {
                FileModel model = row.getItem();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                db.setDragView(row.snapshot(null, null));
                ClipboardContent cc = new ClipboardContent();
                cc.put(SERIALIZED_MIME_TYPE, gsonInstance.toJson(new DragItem(position, model)));
                db.setContent(cc);
                event.consume();
            }
        });

        row.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE) && !gsonInstance.toJson(row.getItem()).equals(db.getContent(SERIALIZED_MIME_TYPE))) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });

        row.setOnDragDropped(event -> {
            try {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE) && db.getContent(SERIALIZED_MIME_TYPE) instanceof String) {
                    DragItem draggedItem = gsonInstance.fromJson(db.getContent(SERIALIZED_MIME_TYPE).toString(), DragItem.class);

                    if (draggedItem.getPosition() != null && draggedItem.getPosition() != position) {
                        Files.move(Paths.get(draggedItem.getItem().getAbsolutePath()), Paths.get(targetPath, draggedItem.getItem().getName()), StandardCopyOption.REPLACE_EXISTING);
                        getDirectoryContent(left_header.getText(), Position.LEFT_TABLE);
                        getDirectoryContent(right_header.getText(), Position.RIGHT_TABLE);
                    } else {
                        dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.ProhibitedAction, "Nie jest możliwe przeniesienie w obrębie tego samego katalogu.");
                    }

                    event.setDropCompleted(true);
                    event.consume();
                }
            } catch (Exception ex) {
                if (ex instanceof AccessDeniedException) {
                    dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.InsufficientPermissions, "Brak uprawnień do umieszczenia pliku/katalogu w wybranym katalogu");
                    return;
                }
                ex.printStackTrace();
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Operacja nie powiodła się z powodu krytycznego błędu.");
            }
        });
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

    private void onRenameAction(Position position, FileModel model) {
        try {

            if (model.isDisk()) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.ProhibitedAction, "Nie jest możliwa zmiana nazwy dysku.");
                return;
            }

            String type = model.isFile() ? "pliku" : "katalogu";
            Optional<String> result = dialogsHelper.showInputDialog("Zmiana nazwy", "Wpisz nową nazwę " + type + ":");

            if (!result.isPresent() || !Utils.isDirectoryNameValid(result.get())) {
                dialogsHelper.showAlert(Alert.AlertType.WARNING, "Zła nazwa " + type, "Nazwa " + type + " jest błędna lub zawiera niedozwolony znak.");
                return;
            }

            Optional<String> parent = pathHelper.getParentAbsolutePath(model.getAbsolutePath());

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
            dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Zmiana nazwy " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodła się z powodu krytycznego błędu.");
        }
    }

    private void onCopyAction(FileModel model) {
        try {
            if (model.isDisk()) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.ProhibitedAction, "Nie jest możliwa skopiowanie całego dysku.");
                return;
            }

            if (Utils.isNullOrWhitespace(model.getAbsolutePath()) || !Files.exists(Paths.get(model.getAbsolutePath()))) {
                return;
            }

            copiedFile = Paths.get(model.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Skopiowanie " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private void onMoveAction(FileModel model, Position position) {
        try {
            if (model.isDisk()) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.ProhibitedAction, "Nie jest możliwe przeniesienie całego dysku.");
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
            dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Przeniesienie " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private void onPasteAction(Position position) {
        try {

            String targetPath = position == Position.LEFT_TABLE ? left_header.getText() : right_header.getText();

            if (!Files.exists(copiedFile) || Utils.isNullOrWhitespace(targetPath)) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.ProhibitedAction, "Wklejenie pliku w tym miejscu nie jest możliwe.");
                return;
            }

            if (Files.isDirectory(copiedFile)) {
                onDirectoryPaste(targetPath, position);
                return;
            }

            Path targetFile = Files.copy(copiedFile, Paths.get(targetPath, copiedFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);

            if (Files.exists(targetFile)) {
                getDirectoryContent(targetPath, position);
            }
        } catch (Exception ex) {
            if (ex instanceof AccessDeniedException) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.InsufficientPermissions, "Brak uprawnień do umieszczenia pliku/katalogu w wybranym katalogu");
                return;
            }

            ex.printStackTrace();
            dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Wklejenie nie powiodło się z powodu krytycznego błędu.");
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
                        dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Wklejenie nie powiodło się z powodu krytycznego błędu.");
                    }
                });

        if (Files.exists(targetDirectory)) {
            getDirectoryContent(targetPath, position);
        }
    }

    private void onNewDirectoryAction(Position position, FileModel model) {
        try {
            if (model.isDisk()) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.ProhibitedAction, "Utworzenie nowego katalogu w tym miejscu jest niedozwolone.");
                return;
            }

            Optional<String> result = dialogsHelper.showInputDialog("Tworzenie katalogu", "Wpisz nazwę katalogu:");

            if (!result.isPresent() || !Utils.isDirectoryNameValid(result.get())) {
                dialogsHelper.showAlert(Alert.AlertType.WARNING, "Zła nazwa katalogu", "Nazwa katalogu jest błędna lub zawiera niedozwolony znak.");
                return;
            }

            Optional<String> parent = pathHelper.getParentAbsolutePath(model.getAbsolutePath());

            if (!parent.isPresent()) {
                return;
            }

            Path resultPath = Files.createDirectory(Paths.get(parent.get(), result.get()));

            if (!Files.exists(resultPath)) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Utworzenie nowego katalogu nie powiodło się.");
                return;
            }

            getDirectoryContent(resultPath.getParent().toString(), position);
        } catch (Exception ex) {
            ex.printStackTrace();
            dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Utworzenie nowego katalogu nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private void onRemoveAction(Position position, FileModel model) {
        try {
            if (model.isDisk()) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.ProhibitedAction, "Usunięcie partycji jest niedozwolone.");
                return;
            }

            if (!dialogsHelper.getFeedbackFromConfirmationAlert("Usunięcie pliku", "Czy na pewno chcesz usunąć " + model.getName() + "?")) {
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
            dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Usunięcie " + (model.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private void onOpenAction(FileModel rowData, Position position) {
        try {
            if (rowData.isFile()) {
                filesHelper.openFile(rowData.getAbsolutePath());
                return;
            }

            getDirectoryContent(rowData.getAbsolutePath(), position);
            setCurrentPathField(rowData.getAbsolutePath(), position);
        } catch (Exception ex) {
            if (ex instanceof AccessDeniedException) {
                dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.InsufficientPermissions, "Otwarcie " + (rowData.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu braku wymaganych uprawnień.");
                return;
            }

            ex.printStackTrace();
            dialogsHelper.showAlert(Alert.AlertType.ERROR, GuiResources.CriticalErrorAlertTitle, "Otwarcie " + (rowData.isFile() ? "pliku" : "katalogu") + "nie powiodło się z powodu krytycznego błędu.");
        }
    }

    private boolean removeSelectedElement(FileModel model) {
        if (model.isFile()) {
            return filesHelper.deleteFile(model.getAbsolutePath());
        }

        return directoriesHelper.deleteDirectory(model.getAbsolutePath());
    }

    private void getDirectoryContent(String directoryPath, Position position) throws IOException {
        if (position == Position.LEFT_TABLE) {
            left_files.setAll(directoriesHelper.getCurrentDirectoryContent(directoryPath));
            return;
        }

        right_files.setAll(directoriesHelper.getCurrentDirectoryContent(directoryPath));
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
        List<FileModel> disks = getDisksList();

        if (position == Position.LEFT_TABLE) {
            left_header.setText("");
            left_files.setAll(disks);
            return;
        }

        right_header.setText("");
        right_files.setAll(disks);
    }

    private void initializeFilesViews() throws IOException {
        List<FileModel> disks = getDisksList();

        if(disks.isEmpty()){
            dialogsHelper.showAlert(Alert.AlertType.WARNING, "Wystąpił błąd", "Nie odnaleziono żadnych dysków.");
            return;
        }

        getDirectoryContent(disks.get(0).getAbsolutePath(), Position.LEFT_TABLE);
        setCurrentPathField(disks.get(0).getAbsolutePath(), Position.LEFT_TABLE);

        if(disks.size() > 1){
            getDirectoryContent(disks.get(1).getAbsolutePath(), Position.RIGHT_TABLE);
            setCurrentPathField(disks.get(1).getAbsolutePath(), Position.RIGHT_TABLE);
        }
        else {
            getDirectoryContent(disks.get(0).getAbsolutePath(), Position.LEFT_TABLE);
            setCurrentPathField(disks.get(0).getAbsolutePath(), Position.LEFT_TABLE);
        }
    }

    private List<FileModel> getDisksList(){
        return  getDisksCollection()
                .stream()
                .map(disk -> new FileModel(disk.toString(), disk.toString(), false, true))
                .collect(Collectors.toList());
    }

    private MenuItem createContextMenuItem(String text, EventHandler<ActionEvent> value) {
        final MenuItem item = new MenuItem(text);
        item.setOnAction(value);

        return item;
    }
}
