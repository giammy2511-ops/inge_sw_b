package ingegneriaSoftware.controller;

import ingegneriaSoftware.domain.Utente;
import it.unibs.fp.mylib.InputDati;

import java.io.IOException;

public class CredenzialiController {

    private final GestoreDati gestoreDati;

    public CredenzialiController(GestoreDati gestoreDati) {
        if (gestoreDati == null) throw new IllegalArgumentException("gestoreDati nullo");
        this.gestoreDati = gestoreDati;
    }

    /** Esegue cambio credenziali rispettando la regola: per volontario username invariato. */
    public boolean cambioCredenziali(Utente utenteCorrente) {
        if (utenteCorrente == null) throw new IllegalArgumentException("utenteCorrente nullo");

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
}
