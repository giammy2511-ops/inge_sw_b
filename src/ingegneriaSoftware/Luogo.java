package ingegneriaSoftware;

import java.util.ArrayList;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonIgnore;

/*
Rappresenta un luogo visitabile appartenente all’ambito territoriale
gestito dall’applicazione.

Un luogo è caratterizzato da una posizione geografica, da informazioni
descrittive e dall’insieme delle visite ad esso associate.
Lo stato di attività del luogo dipende dallo stato delle visite collegate.

Invarianti di classe:
- l’identificativo del luogo è derivato da nome e coordinate
- un luogo è attivo se e solo se esiste almeno una visita attiva associata
- le visite associate appartengono tutte allo stesso luogo
*/
public class Luogo {

    private String id;
    private String nome;
    private String descrizione;
    private HashMap<String, Integer> coordinate = new HashMap<String, Integer>();
    private boolean attivo;

    @JsonIgnore
    private ArrayList<Visita> visite = new ArrayList<Visita>();

    public Luogo() {
    }

    public Luogo(String nome, String descrizione, HashMap<String, Integer> coordinate) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.coordinate = coordinate;
        this.id = creaID(nome, coordinate);
        this.attivo = true;
    }

    public String creaID(String nome, HashMap<String, Integer> coordinate) {
        return nome + "-" + coordinate.get("latitudine") + "-" + coordinate.get("longitudine");
    }

    public void aggiungiVisita(Visita v) {
        if (v == null) return;
        this.visite.add(v);
        aggiornaAttivoDaVisite();
    }

    public void aggiornaAttivoDaVisite() {
        if (visite == null || visite.isEmpty()) {
            this.attivo = false;
            return;
        }

        for (Visita v : visite) {
            if (v != null && v.isAttivo()) {
                this.attivo = true;
                return;
            }
        }

        this.attivo = false;
    }

    @Override
    public String toString() {
        return "Luogo:\n" +
                "  ID: " + id + "\n" +
                "  Attivo: " + (attivo ? "SÌ" : "NO") + "\n" +
                "  Nome: " + nome + "\n" +
                "  Descrizione: " + descrizione + "\n" +
                "  Coordinate: " + coordinate + "\n" +
                "  Numero visite: " + visite.size() + "\n";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public HashMap<String, Integer> getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(HashMap<String, Integer> coordinate) {
        this.coordinate = coordinate;
    }

    public ArrayList<Visita> getVisite() {
        return visite;
    }

    public void setVisite(ArrayList<Visita> visite) {
        this.visite = visite;
        aggiornaAttivoDaVisite();
    }

    public boolean isAttivo() {
        return attivo;
    }
    
	public void setAttivo(boolean b) {
		this.attivo = b;	
	}
}