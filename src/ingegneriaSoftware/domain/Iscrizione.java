package ingegneriaSoftware.domain;

/*
Rappresenta una prenotazione effettuata da un fruitore per una specifica
istanza di visita.

Ogni iscrizione è identificata da un codice di prenotazione univoco
e indica il numero di persone associate alla prenotazione stessa.

Invarianti di classe:
- codicePrenotazione identifica univocamente l’iscrizione
- usernameFruitore identifica il fruitore che ha effettuato la prenotazione
- numPersone rappresenta il numero di partecipanti associati all’iscrizione
*/
public class Iscrizione {

    private String codicePrenotazione;
    private String usernameFruitore;
    private int numPersone;

    public Iscrizione() {}

    public Iscrizione(String codicePrenotazione, String usernameFruitore, int numPersone) {
        this.codicePrenotazione = codicePrenotazione;
        this.usernameFruitore = usernameFruitore;
        this.numPersone = numPersone;
    }

    public String toStringBreve() {
        return "Codice: " + codicePrenotazione + " | Persone: " + numPersone;
    }

    public String getCodicePrenotazione() {
        return codicePrenotazione;
    }

    public void setCodicePrenotazione(String codicePrenotazione) {
        this.codicePrenotazione = codicePrenotazione;
    }

    public String getUsernameFruitore() {
        return usernameFruitore;
    }

    public void setUsernameFruitore(String usernameFruitore) {
        this.usernameFruitore = usernameFruitore;
    }

    public int getNumPersone() {
        return numPersone;
    }

    public void setNumPersone(int numPersone) {
        this.numPersone = numPersone;
    }
}