package ingegneriaSoftware.domain;

/*
Rappresenta i giorni della settimana utilizzati per definire
la programmabilità dei tipi di visita.

Ogni valore enum è associato a una descrizione testuale in lingua italiana,
usata per la visualizzazione verso l’utente.

Invarianti di classe:
- l’insieme dei giorni è fisso e completo
- a ogni giorno è sempre associata una descrizione non nulla
- la rappresentazione testuale di un giorno coincide con la sua descrizione
*/
public enum GiorniSettimana {

    MONDAY("LUNEDÌ"),
    TUESDAY("MARTEDÌ"),
    WEDNESDAY("MERCOLEDÌ"),
    THURSDAY("GIOVEDÌ"),
    FRIDAY("VENERDÌ"),
    SATURDAY("SABATO"),
    SUNDAY("DOMENICA");

    private final String descrizione;

    GiorniSettimana(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    @Override
    public String toString() {
        return descrizione;
    }
}