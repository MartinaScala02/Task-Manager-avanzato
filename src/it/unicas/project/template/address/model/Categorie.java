package it.unicas.project.template.address.model;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class Categorie {
    private StringProperty nomeCategoria;
    private IntegerProperty idCategoria;

    //costruttore
    public Categorie() {
        this(null, null);
    }

    public Categorie(String nomeCategoria, Integer idCategoria) {
        this.nomeCategoria = new SimpleStringProperty(nomeCategoria);
        if (idCategoria != null) {
            this.idCategoria = new SimpleIntegerProperty(idCategoria);
        } else {
            this.idCategoria = null;
        }
    }

    /**
     * Constructor with some initial data.
     *
     * @param nomeCategoria
     */

    public Categorie(String nomeCategoria) {
        this.nomeCategoria = new SimpleStringProperty(nomeCategoria);
        this.idCategoria = null;

    }

    public Integer getIdCategoria(){
        if (idCategoria == null){
            idCategoria = new SimpleIntegerProperty(-1);
        }
        return idCategoria.get();
    }

    public void setIdCategoria(Integer idCategoria) {
        if (this.idCategoria == null){
            this.idCategoria = new SimpleIntegerProperty();
        }
        this.idCategoria.set(idCategoria);
    }

    public String getNomeCategoria() {
        return nomeCategoria.get();
    }

    public void setNomeCategoria(String nomeCategoria) {
        this.nomeCategoria.set(nomeCategoria);
    }

    public StringProperty nomeCategoriaProperty() {
        return nomeCategoria;
    }

    public String toString(){
        return nomeCategoria.getValue() + ", (" + idCategoria.getValue() + ")";
    }


}
