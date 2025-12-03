package it.unicas.project.template.address.view;

import it.unicas.project.template.address.MainApp;
import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.model.dao.mysql.DAOCategorie;
import it.unicas.project.template.address.model.dao.mysql.DAOTasks;
import it.unicas.project.template.address.util.DateUtil;
import it.unicas.project.template.address.model.Categorie;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate; // <-- IMPORT NECESSARIO
import java.util.List;

public class TaskEditDialogController {

    @FXML
    private TextField titoloField;
    @FXML
    private TextArea descrizioneField;
    @FXML
    private ComboBox<Categorie> categoryComboBox;
    @FXML
    private DatePicker scadenzaField;
    @FXML
    private ComboBox<String> priorityComboBox;
    @FXML
    private ListView<Tasks> taskListView;

    private ObservableList<Tasks> tasks;
    private FilteredList<Tasks> filteredTasks;


    private Stage dialogStage;
    private Tasks task;
    private boolean okClicked = false;
    private MainApp mainApp;

    @FXML
    private void initialize() {
        tasks = FXCollections.observableArrayList();
        filteredTasks = new FilteredList<>(tasks, t -> true);
        taskListView.setItems(filteredTasks);

        setupComboBoxes();
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
        priorityComboBox.getItems().addAll("Priorità", "BASSA", "MEDIA", "ALTA");
        priorityComboBox.getSelectionModel().selectFirst();

        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().add(new Categorie("Tutte le categorie", -1));

        try {
            List<Categorie> listaCategorie = DAOCategorie.getInstance().select(null);
            categoryComboBox.getItems().addAll(listaCategorie);
        } catch (DAOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore caricamento categorie: " + e.getMessage());
        }

        categoryComboBox.setEditable(true);
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categorie c) {
                return c == null ? "" : c.getNomeCategoria();
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


    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public void setTask(Tasks task) {
        this.task = task;

        titoloField.setText(task.getTitolo());
        descrizioneField.setText(task.getDescrizione());
        scadenzaField.setValue(DateUtil.parse(task.getScadenza()));
        priorityComboBox.setValue(task.getPriorita());

    }

    @FXML
    private void handleOk() {
        if (isInputValid()) {
            task.setTitolo(titoloField.getText());
            task.setDescrizione(descrizioneField.getText());
            LocalDate scadenzaLocalDate = scadenzaField.getValue();
            task.setScadenza(DateUtil.format(scadenzaLocalDate));
            task.setPriorita(priorityComboBox.getValue());

            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    // Metodo isInputValid lasciato invariato (ma ora controlla la LocalDate)
    private boolean isInputValid() {
        String errorMessage = "";

        if (titoloField.getText() == null || titoloField.getText().isEmpty()) {
            errorMessage += "Titolo non valido!\n";
        }
        if (descrizioneField.getText() == null || descrizioneField.getText().isEmpty()) {
            errorMessage += "Descrizione non valida!\n";
        }
        // Il controllo del DatePicker è corretto: getValue() torna null se vuoto
        if (scadenzaField.getValue() == null) {
            errorMessage += "Data di scadenza non valida!\n";
        }
        if (priorityComboBox.getValue() == null || priorityComboBox.getValue().isEmpty()) {
            errorMessage += "Priorità non valida!\n";
        }

        if (!errorMessage.isEmpty()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Campi non validi");
            alert.setHeaderText("Per favore correggi i campi non validi");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
        return true;
    }


    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}