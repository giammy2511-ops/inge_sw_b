package ingegneriaSoftware;

/*
Rappresenta il punto di incontro per l’avvio di una visita guidata.

Il punto di incontro è descritto da un indirizzo testuale e da una
eventuale descrizione aggiuntiva utile a evitare ambiguità
(per esempio indicazioni sul luogo preciso del ritrovo).

Invarianti di classe:
- via identifica il contesto principale del punto di incontro
- numeroCivico e descrizione sono informazioni opzionali
- la rappresentazione testuale combina in modo coerente le informazioni disponibili
*/
public class PuntoIncontro {

    private String via;
    private String numeroCivico;
    private String descrizione;

    public PuntoIncontro() {}

    public PuntoIncontro(String via, String numeroCivico, String descrizione) {
        this.via = via;
        this.numeroCivico = numeroCivico;
        this.descrizione = descrizione;
    }

    @Override
    public String toString() {
        return via +
               (numeroCivico != null && !numeroCivico.isBlank() ? " " + numeroCivico : "") +
               (descrizione != null && !descrizione.isBlank() ? " (" + descrizione + ")" : "");
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getNumeroCivico() {
        return numeroCivico;
    }

    public void setNumeroCivico(String numeroCivico) {
        this.numeroCivico = numeroCivico;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
}