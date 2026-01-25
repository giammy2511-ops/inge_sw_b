package ingegneriaSoftware.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ingegneriaSoftware.domain.Iscrizione;
import ingegneriaSoftware.domain.Luogo;
import ingegneriaSoftware.domain.StatiVisita;
import ingegneriaSoftware.domain.StatoSistema;
import ingegneriaSoftware.domain.Utente;
import ingegneriaSoftware.domain.Visita;
import ingegneriaSoftware.domain.VisitaIstanza;
import ingegneriaSoftware.domain.dati.ArchivioStorico;
import ingegneriaSoftware.domain.dati.FileIO;

/*
Gestisce l’accesso centralizzato ai dati dell’applicazione e coordina
operazioni applicative e persistenza su file.

La classe mantiene in memoria:
- utenti e relativi ruoli (configuratore/volontario/fruitore) e relative regole di autenticazione
- luoghi e tipi di visita attivi (tramite GestoreCalendario) con ricostruzione delle associazioni
- date precluse e disponibilità dei volontari
- stato operativo del sistema (finestra temporale, raccolta disponibilità, piano prodotto)
- piano visite corrente (istanze) con iscrizioni e transizioni di stato fino ad archiviazione

È inoltre responsabile di:
- produrre/leggere/salvare il piano visite (VisitaIstanza) e gestire iscrizioni/disdette
- aggiornare automaticamente lo stato delle istanze (proposta/completa → confermata/cancellata → effettuata)
  in base a: chiusura iscrizioni a -3 giorni, soglia minima partecipanti, capienza, e data visita
- archiviare le visite effettuate nell’ArchivioStorico e rimuoverle dal piano corrente
- applicare rimozioni con cascata (luogo / tipo visita / volontario) riallineando dati e persistenza
- mantenere sincronizzati stati “attivo/non attivo” di visite, luoghi e volontari e l’archivio storico dei tipi scaduti

Invarianti di classe:
- fileIO e gestoreCalendario sono riferimenti validi e costituiscono il canale unico di persistenza
  e di gestione dei tipi di visita attivi
- gestoreCalendario contiene solo tipi di visita attivi; i tipi non più attivi vengono rimossi e,
  se necessario, registrati nell’ArchivioStorico come tipi visita scaduti
- le associazioni luogo visite in memoria sono ricostruite a partire da gestoreCalendario e restano coerenti:
  ogni tipo di visita attivo appartiene a un luogo esistente, e ogni luogo contiene solo visite attive
- lo stato “attivo” di luoghi e volontari è derivato dalle visite attive associate (non arbitrario)
- disponibilitaVolontari è indicizzata per nome utente e ogni insieme di date non contiene duplicati
- pianoVisite rappresenta il piano corrente: se non caricato viene letto da file; quando modificato viene salvato
- il piano prodotto rispetta: assenza di date precluse, vincolo “un volontario al giorno”, e programmabilità dei tipi visita
- iscrizioni consentite solo prima della data di chiusura (3 giorni prima della visita) e solo su visite in stato proposta;
  un fruitore non può avere più iscrizioni sulla stessa istanza e non si supera la capienza massima
- aggiornamenti di stato delle istanze seguono le regole implementate:
  a chiusura iscrizioni: completa→confermata; proposta→confermata se >= minimo altrimenti cancellata;
  dopo la data visita: cancellata rimossa dal piano; confermata→effettuata, archiviata e rimossa dal piano
- dopo operazioni che impattano struttura o contenuti (aggiunte/rimozioni/iscrizioni/stati/produzione piano),
  i dati persistiti vengono aggiornati per mantenere coerenza con la memoria
*/

public class GestoreDati {

    private List<Utente> utenti;
    private List<Luogo> luoghi;
    private Set<LocalDate> datePrecluse;
    private GestoreCalendario gestoreCalendario;
    private FileIO fileIO;
    private Map<String, Set<LocalDate>> disponibilitaVolontari;
    private List<VisitaIstanza> pianoVisite = new ArrayList<>();

    public GestoreDati(List<Utente> utenti, List<Luogo> luoghi, GestoreCalendario gestoreCalendario, FileIO fileIO) {
        this.utenti = utenti;
        this.luoghi = luoghi;
        this.gestoreCalendario = gestoreCalendario;
        this.fileIO = fileIO;
        this.datePrecluse = new HashSet<>();
        this.disponibilitaVolontari = new HashMap<>();      
    }

