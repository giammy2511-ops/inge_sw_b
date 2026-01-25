package ingegneriaSoftware;

import ingegneriaSoftware.controller.CredenzialiController;
import ingegneriaSoftware.controller.DatiGeneraliController;
import ingegneriaSoftware.controller.GestoreDati;
import ingegneriaSoftware.controller.LoginController;
import ingegneriaSoftware.domain.Utente;
import ingegneriaSoftware.ui.MenuConfiguratore;
import ingegneriaSoftware.ui.MenuFruitore;
import ingegneriaSoftware.ui.MenuVolontario;
import it.unibs.fp.mylib.MyMenu;

import java.io.IOException;

public class VisiteGuidateApp {

    private final GestoreDati gestoreDati;

    private final LoginController loginController;
    private final CredenzialiController credenzialiController;
    private final DatiGeneraliController datiGeneraliController;

    private Utente utenteCorrente;

    public VisiteGuidateApp(GestoreDati gestoreDati) {
        if (gestoreDati == null) throw new IllegalArgumentException("gestoreDati nullo");
        this.gestoreDati = gestoreDati;

        // Controller per contesto (minimo sforzo)
        this.loginController = new LoginController(gestoreDati);
        this.credenzialiController = new CredenzialiController(gestoreDati);
        this.datiGeneraliController = new DatiGeneraliController(gestoreDati);
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
                case 1 -> flussoLoginConfiguratoreVolontario();
                case 2 -> flussoLoginFruitore();
                case 0 -> fine = true;
            }
        }
    }

    private void flussoLoginConfiguratoreVolontario() {
        utenteCorrente = loginController.loginConfiguratoreVolontario();

        if (utenteCorrente.isPrimoAccesso()) {
            while (!credenzialiController.cambioCredenziali(utenteCorrente)) {
                // riprova
            }
        }

        postLoginCommonFlow();
    }

    private void flussoLoginFruitore() {
        utenteCorrente = loginController.loginFruitore();
        if (utenteCorrente == null) return;

        postLoginCommonFlow();
    }

    private void postLoginCommonFlow() {
        try {
            boolean ok = datiGeneraliController.assicuratiDatiGeneraliPresenti(utenteCorrente);
            if (!ok) {
                // come prima: torno al benvenuto
                return;
            }
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
}
