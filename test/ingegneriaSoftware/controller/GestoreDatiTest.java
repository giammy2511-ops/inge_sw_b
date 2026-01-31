package ingegneriaSoftware.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import ingegneriaSoftware.domain.*;
import ingegneriaSoftware.domain.dati.*;

class GestoreDatiTest {

    @TempDir
    Path tempDir;

    private GestoreDati gestore;

    @BeforeEach
    void setup() throws IOException {
        FileIO fileIO = new FileIO(tempDir.toString());

        gestore = new GestoreDati(
            new ArrayList<>(),        // utenti
            new ArrayList<>(),        // luoghi
            new GestoreCalendario(),  // calendario
            fileIO
        );
    }
    
    @Test
    void aggiungiDisponibilitaVolontario_aggiungeLaDataInMemoria() throws IOException {
        LocalDate data = LocalDate.of(2026, 5, 10);

        gestore.aggiungiDisponibilitaVolontario("luca", data);

        Set<LocalDate> disp = gestore.getDisponibilitaVolontario("luca");

        assertNotNull(disp);
        assertTrue(disp.contains(data));
    }
    
    @Test
    void aggiungiDisponibilitaVolontario_persistitaSuFile() throws IOException {
        LocalDate data = LocalDate.of(2026, 6, 15);

        gestore.aggiungiDisponibilitaVolontario("anna", data);

        // Ricreiamo FileIO sulla stessa directory
        FileIO nuovoFileIO = new FileIO(tempDir.toString());
        Map<String, Set<LocalDate>> mappa = nuovoFileIO.leggiDisponibilitaVolontari();

        assertTrue(mappa.containsKey("anna"));
        assertTrue(mappa.get("anna").contains(data));
    }
    
    @Test
    void disponibilitaNelMese_filtraCorrettamente() throws IOException {
        LocalDate base = LocalDate.now().withDayOfMonth(1);

        gestore.aggiungiDisponibilitaVolontario("luca", base.plusDays(1));
        gestore.aggiungiDisponibilitaVolontario("luca", base.plusMonths(1));

        List<LocalDate> nelMese =
            gestore.getDisponibilitaVolontarioNelMese("luca", base);

        assertEquals(1, nelMese.size());
        assertEquals(base.plusDays(1), nelMese.get(0));
    }



}
