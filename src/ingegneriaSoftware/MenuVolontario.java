package ingegneriaSoftware;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import it.unibs.fp.mylib.MyMenu;

/*
Gestisce l’interazione da console per un utente con ruolo volontario.

Il periodo di raccolta e il mese di riferimento vengono letti dallo stato del sistema.
Il volontario può inserire disponibilità solo se la raccolta è aperta e solo per date
compatibili con i tipi di visita a cui è associato, escludendo date precluse e già inserite.

Rispetto alla versione precedente, viene aggiunta la possibilità di visualizzare il piano
delle visite associate al volontario.

Invarianti di classe:
- gestoreDati fornisce accesso ai dati e alle operazioni applicative
- volontario rappresenta l’utente autenticato e deve avere ruolo volontario
*/
public class MenuVolontario {

    private final GestoreDati gestoreDati;
    private final Utente volontario;

    private List<Luogo> luoghi() { return gestoreDati.getLuoghi(); }
    private Set<LocalDate> datePrecluse() { return gestoreDati.getDatePrecluse(); }
    private Set<LocalDate> disponibilitaCorrenti() { return gestoreDati.getDisponibilitaVolontario(volontario.getNomeUtente()); }

    public MenuVolontario(GestoreDati gestoreDati, Utente volontario) {
        this.gestoreDati = gestoreDati;
        this.volontario = volontario;
    }

    public void menu() {
        while (true) {
            StatoSistema stato = null;
            try {
                stato = gestoreDati.caricaOInizializzaStatoSistema();
            } catch (IOException e) {
                System.out.println("Errore nel caricamento dello stato del sistema.");
                e.printStackTrace();
            }

            String nome = volontario.getNomeUtente();
            YearMonth ymRaccolta = stato.getMeseRaccolta();
            LocalDate meseRiferimento = ymRaccolta.atDay(1);

            String[] voci = {
                    "Visualizza i tipi di visita associati",
                    "Esprimi disponibilità (" + gestoreDati.nomeMeseMaiuscoloIt(meseRiferimento) + ")",
                    "Visualizza il piano delle visite associate"
            };

            MyMenu menu = new MyMenu("Menù volontario (" + nome + ")", voci);

            int scelta;
            do {
                scelta = menu.scegli();
                switch (scelta) {
                    case 1 -> visualizzaTipiVisitaAssociati();
                    case 2 -> inserisciDisponibilita(stato);
                    case 3 -> visualizzaPianoComeVolontario();
                    case 0 -> { return; }
                    default -> System.out.println("Scelta non disponibile");
                }
            } while (scelta != 0);
        }
    }

    private void visualizzaTipiVisitaAssociati() {
        List<Visita> associate = getTipiVisitaAssociati(volontario.getNomeUtente());
        if (associate.isEmpty()) {
            System.out.println("Non risulti associato ad alcun tipo di visita.");
            return;
        }
        for (Visita visita : associate) {
            System.out.println(visita.toStringSemplificato());
            System.out.println();
        }
    }

    private void inserisciDisponibilita(StatoSistema stato){
        if (stato == null) {
            System.out.println("Stato del sistema non disponibile.");
            return;
        }

        if (!stato.isRaccoltaAperta()) {
            System.out.println("Raccolta disponibilità chiusa.");
            return;
        }

        YearMonth ym = stato.getMeseRaccolta();
        LocalDate meseRiferimento = ym.atDay(1);

        if (!haAlmenoUnTipoProgrammabileNelMese(meseRiferimento)) {
            System.out.println("Non risulti associato ad alcun tipo di visita in questo periodo.");
            return;
        }

        String[] voci = {
                "Aggiungi una data di disponibilità",
                "Visualizza disponibilità inserite",
                "Visualizza date precluse del mese"
        };

        MyMenu menu = new MyMenu("Disponibilità " + gestoreDati.nomeMeseMaiuscoloIt(meseRiferimento) + " " + ym.getYear(), voci);

        int scelta;
        do {
            scelta = menu.scegli();
            switch (scelta) {
                case 1 -> menuSceltaDisponibilitaMese(meseRiferimento);
                case 2 -> stampaDisponibilitaMese(meseRiferimento);
                case 3 -> stampaDatePrecluseMese(meseRiferimento);
                case 0 -> { }
                default -> System.out.println("Scelta non disponibile");
            }
        } while (scelta != 0);
    }

