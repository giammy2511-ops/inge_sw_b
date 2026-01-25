package ingegneriaSoftware.controller;

import ingegneriaSoftware.domain.Utente;
import ingegneriaSoftware.ui.MenuFruitore;
import it.unibs.fp.mylib.InputDati;

public class LoginController {

    private final GestoreDati gestoreDati;

    public LoginController(GestoreDati gestoreDati) {
        if (gestoreDati == null) throw new IllegalArgumentException("gestoreDati nullo");
        this.gestoreDati = gestoreDati;
    }

    /** Login per configuratore/volontario: ripete finché non va a buon fine. */
    public Utente loginConfiguratoreVolontario() {
        Utente u = verificaCredenziali();
        while (u == null) {
            System.out.println("Utente non attivo o credenziali errate.");
            u = verificaCredenziali();
        }
        return u;
    }

    /** Login/registrazione fruitore delegata al flusso esistente. */
    public Utente loginFruitore() {
        return MenuFruitore.accessoORRegistrazione(gestoreDati);
    }

    private Utente verificaCredenziali() {
        String[] credenziali = richiediCredenziali("Nome utente: ", "Password: ");
        return gestoreDati.autentica(credenziali[0], credenziali[1]);
    }

    private String[] richiediCredenziali(String nome, String password) {
        String[] credenziali = new String[2];
        credenziali[0] = InputDati.leggiStringaNonVuota(nome);
        credenziali[1] = InputDati.leggiStringaNonVuota(password);
        return credenziali;
    }
}
