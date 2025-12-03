package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.Categorie;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.MainApp;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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

    public MainScreenController() {}

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        Utenti currentUser = MainApp.getCurrentUser();
        if (currentUser != null && currentUser.getNome() != null) {
            usernameLabelHeader.setText(currentUser.getNome());
        }
    }

    @FXML
    private void initialize() {
        Utenti currentUser = MainApp.getCurrentUser();
        if (currentUser != null && currentUser.getNome() != null) {
            usernameLabelHeader.setText(currentUser.getNome());
        }


        tasks = FXCollections.observableArrayList();
        filteredTasks = new FilteredList<>(tasks, t -> true);
        taskListView.setItems(filteredTasks);

        setupComboBoxes();
        setupCellFactory();
        setupFilters(); // Configura i filtri

        refreshTasks();
    }

    private void refreshTasks() {
        try {
            Tasks filtro = new Tasks();
            filtro.setIdUtente(MainApp.getCurrentUser().getIdUtente());

            List<Tasks> list = DAOTasks.getInstance().select(filtro);
            tasks.setAll(list);
        } catch (DAOException e) {
            showAlert(Alert.AlertType.ERROR, "Impossibile caricare i task: " + e.getMessage());
        }
    }


    private void setupComboBoxes() {
        // Setup Priorità
        priorityComboBox.getItems().addAll("Priorità", "BASSA", "MEDIA", "ALTA");
        priorityComboBox.getSelectionModel().selectFirst();

        // Setup Categorie
        categoryComboBox.getItems().clear();

        // Dummy category
        Categorie dummyCat = new Categorie("Tutte le categorie", -1);
        categoryComboBox.getItems().add(dummyCat);

        try {
            List<Categorie> listaCategorie = DAOCategorie.getInstance().select(null);
            categoryComboBox.getItems().addAll(listaCategorie);
        } catch (DAOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Errore caricamento categorie: " + e.getMessage());
        }

        categoryComboBox.setEditable(true);

        categoryComboBox.setConverter(new StringConverter<Categorie>() {
            @Override
            public String toString(Categorie c) {
                if (c == null) return "";
                return c.getNomeCategoria();
            }

            @Override
            public Categorie fromString(String string) {
                return new Categorie(string, null);
            }
        });

        categoryComboBox.getSelectionModel().selectFirst();
    }

    private void setupFilters() {
        categoryComboBox.setOnAction(event -> applyFilters());
        priorityComboBox.setOnAction(event -> applyFilters());
    }

    private void applyFilters() {
        Categorie selectedCategory = categoryComboBox.getValue();
        String selectedPriority = priorityComboBox.getValue();

        filteredTasks.setPredicate(task -> {
            boolean categoryMatches = true;
            if (selectedCategory != null && selectedCategory.getIdCategoria() != null && selectedCategory.getIdCategoria() > 0) {
                categoryMatches = task.getIdCategoria() != null && task.getIdCategoria().equals(selectedCategory.getIdCategoria());
            }

            boolean priorityMatches = selectedPriority == null || selectedPriority.equals("Priorità") || task.getPriorita().equals(selectedPriority);

            return categoryMatches && priorityMatches;
        });
    }
    private void setupCellFactory() {
        taskListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Tasks task, boolean empty) {
                super.updateItem(task, empty);

                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                CheckBox completeBox = new CheckBox();
                completeBox.setSelected(task.getCompletamento());

                String p = task.getPriorita() == null ? "" : task.getPriorita().trim();
                Label priorityBadge = new Label(p);
                String coloreSfondo = switch (p.toUpperCase()) {
                    case "ALTA" -> "#e74c3c";
                    case "MEDIA" -> "#f39c12";
                    case "BASSA" -> "#27ae60";
                    default -> "grey";
                };

                priorityBadge.setStyle("-fx-text-fill: white; -fx-padding: 3 7 3 7; -fx-background-radius: 5; -fx-background-color:" + coloreSfondo);

                String nomeCat = getNomeCategoriaDaId(task.getIdCategoria());
                String text = task.getTitolo() + (!nomeCat.isEmpty() ? " (" + nomeCat + ")" : "");
                Label textLabel = new Label(text);
                textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

                // MENU A TRE PUNTINI
                MenuItem editItem = new MenuItem("Modifica");
                MenuItem deleteItem = new MenuItem("Elimina");

                deleteItem.setOnAction(e -> handleDeleteTask(task));
                editItem.setOnAction(e -> handleEditTask(task));

                MenuButton menuButton = new MenuButton("⋮", null, editItem, deleteItem);
                menuButton.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 0 10 0 10;");

                HBox hbox = new HBox(10);
                hbox.getChildren().addAll(completeBox, priorityBadge, textLabel, menuButton);
                hbox.setAlignment(Pos.CENTER_LEFT);

                HBox.setHgrow(textLabel, javafx.scene.layout.Priority.ALWAYS);
                textLabel.setMaxWidth(Double.MAX_VALUE);

                setGraphic(hbox);
            }
        });
    }

    private void handleEditTask(Tasks task) {
        boolean okClicked = mainApp.showTasksEditDialog(task);

        if (okClicked) {
            try {
                DAOTasks.getInstance().update(task);
                taskListView.refresh();
            } catch (DAOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleDeleteTask(Tasks task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Sei sicuro di voler eliminare questo task?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            try {
                DAOTasks.getInstance().delete(task);
                tasks.remove(task);
            } catch (DAOException e) {
                showAlert(Alert.AlertType.ERROR, "Errore eliminazione task: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleNewTask() {
        String testo = newTaskField.getText().trim();
        String descrizione = descriptionArea.getText().trim();

        String nomeCategoriaInput = categoryComboBox.getEditor().getText().trim();
        String priorita = priorityComboBox.getValue();
        LocalDate scadenza = dueDateField.getValue();

        if (testo.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Il titolo non può essere vuoto.");
            return;
        }

        if (priorita == null || priorita.equals("Priorità")) {
            showAlert(Alert.AlertType.WARNING, "Seleziona una priorità valida.");
            return;
        }

        try {
            Integer idCategoriaFinale = null;

            // Logica Categorie
            if (!nomeCategoriaInput.isEmpty() && !nomeCategoriaInput.equals("Tutte le categorie") && !nomeCategoriaInput.equals("Seleziona Categoria")) {

                // 1. Cerca in memoria
                for (Categorie c : categoryComboBox.getItems()) {
                    if (c.getNomeCategoria() != null && c.getNomeCategoria().equalsIgnoreCase(nomeCategoriaInput)) {
                        idCategoriaFinale = c.getIdCategoria();
                        break;
                    }
                }

                // 2. Crea se non esiste
                if (idCategoriaFinale == null || idCategoriaFinale <= 0) {
                    Categorie nuovaCat = new Categorie(nomeCategoriaInput, null);
                    DAOCategorie.getInstance().insert(nuovaCat);

                    List<Categorie> aggiornate = DAOCategorie.getInstance().select(null);
                    for (Categorie c : aggiornate) {
                        if (c.getNomeCategoria().equalsIgnoreCase(nomeCategoriaInput)) {
                            idCategoriaFinale = c.getIdCategoria();
                            break;
                        }
                    }

                    categoryComboBox.getItems().setAll(aggiornate);
                    categoryComboBox.getItems().add(0, new Categorie("Tutte le categorie", -1));
                }
            }

            // Creazione Task
            Tasks nuovaTask = new Tasks();
            nuovaTask.setTitolo(testo);
            nuovaTask.setDescrizione(descrizione);
            nuovaTask.setIdCategoria(idCategoriaFinale);
            nuovaTask.setPriorita(priorita);
            nuovaTask.setScadenza(scadenza != null ? scadenza.toString() : "");
            nuovaTask.setIdUtente(MainApp.getCurrentUser().getIdUtente());
            nuovaTask.setCompletamento(false);

            DAOTasks.getInstance().insert(nuovaTask);
            tasks.add(0, nuovaTask);
            taskListView.scrollTo(0);

            // Reset UI
            newTaskField.clear();
            descriptionArea.clear();
            dueDateField.setValue(null);
            categoryComboBox.getEditor().clear();
            categoryComboBox.getSelectionModel().selectFirst();
            priorityComboBox.getSelectionModel().selectFirst();

        } catch (DAOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore salvataggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleTaskStatusChange(Tasks task) {
        try {
            DAOTasks.getInstance().update(task);
        } catch (DAOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore aggiornamento stato: " + e.getMessage());
        }
    }

    private String getNomeCategoriaDaId(Integer id) {
        if (id == null || id <= 0) return "";
        for (Categorie c : categoryComboBox.getItems()) {
            if (c.getIdCategoria() != null && c.getIdCategoria().equals(id)) {
                return c.getNomeCategoria();
            }
        }
        return "";
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML private void handleLogout() { mainApp.showUtentiLogin(); }

    @FXML
    private void toggleMenu() {
        double target = isOpen ? -300 : 0;
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), sideMenu);
        tt.setToX(target);
        tt.play();
        isOpen = !isOpen;
    }

    @FXML
    private void handleProfile() {
        mainApp.showUtentiProfile(mainApp.getCurrentUser());
    }
    @FXML
    private void handleExit() { System.exit(0); }
}
