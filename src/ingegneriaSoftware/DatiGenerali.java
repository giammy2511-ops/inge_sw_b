package ingegneriaSoftware;

/*
Rappresenta i dati generali di configurazione dell’applicazione, impostati a livello globale.

Invarianti di classe:
- ambitoTerritoriale è definito e non è vuoto
- maxPersoneIscrivibiliDaFruitore è un valore positivo
- l’ambitoTerritoriale, una volta impostato, non deve essere modificato durante l’esecuzione
*/
public record DatiGenerali(
        String ambitoTerritoriale,
        int maxPersoneIscrivibiliDaFruitore
) {}