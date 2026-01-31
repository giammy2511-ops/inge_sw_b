package ingegneriaSoftware.domain.dati;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

class FileIOTest {

    @TempDir
    Path tempDir;

    private FileIO fileIO;

    @BeforeEach
    void setup() throws IOException {
        fileIO = new FileIO(tempDir.toString());
    }
    
    @Test
    void leggiUtenti_senzaFile_restituisceListaVuota() throws IOException {
        List<?> utenti = fileIO.leggiUtenti();

        assertNotNull(utenti);
        assertTrue(utenti.isEmpty());
    }
    
    @Test
    void salvaELeggiUtenti_roundTrip() throws IOException {
        var utenti = List.of(
            new ingegneriaSoftware.domain.Utente(
                "mario", "pwd", "fruitore", true, true
            )
        );

        fileIO.salvaUtenti(utenti);
        var letti = fileIO.leggiUtenti();

        assertEquals(1, letti.size());
        assertEquals("mario", letti.get(0).getNomeUtente());
    }
    
    @Test
    void caricaStatoSistema_fileCorrotto_restituisceNull() throws IOException {
        Path p = tempDir.resolve("statoSistema.json");
        java.nio.file.Files.writeString(p, "### NON JSON ###");

        var stato = fileIO.caricaStatoSistema();

        assertNull(stato);
    }



}
