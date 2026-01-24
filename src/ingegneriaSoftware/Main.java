package ingegneriaSoftware;

import java.io.IOException;
import it.unibs.fp.mylib.InputDati;
import it.unibs.fp.mylib.MyMenu;

/*
Classe di avvio dell’applicazione console.

Responsabilità principali:
- carica il GestoreDati dalla directory di persistenza e avvia il ciclo di interazione
- presenta un menu di benvenuto che separa i flussi di accesso:
  - login per configuratore/volontario tramite credenziali
  - accesso/registrazione per fruitore delegata a MenuFruitore
- gestisce il ciclo di autenticazione e mantiene l’utente corrente della sessione
- forza il cambio credenziali al primo accesso (password diversa dalla precedente; per i volontari lo username resta invariato)
- legge i DatiGenerali (ambito territoriale e max iscrivibili per fruitore) e, se assenti, ne consente l’inizializzazione solo al configuratore
- avvia il menu coerente con il ruolo autenticato (configuratore / volontario / fruitore)

Invarianti di classe:
- gestoreDati viene inizializzato tramite caricaDaDirectory prima di qualunque login e fornisce accesso alla persistenza (FileIO)
- utenteCorrente è diverso da null solo dopo un accesso riuscito (login oppure accesso/registrazione fruitore)
- se utenteCorrente è al primo accesso, l’esecuzione prosegue verso i menu solo dopo un cambio credenziali valido
  (nuova password != password attuale)
- i DatiGenerali devono esistere per poter proseguire con l’uso completo dell’applicazione:
  se mancano e l’utente non è configuratore, il flusso ritorna al menu di benvenuto
- l’avvio dei menu dipende esclusivamente dal ruolo dell’utente corrente; ruoli non previsti non avviano alcun menu
- il ciclo del menu di benvenuto continua finché l’utente non sceglie esplicitamente l’uscita; in uscita l’app termina
*/

public class Main {

    private static GestoreDati gestoreDati;
    private static Utente utenteCorrente;

    public static void main(String[] args) {
        try {
            gestoreDati = GestoreDati.caricaDaDirectory("./DATA");
            menuBenvenuto();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void menuBenvenuto() {
        String[] voci = {
            "Accedi come configuratore/volontario",
            "Accedi come fruitore",
        };

        MyMenu menu = new MyMenu("Benvenuto", voci);

        boolean fine = false;
        while (!fine) {
            int scelta = menu.scegli();
            switch (scelta) {
                case 1 -> login();
                case 2 -> loginFruitore();
                case 0 -> fine=true;
            }
        }
        System.exit(0);
    }

    private static void login() {
        utenteCorrente = verificaCredenziali();
        while (utenteCorrente == null) {
            System.out.println("Utente non attivo o credenziali errate.");
            utenteCorrente = verificaCredenziali();
        }

        if (utenteCorrente.isPrimoAccesso()) {
            while (!cambioCredenziali()) {            
            }
        }

        try {
            chiediDatiGenerali();
            avviaMenu();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void loginFruitore() {
        utenteCorrente = MenuFruitore.accessoORRegistrazione(gestoreDati);

        if (utenteCorrente == null) {
            return;
        }

        try {
            chiediDatiGenerali();
            avviaMenu();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void avviaMenu() {
        String ruolo = utenteCorrente.getRuolo();

        if ("configuratore".equals(ruolo)) {
            MenuConfiguratore menuConfiguratore = new MenuConfiguratore(gestoreDati);
            menuConfiguratore.menu();
            return;
        }

        if ("volontario".equals(ruolo)) {
            MenuVolontario menuVolontario = new MenuVolontario(gestoreDati, utenteCorrente);
            menuVolontario.menu();
            return;
        }

        if ("fruitore".equals(ruolo)) {
            MenuFruitore menuFruitore = new MenuFruitore(gestoreDati, utenteCorrente);
            menuFruitore.menu();
            return;
        }
        
        System.out.println("Ruolo non supportato: " + ruolo);
    }

    private static void chiediDatiGenerali() throws IOException {
        DatiGenerali dati = gestoreDati.getFileIO().leggiDatiGenerali();

        if (dati != null) {
            System.out.println("Ambito territoriale: " + dati.ambitoTerritoriale());
            System.out.println("Numero max persone iscrivibili da un fruitore: " + dati.maxPersoneIscrivibiliDaFruitore());
            return;
        }

        if (!"configuratore".equals(utenteCorrente.getRuolo())) {
            System.out.println("Dati generali non presenti. Accedi come configuratore per inizializzare il sistema.");
            menuBenvenuto();
        }

        String ambito = InputDati.leggiStringaNonVuota("Ambito territoriale: ");
        int max = InputDati.leggiIntero("Numero max persone iscrivibili da un fruitore: ");
        dati = new DatiGenerali(ambito, max);
        gestoreDati.getFileIO().salvaDatiGenerali(dati);
    }

    private static Utente verificaCredenziali() {
        String[] credenziali = richiediCredenziali("Nome utente: ", "Password: ");
        return gestoreDati.autentica(credenziali[0], credenziali[1]);
    }

    private static boolean cambioCredenziali() {
        String nuovoNome;

        if ("volontario".equals(utenteCorrente.getRuolo())) {
            nuovoNome = utenteCorrente.getNomeUtente();
        } else {
            nuovoNome = InputDati.leggiStringaNonVuota("Nuovo nome utente: ");
        }

        String nuovaPassword;

        do {
            nuovaPassword = InputDati.leggiStringaNonVuota("Nuova password: ");

            if (nuovaPassword.equals(utenteCorrente.getPassword())) {
                System.out.println("La nuova password deve essere diversa da quella attuale. Riprova.");
            }

        } while (nuovaPassword.equals(utenteCorrente.getPassword()));

        try {
            gestoreDati.aggiornaCredenziali(utenteCorrente, nuovoNome, nuovaPassword);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        System.out.println("Credenziali aggiornate correttamente!");
        return true;
    }

    private static String[] richiediCredenziali(String nome, String password) {
        String[] credenziali = new String[2];
        credenziali[0] = InputDati.leggiStringaNonVuota(nome);
        credenziali[1] = InputDati.leggiStringaNonVuota(password);
        return credenziali;
    }
}