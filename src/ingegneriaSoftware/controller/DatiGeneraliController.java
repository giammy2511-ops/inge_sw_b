package ingegneriaSoftware.controller;

import ingegneriaSoftware.domain.Utente;
import ingegneriaSoftware.domain.dati.DatiGenerali;
import it.unibs.fp.mylib.InputDati;

import java.io.IOException;

public class DatiGeneraliController {

    private final GestoreDati gestoreDati;

    public DatiGeneraliController(GestoreDati gestoreDati) {
        if (gestoreDati == null) throw new IllegalArgumentException("gestoreDati nullo");
        this.gestoreDati = gestoreDati;
    }

    /**
     * Assicura che i DatiGenerali esistano.
     * @return true se si può proseguire; false se bisogna tornare al benvenuto (utente non configuratore).
     */
    public boolean assicuratiDatiGeneraliPresenti(Utente utenteCorrente) throws IOException {
        if (utenteCorrente == null) throw new IllegalArgumentException("utenteCorrente nullo");

        DatiGenerali dati = gestoreDati.getFileIO().leggiDatiGenerali();
        if (dati != null) {
            System.out.println("Ambito territoriale: " + dati.ambitoTerritoriale());
            System.out.println("Numero max persone iscrivibili da un fruitore: " + dati.maxPersoneIscrivibiliDaFruitore());
            return true;
        }

        if (!"configuratore".equals(utenteCorrente.getRuolo())) {
            System.out.println("Dati generali non presenti. Accedi come configuratore per inizializzare il sistema.");
            return false;
        }

        String ambito = InputDati.leggiStringaNonVuota("Ambito territoriale: ");
        int max = InputDati.leggiIntero("Numero max persone iscrivibili da un fruitore: ");
        dati = new DatiGenerali(ambito, max);
        gestoreDati.getFileIO().salvaDatiGenerali(dati);
        return true;
    }
}