    public static GestoreDati caricaDaDirectory(String cartellaDati) throws IOException {
        FileIO fileIO = new FileIO(cartellaDati);

        List<Utente> utenti = fileIO.leggiUtenti();
        Set<LocalDate> datePrecluse = fileIO.leggiDatePrecluse();
        List<Luogo> luoghi = fileIO.leggiLuoghi();

        GestoreCalendario gestoreCalendario = new GestoreCalendario();
        gestoreCalendario.setVisite(new ArrayList<>(fileIO.leggiVisite()));

        Map<String, Luogo> mappaLuoghi = new HashMap<>();
        for (Luogo l : luoghi) {
            l.setVisite(new ArrayList<>());
            mappaLuoghi.put(l.getId(), l);
        }

        if (gestoreCalendario.getVisite() != null) {
            for (Visita v : gestoreCalendario.getVisite()) {
                Luogo l = mappaLuoghi.get(v.getIdLuogo());
                if (l != null) l.aggiungiVisita(v);
            }
        }

        Map<String, Set<LocalDate>> disponibilitaVolontari = fileIO.leggiDisponibilitaVolontari();
        if (disponibilitaVolontari == null) disponibilitaVolontari = new HashMap<>();

        GestoreDati gd = new GestoreDati(utenti, luoghi, gestoreCalendario, fileIO);
        gd.setDatePrecluse(datePrecluse);
        gd.setDisponibilitaVolontari(disponibilitaVolontari);
        gd.sincronizzaStatiAttiviEArchivio(LocalDate.now());

        return gd;
    }

    public List<Utente> getUtenti() {
        return utenti;
    }

    public void setUtenti(List<Utente> utenti) {
        this.utenti = utenti;
    }

    public List<Luogo> getLuoghi() {
        return luoghi;
    }

    public void setLuoghi(List<Luogo> luoghi) {
        this.luoghi = luoghi;
    }

    public Set<LocalDate> getDatePrecluse() {
        return datePrecluse;
    }

    public void setDatePrecluse(Set<LocalDate> datePrecluse) {
        this.datePrecluse = datePrecluse;
    }

    public GestoreCalendario getGestoreCalendario() {
        return gestoreCalendario;
    }

    public void setGestoreCalendario(GestoreCalendario gestoreCalendario) {
        this.gestoreCalendario = gestoreCalendario;
    }

    public FileIO getFileIO() {
        return fileIO;
    }

    public void setFileIO(FileIO fileIO) {
        this.fileIO = fileIO;
    }
    
    public Map<String, Set<LocalDate>> getDisponibilitaVolontari() {
        return disponibilitaVolontari;
    }

    public void setDisponibilitaVolontari(Map<String, Set<LocalDate>> disponibilitaVolontari) {
        this.disponibilitaVolontari = disponibilitaVolontari;
    }

    public Utente autentica(String username, String password) {
        for (Utente u : this.utenti) {
            if (u.getNomeUtente().equals(username)
                    && u.getPassword().equals(password)
                    && ("configuratore".equals(u.getRuolo()) || "volontario".equals(u.getRuolo()) || "fruitore".equals(u.getRuolo()))) {

                if ("volontario".equals(u.getRuolo()) && !u.isAttivo()) {
                    return null;
                }

                return u;
            }
        }
        return null;
    }

    public void aggiornaCredenziali(Utente utente, String nuovoNome, String nuovaPassword) throws IOException {
        if ("volontario".equals(utente.getRuolo())) {
            nuovoNome = utente.getNomeUtente();
        } else {
            if (nuovoNome == null || nuovoNome.isBlank()) {
                throw new IllegalArgumentException("Username non valido.");
            }
            if (!nuovoNome.equals(utente.getNomeUtente()) && esisteNomeUtente(nuovoNome)) {
                throw new IllegalArgumentException("Username già esistente: " + nuovoNome);
            }
        }

        if (nuovaPassword == null || nuovaPassword.isBlank()) {
            throw new IllegalArgumentException("Password non valida.");
        }

        if (nuovaPassword.equals(utente.getPassword())) {
            throw new IllegalArgumentException("La nuova password deve essere diversa da quella attuale.");
        }

        utente.setNomeUtente(nuovoNome);
        utente.setPassword(nuovaPassword);
        utente.setPrimoAccesso(false);
        this.fileIO.salvaUtenti(this.utenti);
    }

    public Utente creaVolontario(String username, String passwordPredefinita) throws IOException {
        if (esisteNomeUtente(username)) {
            throw new IllegalArgumentException("Username già esistente: " + username);
        }

        Utente nuovo = new Utente(username, passwordPredefinita, "volontario", true, true);
        this.utenti.add(nuovo);
        this.fileIO.salvaUtenti(this.utenti);
        return nuovo;
    }
    
    public void aggiungiLuogo(Luogo luogo) throws IOException {
        this.luoghi.add(luogo);
        this.fileIO.salvaLuoghi(this.luoghi);
    }

    public String nomeMeseMaiuscoloIt(LocalDate data) {
	    return data.getMonth()
	            .getDisplayName(TextStyle.FULL, Locale.ITALIAN)
	            .toUpperCase(Locale.ITALIAN);
	}

	public boolean aggiungiVisitaALuogo(Luogo luogo, Visita visita) throws IOException {
        boolean ok = this.gestoreCalendario.aggiungiVisita(visita);
        if (!ok) return false;
        luogo.aggiungiVisita(visita);
        this.fileIO.salvaVisite(this.gestoreCalendario.getVisite());
        this.fileIO.salvaLuoghi(this.luoghi);
        sincronizzaStatiAttiviEArchivio(LocalDate.now());

        return true;
    }

