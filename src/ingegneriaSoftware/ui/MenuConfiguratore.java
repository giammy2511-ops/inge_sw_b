package ingegneriaSoftware.ui;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ingegneriaSoftware.controller.GestoreCalendario;
import ingegneriaSoftware.controller.GestoreDati;
import ingegneriaSoftware.domain.GiorniSettimana;
import ingegneriaSoftware.domain.Luogo;
import ingegneriaSoftware.domain.PuntoIncontro;
import ingegneriaSoftware.domain.SchedulingVisita;
import ingegneriaSoftware.domain.StatiVisita;
import ingegneriaSoftware.domain.StatoSistema;
import ingegneriaSoftware.domain.Utente;
import ingegneriaSoftware.domain.Visita;
import ingegneriaSoftware.domain.VisitaIstanza;
import ingegneriaSoftware.domain.dati.DatiGenerali;
import ingegneriaSoftware.domain.dati.FileIO;
import it.unibs.fp.mylib.InputDati;
import it.unibs.fp.mylib.MyMenu;

/*
Gestisce l’interazione da console per l’utente configuratore.

Il configuratore governa la configurazione iniziale e la gestione generale:
- setup del sistema (creazione di luoghi e prime visite)
- gestione di date precluse
- modifica parametri generali (numero massimo di iscritti per fruitore)
- consultazione di elenchi (volontari associati, luoghi, tipi visita)

In questa versione viene gestito anche il ciclo operativo mensile:
- chiusura raccolta disponibilità
- produzione del piano visite
- gestione richieste di aggiunta/rimozione con cascata
- riapertura della raccolta per il mese successivo
- consultazione stato delle visite e visualizzazione del piano prodotto

Invarianti di classe:
- gestoreDati è il punto di accesso ai dati e alle operazioni applicative
- le operazioni di creazione visita rispettano i vincoli di non sovrapposizione nel luogo
- il setup iniziale richiede che ogni luogo creato abbia almeno una visita associata
*/
public class MenuConfiguratore {

    private GestoreDati gestoreDati;

    private List<Utente> utenti() { return gestoreDati.getUtenti(); }
    private List<Luogo> luoghi() { return gestoreDati.getLuoghi(); }
    private FileIO fileIO() { return gestoreDati.getFileIO(); }
    private GestoreCalendario calendario() { return gestoreDati.getGestoreCalendario(); }
    private Set<LocalDate> datePrecluse() { return gestoreDati.getDatePrecluse(); }

    public MenuConfiguratore(GestoreDati gestoreDati) {
        this.gestoreDati = gestoreDati;
    }

    public void menu() {
        while (true) {
            boolean inizializzato = sistemaInizializzato();

            List<String> voci = new ArrayList<>();
            voci.add("Menu generale");
            if (!inizializzato) {
                voci.add("Setup iniziale");
            } else {
                voci.add("Attività");
                try {
                    gestoreDati.caricaOInizializzaStatoSistema();
                } catch (IOException e) {
                    System.out.println("Errore caricando stato sistema.");
                    e.printStackTrace();
                }
            }

            MyMenu menuPrincipale = new MyMenu("Menù configuratore", voci.toArray(new String[0]));
            int scelta = menuPrincipale.scegli();
            if (scelta == 0) break;

            if (!inizializzato) {
                switch (scelta) {
                    case 1 -> menuGenerale();
                    case 2 -> menuSetupIniziale();
                    default -> System.out.println("Scelta non disponibile");
                }
            } else {
                switch (scelta) {
                    case 1 -> menuGenerale();
                    case 2 -> menuAttivita();
                    default -> System.out.println("Scelta non disponibile");
                }
            }
        }
    }

    private boolean sistemaInizializzato() {
        if (luoghi() == null || luoghi().isEmpty()) return false;
        for (Luogo l : luoghi()) {
            if (l != null && l.getVisite() != null && !l.getVisite().isEmpty()) return true;
        }
        return false;
    }

