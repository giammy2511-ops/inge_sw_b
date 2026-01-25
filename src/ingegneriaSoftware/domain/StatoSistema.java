package ingegneriaSoftware.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/*
Rappresenta lo stato globale del sistema rispetto al ciclo mensile
di raccolta delle disponibilità e produzione del piano visite.

La classe incapsula il mese di riferimento e i flag che indicano
se la raccolta è attualmente aperta e se il piano per tale mese
è già stato prodotto.

Invarianti di classe:
- meseRaccolta è sempre definito
- se pianoProdotto è vero, la raccolta per quel mese è conclusa
- le finestre temporali di raccolta sono derivate in modo deterministico da meseRaccolta
*/
public class StatoSistema {

    private YearMonth meseRaccolta;   
    private boolean raccoltaAperta;   
    private boolean pianoProdotto;    

    public StatoSistema() { }

    public StatoSistema(YearMonth meseRaccolta, boolean raccoltaAperta, boolean pianoProdotto) {
        this.meseRaccolta = Objects.requireNonNull(meseRaccolta);
        this.raccoltaAperta = raccoltaAperta;
        this.pianoProdotto = pianoProdotto;
    }

    public void chiudiRaccolta() {
        this.raccoltaAperta = false;
    }

    public void apriRaccoltaPer(YearMonth nuovoMeseRaccolta) {
        setMeseRaccolta(nuovoMeseRaccolta);
        this.raccoltaAperta = true;
        this.pianoProdotto = false;
    }

    public void segnaPianoProdotto() {
        this.pianoProdotto = true;
    }

    public LocalDate inizioFinestra() {
        return meseRaccolta.minusMonths(2).atDay(16);
    }

    public LocalDate fineFinestra() {
        return meseRaccolta.minusMonths(1).atDay(15);
    }

    @Override
    public String toString() {
        return "StatoSistema{" +
                "meseRaccolta=" + meseRaccolta +
                ", raccoltaAperta=" + raccoltaAperta +
                ", pianoProdotto=" + pianoProdotto +
                '}';
    }

    public YearMonth getMeseRaccolta() {
        return meseRaccolta;
    }

    public void setMeseRaccolta(YearMonth meseRaccolta) {
        this.meseRaccolta = Objects.requireNonNull(meseRaccolta);
    }

    public boolean isRaccoltaAperta() {
        return raccoltaAperta;
    }

    public void setRaccoltaAperta(boolean raccoltaAperta) {
        this.raccoltaAperta = raccoltaAperta;
    }

    public boolean isPianoProdotto() {
        return pianoProdotto;
    }

    public void setPianoProdotto(boolean pianoProdotto) {
        this.pianoProdotto = pianoProdotto;
    }
}