    public boolean esisteVisitaConTitoloNelLuogo(String idLuogo, String titolo) {
	    if (this.gestoreCalendario.getVisite() == null) return false;
	
	    for (Visita v : this.gestoreCalendario.getVisite()) {
	        if (v.getIdLuogo().equals(idLuogo) && v.getTitolo().equalsIgnoreCase(titolo)) {
	            return true;
	        }
	    }
	    return false;
	}

	public boolean esisteNomeUtente(String nome) {
	    for (Utente u : this.utenti) {
	        if (u.getNomeUtente().equals(nome)) {
	            return true;
	        }
	    }
	    return false;
	}

	private void sincronizzaStatiAttiviEArchivio(LocalDate oggi) throws IOException {
	    if (oggi == null) oggi = LocalDate.now();
	
	    ArchivioStorico archivio = fileIO.leggiArchivioStorico();
	    if (archivio == null) archivio = new ArchivioStorico();
	
	    if (gestoreCalendario.getVisite() == null) {
	        gestoreCalendario.setVisite(new ArrayList<>());
	    }
	
	    boolean cambiatoVisite = false;
	
	    var it = gestoreCalendario.getVisite().iterator();
	    while (it.hasNext()) {
	        Visita v = it.next();
	        if (v == null) continue;
	
	        v.aggiornaAttivoDaOggi(oggi);
	
	        if (!v.isAttivo()) {
	            aggiungiTipoScadutoSeManca(archivio, v);
	            it.remove();
	            cambiatoVisite = true;
	        }
	    }
	
	    if (cambiatoVisite) {
	        fileIO.salvaVisite(gestoreCalendario.getVisite());
	        fileIO.salvaArchivioStorico(archivio);
	    }
	
	    ricostruisciVisiteDentroLuoghi();
	
	    if (luoghi != null) {
	        for (Luogo l : luoghi) {
	            if (l != null) l.aggiornaAttivoDaVisite();
	        }
	    }
	
	    aggiornaAttivoVolontariDaVisiteAttive();
	
	    fileIO.salvaUtenti(utenti);
	    fileIO.salvaLuoghi(luoghi);
	}

	private void ricostruisciVisiteDentroLuoghi() {
	    Map<String, Luogo> mappa = new HashMap<>();
	    for (Luogo l : luoghi) {
	        l.setVisite(new ArrayList<>());
	        mappa.put(l.getId(), l);
	    }
	
	    if (gestoreCalendario.getVisite() == null) return;
	
	    for (Visita v : gestoreCalendario.getVisite()) {
	        Luogo l = mappa.get(v.getIdLuogo());
	        if (l != null) l.aggiungiVisita(v);
	    }
	}

	private void aggiungiTipoScadutoSeManca(ArchivioStorico archivio, Visita v) {
	    if (archivio.getTipiVisitaScaduti() == null) {
	        archivio.setTipiVisitaScaduti(new ArrayList<>());
	    }
	
	    for (Visita old : archivio.getTipiVisitaScaduti()) {
	        if (old != null && v.getIdTipoVisita().equals(old.getIdTipoVisita())) {
	            return;
	        }
	    }
	
	    v.setAttivo(false);
	    archivio.getTipiVisitaScaduti().add(v);
	}

	private void aggiornaAttivoVolontariDaVisiteAttive() {
	    if (utenti == null) return;
	    List<Visita> visiteAttive = (gestoreCalendario.getVisite() != null) ? gestoreCalendario.getVisite() : List.of();
	
	    for (Utente u : utenti) {
	        if (u == null) continue;
	
	        if (!"volontario".equals(u.getRuolo())) {
	            u.setAttivo(true);
	            continue;
	        }

	        List<Visita> associate = new ArrayList<>();
	        for (Visita v : visiteAttive) {
	            if (v == null || !v.isAttivo()) continue;
	            if (v.getVolontari() != null && v.getVolontari().contains(u.getNomeUtente())) {
	                associate.add(v);
	            }
	        }
	
	        u.aggiornaAttivoDaVisite(associate);
	    }
	}

	public Set<LocalDate> getDisponibilitaVolontario(String nome) {
        getDisponibilitaVolontari().putIfAbsent(nome, new HashSet<>());
        return getDisponibilitaVolontari().get(nome);
    }

    public boolean verificaDisponibilitaVolontario(String nome, LocalDate data) {
        return getDisponibilitaVolontario(nome).contains(data);
    }

    public List<LocalDate> getDisponibilitaVolontarioNelMese(String nome, LocalDate meseEntrante) {
        return getDisponibilitaVolontario(nome).stream()
                .filter(d -> d.getMonth() == meseEntrante.getMonth() && d.getYear() == meseEntrante.getYear())
                .sorted()
                .toList();
    }

    public void aggiungiDisponibilitaVolontario(String nome, LocalDate data) throws IOException {
        getDisponibilitaVolontario(nome).add(data);
        fileIO.salvaDisponibilitaVolontari(getDisponibilitaVolontari());
    }

