package it.unicas.project.template.address.model.dao.mysql;

import it.unicas.project.template.address.model.Tasks;
import it.unicas.project.template.address.model.dao.DAO;
import it.unicas.project.template.address.model.dao.DAOException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DAOTasks implements DAO<Tasks> {

    private DAOTasks(){}

    private static DAOTasks dao = null;
    private static Logger logger = null;

    public static DAO getInstance(){
        if (dao == null){
            dao = new DAOTasks();
            logger = Logger.getLogger(DAOTasks.class.getName());
        }
        return dao;
    }

    @Override
    public List<Tasks> select(Tasks t) throws DAOException {
        ArrayList<Tasks> lista = new ArrayList<>();
        Statement st = null;

        try {
            st = DAOMySQLSettings.getStatement();
            String sql = "SELECT * FROM Tasks WHERE 1=1 ";

            if (t != null) {
                if (t.getTitolo() != null && !t.getTitolo().isEmpty()) {
                    // Anche qui proteggiamo la ricerca se il titolo ha apostrofi
                    sql += " AND titolo LIKE '" + t.getTitolo().replace("'", "\\'") + "%'";
                }
                if (t.getPriorita() != null && !t.getPriorita().isEmpty()) {
                    sql += " AND priorità = '" + t.getPriorita() + "'";
                }
                if (t.getIdUtente() != null && t.getIdUtente() > 0) {
                    sql += " AND idUtente = " + t.getIdUtente();
                }
                if (t.getIdCategoria() != null && t.getIdCategoria() > 0) {
                    sql += " AND idCategoria = " + t.getIdCategoria();
                }
            }

            logger.info("SQL Select: " + sql);

            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                lista.add(new Tasks(
                        rs.getString("titolo"),
                        rs.getString("descrizione"),
                        rs.getString("scadenza"),
                        rs.getString("priorità"),
                        rs.getString("data_creazione"),
                        rs.getBoolean("completamento"),
                        rs.getInt("idTask"),
                        rs.getInt("idUtente"),
                        rs.getInt("idCategoria")
                ));
            }

        } catch (SQLException sq) {
            throw new DAOException("Errore nella select: " + sq.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
        return lista;
    }

    @Override
    public void delete(Tasks t) throws DAOException {
        if (t == null || t.getIdTask() == null || t.getIdTask() <= 0) {
            throw new DAOException("Impossibile eliminare: idTask mancante.");
        }

        String query = "DELETE FROM Tasks WHERE idTask = " + t.getIdTask();
        logger.info("SQL Delete: " + query);

        executeUpdate(query);
    }

    @Override
    public void insert(Tasks t) throws DAOException {
        verifyObject(t);

        Statement st = null;
        try {
            // Otteniamo lo Statement per eseguire l'operazione
            st = DAOMySQLSettings.getStatement();

            int completatoInt = (t.getCompletamento() != null && t.getCompletamento()) ? 1 : 0;
            String idCatVal = "NULL";
            if (t.getIdCategoria() != null && t.getIdCategoria() > 0) {
                idCatVal = t.getIdCategoria().toString();
            }

            // Sanitizzazione dei campi di testo per prevenire SQL Injection
            String titoloSafe = t.getTitolo().replace("'", "\\'");
            String descrizioneSafe = (t.getDescrizione() != null) ? t.getDescrizione().replace("'", "\\'") : "";

            String query = "INSERT INTO Tasks (titolo, descrizione, scadenza, priorità, completamento, idUtente, idCategoria) VALUES ('"
                    + titoloSafe + "', '"
                    + descrizioneSafe + "', '"
                    + t.getScadenza() + "', '"
                    + t.getPriorita() + "', "
                    + completatoInt + ", "
                    + t.getIdUtente() + ", "
                    + idCatVal + ")";

            logger.info("SQL Insert: " + query);

            // 1. Esegui l'UPDATE richiedendo le chiavi generate
            st.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);

            // 2. Recupera la chiave generata (idTask)
            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                int idGenerato = rs.getInt(1);
                t.setIdTask(idGenerato); // <--- FIX CRUCIALE: Aggiorna l'oggetto Tasks in memoria
            } else {
                logger.warning("Nessuna chiave generata per la Task inserita.");
            }

            if (rs != null) rs.close(); // Chiude il ResultSet

        } catch (SQLException e) {
            throw new DAOException("Errore Database durante l'insert: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }

    @Override
    public void update(Tasks t) throws DAOException {
        if (t == null || t.getIdTask() == null || t.getIdTask() <= 0) {
            throw new DAOException("Impossibile aggiornare: idTask non valido.");
        }

        int completatoInt = (t.getCompletamento() != null && t.getCompletamento()) ? 1 : 0;
        String idCatVal = "NULL";
        if (t.getIdCategoria() != null && t.getIdCategoria() > 0) {
            idCatVal = t.getIdCategoria().toString();
        }

        // --- CORREZIONE ANCHE QUI ---
        String titoloSafe = t.getTitolo().replace("'", "\\'");
        String descrizioneSafe = (t.getDescrizione() != null) ? t.getDescrizione().replace("'", "\\'") : "";

        String query = "UPDATE Tasks SET "
                + "titolo = '" + titoloSafe + "', "
                + "descrizione = '" + descrizioneSafe + "', "
                + "scadenza = '" + t.getScadenza() + "', "
                + "priorità = '" + t.getPriorita() + "', "
                + "idCategoria = " + idCatVal + ", "
                + "completamento = " + completatoInt
                + " WHERE idTask = " + t.getIdTask();

        logger.info("SQL Update: " + query);
        executeUpdate(query);
    }

    private void verifyObject(Tasks t) throws DAOException {
        if (t == null) throw new DAOException("Task è null");
        if (t.getTitolo() == null || t.getTitolo().isEmpty()) {
            throw new DAOException("Il titolo del task è obbligatorio");
        }
        if (t.getIdUtente() == null || t.getIdUtente() <= 0) {
            throw new DAOException("Task deve essere associato a un utente (idUtente mancante)");
        }
    }

    private void executeUpdate(String query) throws DAOException {
        Statement st = null;
        try {
            st = DAOMySQLSettings.getStatement();
            st.executeUpdate(query);
        } catch (SQLException e) {
            throw new DAOException("Errore Database: " + e.getMessage());
        } finally {
            DAOMySQLSettings.closeStatement(st);
        }
    }
}
