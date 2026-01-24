package ingegneriaSoftware;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
Rappresenta una singola occorrenza pianificata di un tipo di visita
in una data specifica, con il volontario assegnato come guida.

Rispetto alla versione precedente, l’istanza mantiene anche le iscrizioni
effettuate dai fruitori, così da poter calcolare i posti occupati e gestire
le prenotazioni tramite codice.

Invarianti di classe:
- idTipoVisita identifica il tipo di visita a cui l’istanza fa riferimento
- data rappresenta il giorno in cui l’istanza è pianificata o svolta
- nomeVolontario identifica la guida assegnata all’istanza
- stato rappresenta la fase operativa dell’istanza
- iscrizioni contiene l’elenco delle prenotazioni associate all’istanza
*/
public class VisitaIstanza {

    private String idTipoVisita;
    private LocalDate data;
    private String nomeVolontario;
    private StatiVisita stato;
    private List<Iscrizione> iscrizioni = new ArrayList<>();

    public VisitaIstanza() {
    }

    public VisitaIstanza(String idTipoVisita, LocalDate data, String nomeVolontario, StatiVisita stato) {
        this.idTipoVisita = idTipoVisita;
        this.data = data;
        this.nomeVolontario = nomeVolontario;
        this.stato = stato;
    }

    public int postiOccupati() {
        int tot = 0;
        for (Iscrizione i : getIscrizioni()) tot += i.getNumPersone();
        return tot;
    }

    public Iscrizione trovaIscrizione(String codice) {
        for (Iscrizione i : getIscrizioni()) {
            if (i.getCodicePrenotazione().equals(codice)) return i;
        }
        return null;
    }

    public void aggiungiIscrizione(Iscrizione iscrizione) {
        getIscrizioni().add(iscrizione);
    }

    public boolean rimuoviIscrizionePerCodice(String codice) {
        return getIscrizioni().removeIf(i -> i.getCodicePrenotazione().equals(codice));
    }

    public String toStringBreve(Visita tipo) {
        String titolo = (tipo != null) ? tipo.getTitolo() : idTipoVisita;

        int max = (tipo != null) ? tipo.getNumMaxPartecipanti() : -1;
        String cap = (max > 0) ? (postiOccupati() + "/" + max) : String.valueOf(postiOccupati());

        return data + " | " + titolo + " | guida: " + nomeVolontario + " | " + cap;
    }

    public String toStringDettagliato(Visita tipo, String usernameFruitoreOrNull) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n----------------------------------\n");
        sb.append("Data: ").append(data).append(" | Stato: ").append(stato).append("\n");

        String titolo = (tipo != null) ? tipo.getTitolo() : idTipoVisita;

        if (stato == StatiVisita.cancellata) {
            sb.append("Titolo: ").append(titolo).append("\n");
            return sb.toString();
        }

        if (tipo == null) {
            sb.append("Titolo: ").append(titolo).append("\n");
            sb.append("Dettagli non disponibili: tipo visita mancante.\n");
            return sb.toString();
        }

        sb.append("Titolo: ").append(tipo.getTitolo()).append("\n");
        sb.append("Descrizione: ").append(tipo.getDescrizione()).append("\n");
        sb.append("Punto di incontro: ").append(tipo.getPuntoDiIncontro()).append("\n");
        sb.append("Ora inizio: ").append(tipo.getScheduling().oraInizio()).append("\n");
        sb.append("Biglietto: ").append(tipo.isAcquistoBiglietto() ? "Sì" : "No").append("\n");

        sb.append("Posti occupati: ")
          .append(postiOccupati())
          .append("/")
          .append(tipo.getNumMaxPartecipanti())
          .append("\n");

        return sb.toString();
    }

    public String toStringPerVolontario(Visita tipo) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n----------------------------------\n");
        sb.append("Data: ").append(data).append(" | Stato: ").append(stato).append("\n");

        String titolo = (tipo != null) ? tipo.getTitolo() : idTipoVisita;

        sb.append("Titolo: ").append(titolo).append("\n");

        if (tipo == null) {
            sb.append("Dettagli non disponibili: tipo visita mancante.\n");
            return sb.toString();
        }

        sb.append("Descrizione: ").append(tipo.getDescrizione()).append("\n");
        sb.append("Punto di incontro: ").append(tipo.getPuntoDiIncontro()).append("\n");
        sb.append("Ora inizio: ").append(tipo.getScheduling().oraInizio()).append("\n");
        sb.append("Biglietto: ").append(tipo.isAcquistoBiglietto() ? "Sì" : "No").append("\n");
        sb.append("Partecipanti min: ").append(tipo.getNumMinPartecipanti()).append("\n");
        sb.append("Partecipanti max: ").append(tipo.getNumMaxPartecipanti()).append("\n");
        sb.append("Posti occupati: ").append(postiOccupati()).append("/").append(tipo.getNumMaxPartecipanti()).append("\n");

        sb.append("\nElenco iscritti (codice -> persone):\n");
        if (getIscrizioni().isEmpty()) {
            sb.append("  (nessun iscritto)\n");
        } else {
            for (Iscrizione i : getIscrizioni()) {
                sb.append("  - ").append(i.getCodicePrenotazione())
                  .append(" -> ").append(i.getNumPersone()).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "VisitaIstanza{" +
                "idTipoVisita='" + idTipoVisita + '\'' +
                ", data=" + data +
                ", guida='" + nomeVolontario + '\'' +
                ", stato=" + stato +
                '}';
    }

    public String getIdTipoVisita() {
        return idTipoVisita;
    }

    public void setIdTipoVisita(String idTipoVisita) {
        this.idTipoVisita = idTipoVisita;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getNomeVolontario() {
        return nomeVolontario;
    }

    public void setNomeVolontario(String nomeVolontario) {
        this.nomeVolontario = nomeVolontario;
    }

    public StatiVisita getStato() {
        return stato;
    }

    public void setStato(StatiVisita stato) {
        this.stato = stato;
    }

    public List<Iscrizione> getIscrizioni() {
        return iscrizioni;
    }

    public void setIscrizioni(List<Iscrizione> iscrizioni) {
        this.iscrizioni = iscrizioni;
    }
}