    public StatoSistema caricaOInizializzaStatoSistema() throws IOException {
	    LocalDate oggi = LocalDate.now();
	
	    StatoSistema stato = fileIO.caricaStatoSistema();
	
	    if (stato == null) {
	        YearMonth target = (oggi.getDayOfMonth() >= 16)
	                ? YearMonth.from(oggi).plusMonths(2)
	                : YearMonth.from(oggi).plusMonths(1);
	
	        stato = new StatoSistema(target, true, false);
	        fileIO.salvaStatoSistema(stato);
	        return stato;
	    }
	
	    boolean cambiato = sincronizzaStatoConCalendario(stato, oggi, true);
	    if (cambiato) {
	        fileIO.salvaStatoSistema(stato);
	    }
	
	    return stato;
	}

	private YearMonth meseTargetPerOggi(LocalDate oggi) {
	    YearMonth base = YearMonth.from(oggi);
	    return (oggi.getDayOfMonth() >= 16) ? base.plusMonths(2) : base.plusMonths(1);
	}

	private boolean oggiInFinestra(LocalDate oggi, YearMonth target) {
	    LocalDate start = target.minusMonths(2).atDay(16);
	    LocalDate end = target.minusMonths(1).atDay(15);
	    return (!oggi.isBefore(start)) && (!oggi.isAfter(end));
	}


	private boolean sincronizzaStatoConCalendario(StatoSistema stato, LocalDate oggi, boolean ignoraAutoRiapertura) {
	    boolean cambiato = false;
	
	    YearMonth targetAtteso = meseTargetPerOggi(oggi);
	
	    if (!stato.getMeseRaccolta().equals(targetAtteso)) {
	        stato.setMeseRaccolta(targetAtteso);
	        stato.setPianoProdotto(false);
	
	        stato.setRaccoltaAperta(oggi.getDayOfMonth() >= 16);
	        return true;
	    }
	
	    if (oggi.getDayOfMonth() == 15 && stato.isRaccoltaAperta()) {
	        stato.chiudiRaccolta();
	        cambiato = true;
	    }
	
	    if (!ignoraAutoRiapertura && oggi.getDayOfMonth() >= 16 && !stato.isRaccoltaAperta() && !stato.isPianoProdotto()) {
	        stato.setRaccoltaAperta(true);
	        cambiato = true;
	    }
	
	    return cambiato;
	}

	public void chiudiRaccoltaDisponibilita() throws IOException {
	    LocalDate oggi = LocalDate.now();
	    StatoSistema stato = caricaOInizializzaStatoSistema();
	
	    if (!oggiInFinestra(oggi, stato.getMeseRaccolta())) {
	        throw new IllegalArgumentException("Fuori finestra: non puoi chiudere la raccolta in questa data.");
	    }
	
	    if (!stato.isRaccoltaAperta()) {
	        throw new IllegalArgumentException("Raccolta già chiusa per " + stato.getMeseRaccolta() + ".");
	    }
	    if (stato.isPianoProdotto()) {
	        throw new IllegalArgumentException("Piano già prodotto per " + stato.getMeseRaccolta() + ".");
	    }
	
	    stato.chiudiRaccolta();
	    fileIO.salvaStatoSistema(stato);
	}

	public void riapriRaccoltaMeseSuccessivo() throws IOException {
	    LocalDate oggi = LocalDate.now();
	    StatoSistema stato = caricaOInizializzaStatoSistema();
	
	    if (!oggiInFinestra(oggi, stato.getMeseRaccolta())) {
	        System.out.println("Fuori finestra: non puoi aprire la raccolta in questa data.");
	        return;
	    }

	    if (oggi.getDayOfMonth() < 16) {
	        System.out.println("Raccolta non apribile prima del 16. Oggi è il " + oggi.getDayOfMonth() + ".");
	        return;
	    }
	
	    if (stato.isPianoProdotto()) {
	        System.out.println("Piano già prodotto per " + stato.getMeseRaccolta() + ". La raccolta per questo mese non si riapre.");
	        return;
	    }
	
	    if (stato.isRaccoltaAperta()) {
	        System.out.println("Raccolta già aperta per " + stato.getMeseRaccolta() + ".");
	        return;
	    }
	
	    stato.setRaccoltaAperta(true);
	    fileIO.salvaStatoSistema(stato);
	
	    System.out.println("Raccolta disponibilità APERTA per " + stato.getMeseRaccolta() + ".");
	}

    public List<VisitaIstanza> leggiPianoVisite() throws IOException {
        List<VisitaIstanza> istanze = fileIO.leggiVisiteIstanze();
        return (istanze != null) ? istanze : new ArrayList<>();
    }

