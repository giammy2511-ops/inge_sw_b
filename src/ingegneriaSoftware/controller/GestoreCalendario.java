package ingegneriaSoftware.controller;

import java.time.LocalDate;
import java.util.ArrayList;

import ingegneriaSoftware.domain.Visita;

/*
Gestisce l’insieme dei tipi di visita memorizzati nel sistema e verifica
i vincoli temporali legati alle sovrapposizioni tra visite dello stesso luogo.

Il controllo serve a garantire che, per un medesimo luogo, due visite che risultano
programmabili nello stesso giorno non occupino intervalli temporali sovrapposti.

Invarianti di classe:
- la lista visite contiene i tipi di visita noti al gestore
- non possono coesistere visite associate allo stesso luogo che si sovrappongono
  in almeno un giorno del periodo comune
*/
public class GestoreCalendario {

    private ArrayList<Visita> visite = new ArrayList<Visita>();

    public ArrayList<Visita> getVisite() {
        return visite;
    }

    public void setVisite(ArrayList<Visita> visite) {
        this.visite = visite;
    }

    public boolean aggiungiVisita(Visita nuova) {
        for (Visita esistente : visite) {
            if (!nuova.getIdLuogo().equals(esistente.getIdLuogo())) {
                continue;
            }

            LocalDate dataInizio = nuova.getDataInizio().isAfter(esistente.getDataInizio())
                    ? nuova.getDataInizio()
                    : esistente.getDataInizio();

            LocalDate dataFine = nuova.getDataFine().isBefore(esistente.getDataFine())
                    ? nuova.getDataFine()
                    : esistente.getDataFine();

            LocalDate giorno = dataInizio;
            while (!giorno.isAfter(dataFine)) {
                if (nuova.visitaSovrappostaCon(esistente, giorno)) {
                    return false;
                }
                giorno = giorno.plusDays(1);
            }
        }
        visite.add(nuova);
        return true;
    }
}