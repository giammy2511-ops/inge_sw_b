package ingegneriaSoftware;

import java.util.ArrayList;
import java.util.List;

/*
Rappresenta un contenitore per la memorizzazione storica di informazioni
che non devono più essere trattate come “correnti”.

Nelle versioni precedenti l’archivio manteneva solo i tipi di visita scaduti.
In questa versione viene esteso per includere anche le istanze di visita
effettivamente svolte, che devono essere conservate a fini storici.

Invarianti di classe:
- le liste gestite dall’archivio non sono mai nulle durante l’uso della classe
- i tipi di visita scaduti non sono più utilizzabili per la pianificazione
- le istanze di visita effettuate rappresentano visite concluse
*/
public class ArchivioStorico {

    private List<VisitaIstanza> istanzeEffettuate = new ArrayList<>();
    private List<Visita> tipiVisitaScaduti = new ArrayList<>();

    public ArchivioStorico() {}

    public List<VisitaIstanza> getVisiteEffettuate() {
        if (istanzeEffettuate == null) istanzeEffettuate = new ArrayList<>();
        return istanzeEffettuate;
    }

    public void setVisiteEffettuate(List<VisitaIstanza> istanzeEffettuate) {
        this.istanzeEffettuate = (istanzeEffettuate != null)
                ? istanzeEffettuate
                : new ArrayList<>();
    }

    public List<Visita> getTipiVisitaScaduti() {
        if (tipiVisitaScaduti == null) tipiVisitaScaduti = new ArrayList<>();
        return tipiVisitaScaduti;
    }

    public void setTipiVisitaScaduti(List<Visita> tipiVisitaScaduti) {
        this.tipiVisitaScaduti = (tipiVisitaScaduti != null)
                ? tipiVisitaScaduti
                : new ArrayList<>();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ArchivioStorico{\n");

        List<Visita> tipi = getTipiVisitaScaduti();
        sb.append("  numTipiVisitaScaduti=")
          .append(tipi.size())
          .append("\n");

        for (Visita v : tipi) {
            sb.append("    - ").append(v).append("\n");
        }

        List<VisitaIstanza> istanze = getVisiteEffettuate();
        sb.append("  numVisiteEffettuate=")
          .append(istanze.size())
          .append("\n");

        for (VisitaIstanza vi : istanze) {
            sb.append("    - ").append(vi).append("\n");
        }

        sb.append("}");
        return sb.toString();
    }
}