	public List<VisitaIstanza> produciPianoVisite() throws IOException {
	    	
	        LocalDate oggi = LocalDate.now();
	        StatoSistema stato = caricaOInizializzaStatoSistema();
	
	        if (!oggiInFinestra(oggi, stato.getMeseRaccolta())) {
	            throw new IllegalArgumentException("Fuori finestra: puoi produrre il piano solo tra "
	                    + stato.inizioFinestra() + " e " + stato.fineFinestra() + ".");
	        }
	
	        if (stato.isRaccoltaAperta()) {
	            throw new IllegalArgumentException("Prima devi CHIUDERE la raccolta disponibilità.");
	        }
	        if (stato.isPianoProdotto()) {
	            return leggiPianoVisite();
	        }
	    	
	        Map<String, Set<LocalDate>> disponibilita = fileIO.leggiDisponibilitaVolontari();
	        if (disponibilita == null) disponibilita = new HashMap<>();
	
	        Set<LocalDate> precluseEffettive = fileIO.leggiDatePrecluse();
	        if (precluseEffettive == null) precluseEffettive = new HashSet<>();
	
	        this.datePrecluse = precluseEffettive;
	
	        YearMonth ym = stato.getMeseRaccolta();
	        LocalDate first = ym.atDay(1);
	        LocalDate last = ym.atEndOfMonth();
	
	        List<VisitaIstanza> istanze = new ArrayList<>();
	        Set<String> occupatoVolontarioGiorno = new HashSet<>();
	
	        for (LocalDate giorno = first; !giorno.isAfter(last); giorno = giorno.plusDays(1)) {
	            if (precluseEffettive.contains(giorno)) continue;
	
	            for (Luogo luogo : luoghi) {
	                List<Visita> tipi = luogo.getVisite();
	                if (tipi == null || tipi.isEmpty()) continue;
	
	                for (Visita tipo : tipi) {
	                    if (!tipo.isProgrammabile(giorno)) continue;
	
	                    String nickScelto = scegliVolontarioDisponibile(
	                            tipo, giorno, disponibilita, occupatoVolontarioGiorno
	                    );
	                    if (nickScelto == null) continue;
	
	                    occupatoVolontarioGiorno.add(keyVol(nickScelto, giorno));
	
	                    VisitaIstanza vi = new VisitaIstanza(
	                            tipo.getIdTipoVisita(),
	                            giorno,
	                            nickScelto,
	                            StatiVisita.proposta
	                    );
	
	                    istanze.add(vi);
	                }
	            }
	        }
	
	        fileIO.salvaVisiteIstanze(istanze);
	        stato.segnaPianoProdotto();
	        fileIO.salvaStatoSistema(stato);
	        fileIO.dimenticaDatiFinoAlMeseIncluso(ym);
	
	        return istanze;
	    }

    private String scegliVolontarioDisponibile(
            Visita tipo,
            LocalDate giorno,
            Map<String, Set<LocalDate>> disponibilita,
            Set<String> volontarioOccupatoGiorno) {

        List<String> volontariTipo = tipo.getVolontari();
        if (volontariTipo == null) return null;

        for (String nome : volontariTipo) {
            if (nome == null || nome.isBlank()) continue;

            Set<LocalDate> disp = disponibilita.get(nome);
            if (disp == null || !disp.contains(giorno)) continue;

            if (volontarioOccupatoGiorno.contains(keyVol(nome, giorno))) continue;

            return nome;
        }
        return null;
    }

    private String keyVol(String nome, LocalDate giorno) {
        return nome + "::" + giorno;
    }

	public void rimuoviLuogoConCascata(String idLuogo) throws IOException {
	    rimuoviLuogoDiretto(idLuogo);
	    chiusuraTransitiva("luogo", idLuogo);
	    salvaTuttoCorpoDati();
	
	    sincronizzaStatiAttiviEArchivio(LocalDate.now());
	}

	public void rimuoviTipoVisitaConCascata(String idTipoVisita) throws IOException {
	    rimuoviTipoDiretto(idTipoVisita);
	    chiusuraTransitiva("visita", idTipoVisita);
	    salvaTuttoCorpoDati();
	
	    sincronizzaStatiAttiviEArchivio(LocalDate.now());
	}

	public void rimuoviVolontarioConCascata(String nome) throws IOException {
	    rimuoviVolontarioDiretto(nome);
	    chiusuraTransitiva("volontario", nome);
	    salvaTuttoCorpoDati();

	    sincronizzaStatiAttiviEArchivio(LocalDate.now());
	}

	private void rimuoviLuogoDiretto(String idLuogo) {
	    luoghi.removeIf(l -> l.getId().equals(idLuogo));
	    if (gestoreCalendario.getVisite() != null) {
	        gestoreCalendario.getVisite().removeIf(v -> idLuogo.equals(v.getIdLuogo()));
	    }
	}

	private void rimuoviTipoDiretto(String idTipoVisita) {
	    if (gestoreCalendario.getVisite() != null) {
	        gestoreCalendario.getVisite().removeIf(v -> idTipoVisita.equals(v.getIdTipoVisita()));
	    }
	    for (Luogo l : luoghi) {
	        if (l.getVisite() != null) {
	            l.getVisite().removeIf(v -> idTipoVisita.equals(v.getIdTipoVisita()));
	        }
	    }
	}

	private void rimuoviVolontarioDiretto(String nome) {
	    utenti.removeIf(u -> "volontario".equals(u.getRuolo()) && nome.equals(u.getNomeUtente()));
	
	    if (gestoreCalendario.getVisite() != null) {
	        for (Visita v : gestoreCalendario.getVisite()) {
	            if (v.getVolontari() != null) {
	                v.getVolontari().removeIf(vol -> nome.equals(vol));
	            }
	        }
	    }
	
	    try {
	        Map<String, Set<LocalDate>> disp = fileIO.leggiDisponibilitaVolontari();
	        if (disp != null) {
	            disp.remove(nome);
	            fileIO.salvaDisponibilitaVolontari(disp);
	        }
	    } catch (IOException ignored) {
	    }
	}

