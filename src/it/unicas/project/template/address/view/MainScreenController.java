package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.MainApp;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;

public class MainScreenController {

    @FXML
    private VBox sideMenu;
    @FXML
    private VBox categoryMenuContainer;
    @FXML
    private TextField newTaskField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<Categorie> categoryComboBox;
    @FXML
    private ComboBox<String> priorityComboBox;
    @FXML
    private DatePicker dueDateField;
    @FXML
    private Label usernameLabelHeader;
    @FXML
    private ListView<Tasks> taskListView;

    private MainApp mainApp;
    private boolean isOpen = false;

    private ObservableList<Tasks> tasks;
    private FilteredList<Tasks> filteredTasks;
    private SortedList<Tasks> sortedTasks;

    private Categorie filterSelectedCategory = null;
    private Boolean filterCompletionStatus = null;


    public MainScreenController() {}

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        Utenti currentUser = MainApp.getCurrentUser();
        if (currentUser != null && currentUser.getNome() != null) {
            usernameLabelHeader.setText(currentUser.getNome());
        }
        refreshTasks();
        refreshCategories();
    }

    @FXML
    private void initialize() {
        Utenti currentUser = MainApp.getCurrentUser();
        if (currentUser != null && currentUser.getNome() != null) {
            usernameLabelHeader.setText(currentUser.getNome());
        }

        tasks = FXCollections.observableArrayList();
        filteredTasks = new FilteredList<>(tasks, t -> true);
        sortedTasks = new SortedList<>(filteredTasks);

        sortedTasks.setComparator((t1, t2) -> {
            boolean c1 = t1.getCompletamento();
            boolean c2 = t2.getCompletamento();
            if (c1 == c2) return 0;
            return c1 ? 1 : -1;
        });

        taskListView.setItems(sortedTasks);

        setupComboBoxes();
        setupCellFactory();
    }


    private void refreshTasks() {
        try {
            Tasks filtro = new Tasks();
            filtro.setIdUtente(MainApp.getCurrentUser().getIdUtente());
            List<Tasks> list = DAOTasks.getInstance().select(filtro);
            tasks.setAll(list);
            applyFilters();
        } catch (DAOException e) {
            showAlert(Alert.AlertType.ERROR, "Impossibile caricare i task: " + e.getMessage());
        }
    }

    @FXML
    private void handleNewTask() {
        String titolo = newTaskField.getText().trim();
        String descrizione = descriptionArea.getText().trim();
        String priorita = priorityComboBox.getValue();
        LocalDate scadenza = dueDateField.getValue();
        Categorie categoriaSelezionata = categoryComboBox.getValue();
        String categoriaInput = categoriaSelezionata != null ? categoriaSelezionata.getNomeCategoria() : null;

        if (titolo.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Il titolo non può essere vuoto."); return; }
        if (priorita == null || priorita.isBlank()) { showAlert(Alert.AlertType.WARNING, "Seleziona una priorità valida."); return; }
        if (scadenza == null) { showAlert(Alert.AlertType.WARNING, "Seleziona una data di scadenza valida."); return; }

        try {
            Integer idCategoriaFinale = null;

            if (categoriaInput != null && !categoriaInput.isBlank()) {
                for (Categorie c : categoryComboBox.getItems()) {
                    if (c.getNomeCategoria().equalsIgnoreCase(categoriaInput.trim())) {
                        idCategoriaFinale = c.getIdCategoria();
                        break;
                    }
                }
                if (idCategoriaFinale == null) {
                    Categorie nuovaCat = new Categorie(categoriaInput.trim(), null);
                    DAOCategorie.getInstance().insert(nuovaCat);
                    refreshCategories();
                    for (Categorie c : categoryComboBox.getItems()) {
                        if (c.getNomeCategoria().equalsIgnoreCase(categoriaInput.trim())) {
                            idCategoriaFinale = c.getIdCategoria();
                            break;
                        }
                    }
                }
            }

            Tasks t = new Tasks();
            t.setTitolo(titolo);
            t.setDescrizione(descrizione);
            t.setPriorita(priorita);
            t.setScadenza(scadenza.toString());
            t.setIdUtente(MainApp.getCurrentUser().getIdUtente());
            t.setIdCategoria(idCategoriaFinale);
            t.setCompletamento(false);

            DAOTasks.getInstance().insert(t);
            tasks.add(0, t);
            Platform.runLater(() -> taskListView.scrollTo(0));

            newTaskField.clear();
            descriptionArea.clear();
            dueDateField.setValue(null);
            categoryComboBox.getEditor().clear();
            priorityComboBox.getSelectionModel().clearSelection();

        } catch (DAOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore creazione task: " + e.getMessage());
        }
    }

    private void handleEditTask(Tasks task) {
        boolean ok = mainApp.showTasksEditDialog(task);
        if (ok) {
            try {
                DAOTasks.getInstance().update(task);
                int index = tasks.indexOf(task);
                if (index >= 0) tasks.set(index, task);
            } catch (DAOException e) { e.printStackTrace(); }
        }
    }

    private void handleDeleteTask(Tasks task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare questa task?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            try { DAOTasks.getInstance().delete(task); tasks.remove(task); }
            catch (DAOException e) { showAlert(Alert.AlertType.ERROR, "Errore eliminazione task: " + e.getMessage()); }
        }
    }

    private void handleTaskStatusChange(Tasks task) {
        try { DAOTasks.getInstance().update(task); }
        catch (DAOException e) { showAlert(Alert.AlertType.ERROR, "Errore aggiornamento stato: " + e.getMessage()); }
    }


    private void setupComboBoxes() {
        priorityComboBox.getItems().setAll("BASSA", "MEDIA", "ALTA");
        refreshCategories();

        categoryComboBox.setEditable(true);
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categorie c) { return c == null ? "" : c.getNomeCategoria(); }

            @Override
            public Categorie fromString(String string) {
                if (string == null || string.isBlank()) return null;
                for (Categorie c : categoryComboBox.getItems()) {
                    if (c.getNomeCategoria().equalsIgnoreCase(string.trim())) return c;
                }
                return new Categorie(string.trim(), null);
            }
        });
    }

    private void refreshCategories() {
        try {
            List<Categorie> listaCategorie = DAOCategorie.getInstance().select(null);
            categoryComboBox.setItems(FXCollections.observableArrayList(listaCategorie));
            loadCategoriaContainer();
        } catch (DAOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore caricamento categorie: " + e.getMessage());
        }
    }

    private void eliminaCategoria(Categorie c) {
        if (tasks.stream().anyMatch(t -> t.getIdCategoria() != null && t.getIdCategoria().equals(c.getIdCategoria()))) {
            showAlert(Alert.AlertType.WARNING, "Impossibile eliminare: ci sono task collegati.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminare categoria '" + c.getNomeCategoria() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            try {
                DAOCategorie.getInstance().delete(c);
                refreshCategories();
                if (filterSelectedCategory != null && filterSelectedCategory.getIdCategoria().equals(c.getIdCategoria())) {
                    filterSelectedCategory = null;
                    applyFilters();
                }
            } catch (DAOException ex) {
                showAlert(Alert.AlertType.ERROR, "Errore eliminazione categoria: " + ex.getMessage());
            }
        }
    }

    private void loadCategoriaContainer() {
        if (categoryMenuContainer == null) return;
        categoryMenuContainer.getChildren().clear();

        Hyperlink allLink = new Hyperlink("Tutte le Categorie");
        allLink.setOnAction(e -> handleShowAll());
        categoryMenuContainer.getChildren().add(allLink);

        for (Categorie c : categoryComboBox.getItems()) {
            if (c.getIdCategoria() != null && c.getIdCategoria() > 0) {
                HBox h = new HBox(5);
                h.setAlignment(Pos.CENTER_LEFT);

                Hyperlink catLink = new Hyperlink(c.getNomeCategoria());
                catLink.setOnAction(e -> {
                    filterSelectedCategory = c;
                    filterCompletionStatus = null;
                    applyFilters();
                });
                HBox.setHgrow(catLink, javafx.scene.layout.Priority.ALWAYS);

                Button btnX = new Button("x");
                btnX.setStyle("-fx-font-size: 10px; -fx-padding: 2 5; -fx-background-color: transparent; -fx-text-fill: red;");
                btnX.setOnAction(e -> eliminaCategoria(c));

                h.getChildren().addAll(catLink, btnX);
                categoryMenuContainer.getChildren().add(h);
            }
        }
    }

    /*** FILTERS ***/
    private void applyFilters() {
        filteredTasks.setPredicate(task -> {
            boolean categoryMatches = true;
            if (filterSelectedCategory != null && filterSelectedCategory.getIdCategoria() != null) {
                categoryMatches = task.getIdCategoria() != null &&
                        task.getIdCategoria().equals(filterSelectedCategory.getIdCategoria());
            }

            boolean statusMatches = true;
            if (filterCompletionStatus != null) {
                statusMatches = task.getCompletamento() == filterCompletionStatus;
            }

            return categoryMatches && statusMatches;
        });

        Platform.runLater(() -> {
            taskListView.scrollTo(0);
            taskListView.getSelectionModel().clearSelection();
        });
    }

    @FXML
    private void handleFilterToDo() { filterCompletionStatus = false; applyFilters(); }
    @FXML
    private void handleFilterCompleted() { filterCompletionStatus = true; applyFilters(); }
    @FXML
    private void handleShowAll() { filterSelectedCategory = null; filterCompletionStatus = null; applyFilters(); }


    private void setupCellFactory() {
        taskListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Tasks task, boolean empty) {
                super.updateItem(task, empty);

                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                    setOpacity(1.0);
                    setStyle("");
                    return;
                }

                CheckBox completeBox = new CheckBox();
                completeBox.setSelected(task.getCompletamento());
                completeBox.setOnAction(e -> {
                    boolean nuovoStato = completeBox.isSelected();
                    task.setCompletamento(nuovoStato);

                    if (filterCompletionStatus != null) applyFilters();
                    else {
                        int index = tasks.indexOf(task);
                        if (index >= 0) tasks.set(index, task);
                    }

                    new Thread(() -> handleTaskStatusChange(task)).start();
                });

                Label priorityBadge = new Label(task.getPriorita() != null ? task.getPriorita().trim() : "");
                String colore = switch(priorityBadge.getText().toUpperCase()) {
                    case "ALTA" -> "#e74c3c";
                    case "MEDIA" -> "#f39c12";
                    case "BASSA" -> "#27ae60";
                    default -> "grey";
                };
                priorityBadge.setStyle("-fx-text-fill:white;-fx-background-color:" + colore + ";-fx-padding:3 7 3 7;-fx-background-radius:5;");

                String nomeCat = getNomeCategoriaDaId(task.getIdCategoria());
                Label textLabel = new Label(task.getTitolo() + (!nomeCat.isEmpty() ? " (" + nomeCat + ")" : ""));
                textLabel.setStyle(task.getCompletamento() ? "-fx-text-fill: #aaaaaa; -fx-font-size:14px; -fx-strikethrough: true;" :
                        "-fx-text-fill: white; -fx-font-size:14px; -fx-strikethrough: false;");

                MenuItem editItem = new MenuItem("Modifica");
                MenuItem deleteItem = new MenuItem("Elimina");
                editItem.setOnAction(e -> handleEditTask(task));
                deleteItem.setOnAction(e -> handleDeleteTask(task));
                MenuButton menuButton = new MenuButton("⋮", null, editItem, deleteItem);
                menuButton.getStyleClass().add("task-menu-button");

                HBox taskContent = new HBox(10, completeBox, priorityBadge, textLabel, menuButton);
                taskContent.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textLabel, javafx.scene.layout.Priority.ALWAYS);
                textLabel.setMaxWidth(Double.MAX_VALUE);
                taskContent.setOpacity(task.getCompletamento() ? 0.5 : 1.0);

                boolean showSeparator = false;
                if (task.getCompletamento()) {
                    int currentIndex = getIndex();
                    var items = getListView().getItems();
                    if (currentIndex > 0 && currentIndex < items.size()) {
                        Tasks prev = items.get(currentIndex - 1);
                        if (!prev.getCompletamento()) showSeparator = true;
                    } else if (currentIndex == 0) showSeparator = true;
                }

                //mi dà errore strano con questo