    private void menuSetupIniziale() {
        String[] voci = {
                "Crea luogo + prima visita (obbligatoria)",
                "Crea visita (associa subito a luogo esistente o nuovo)"
        };
        MyMenu menu = new MyMenu("Setup iniziale", voci);

        int scelta;
        while ((scelta = menu.scegli()) != 0) {
            try {
                switch (scelta) {
                    case 1 -> creaLuogoConVisitaObbligatoria();
                    case 2 -> creaVisitaConAssociazioneObbligatoria();
                    default -> System.out.println("Scelta non disponibile");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void menuGenerale() {
        String[] vociMenu = {
                "Modificare numero max iscrivibili da fruitore",
                "Visualizzare elenco volontari con visite associate",
                "Visualizzare elenco luoghi visitabili",
                "Visualizzare l'elenco dei tipi di visita",
                "Visualizzare stato delle visite",
                "Visualizzare piano visite prodotto",
                "visualizza archivio storico"
        };

        MyMenu menu = new MyMenu("Menù generale", vociMenu);
        int scelta;

        do {
            scelta = menu.scegli();
            switch (scelta) {
                case 1 -> modificaNumeroMaxIscrivibili();
                case 2 -> mostraElencoVolontariAssociatiVisita();
                case 3 -> mostraElencoLuoghi();
                case 4 -> mostraElencoTipiVisita();
                case 5 -> mostraStatoVisite();
                case 6 -> visualizzaPianoProdotto();
                case 7 -> stampaArchivio();
                case 0 -> { }
                default -> System.out.println("Scelta non disponibile");
            }
        } while (scelta != 0);
    }

    private void menuAttivita() {
        try {
            StatoSistema stato = gestoreDati.caricaOInizializzaStatoSistema();

            YearMonth ymMeseCorrente = stato.getMeseRaccolta();
            YearMonth ymMeseSuccessivo = ymMeseCorrente.plusMonths(1);
            LocalDate meseCorrente = ymMeseCorrente.atDay(1);
            LocalDate meseSuccessivo = ymMeseSuccessivo.atDay(1);

            String nomeMeseCorrente = gestoreDati.nomeMeseMaiuscoloIt(meseCorrente);
            String nomeMeseSuccessivo = gestoreDati.nomeMeseMaiuscoloIt(meseSuccessivo);

            String[] voci = {
                    "Chiudi raccolta disponibilità (" + nomeMeseCorrente + ")",
                    "Produci piano visite (" + nomeMeseCorrente + ")",
                    "Gestisci richieste di aggiunta/rimozione dati",
                    "Riapri raccolta disponibilità (" + nomeMeseSuccessivo + ")",
                    "Inserimento date precluse"
            };

            MyMenu menu = new MyMenu("menù attività", voci);

            int scelta;
            while ((scelta = menu.scegli()) != 0) {
                switch (scelta) {
                    case 1 -> chiudiRaccoltaDisponibilita();
                    case 2 -> produciPianoVisite();
                    case 3 -> menuModifiche();
                    case 4 -> riapriRaccoltaMeseSuccessivo();
                    case 5 -> inserimentoDatePrecluse();
                    default -> System.out.println("Scelta non disponibile");
                }
            }

        } catch (IOException e) {
            System.out.println("Errore nel caricamento dello stato del sistema.");
            e.printStackTrace();
        }
    }

    private void menuModifiche() {
        try {
            StatoSistema stato = gestoreDati.caricaOInizializzaStatoSistema();

            if (!stato.isPianoProdotto()) {
                System.out.println("Prima devi produrre il piano per " + stato.getMeseRaccolta() + ".");
                return;
            }

            String[] voci = {
                    "Aggiungi luogo",
                    "Aggiungi tipo visita",
                    "Rimuovi luogo (con cascata)",
                    "Rimuovi tipo visita (con cascata)",
                    "Rimuovi volontario (con cascata)"
            };

            MyMenu menu = new MyMenu("Richieste di aggiunta/rimozione", voci);

            int scelta;
            while ((scelta = menu.scegli()) != 0) {
                switch (scelta) {
                    case 1 -> creaLuogoConVisitaObbligatoria();
                    case 2 -> creaVisitaConAssociazioneObbligatoria();
                    case 3 -> richiestaRimozioneLuogo();
                    case 4 -> richiestaRimozioneTipoVisita();
                    case 5 -> richiestaRimozioneVolontario();
                    default -> System.out.println("Scelta non disponibile");
                }
            }

        } catch (IOException e) {
            System.out.println("Errore.");
            e.printStackTrace();
        }
    }

    private void chiudiRaccoltaDisponibilita() {
        try {
            StatoSistema stato = gestoreDati.caricaOInizializzaStatoSistema();

            if (!stato.isRaccoltaAperta()) {
                System.out.println("Raccolta già chiusa per " + stato.getMeseRaccolta() + ".");
                return;
            }

            if (stato.isPianoProdotto()) {
                System.out.println("Piano già prodotto per " + stato.getMeseRaccolta() + ". Non ha senso chiudere ora.");
                return;
            }

            boolean nessunaDisponibilita = gestoreDati.getDisponibilitaVolontari()
                    .values()
                    .stream()
                    .allMatch(Set::isEmpty);

            if (nessunaDisponibilita) {
                System.out.println("ATTENZIONE: non è stata inserita alcuna disponibilità dai volontari.");

                boolean conferma = InputDati.yesOrNo(
                        "Vuoi chiudere comunque la raccolta delle disponibilità?"
                );

                if (!conferma) {
                    System.out.println("Operazione annullata. Raccolta ancora aperta.");
                    return;
                }
            }

            gestoreDati.chiudiRaccoltaDisponibilita();
            StatoSistema dopo = gestoreDati.caricaOInizializzaStatoSistema();

            System.out.println("Raccolta disponibilità CHIUSA per " + dopo.getMeseRaccolta() + ".");

        } catch (IOException e) {
            System.out.println("Errore nel salvataggio/caricamento dello stato del sistema.");
            e.printStackTrace();
        }
    }

    private void produciPianoVisite() {
        try {
            StatoSistema stato = gestoreDati.caricaOInizializzaStatoSistema();

            if (stato.isRaccoltaAperta()) {
                System.out.println("Prima devi CHIUDERE la raccolta disponibilità.");
                return;
            }
            if (stato.isPianoProdotto()) {
                System.out.println("Piano già prodotto per " + stato.getMeseRaccolta() + ".");
                return;
            }

            List<VisitaIstanza> istanze = gestoreDati.produciPianoVisite();
            System.out.println("Piano prodotto per " + stato.getMeseRaccolta() + ": create " + istanze.size() + " visite proposte.");

        } catch (IOException e) {
            System.out.println("Errore durante la produzione del piano.");
            e.printStackTrace();
        }
    }

    private void riapriRaccoltaMeseSuccessivo() {
        try {
            StatoSistema stato = gestoreDati.caricaOInizializzaStatoSistema();

            if (!stato.isPianoProdotto()) {
                System.out.println("Non puoi riaprire la raccolta: prima devi produrre il piano per " + stato.getMeseRaccolta() + ".");
                return;
            }

            gestoreDati.riapriRaccoltaMeseSuccessivo();

        } catch (IOException e) {
            System.out.println("Errore nel salvataggio/caricamento dello stato del sistema.");
            e.printStackTrace();
        }
    }

    private void richiestaRimozioneLuogo() {
        List<Luogo> attivi = luoghi().stream()
                .filter(l -> l != null)
                .toList();

        if (attivi.isEmpty()) {
            System.out.println("Non ci sono luoghi attivi.");
            return;
        }

        String[] nomi = attivi.stream().map(Luogo::getNome).toArray(String[]::new);
        MyMenu menu = new MyMenu("Seleziona luogo da rimuovere (0 = annulla)", nomi);

        int scelta = menu.scegli();
        if (scelta == 0) return;

        Luogo sel = attivi.get(scelta - 1);
        String idLuogo = sel.getId();
        String nomeLuogo = sel.getNome();

        boolean conferma = InputDati.yesOrNo(
                "ATTENZIONE: la rimozione del luogo \"" + nomeLuogo +
                        "\" comporterà l'eliminazione di tutti i tipi di visita associati,\n" +
                        "e potenzialmente di volontari e altri luoghi per effetto cascata.\n" +
                        "Confermi la rimozione?"
        );

        if (!conferma) {
            System.out.println("Rimozione annullata.");
            return;
        }

        try {
            gestoreDati.rimuoviLuogoConCascata(idLuogo);
            System.out.println("Rimozione luogo completata (con cascata).");
        } catch (IOException e) {
            System.out.println("Errore durante rimozione luogo.");
            e.printStackTrace();
        }
    }

    private void richiestaRimozioneTipoVisita() {
        if (calendario().getVisite() == null) {
            System.out.println("Non ci sono tipi visita.");
            return;
        }

        List<Visita> attive = calendario().getVisite().stream()
                .filter(v -> v != null && v.isAttivo())
                .toList();

        if (attive.isEmpty()) {
            System.out.println("Non ci sono tipi visita attivi.");
            return;
        }

        String[] etichette = attive.stream()
                .map(v -> v.getIdLuogo() + " :: " + v.getTitolo())
                .toArray(String[]::new);

        MyMenu menu = new MyMenu("Seleziona tipo visita da rimuovere (0 = annulla)", etichette);

        int scelta = menu.scegli();
        if (scelta == 0) return;

        Visita sel = attive.get(scelta - 1);
        String idTipo = sel.getIdTipoVisita();
        String titolo = sel.getTitolo();

        boolean conferma = InputDati.yesOrNo(
                "ATTENZIONE: la rimozione del tipo di visita \"" + titolo + "\"\n" +
                        "comporterà l'eliminazione dell'associazione col luogo e coi volontari,\n" +
                        "e potrebbe causare la rimozione di luoghi e/o volontari per effetto cascata.\n" +
                        "Confermi la rimozione?"
        );

        if (!conferma) {
            System.out.println("Rimozione annullata.");
            return;
        }

        try {
            gestoreDati.rimuoviTipoVisitaConCascata(idTipo);
            System.out.println("Rimozione tipo visita completata (con cascata).");
        } catch (IOException e) {
            System.out.println("Errore durante rimozione tipo visita.");
            e.printStackTrace();
        }
    }

    private void richiestaRimozioneVolontario() {
        List<Utente> volontari = utenti().stream()
                .filter(u -> u != null
                        && "volontario".equals(u.getRuolo()))
                .toList();

        if (volontari.isEmpty()) {
            System.out.println("Non ci sono volontari.");
            return;
        }

        String[] nomi = volontari.stream().map(Utente::getNomeUtente).toArray(String[]::new);
        MyMenu menu = new MyMenu("Seleziona volontario da rimuovere (0 = annulla)", nomi);

        int scelta = menu.scegli();
        if (scelta == 0) return;

        String nick = volontari.get(scelta - 1).getNomeUtente();

        boolean conferma = InputDati.yesOrNo(
                "ATTENZIONE: la rimozione del volontario \"" + nick + "\"\n" +
                        "comporterà l'eliminazione della sua associazione a tutti i tipi di visita.\n" +
                        "Se alcuni tipi di visita resteranno senza volontari, verranno eliminati,\n" +
                        "e ciò potrebbe causare la rimozione di luoghi per effetto cascata.\n" +
                        "Confermi la rimozione?"
        );

        if (!conferma) {
            System.out.println("Rimozione annullata.");
            return;
        }

        try {
            gestoreDati.rimuoviVolontarioConCascata(nick);
            System.out.println("Rimozione volontario completata (con cascata).");
        } catch (IOException e) {
            System.out.println("Errore durante rimozione volontario.");
            e.printStackTrace();
        }
    }

    private void inserimentoDatePrecluse() {
        YearMonth meseCorrente = YearMonth.now();
        YearMonth meseBase = (LocalDate.now().getDayOfMonth() >= 16) ? meseCorrente : meseCorrente.minusMonths(1);
        YearMonth ymPrecluse = meseBase.plusMonths(3);

        LocalDate mesePrecluse = ymPrecluse.atDay(1);

        MyMenu menu = new MyMenu(
                "menù date precluse per " + gestoreDati.nomeMeseMaiuscoloIt(mesePrecluse) + " " + ymPrecluse.getYear(),
                new String[]{"aggiunta data preclusa", "visualizza date precluse"}
        );

        int scelta;
        while ((scelta = menu.scegli()) != 0) {
            if (scelta == 1) {
                int giorno = InputDati.leggiIntero("Inserire giorno", 1, ymPrecluse.lengthOfMonth());
                LocalDate data = ymPrecluse.atDay(giorno);

                if (datePrecluse().contains(data)) {
                    System.out.println("Data già preclusa.");
                    continue;
                }

                datePrecluse().add(data);
                try {
                    fileIO().appendDataPreclusa(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                List<LocalDate> filtrate = datePrecluse().stream()
                        .filter(d -> YearMonth.from(d).equals(ymPrecluse))
                        .sorted()
                        .toList();

                if (filtrate.isEmpty()) {
                    System.out.println("Non ci sono date precluse per " + ymPrecluse + ".");
                } else {
                    filtrate.forEach(System.out::println);
                }
            }
        }
    }

    private void modificaNumeroMaxIscrivibili() {
        try {
            DatiGenerali dati = fileIO().leggiDatiGenerali();
            int nuovoMax = InputDati.leggiIntero("Numero max persone iscrivibili da un fruitore: ");
            fileIO().salvaDatiGenerali(new DatiGenerali(dati.ambitoTerritoriale(), nuovoMax));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostraElencoVolontariAssociatiVisita() {
        Map<String, List<String>> mappa = new HashMap<>();

        for (Utente u : utenti()) {
            if (u != null && "volontario".equals(u.getRuolo()) && u.isAttivo()) {
                mappa.put(u.getNomeUtente(), new ArrayList<>());
            }
        }

        if (calendario().getVisite() == null) {
            System.out.println("Non ci sono tipi visita.");
            return;
        }

        for (Visita v : calendario().getVisite()) {
            if (v == null || !v.isAttivo()) continue;

            for (String nickname : v.getVolontari()) {
                List<String> lista = mappa.get(nickname);
                if (lista != null) lista.add(v.getTitolo());
            }
        }

        if (mappa.isEmpty()) {
            System.out.println("Non ci sono volontari attivi.");
            return;
        }

        mappa.forEach((k, v) -> {
            if (v.isEmpty()) System.out.println("- " + k + " (nessun tipo visita attivo)");
            else System.out.println("- " + k + ": " + String.join(", ", v));
        });
    }

    private void mostraElencoLuoghi() {
        List<Luogo> attivi = luoghi().stream().filter(Luogo::isAttivo).toList();

        if (attivi.isEmpty()) {
            System.out.println("Non esistono luoghi attivi.");
            return;
        }
        attivi.forEach(l -> System.out.println("- " + l.getNome()));
    }

    private void mostraElencoTipiVisita() {
        boolean trovata = false;

        for (Luogo l : luoghi()) {
            if (l == null || !l.isAttivo()) continue;

            List<Visita> visite = l.getVisite();
            if (visite == null) continue;

            List<Visita> attive = visite.stream().filter(Visita::isAttivo).toList();
            if (attive.isEmpty()) continue;

            trovata = true;
            System.out.println(l.getNome());
            for (Visita v : attive) {
                System.out.println("  - " + v.toStringSemplificato());
            }
            System.out.println();
        }

        if (!trovata) {
            System.out.println("Non esistono tipi di visita attivi.");
        }
    }

    private void mostraStatoVisite() {
        if (calendario().getVisite() == null || calendario().getVisite().isEmpty()) {
            System.out.println("Non ci sono visite (nessun tipo di visita configurato).");
            return;
        }

        try {
            List<VisitaIstanza> istanze = gestoreDati.leggiPianoVisite();
            if (istanze == null || istanze.isEmpty()) {
                System.out.println("Lo stato delle visite non è ancora disponibile: il piano non è stato prodotto.");
                return;
            }

            Map<StatiVisita, Long> conteggi = istanze.stream()
                    .filter(i -> i.getStato() != null)
                    .collect(Collectors.groupingBy(VisitaIstanza::getStato, Collectors.counting()));

            if (conteggi.isEmpty()) {
                System.out.println("Esistono istanze di visita, ma non hanno ancora uno stato assegnato.");
                return;
            }

            System.out.println("Stato visite (istanze del piano):");
            for (StatiVisita s : StatiVisita.values()) {
                long n = conteggi.getOrDefault(s, 0L);
                System.out.println("- " + s + ": " + n);
            }

        } catch (IOException e) {
            System.out.println("Errore nel caricamento dello stato delle visite (piano visite).");
            e.printStackTrace();
        }
    }

    private void visualizzaPianoProdotto() {
        try {
            List<VisitaIstanza> istanze = gestoreDati.leggiPianoVisite();

            if (istanze == null || istanze.isEmpty()) {
                System.out.println("Il piano visite è vuoto.");
                return;
            }

            istanze.stream()
                    .sorted((a, b) -> {
                        int c = a.getData().compareTo(b.getData());
                        if (c != 0) return c;
                        return a.getIdTipoVisita().compareToIgnoreCase(b.getIdTipoVisita());
                    })
                    .forEach(vi -> System.out.println(
                            vi.getData() + " | " +
                                    vi.getIdTipoVisita() + " | volontario: " +
                                    vi.getNomeVolontario() + " | stato: " +
                                    vi.getStato()
                    ));

        } catch (IOException e) {
            System.out.println("Errore nel caricamento del piano prodotto.");
            e.printStackTrace();
        }
    }

    private void creaLuogoConVisitaObbligatoria() throws IOException {
        Luogo nuovoLuogo = creaLuogo();
        gestoreDati.aggiungiLuogo(nuovoLuogo);

        while (true) {
            Visita visita = creaVisita(nuovoLuogo.getId());
            if (visita == null) {
                luoghi().remove(nuovoLuogo);
                fileIO().salvaLuoghi(luoghi());
                return;
            }

            boolean ok = gestoreDati.aggiungiVisitaALuogo(nuovoLuogo, visita);
            if (ok) {
                System.out.println("Luogo e prima visita inseriti correttamente.");
                return;
            }

            if (!menuOverlap()) {
                System.out.println("Operazione annullata.");
                luoghi().remove(nuovoLuogo);
                fileIO().salvaLuoghi(luoghi());
                return;
            }

            System.out.println("Reinserisci SOLO i parametri di scheduling.");
            aggiornaScheduling(visita);
        }
    }

    private void creaVisitaConAssociazioneObbligatoria() throws IOException {
        String[] voci = {
                "Associa a luogo esistente",
                "Crea nuovo luogo e associa la visita"
        };
        MyMenu menu = new MyMenu("Creazione visita", voci);

        int scelta;
        while ((scelta = menu.scegli()) != 0) {
            switch (scelta) {
                case 1 -> {
                    if (luoghi().isEmpty()) {
                        System.out.println("Non esistono luoghi. Devi creare un nuovo luogo con la visita.");
                        creaLuogoConVisitaObbligatoria();
                        return;
                    }
                    Luogo luogoScelto = scegliLuogoEsistente();
                    if (luogoScelto == null) {
                        break; 
                    }
                    Visita visita = creaVisita(luogoScelto.getId());

                    while (true) {
                        boolean ok = gestoreDati.aggiungiVisitaALuogo(luogoScelto, visita);
                        if (ok) {
                            System.out.println("Visita inserita correttamente.");
                            return;
                        }

                        if (!menuOverlap()) {
                            System.out.println("Creazione visita annullata.");
                            return;
                        }

                        System.out.println("Reinserisci SOLO i parametri di scheduling.");
                        aggiornaScheduling(visita);
                    }
                }
                case 2 -> creaLuogoConVisitaObbligatoria();
                default -> System.out.println("Scelta non disponibile");
            }
        }
    }

    private Luogo scegliLuogoEsistente() {
        List<Luogo> lista = luoghi();

        String[] etichette = lista.stream()
                .map(l -> l.getNome())
                .toArray(String[]::new);

        MyMenu menu = new MyMenu("Scegli un luogo (0 = indietro)", etichette);
        int scelta = menu.scegli();
        if (scelta == 0) return null;

        Luogo scelto = lista.get(scelta - 1);

        if (!scelto.isAttivo()) {
            scelto.setAttivo(true);
            try {
                fileIO().salvaLuoghi(luoghi());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return scelto;
    }

    private Luogo creaLuogo() {
        while (true) {
            HashMap<String, Integer> coordinate = new HashMap<>();

            String nome = InputDati.leggiStringaNonVuota("Nome luogo: ").trim();
            String descrizione = InputDati.leggiStringaNonVuota("Descrizione luogo: ");

            int lat = InputDati.leggiIntero("Latitudine (-90,90): ", -90, 90);
            int lon = InputDati.leggiIntero("Longitudine (-180,180): ", -180, 180);

            coordinate.put("latitudine", lat);
            coordinate.put("longitudine", lon);

            Luogo nuovo = new Luogo(nome, descrizione, coordinate);

            boolean idEsistente = false;
            for (Luogo l : luoghi()) {
                if (l.getId().equals(nuovo.getId())) {
                    idEsistente = true;
                    break;
                }
            }

            if (idEsistente) {
                System.out.println("Esiste già un luogo con lo stesso nome e coordinate. Reinserisci i dati.");
                continue;
            }

            return nuovo;
        }
    }

    private Visita creaVisita(String idLuogo) {
        String titolo = InputDati.leggiStringaNonVuota("Titolo visita: ").trim();
        while (gestoreDati.esisteVisitaConTitoloNelLuogo(idLuogo, titolo)) {
            titolo = InputDati.leggiStringaNonVuota("Titolo già usato, reinserire: ").trim();
        }

        String descrizione = InputDati.leggiStringaNonVuota("Descrizione: ");
        PuntoIncontro p = chiediPuntoIncontro();

        int min = InputDati.leggiIntero("Min partecipanti: ");
        int max = InputDati.leggiInteroConMinimo("Max partecipanti >= " + min, min);

        boolean biglietto = InputDati.yesOrNo("Biglietto acquistabile?");
        ArrayList<String> volontari = menuSceltaVolontari();

        SchedulingVisita scheduling = chiediScheduling(LocalDate.now());
        return new Visita(idLuogo, titolo, descrizione, p, scheduling, min, max, volontari, biglietto);
    }

    private SchedulingVisita chiediScheduling(LocalDate minDataInizio) {
        LocalDate inizio = chiediData("Data inizio", minDataInizio);
        LocalDate fine = chiediData("Data fine", inizio);
        Duration durata = Duration.ofMinutes(InputDati.leggiIntero("Durata (min): "));
        LocalTime ora = chiediOra();
        ArrayList<GiorniSettimana> giorni = menuSceltaGiorniSettimana(inizio, fine);
        return new SchedulingVisita(inizio, fine, ora, durata, giorni);
    }

    private void aggiornaScheduling(Visita visita) {
        SchedulingVisita nuovo = chiediScheduling(LocalDate.now());
        visita.setScheduling(nuovo);
    }

    private ArrayList<GiorniSettimana> menuSceltaGiorniSettimana(LocalDate dataInizio, LocalDate dataFine) {
        ArrayList<GiorniSettimana> giorniNelRange = new ArrayList<>();
        for (LocalDate d = dataInizio; !d.isAfter(dataFine); d = d.plusDays(1)) {
            GiorniSettimana g = GiorniSettimana.valueOf(d.getDayOfWeek().name());
            if (!giorniNelRange.contains(g)) giorniNelRange.add(g);
        }

        ArrayList<GiorniSettimana> scelti = new ArrayList<>();
        GiorniSettimana[] ordineFisso = GiorniSettimana.values();
        boolean almenoUno = false;

        while (true) {
            ArrayList<GiorniSettimana> disponibili = new ArrayList<>();
            ArrayList<String> etichette = new ArrayList<>();

            for (GiorniSettimana g : ordineFisso) {
                if (giorniNelRange.contains(g) && !scelti.contains(g)) {
                    disponibili.add(g);
                    etichette.add(g.getDescrizione());
                }
            }

            if (disponibili.isEmpty()) break;

            MyMenu menu = new MyMenu("Menù scelta giorni (0 = termina)", etichette.toArray(new String[0]));
            int scelta = menu.scegli();

            if (scelta == 0) {
                if (almenoUno) break;
                System.out.println("Devi selezionare almeno un giorno.");
                continue;
            }

            GiorniSettimana giornoSelezionato = disponibili.get(scelta - 1);
            scelti.add(giornoSelezionato);
            almenoUno = true;
        }

        return scelti;
    }

    private ArrayList<String> menuSceltaVolontari() {
        ArrayList<String> volontariScelti = new ArrayList<>();
        boolean almenoUno = false;

        while (true) {
            String[] giaScelti = volontariScelti.toArray(new String[0]);
            ArrayList<Utente> disponibiliUtenti = new ArrayList<>();
            ArrayList<String> etichette = new ArrayList<>();

            for (Utente u : utenti()) {
                if (u == null) continue;
                if (!"volontario".equals(u.getRuolo())) continue;
                if (Arrays.asList(giaScelti).contains(u.getNomeUtente())) continue;

                disponibiliUtenti.add(u);
                etichette.add(u.getNomeUtente());
            }

            ArrayList<String> opzioni = new ArrayList<>(etichette);
            opzioni.add("Aggiungi nuovo volontario");

            MyMenu menu = new MyMenu("Menù scelta volontari (0 = termina)", opzioni.toArray(new String[0]));
            int scelta = menu.scegli();

            if (scelta == 0) {
                if (almenoUno) break;
                System.out.println("Devi selezionare almeno un volontario.");
                continue;
            }

            if (scelta == opzioni.size()) {
                Utente nuovo = inserimentoCredenzialiNuovoVolontario();
                if (nuovo != null) {
                    volontariScelti.add(nuovo.getNomeUtente());
                    almenoUno = true;
                }
                continue;
            }

            Utente selezionato = disponibiliUtenti.get(scelta - 1);

            if (!selezionato.isAttivo()) {
                selezionato.setAttivo(true);
                try {
                    fileIO().salvaUtenti(utenti());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            volontariScelti.add(selezionato.getNomeUtente());
            almenoUno = true;
        }

        return volontariScelti;
    }

    private Utente inserimentoCredenzialiNuovoVolontario() {
        String username = InputDati.leggiStringaNonVuota("Username nuovo volontario: ");
        while (gestoreDati.esisteNomeUtente(username)) {
            System.out.println("Username già esistente. Scegline un altro.");
            username = InputDati.leggiStringaNonVuota("Username nuovo volontario: ");
        }

        String passwordPredefinita = InputDati.leggiStringaNonVuota("Password predefinita: ");

        try {
            Utente nuovo = gestoreDati.creaVolontario(username, passwordPredefinita);
            System.out.println("Volontario creato! Al primo accesso dovrà cambiare le credenziali.");
            return nuovo;
        } catch (IOException e) {
            System.out.println("Errore nel salvataggio delle credenziali.");
            e.printStackTrace();
            return null;
        }
    }

    private boolean menuOverlap() {
        MyMenu menuOverlap = new MyMenu(
                "OVERLAP rilevato: la visita si sovrappone ad un'altra visita dello stesso luogo.\n" +
                        "Scegli cosa fare (0 = annulla creazione visita):",
                new String[]{"Modifica parametri (date/ora/durata/giorni) e riprova"}
        );

        int scelta = menuOverlap.scegli();
        return scelta != 0;
    }

    private PuntoIncontro chiediPuntoIncontro() {
        String via = InputDati.leggiStringaNonVuota("Via/Piazza: ");
        String civico = InputDati.leggiStringa("Numero civico: ");
        String descrizione = InputDati.leggiStringa("Descrizione: ");
        return new PuntoIncontro(via, civico, descrizione);
    }

    private LocalDate chiediData(String msg, LocalDate min) {
        while (true) {
            try {
                LocalDate d = LocalDate.parse(InputDati.leggiStringa(msg + " (YYYY-MM-DD): "));
                if (min != null && d.isBefore(min)) {
                    System.out.println("La data deve essere uguale o successiva a " + min + ".");
                    continue;
                }
                return d;
            } catch (DateTimeParseException e) {
                System.out.println("Formato non valido. Usa YYYY-MM-DD.");
            }
        }
    }

    private LocalTime chiediOra() {
        while (true) {
            try {
                return LocalTime.parse(InputDati.leggiStringa("Ora (HH:mm): "));
            } catch (DateTimeParseException e) {
                System.out.println("Formato non valido.");
            }
        }
    }
    
    private void stampaArchivio() {
    	try {
    		System.out.println(this.gestoreDati.getFileIO().leggiArchivioStorico().toString());			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}