	 private void chiusuraTransitiva(String tipo, String chiave) {
	        boolean cambiato;
	        do {
	            cambiato = false;

	            List<String> tipiDaEliminare = new ArrayList<>();
	            if (gestoreCalendario.getVisite() != null) {
	                for (Visita v : gestoreCalendario.getVisite()) {
	                    if (v.getVolontari() == null || v.getVolontari().isEmpty()) {
	                    	if(tipo.equals("visita") && v.getIdTipoVisita().equals(chiave)) {
	                            tipiDaEliminare.add(v.getIdTipoVisita());
	                    	}else if (!tipo.equals("visita")){
	                    		tipiDaEliminare.add(v.getIdTipoVisita());
	                    	}
	                    }
	                }
	            }

	            if (!tipiDaEliminare.isEmpty()) {
	                gestoreCalendario.getVisite().removeIf(v -> tipiDaEliminare.contains(v.getIdTipoVisita()));
	                cambiato = true;
	            }

	            Map<String, Integer> countTipiPerLuogo = new HashMap<>();
	            if (gestoreCalendario.getVisite() != null) {
	                for (Visita v : gestoreCalendario.getVisite()) {
	                    countTipiPerLuogo.merge(v.getIdLuogo(), 1, Integer::sum);
	                }
	            }

	            List<String> luoghiDaEliminare = new ArrayList<>();
	            for (Luogo l : luoghi) {
	                if (countTipiPerLuogo.getOrDefault(l.getId(), 0) == 0) {
	                	if(tipo.equals("luogo") && l.getId().equals(chiave)) {
	                		luoghiDaEliminare.add(l.getId());
	                	}else if (!tipo.equals("luogo") && l.isAttivo()){
	                		luoghiDaEliminare.add(l.getId());
	                	}
	                    
	                }
	            }

	            if (!luoghiDaEliminare.isEmpty()) {
	                luoghi.removeIf(l -> luoghiDaEliminare.contains(l.getId()));
	                cambiato = true;
	            }

	            Set<String> volontariAncoraAssociati = new HashSet<>();
	            if (gestoreCalendario.getVisite() != null) {
	                for (Visita v : gestoreCalendario.getVisite()) {
	                    if (v.getVolontari() != null) {
	                        volontariAncoraAssociati.addAll(v.getVolontari());
	                    }
	                }
	            }

	            List<String> volontariDaEliminare = new ArrayList<>();
	            for (Utente u : utenti) {
	                if ("volontario".equals(u.getRuolo())
	                        && !volontariAncoraAssociati.contains(u.getNomeUtente())) {
		                	if(tipo.equals("volontario") && u.getNomeUtente().equals(chiave)) {
		                		volontariDaEliminare.add(u.getNomeUtente());
		                	}else if(!tipo.equals("volontario") && u.isAttivo()) {
		                		volontariDaEliminare.add(u.getNomeUtente());
		                	}
	                    
	                }
	            }

	            if (!volontariDaEliminare.isEmpty()) {
	                for (String nick : volontariDaEliminare) {
	                    rimuoviVolontarioDiretto(nick);
	                }
	                cambiato = true;
	            }

	        } while (cambiato);

	        ricostruisciVisiteDentroLuoghi();
	    }

	private void salvaTuttoCorpoDati() throws IOException {
	    fileIO.salvaUtenti(utenti);
	    fileIO.salvaVisite(gestoreCalendario.getVisite());
	    fileIO.salvaLuoghi(luoghi);
	}

	private String generaCodicePrenotazioneUnico(List<VisitaIstanza> piano) {
        while (true) {
            String codice = java.util.UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();

            boolean esiste = false;

            for (VisitaIstanza vi : piano) {
                for (Iscrizione i : vi.getIscrizioni()) {
                    if (codice.equals(i.getCodicePrenotazione())) {
                        esiste = true;
                        break;
                    }
                }
                if (esiste) break;
            }

            if (!esiste) return codice;
        }
    }
    
    private Visita trovaTipoVisita(String idTipoVisita) {
        if (gestoreCalendario.getVisite() == null) return null;

        for (Visita v : gestoreCalendario.getVisite()) {
            if (idTipoVisita.equals(v.getIdTipoVisita())) {
                return v;
            }
        }
        return null;
    }
    
    private void salvaPianoVisite() throws IOException {
        fileIO.salvaVisiteIstanze(pianoVisite);
    }

    private List<VisitaIstanza> getPianoVisiteCaricato() throws IOException {
        if (pianoVisite == null || pianoVisite.isEmpty()) {
            pianoVisite = leggiPianoVisite();
        }
        return pianoVisite;
    }
    