//                if (showSeparator) {
//                    VBox container = new VBox(5);
//                    Label sepLabel = new Label("COMPLETATE");
//                    sepLabel.setStyle("-fx-text-fill: #F071A7; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
//                    Separator line = new Separator();
//                    line.setStyle("-fx-opacity: 0.3; -fx-background-color: #F071A7;");
//                    container.getChildren().addAll(line, sepLabel, taskContent);
//                    setGraphic(container);
//                } else setGraphic(taskContent);

                //così non mi esce l'errore
                Label sepLabel = new Label("COMPLETATE");
                sepLabel.setStyle("-fx-text-fill: #F071A7; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 5 0 5 0;");
                sepLabel.setVisible(showSeparator);

                VBox cellContainer = new VBox(5, sepLabel, taskContent);
                setGraphic(cellContainer);


            }
        });
    }

    private String getNomeCategoriaDaId(Integer id) {
        if (id == null || id <= 0) return "";
        for (Categorie c : categoryComboBox.getItems()) {
            if (c.getIdCategoria() != null && c.getIdCategoria().equals(id)) return c.getNomeCategoria();
        }
        return "";
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg); a.setHeaderText(null); a.showAndWait();
    }


    @FXML
    private void handleLogout() { mainApp.showUtentiLogin(); }
    @FXML
    private void handleExit() { System.exit(0); }
    @FXML
    private void handleProfile() { mainApp.showUtentiProfile(mainApp.getCurrentUser()); }

    @FXML
    private void toggleMenu() {
        double target = isOpen ? -300 : 0;
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), sideMenu);
        tt.setToX(target); tt.play(); isOpen = !isOpen;
    }

    @FXML
    private void handleStatistics() {

    }
    @FXML
    private void handleViewSwitch() {
      }
}
