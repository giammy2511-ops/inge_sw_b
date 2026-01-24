package ingegneriaSoftware;

import java.io.IOException;
import it.unibs.fp.mylib.InputDati;
import it.unibs.fp.mylib.MyMenu;

/**
 * Orchestratore dell'applicazione console.
 *
 * Refactoring: Extract Class da Main.
 * - Main ora è solo bootstrap.
 * - Questa classe gestisce il flusso: login, dati generali, avvio menu per ruolo.
 */
public class VisiteGuidateApp {

    private final GestoreDati gestoreDati;
    private Utente utenteCorrente;

    public VisiteGuidateApp(GestoreDati gestoreDati) {
        if (gestoreDati == null) throw new IllegalArgumentException("gestoreDati nullo");
        this.gestoreDati = gestoreDati;
    }

    public void run() {
        menuBenvenuto();
        System.exit(0);
    }

    private void menuBenvenuto() {
        String[] voci = {
            "Accedi come configuratore/volontario",
            "Accedi come fruitore",
        };

        MyMenu menu = new MyMenu("Benvenuto", voci);

        boolean fine = false;
        while (!fine) {
            int scelta = menu.scegli();
            switch (scelta) {
                case 1 -> loginConfiguratoreVolontario();
                case 2 -> loginFruitore();
                case 0 -> fine = true;
            }
        }
    }

    private void loginConfiguratoreVolontario() {
        utenteCorrente = verificaCredenziali();
        while (utenteCorrente == null) {
            System.out.println("Utente non attivo o credenziali errate.");
            utenteCorrente = verificaCredenziali();
        }

        if (utenteCorrente.isPrimoAccesso()) {
            while (!cambioCredenziali()) {
                // riprova
            }
        }

        try {
            chiediDatiGenerali();
            avviaMenu();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loginFruitore() {
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

    private void avviaMenu() {
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

    private void chiediDatiGenerali() throws IOException {
        DatiGenerali dati = gestoreDati.getFileIO().leggiDatiGenerali();

        if (dati != null) {
            System.out.println("Ambito territoriale: " + dati.ambitoTerritoriale());
            System.out.println("Numero max persone iscrivibili da un fruitore: " + dati.maxPersoneIscrivibiliDaFruitore());
            return;
        }

        if (!"configuratore".equals(utenteCorrente.getRuolo())) {
            System.out.println("Dati generali non presenti. Accedi come configuratore per inizializzare il sistema.");
            // Mantengo il comportamento originale: torno al menu di benvenuto
            menuBenvenuto();
            return;
        }

        String ambito = InputDati.leggiStringaNonVuota("Ambito territoriale: ");
        int max = InputDati.leggiIntero("Numero max persone iscrivibili da un fruitore: ");
        dati = new DatiGenerali(ambito, max);
        gestoreDati.getFileIO().salvaDatiGenerali(dati);
    }

    private Utente verificaCredenziali() {
        String[] credenziali = richiediCredenziali("Nome utente: ", "Password: ");
        return gestoreDati.autentica(credenziali[0], credenziali[1]);
    }

    private boolean cambioCredenziali() {
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

    private String[] richiediCredenziali(String nome, String password) {
        String[] credenziali = new String[2];
        credenziali[0] = InputDati.leggiStringaNonVuota(nome);
        credenziali[1] = InputDati.leggiStringaNonVuota(password);
        return credenziali;
    }
}