    public List<VisitaIstanza> visiteIscrivibili() throws IOException {
        List<VisitaIstanza> piano = getPianoVisiteCaricato();
        aggiornaStatiIscrizioni(LocalDate.now());

        List<VisitaIstanza> out = new ArrayList<>();
        LocalDate oggi = LocalDate.now();

        for (VisitaIstanza vi : piano) {
            if (vi.getStato() != StatiVisita.proposta) continue;

            LocalDate chiusura = vi.getData().minusDays(3);
            if (!oggi.isBefore(chiusura)) continue;

            Visita tipo = trovaTipoVisita(vi.getIdTipoVisita());
            if (tipo == null) continue;

            if (vi.postiOccupati() >= tipo.getNumMaxPartecipanti()) continue;

            out.add(vi);
        }

        out.sort((a, b) -> a.getData().compareTo(b.getData()));
        return out;
    }
    
    public String iscriviFruitoreAVisita(VisitaIstanza selezionata, String usernameFruitore, int numPersone) throws IOException {
        if (selezionata == null) throw new IllegalArgumentException("Visita non valida.");

        List<VisitaIstanza> piano = getPianoVisiteCaricato();
        aggiornaStatiIscrizioni(LocalDate.now());

        VisitaIstanza target = null;
        for (VisitaIstanza vi : piano) {
            if (vi.getData().equals(selezionata.getData())
                    && vi.getIdTipoVisita().equals(selezionata.getIdTipoVisita())
                    && vi.getNomeVolontario().equals(selezionata.getNomeVolontario())) {
                target = vi;
                break;
            }
        }
        if (target == null) throw new IllegalArgumentException("Visita non trovata nel piano.");

        LocalDate oggi = LocalDate.now();
        LocalDate chiusura = target.getData().minusDays(3);

        if (!oggi.isBefore(chiusura)) {
            throw new IllegalArgumentException("Iscrizioni chiuse: chiudono il " + chiusura + " (3 giorni prima della visita).");
        }

        if (target.getStato() != StatiVisita.proposta) {
            throw new IllegalArgumentException("Iscrizione consentita solo su visite proposte.");
        }

        for (Iscrizione i : target.getIscrizioni()) {
            if (usernameFruitore.equals(i.getUsernameFruitore())) {
                throw new IllegalArgumentException("Sei già iscritto a questa visita: non puoi fare una seconda iscrizione.");
            }
        }

        Visita tipo = trovaTipoVisita(target.getIdTipoVisita());
        if (tipo == null) throw new IllegalArgumentException("Tipo visita non trovato.");

        int capienza = tipo.getNumMaxPartecipanti();
        int occupati = target.postiOccupati();
        int rimasti = capienza - occupati;

        if (numPersone > rimasti) {
            throw new IllegalArgumentException("Iscrizione non accettata: posti rimasti disponibili = " + rimasti + ".");
        }

        String codice = generaCodicePrenotazioneUnico(piano);
        target.aggiungiIscrizione(new Iscrizione(codice, usernameFruitore, numPersone));

        if (target.postiOccupati() == capienza) {
            target.setStato(StatiVisita.completa);
        }

        salvaPianoVisite();
        return codice;
    }
    
    public Utente creaFruitore(String username, String passwordPredefinita) throws IOException {
	    if (esisteNomeUtente(username)) {
	        throw new IllegalArgumentException("Username già esistente: " + username);
	    }
	
	    Utente nuovo = new Utente(username, passwordPredefinita, "fruitore", false, true);
	    this.utenti.add(nuovo);
	    this.fileIO.salvaUtenti(this.utenti);
	    return nuovo;
	}

	public boolean disdiciIscrizioneByCodice(String usernameFruitore, String codicePrenotazione) throws IOException {
        List<VisitaIstanza> piano = getPianoVisiteCaricato();
        aggiornaStatiIscrizioni(LocalDate.now());

        String codice = codicePrenotazione.trim().toUpperCase();
        LocalDate oggi = LocalDate.now();

        for (VisitaIstanza vi : piano) {

            if (!(vi.getStato() == StatiVisita.proposta || vi.getStato() == StatiVisita.completa)) continue;

            LocalDate chiusura = vi.getData().minusDays(3);

            if (!oggi.isBefore(chiusura)) {
                continue;
            }

            Iscrizione iscr = vi.trovaIscrizione(codice);
            if (iscr == null) continue;

            if (!usernameFruitore.equals(iscr.getUsernameFruitore())) {
                throw new IllegalArgumentException("Operazione negata: il codice non appartiene al tuo utente.");
            }

            boolean rimossa = vi.rimuoviIscrizionePerCodice(codice);
            if (!rimossa) return false;

            Visita tipo = trovaTipoVisita(vi.getIdTipoVisita());
            if (tipo != null && vi.getStato() == StatiVisita.completa && vi.postiOccupati() < tipo.getNumMaxPartecipanti()) {
                vi.setStato(StatiVisita.proposta);
            }

            salvaPianoVisite();
            return true;
        }

        return false;
    }
    
