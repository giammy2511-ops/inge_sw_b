package ingegneriaSoftware.ui;

import java.io.IOException;

import ingegneriaSoftware.controller.GestoreDati;
import ingegneriaSoftware.domain.Utente;
import ingegneriaSoftware.domain.VisitaIstanza;
import ingegneriaSoftware.domain.dati.DatiGenerali;
import it.unibs.fp.mylib.InputDati;
import it.unibs.fp.mylib.MyMenu;

/*
Gestisce l’interazione da console per un utente con ruolo fruitore.

Il menù permette al fruitore di:
- visualizzare le visite disponibili
- iscriversi a una visita proposta rispettando i vincoli di capienza e i dati generali
- visualizzare le proprie visite
- disdire un’iscrizione tramite codice, se consentito dalla fase del sistema

Invarianti di classe:
- gestoreDati è il punto di accesso alle funzioni applicative e alla persistenza
- fruitore è l’utente autenticato e deve avere ruolo fruitore
*/
public class MenuFruitore {

    private final GestoreDati gestoreDati;
    private final Utente fruitore;

    public MenuFruitore(GestoreDati gestoreDati, Utente fruitore) {
        this.gestoreDati = gestoreDati;
        this.fruitore = fruitore;
    }

    public void menu() {
        String[] voci = {
                "Visualizza visite (proposte / confermate / cancellate)",
                "Iscriviti a una visita proposta",
                "Le mie visite (proposte / complete / confermate / cancellate)",
                "Disdici iscrizione (prima della chiusura iscrizioni)"
        };

        MyMenu menu = new MyMenu("Menù fruitore (" + fruitore.getNomeUtente() + ")", voci);

        while (true) {
            int scelta = menu.scegli();
            if (scelta == 0) return;

            switch (scelta) {
                case 1 -> gestoreDati.stampaVisitePerFruitore(null);
                case 2 -> menuIscrizione();
                case 3 -> gestoreDati.stampaVisitePerFruitore(fruitore.getNomeUtente());
                case 4 -> menuDisdetta();
                default -> System.out.println("Scelta non disponibile");
            }
        }
    }

    public static Utente accessoORRegistrazione(GestoreDati gestoreDati) {
        String[] voci = {"Accedi", "Registrati"};
        MyMenu menu = new MyMenu("Fruitore - Accesso", voci);

        while (true) {
            int scelta = menu.scegli();
            if (scelta == 0) return null;

            switch (scelta) {
                case 1 -> {
                    String username = InputDati.leggiStringaNonVuota("Nome utente: ");
                    String password = InputDati.leggiStringaNonVuota("Password: ");

                    Utente u = gestoreDati.autentica(username, password);

                    if (u != null && "fruitore".equals(u.getRuolo())) {
                        return u;
                    }

                    System.out.println("Credenziali errate o non esistenti, riprova o registrati.");
                }
                case 2 -> {
                    while (true) {
                        String username = InputDati.leggiStringaNonVuota("Scegli nome utente: ").trim();

                        if (gestoreDati.esisteNomeUtente(username)) {
                            System.out.println("Username già esistente: " + username);

                            MyMenu m = new MyMenu(
                                    "Username già esistente. Cosa vuoi fare?",
                                    new String[]{"Inserisci un altro username", "Torna al menu Accesso/Registrazione"}
                            );

                            int s = m.scegli();
                            if (s == 0 || s == 2) {
                                break;
                            }
                            continue;
                        }

                        String password = InputDati.leggiStringaNonVuota("Scegli password: ");

                        try {
                            return gestoreDati.creaFruitore(username, password);
                        } catch (IllegalArgumentException e) {
                            System.out.println(e.getMessage());
                        } catch (IOException e) {
                            System.out.println("Errore salvataggio credenziali: " + e.getMessage());
                        }
                    }
                }
                default -> System.out.println("Scelta non disponibile");
            }
        }
    }

    private void menuIscrizione() {
        try {
            var disponibili = gestoreDati.visiteIscrivibili();

            if (disponibili.isEmpty()) {
                System.out.println("Nessuna visita disponibile per l'iscrizione (potrebbero essere chiuse o piene).");
                return;
            }

            String[] voci = disponibili.stream()
                    .map(gestoreDati::descrizioneBreveVisitaIstanza)
                    .toArray(String[]::new);

            MyMenu menu = new MyMenu("Scegli una visita (0 = annulla)", voci);

            int scelta = menu.scegli();
            if (scelta == 0) return;

            VisitaIstanza selezionata = disponibili.get(scelta - 1);

            DatiGenerali dati = gestoreDati.getFileIO().leggiDatiGenerali();
            if (dati == null) {
                System.out.println("Dati generali mancanti. Impossibile iscriversi.");
                return;
            }

            int max = dati.maxPersoneIscrivibiliDaFruitore();
            int numPersone = InputDati.leggiIntero("Numero persone (1.." + max + "): ", 1, max);

            String codice = gestoreDati.iscriviFruitoreAVisita(selezionata, fruitore.getNomeUtente(), numPersone);
            System.out.println("Iscrizione accettata! Codice prenotazione: " + codice);

        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Errore durante iscrizione: " + e.getMessage());
        }
    }

    private void menuDisdetta() {
        try {
            String codiceInserito = InputDati.leggiStringaNonVuota("Inserisci codice prenotazione da disdire: ")
                    .trim()
                    .toUpperCase();

            boolean ok = gestoreDati.disdiciIscrizioneByCodice(fruitore.getNomeUtente(), codiceInserito);

            if (ok) System.out.println("Disdetta effettuata con successo.");
            else System.out.println("Codice non valido, non trovato oppure disdetta non consentita.");

        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Errore durante disdetta: " + e.getMessage());
        }
    }
}