    private boolean haAlmenoUnTipoProgrammabileNelMese(LocalDate data) {
        List<Visita> associati = getTipiVisitaAssociati(volontario.getNomeUtente());
        if (associati.isEmpty()) return false;

        LocalDate first = data.withDayOfMonth(1);
        LocalDate last = data.withDayOfMonth(data.lengthOfMonth());

        for (Visita tipo : associati) {
            for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
                if (tipo.isProgrammabile(d)) return true;
            }
        }
        return false;
    }

    private void menuSceltaDisponibilitaMese(LocalDate data) {
        while (true) {
            List<LocalDate> dateSelezionabili = buildDateSelezionabili(data);

            if (dateSelezionabili.isEmpty()) {
                System.out.println("Non ci sono date selezionabili per " + gestoreDati.nomeMeseMaiuscoloIt(data) + " " + data.getYear() + ".");
                return;
            }

            String[] voci = dateSelezionabili.stream()
                    .map(LocalDate::toString)
                    .toArray(String[]::new);

            MyMenu menu = new MyMenu("Scegli una data di disponibilità (0 = termina)", voci);

            int scelta = menu.scegli();
            if (scelta == 0) return;

            LocalDate sceltaData = dateSelezionabili.get(scelta - 1);

            try {
                gestoreDati.aggiungiDisponibilitaVolontario(volontario.getNomeUtente(), sceltaData);
                System.out.println("Disponibilità aggiunta per: " + sceltaData);
            } catch (IOException e) {
                System.out.println("Errore nel salvataggio delle disponibilità.");
                e.printStackTrace();
                return;
            }
        }
    }

    private List<LocalDate> buildDateSelezionabili(LocalDate data) {
        LocalDate first = data.withDayOfMonth(1);
        LocalDate last = data.withDayOfMonth(data.lengthOfMonth());

        List<LocalDate> result = new ArrayList<>();
        Set<LocalDate> disp = disponibilitaCorrenti();

        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            if (datePrecluse().contains(d)) continue;
            if (disp.contains(d)) continue;
            if (!dataCompatibileConTipiAssociati(volontario.getNomeUtente(), d)) continue;
            result.add(d);
        }
        return result;
    }

    private void stampaDisponibilitaMese(LocalDate data) {
        List<LocalDate> filtrate = disponibilitaCorrenti().stream()
                .filter(d -> d.getMonth() == data.getMonth()
                        && d.getYear() == data.getYear())
                .sorted()
                .toList();

        if (filtrate.isEmpty()) {
            System.out.println("Non ci sono disponibilità inserite per " + gestoreDati.nomeMeseMaiuscoloIt(data) + ".");
            return;
        }

        for (LocalDate d : filtrate) {
            System.out.println(d);
        }
    }

    private void stampaDatePrecluseMese(LocalDate data) {
        List<LocalDate> filtrate = datePrecluse().stream()
                .filter(d -> d.getMonth() == data.getMonth()
                        && d.getYear() == data.getYear())
                .sorted()
                .toList();

        if (filtrate.isEmpty()) {
            System.out.println("Non ci sono date precluse per " + gestoreDati.nomeMeseMaiuscoloIt(data) + ".");
            return;
        }

        for (LocalDate d : filtrate) {
            System.out.println(d);
        }
    }

    private boolean dataCompatibileConTipiAssociati(String nome, LocalDate data) {
        for (Visita visita : getTipiVisitaAssociati(nome)) {
            if (visita.isProgrammabile(data)) return true;
        }
        return false;
    }

    private List<Visita> getTipiVisitaAssociati(String nome) {
        List<Visita> result = new ArrayList<>();

        for (Luogo luogo : luoghi()) {
            if (luogo.getVisite() == null) continue;

            for (Visita visita : luogo.getVisite()) {
                for (String volNome : visita.getVolontari()) {
                    if (volNome.equals(nome)) {
                        result.add(visita);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private void visualizzaPianoComeVolontario() {
        gestoreDati.stampaVisiteConfermatePerVolontario(volontario.getNomeUtente());
    }
}