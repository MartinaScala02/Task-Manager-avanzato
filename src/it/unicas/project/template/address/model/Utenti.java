package it.unicas.project.template.address.model;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Model class for a Utenti.
 */
public class Utenti {

    private StringProperty nome;
    private StringProperty cognome;
    private StringProperty username;
    private StringProperty email;
    private StringProperty psw;
    private IntegerProperty idUtente;  //wrapper

    //private static String attributoStaticoDiEsempio;

    /**
     * Default constructor.
     */
    public Utenti() {
        this(null, null, null);
    }

    public Utenti(String nome, String cognome /*String username*/, String email, String psw, Integer idColleghi) {
        this.nome = new SimpleStringProperty(nome);
        this.cognome = new SimpleStringProperty(cognome);
        //this.username = new SimpleStringProperty(username);
        this.email = new SimpleStringProperty(email);
        this.psw = new SimpleStringProperty(psw);
        if (idColleghi != null){
            this.idUtente = new SimpleIntegerProperty(idColleghi);
        } else {
            this.idUtente = null;
        }
    }

    /**
     * Constructor with some initial data.
     *
     * @param nome
     * @param cognome
     * @param psw
     */
    public Utenti(String nome, String cognome, String psw) {
        this.nome = new SimpleStringProperty(nome);
        this.cognome = new SimpleStringProperty(cognome);
        // Some initial dummy data, just for convenient testing.
        this.username = new SimpleStringProperty("username");
        this.email = new SimpleStringProperty("email@email.com");
        this.psw = new SimpleStringProperty(psw);
        this.idUtente = null;
    }

    public Integer getIdUtente(){
        if (idUtente == null){
            idUtente = new SimpleIntegerProperty(-1);
        }
        return idUtente.get();
    }

    public void setIdUtente(Integer idUtente) {
        if (this.idUtente == null){
            this.idUtente = new SimpleIntegerProperty();
        }
        this.idUtente.set(idUtente);
    }

    public String getNome() {
        return nome.get();
    }

    public void setNome(String nome) {
        this.nome.set(nome);
    }

    public StringProperty nomeProperty() {
        return nome;
    }

    public String getCognome() {
        return cognome.get();
    }

    public void setCognome(String cognome) {
        this.cognome.set(cognome);
    }

    public StringProperty cognomeProperty() {
        return cognome;
    }

    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getPsw() {
        return psw.getValue();
    }

    public void setPsw(String psw) {
        this.psw.set(psw);
    }

    public StringProperty pswProperty() {
        return psw;
    }


    public String toString(){
        return nome.getValue() + ", " + cognome.getValue() + ", " + username.getValue() + ", " + email.getValue() + ", " + psw.getValue() + ", (" + idUtente.getValue() + ")";
    }


    public static void main(String[] args) {



        // https://en.wikipedia.org/wiki/Observer_pattern
        Utenti collega = new Utenti();
        collega.setNome("Ciao");
        MyChangeListener myChangeListener = new MyChangeListener();
        collega.nomeProperty().addListener(myChangeListener);
        collega.setNome("Mario");


        collega.pswProperty().addListener(myChangeListener);

        collega.pswProperty().addListener(
                (ChangeListener) (o, oldVal, newVal) -> System.out.println("Compleanno property has changed!"));

        collega.pswProperty().addListener(
                (o, old, newVal)-> System.out.println("Compleanno property has changed! (Lambda implementation)")
        );


        collega.setPsw("30-10-1971");



        // Use Java Collections to create the List.
        List<Utenti> list = new ArrayList<>();

        // Now add observability by wrapping it with ObservableList.
        ObservableList<Utenti> observableList = FXCollections.observableList(list);
        observableList.addListener(
                (ListChangeListener) change -> System.out.println("Detected a change! ")
        );

        Utenti c1 = new Utenti();
        Utenti c2 = new Utenti();

        c1.nomeProperty().addListener(
                (o, old, newValue)->System.out.println("Ciao")
        );

        c1.setNome("Pippo");

        // Changes to the observableList WILL be reported.
        // This line will print out "Detected a change!"
        observableList.add(c1);

        // Changes to the underlying list will NOT be reported
        // Nothing will be printed as a result of the next line.
        observableList.add(c2);


        observableList.get(0).setNome("Nuovo valore");

        System.out.println("Size: "+observableList.size());

    }


}