    public void stampaVisiteConfermatePerVolontario(String nicknameVolontario) {
        try {
            List<VisitaIstanza> piano = getPianoVisiteCaricato();
            aggiornaStatiIscrizioni(LocalDate.now());

            boolean almenoUna = false;

            for (VisitaIstanza vi : piano) {
                if (vi.getStato() != StatiVisita.confermata          
                		&& vi.getStato() != StatiVisita.proposta
                		&& vi.getStato() != StatiVisita.completa
                		) continue;
                if (!nicknameVolontario.equals(vi.getNomeVolontario())) continue;

                Visita tipo = trovaTipoVisita(vi.getIdTipoVisita());
                almenoUna = true;

                System.out.println(vi.toStringPerVolontario(tipo));
            }

            if (!almenoUna && caricaOInizializzaStatoSistema().isPianoProdotto()) {
                System.out.println("Non hai visite come guida in questo piano.");
            }else if(!almenoUna && !caricaOInizializzaStatoSistema().isPianoProdotto()) {
            	System.out.println("Piano non prodotto.");
            }

        } catch (IOException e) {
            System.out.println("Errore lettura piano visite: " + e.getMessage());
        }
    }
    
    private void archiviaVisitaEffettuata(VisitaIstanza vi) throws IOException {
        if (vi == null) return;

        ArchivioStorico archivio = fileIO.leggiArchivioStorico();
        if (archivio == null) archivio = new ArchivioStorico();

        List<VisitaIstanza> storico = archivio.getVisiteEffettuate();
        if (storico == null) {
            storico = new ArrayList<>();
            archivio.setVisiteEffettuate(storico);
        }

        boolean giaPresente = false;
        for (VisitaIstanza s : storico) {
            if (s == null) continue;

            if (vi.getIdTipoVisita().equals(s.getIdTipoVisita())
                    && vi.getData().equals(s.getData())
                    && vi.getNomeVolontario().equals(s.getNomeVolontario())) {
                giaPresente = true;
                break;
            }
        }

        if (!giaPresente) {
            storico.add(vi);
            fileIO.salvaArchivioStorico(archivio);
        }
    }
    
    public void stampaVisitePerFruitore(String usernameFruitoreOrNull) {
        try {
            List<VisitaIstanza> piano = getPianoVisiteCaricato();
            aggiornaStatiIscrizioni(LocalDate.now());

            boolean almenoUna = false;

            for (VisitaIstanza vi : piano) {
            	if (usernameFruitoreOrNull == null) {
            	    if (!(vi.getStato() == StatiVisita.proposta
            	            || vi.getStato() == StatiVisita.confermata
            	            || vi.getStato() == StatiVisita.cancellata)) {
            	        continue;
            	    }
            	} else {
            	    if (!(vi.getStato() == StatiVisita.proposta
            	            || vi.getStato() == StatiVisita.completa
            	            || vi.getStato() == StatiVisita.confermata
            	            || vi.getStato() == StatiVisita.cancellata)) {
            	        continue;
            	    }
            	}

                if (usernameFruitoreOrNull != null && !isFruitoreIscritto(vi, usernameFruitoreOrNull)) {
                    continue;
                }

                Visita tipo = trovaTipoVisita(vi.getIdTipoVisita());
                if (tipo == null) continue;

                almenoUna = true;

                System.out.println(vi.toStringDettagliato(tipo, usernameFruitoreOrNull));
            }

            if (!almenoUna) {
                System.out.println("Nessuna visita da visualizzare.");
            }

        } catch (IOException e) {
            System.out.println("Errore lettura piano visite: " + e.getMessage());
        }
    }
    
    private void aggiornaStatiIscrizioni(LocalDate oggi) throws IOException {
        List<VisitaIstanza> piano = getPianoVisiteCaricato();
        boolean cambiato = false;

        var it = piano.iterator();
        while (it.hasNext()) {
            VisitaIstanza vi = it.next();

            Visita tipo = trovaTipoVisita(vi.getIdTipoVisita());
            if (tipo == null) continue;

            LocalDate dataVisita = vi.getData();
            LocalDate chiusura = dataVisita.minusDays(3);

            if (!oggi.isBefore(chiusura) &&
                    (vi.getStato() == StatiVisita.proposta || vi.getStato() == StatiVisita.completa)) {

                if (vi.getStato() == StatiVisita.completa) {
                    vi.setStato(StatiVisita.confermata);
                } else {
                    int partecipanti = vi.postiOccupati();
                    if (partecipanti >= tipo.getNumMinPartecipanti()) {
                        vi.setStato(StatiVisita.confermata);
                    } else {
                        vi.setStato(StatiVisita.cancellata);
                    }
                }

                cambiato = true;
            }

            if (vi.getStato() == StatiVisita.cancellata && oggi.isAfter(dataVisita)) {
                it.remove();
                cambiato = true;
                continue;
            }

            if (vi.getStato() == StatiVisita.confermata && oggi.isAfter(dataVisita)) {
                vi.setStato(StatiVisita.effettuata);
                archiviaVisitaEffettuata(vi);
                it.remove();
                cambiato = true;
            }
        }

        if (cambiato) salvaPianoVisite();
    }
    
    private boolean isFruitoreIscritto(VisitaIstanza vi, String usernameFruitore) {
        for (Iscrizione i : vi.getIscrizioni()) {
            if (usernameFruitore.equals(i.getUsernameFruitore())) return true;
        }
        return false;
    }
    
    public String descrizioneBreveVisitaIstanza(VisitaIstanza vi) {
	    Visita tipo = trovaTipoVisita(vi.getIdTipoVisita());
	    return vi.toStringBreve(tipo);
	}
}