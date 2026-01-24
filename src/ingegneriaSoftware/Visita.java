package ingegneriaSoftware;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({
    "dataInizio",
    "dataFine",
    "oraInizio",
    "durata",
    "giorniProgrammabili"
})
/*
Rappresenta un tipo di visita associato a un luogo.

Oltre ai dati descrittivi e ai vincoli numerici sui partecipanti, la visita contiene
le informazioni di scheduling (periodo, giorni settimanali, ora e durata) e l’elenco
dei volontari abilitati a fare da guida.

Lo stato attivo indica se il tipo di visita è ancora utilizzabile (ad esempio non è scaduto
rispetto alla data corrente). L’identificativo idTipoVisita è derivato in modo stabile da
idLuogo e titolo e viene usato per confrontare visite equivalenti.

Invarianti di classe:
- idTipoVisita identifica univocamente il tipo di visita ed è derivato da idLuogo e titolo
- se scheduling è nullo la visita non è programmabile
- la visita è programmabile solo se attiva, nel periodo valido e nel giorno della settimana previsto
- l’elenco dei volontari non contiene duplicati (gestito in aggiuntaVolontario)
*/
public class Visita {

    private String idTipoVisita;
    private String titolo;
    private String idLuogo;
    private String descrizione;
    private PuntoIncontro puntoDiIncontro;
    private SchedulingVisita scheduling;
    private ArrayList<String> volontari = new ArrayList<>();
    private boolean acquistoBiglietto;
    private int numMinPartecipanti;
    private int numMaxPartecipanti;
    private boolean attivo;

    public Visita() {}

    public Visita(
            String idLuogo,
            String titolo,
            String descrizione,
            PuntoIncontro puntoDiIncontro,
            SchedulingVisita scheduling,
            int numMinPartecipanti,
            int numMaxPartecipanti,
            ArrayList<String> volontari,
            boolean acquistoBiglietto) {

        this.idLuogo = idLuogo;
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.puntoDiIncontro = puntoDiIncontro;
        this.scheduling = scheduling;
        this.numMinPartecipanti = numMinPartecipanti;
        this.numMaxPartecipanti = numMaxPartecipanti;
        this.volontari = (volontari != null) ? volontari : new ArrayList<>();
        this.acquistoBiglietto = acquistoBiglietto;
        this.idTipoVisita = generaIdTipoVisita(idLuogo, titolo);
        this.attivo = true;
    }

    public String getIdTipoVisita() {
        if (idTipoVisita == null || idTipoVisita.isBlank()) {
            idTipoVisita = generaIdTipoVisita(idLuogo, titolo);
        }
        return idTipoVisita;
    }

    public void setIdTipoVisita(String idTipoVisita) {
        this.idTipoVisita = idTipoVisita;
    }

