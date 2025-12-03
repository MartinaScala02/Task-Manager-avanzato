package it.unicas.project.template.address.view;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.mysql.DAOUtenti;
import it.unicas.project.template.address.model.dao.DAOException;
import it.unicas.project.template.address.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField; //gestisce automaticamente la maschera
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

import java.util.List;

public class UtentiLoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField PasswordField;

    @FXML
    private TextField pswVisibleField;

    @FXML
    private ToggleButton showpswBtn;

    private MainApp mainApp;


    public UtentiLoginController() {
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }


    @FXML
    private void initialize() {
        // Sincronizza i due campi (testo condiviso)
        pswVisibleField.textProperty().bindBidirectional(PasswordField.textProperty());

        // All'avvio mostra solo il PasswordField
        pswVisibleField.setVisible(false);
        pswVisibleField.setManaged(false);

//per gestire la visibilità della password -> selezionato mostra il campo di testo normale, altrimenti mostra il PasswordField
        showpswBtn.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                pswVisibleField.setVisible(true);
                pswVisibleField.setManaged(true);
                PasswordField.setVisible(false);
                PasswordField.setManaged(false);
                pswVisibleField.requestFocus();
                pswVisibleField.positionCaret(pswVisibleField.getText().length());
            } else {
                PasswordField.setVisible(true);
                PasswordField.setManaged(true);
                pswVisibleField.setVisible(false);
                pswVisibleField.setManaged(false);
                PasswordField.requestFocus();
                PasswordField.positionCaret(PasswordField.getText().length());
            }
        });
    }

    //per gestire il login
    @FXML
    private void handleLogin() {

        String emailInserita = emailField.getText();
        String passwordInserita = PasswordField.getText();


        if (emailInserita == null || emailInserita.isEmpty() || passwordInserita == null || passwordInserita.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Inserisci email e password.");
            alert.showAndWait();
            return;
        }

        try {

            Utenti userDaCercare = new Utenti();
            userDaCercare.setEmail(emailInserita);
            userDaCercare.setPsw(passwordInserita);



            List<Utenti> risultato = DAOUtenti.getInstance().select(userDaCercare);

            if (!risultato.isEmpty()) {

                Utenti utenteLoggato = risultato.get(0);
                MainApp.setCurrentUser(utenteLoggato);


                // Messaggio di Benvenuto
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Successo");
                alert.setHeaderText(null);
                alert.setContentText("Benvenuto " + utenteLoggato.getNome() + "!");
                alert.showAndWait();

                mainApp.showMainScreen();

            } else {
                // --- LOGIN FALLITO ---
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore");
                alert.setHeaderText("Login Fallito");
                alert.setContentText("Email o Password errate.");
                alert.showAndWait();
            }

        } catch (DAOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleRegister(){
            Utenti tempUtenti = new Utenti();
            boolean okClicked = mainApp.showUtentiEditDialog(tempUtenti);

            if (okClicked) {
                try {
                    DAOUtenti.getInstance().insert(tempUtenti);
                    mainApp.getColleghiData().add(tempUtenti);

                } catch (DAOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(mainApp.getPrimaryStage());
                    alert.setTitle("Error during DB interaction");
                    alert.setHeaderText("Error during insert ...");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            }
        }



}

