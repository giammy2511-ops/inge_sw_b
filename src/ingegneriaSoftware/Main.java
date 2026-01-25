package ingegneriaSoftware;

import java.io.IOException;

import ingegneriaSoftware.controller.GestoreDati;

/**
 * Bootstrap dell'applicazione.
 * Responsabilità: caricare GestoreDati e avviare l'app.
 */
public class Main {

    public static void main(String[] args) {
        try {
            GestoreDati gestoreDati = GestoreDati.caricaDaDirectory("./DATA");
            VisiteGuidateApp app = new VisiteGuidateApp(gestoreDati);
            app.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
