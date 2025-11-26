package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Utenti;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DAOUtenti implements DAO<Utenti> {

    private DAOUtenti(){}

    private static DAO dao = null;
    private static Logger logger = null;

    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOUtenti();
            logger = Logger.getLogger(DAOUtenti.class.getName());
        }
        return dao;
    }

    @Override
    public List<Utenti> select(Utenti a) throws DAOException {

        if (a == null){
            a = new Utenti("", "", "", "", null); // Cerca tutti gli elementi
        }

        ArrayList<Utenti> lista = new ArrayList<>();
        try{

            if (a == null || a.getCognome() == null
                    || a.getNome() == null
                    || a.getEmail() == null){
                throw new DAOException("In select: any field can be null");
            }

            Statement st = DAOMySQLSettings.getStatement();

            // --- CORREZIONE EFFETTUATA QUI ---
            String sql = "select * from Utenti where cognome like '";
            sql += a.getCognome() + "%' and nome like '" + a.getNome() + "%'"; // Aggiunto chiusura %'

            sql += " and email like '" + a.getEmail() + "%'";
            // --------------------------------

            try{
                logger.info("SQL: " + sql);
            } catch(NullPointerException nullPointerException){
                logger.severe("SQL: " + sql);
            }
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                lista.add(new Utenti(rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("email"),
                        rs.getString("psw"),
                        rs.getInt("idUtente")));
            }
            DAOMySQLSettings.closeStatement(st);

        } catch (SQLException sq){
            throw new DAOException("In select(): " + sq.getMessage());
        }
        return lista;
    }

    @Override
    public void delete(Utenti a) throws DAOException {
        if (a == null || a.getIdUtente() == null){
            throw new DAOException("In delete: idUtente cannot be null");
        }
        String query = "DELETE FROM Utenti WHERE idUtente='" + a.getIdUtente() + "';";

        try{
            logger.info("SQL: " + query);
        } catch (NullPointerException nullPointerException){
            System.out.println("SQL: " + query);
        }

        executeUpdate(query);
    }


    @Override
    public void insert(Utenti a) throws DAOException {
        verifyObject(a);

        String query = "INSERT INTO Utenti (nome, cognome, email, psw, idUtente) VALUES  ('" +
                a.getNome() + "', '" + a.getCognome() + "', '" +
                a.getEmail() + "', '" + a.getPsw() +
                "', NULL)";
        try {
            logger.info("SQL: " + query);
        } catch (NullPointerException nullPointerException){
            System.out.println("SQL: " + query);
        }
        executeUpdate(query);
    }


    @Override
    public void update(Utenti a) throws DAOException {
        verifyObject(a);

        String query = "UPDATE Utenti SET nome = '" + a.getNome() + "', cognome = '" + a.getCognome() + "', email = '" + a.getEmail() + "'";
        query = query + " WHERE idUtente = " + a.getIdUtente() + ";";
        logger.info("SQL: " + query);

        executeUpdate(query);
    }


    private void verifyObject(Utenti a) throws DAOException {
        if (a == null || a.getCognome() == null
                || a.getNome() == null
                || a.getEmail() == null){
            throw new DAOException("In select: any field can be null");
        }
    }

    private void executeUpdate(String query) throws DAOException{
        try {
            Statement st = DAOMySQLSettings.getStatement();
            int n = st.executeUpdate(query);

            DAOMySQLSettings.closeStatement(st);

        } catch (SQLException e) {
            throw new DAOException("In insert(): " + e.getMessage());
        }
    }
}