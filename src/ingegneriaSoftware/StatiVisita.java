package ingegneriaSoftware;

/*
Rappresenta i possibili stati in cui può trovarsi una visita
durante il suo ciclo di vita.

Gli stati modellano l’evoluzione temporale di una visita,
dalla fase di proposta fino alla sua conclusione o cancellazione.

Invarianti di classe:
- una visita si trova sempre in uno e un solo stato
- gli stati appartengono a un insieme finito e prefissato
- lo stato effettuata identifica una visita conclusa e archiviata
*/
public enum StatiVisita {
    proposta,
    completa,
    confermata,
    cancellata,
    effettuata;
}