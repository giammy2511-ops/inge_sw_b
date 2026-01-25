package ingegneriaSoftware.domain;

import java.util.List;

/*
Rappresenta un utente dell’applicazione, identificato da uno username
e caratterizzato da un ruolo applicativo.

La classe modella sia lo stato di autenticazione iniziale sia lo stato
di attività dell’utente, che dipende dal ruolo e, nel caso dei volontari,
dalle visite a cui risultano associati.

Invarianti di classe:
- nomeUtente identifica univocamente l’utente
- il ruolo determina il comportamento relativo allo stato attivo
- configuratori e fruitori sono sempre considerati attivi
- lo stato attivo di un volontario dipende dalle visite a cui è associato
*/
public class Utente {

    private String nomeUtente;
    private String password;
    private String ruolo;
    private boolean primoAccesso;
    private boolean attivo;

    public Utente() {
    }

    public Utente(String nomeUtente, String password, String ruolo, boolean primoAccesso, boolean attivo) {
        this.nomeUtente = nomeUtente;
        this.password = password;
        this.ruolo = ruolo;
        this.primoAccesso = primoAccesso;
        this.attivo = attivo;
    }

    public void setAttivo(boolean attivo) {
        if ("fruitore".equals(this.ruolo) || "configuratore".equals(this.ruolo)) {
            this.attivo = true;
            return;
        }
        this.attivo = attivo;
    }

    public void aggiornaAttivoDaVisite(List<Visita> visiteAssociate) {
        if (!"volontario".equals(this.ruolo)) {
            this.attivo = true;
            return;
        }

        if (visiteAssociate == null || visiteAssociate.isEmpty()) {
            this.attivo = false;
            return;
        }

        for (Visita v : visiteAssociate) {
            if (v != null && v.isAttivo() && v.getVolontari().contains(this.nomeUtente)) {
                this.attivo = true;
                return;
            }
        }

        this.attivo = false;
    }

    @Override
    public String toString() {
        return this.nomeUtente + " " + this.password + " " + this.ruolo + " " + this.primoAccesso + " attivo =" + this.attivo;
    }

    public String getNomeUtente() {
        return nomeUtente;
    }

    public void setNomeUtente(String nomeUtente) {
        this.nomeUtente = nomeUtente;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    public boolean isPrimoAccesso() {
        return primoAccesso;
    }

    public void setPrimoAccesso(boolean primoAccesso) {
        this.primoAccesso = primoAccesso;
    }

    public boolean isAttivo() {
        return attivo;
    }
}