    private static String generaIdTipoVisita(String idLuogo, String titolo) {
        String base = (safe(idLuogo) + "::" + safe(titolo)).toLowerCase().trim();
        base = base.replaceAll("\\s+", "-");
        base = base.replaceAll("[^a-z0-9\\-:_]", "");
        return "tipo-" + base;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public void aggiornaAttivoDaOggi(LocalDate oggi) {
        this.attivo = !isScaduta(oggi);
    }

    public boolean isScaduta(LocalDate oggi) {
        if (oggi == null) oggi = LocalDate.now();
        LocalDate fine = getDataFine();
        return (fine != null) && fine.isBefore(oggi);
    }

    public boolean isProgrammabile(LocalDate giorno) {
        if (!attivo) return false;
        if (scheduling == null) return false;

        LocalDate dataInizio = scheduling.dataInizio();
        LocalDate dataFine = scheduling.dataFine();
        List<GiorniSettimana> giorni = scheduling.giorniProgrammabili();

        if (giorno.isBefore(dataInizio) || giorno.isAfter(dataFine)) return false;

        GiorniSettimana g = GiorniSettimana.valueOf(giorno.getDayOfWeek().name());
        return giorni.contains(g);
    }

    public boolean visitaSovrappostaCon(Visita altra, LocalDate giorno) {
        if (!this.isProgrammabile(giorno) || !altra.isProgrammabile(giorno)) return false;

        LocalDateTime aInizio = this.getOraInizio(giorno);
        LocalDateTime aFine = this.getOraFine(giorno);
        LocalDateTime bInizio = altra.getOraInizio(giorno);
        LocalDateTime bFine = altra.getOraFine(giorno);

        return aInizio.isBefore(bFine) && aFine.isAfter(bInizio);
    }

    public LocalDateTime getOraInizio(LocalDate giorno) {
        if (scheduling == null) return null;
        return LocalDateTime.of(giorno, scheduling.oraInizio());
    }

    public LocalDateTime getOraFine(LocalDate giorno) {
        if (scheduling == null) return null;
        return getOraInizio(giorno).plusMinutes(scheduling.durata().toMinutes());
    }

    public void aggiungiVolontario(String nome) {
        if (nome == null) return;
        String n = nome.trim();
        if (n.isBlank()) return;
        if (!this.volontari.contains(n)) this.volontari.add(n);
    }

    private String formattaVolontari() {
        if (volontari == null || volontari.isEmpty()) return "NESSUNO";
        return String.join(", ", volontari);
    }

    public String toStringSemplificato() {
        return  "Titolo: " + titolo + "\n" +
                "Punto di incontro: " + puntoDiIncontro + "\n" +
                "Periodo: " + getDataInizio() + " → " + getDataFine() + "\n" +
                "Giorni programmabili: " + getGiorniProgrammabili() + "\n" +
                "Ora inizio: " + getOraInizio() + "\n" +
                "Durata: " + (getDurata() == null ? "0" : getDurata().toMinutes()) + " minuti\n";
    }

    @Override
    public String toString() {
        return "Visita:\n" +
                "ID tipo visita: " + getIdTipoVisita() + "\n" +
                "Attiva: " + (attivo ? "SÌ" : "NO") + "\n" +
                "Titolo: " + titolo + "\n" +
                "Descrizione: " + descrizione + "\n" +
                "Punto di incontro: " + puntoDiIncontro + "\n" +
                "Periodo: " + getDataInizio() + " → " + getDataFine() + "\n" +
                "Giorni programmabili: " + getGiorniProgrammabili() + "\n" +
                "Ora inizio: " + getOraInizio() + "\n" +
                "Durata: " + (getDurata() == null ? "0" : getDurata().toMinutes()) + " minuti\n" +
                "Acquisto biglietto: " + (acquistoBiglietto ? "SÌ" : "NO") + "\n" +
                "Partecipanti min: " + numMinPartecipanti + "\n" +
                "Partecipanti max: " + numMaxPartecipanti + "\n" +
                "Volontari: " + formattaVolontari();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Visita)) return false;
        Visita visita = (Visita) o;
        return Objects.equals(getIdTipoVisita(), visita.getIdTipoVisita());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdTipoVisita());
    }

    public String getTitolo() { return titolo; }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
        this.idTipoVisita = generaIdTipoVisita(this.idLuogo, this.titolo);
    }

    public String getIdLuogo() { return idLuogo; }

    public void setIdLuogo(String idLuogo) {
        this.idLuogo = idLuogo;
        this.idTipoVisita = generaIdTipoVisita(this.idLuogo, this.titolo);
    }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public PuntoIncontro getPuntoDiIncontro() { return puntoDiIncontro; }
    public void setPuntoDiIncontro(PuntoIncontro puntoDiIncontro) { this.puntoDiIncontro = puntoDiIncontro; }

    public ArrayList<String> getVolontari() { return volontari; }
    public void setVolontari(ArrayList<String> volontari) {
        this.volontari = (volontari != null) ? volontari : new ArrayList<>();
    }

    public boolean isAcquistoBiglietto() { return acquistoBiglietto; }
    public void setAcquistoBiglietto(boolean acquistoBiglietto) { this.acquistoBiglietto = acquistoBiglietto; }

    public int getNumMinPartecipanti() { return numMinPartecipanti; }
    public void setNumMinPartecipanti(int numMinPartecipanti) { this.numMinPartecipanti = numMinPartecipanti; }

    public int getNumMaxPartecipanti() { return numMaxPartecipanti; }
    public void setNumMaxPartecipanti(int numMaxPartecipanti) { this.numMaxPartecipanti = numMaxPartecipanti; }

    public SchedulingVisita getScheduling() { return scheduling; }
    public void setScheduling(SchedulingVisita scheduling) { this.scheduling = scheduling; }

    public LocalDate getDataInizio() { return scheduling != null ? scheduling.dataInizio() : null; }
    public void setDataInizio(LocalDate dataInizio) {
        if (scheduling == null) return;
        scheduling = new SchedulingVisita(dataInizio, getDataFine(), getOraInizio(), getDurata(), getGiorniProgrammabili());
    }

    public LocalDate getDataFine() { return scheduling != null ? scheduling.dataFine() : null; }
    public void setDataFine(LocalDate dataFine) {
        if (scheduling == null) return;
        scheduling = new SchedulingVisita(getDataInizio(), dataFine, getOraInizio(), getDurata(), getGiorniProgrammabili());
    }

    public LocalTime getOraInizio() { return scheduling != null ? scheduling.oraInizio() : null; }
    public void setOraInizio(LocalTime oraInizio) {
        if (scheduling == null) return;
        scheduling = new SchedulingVisita(getDataInizio(), getDataFine(), oraInizio, getDurata(), getGiorniProgrammabili());
    }

    public Duration getDurata() { return scheduling != null ? scheduling.durata() : null; }
    public void setDurata(Duration durata) {
        if (scheduling == null) return;
        scheduling = new SchedulingVisita(getDataInizio(), getDataFine(), getOraInizio(), durata, getGiorniProgrammabili());
    }

    public List<GiorniSettimana> getGiorniProgrammabili() {
        return scheduling != null ? scheduling.giorniProgrammabili() : List.of();
    }

    public void setGiorniProgrammabili(ArrayList<GiorniSettimana> giorniProgrammabili) {
        if (scheduling == null) return;
        scheduling = new SchedulingVisita(getDataInizio(), getDataFine(), getOraInizio(), getDurata(), giorniProgrammabili);
    }
}