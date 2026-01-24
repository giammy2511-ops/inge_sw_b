package ingegneriaSoftware;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/*
Rappresenta la configurazione temporale di un tipo di visita,
definendo il periodo dell’anno in cui è programmabile e le sue
caratteristiche orarie.

Invarianti di classe:
- dataInizio è antecedente o uguale a dataFine
- durata è positiva
- giorniProgrammabili è non vuota
- oraInizio identifica un orario valido di inizio visita
- la visita può essere programmata solo nei giorni indicati e
  all’interno dell’intervallo di date specificato
*/
public record SchedulingVisita(
        LocalDate dataInizio,
        LocalDate dataFine,
        LocalTime oraInizio,
        Duration durata,
        List<GiorniSettimana> giorniProgrammabili
) {}