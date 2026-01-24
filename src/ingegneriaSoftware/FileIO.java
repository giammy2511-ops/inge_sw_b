package ingegneriaSoftware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/*
Gestisce la persistenza su file del corpo dati dell’applicazione.

Rispetto alle versioni precedenti:
- mantiene la persistenza delle disponibilità dei volontari
- introduce la persistenza dello stato del sistema (per gestire la fase/periodo corrente)
- introduce la persistenza delle istanze di visita (piano visite e storico operativo)
- supporta la scrittura completa delle date precluse e la rimozione dei dati non più necessari

Invarianti di classe:
- baseDir esiste ed è una directory valida
- i metodi di lettura ritornano strutture vuote (non null) quando il file non esiste o è vuoto,
  tranne leggiDatiGenerali e caricaStatoSistema che possono restituire null se i dati non sono presenti o non validi
- la serializzazione JSON è gestita in modo consistente anche per date e durate
*/
public class FileIO {

    private final Path baseDir;

    private static final String FILE_PATH_CREDENZIALI = "credenziali.json";
    private static final String FILE_PATH_LUOGHI = "luoghi.json";
    private static final String FILE_PATH_VISITE = "visite.json";
    private static final String FILE_PATH_GENERALE = "generale.txt";
    private static final String FILE_PATH_DATE_PRECLUSE = "datePrecluse.txt";
    private static final String FILE_PATH_DISPONIBILITA = "disponibilitaVolontari.txt";
    private static final String FILE_PATH_STATO_SISTEMA = "statoSistema.json";
    private static final String FILE_PATH_VISITE_ISTANZE = "visiteIstanze.json";
    private static final String FILE_PATH_ARCHIVIO_STORICO = "archivioStorico.json";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);

    public FileIO(String baseDir) throws IOException {
        this.baseDir = Path.of(baseDir);
        Files.createDirectories(this.baseDir);
    }

    private Path path(String nomeFile) {
        if (nomeFile == null || nomeFile.isBlank()) {
            return baseDir;
        }
        return baseDir.resolve(nomeFile);
    }

    public List<Utente> leggiUtenti() throws IOException {
        Path p = path(FILE_PATH_CREDENZIALI);
        if (!Files.exists(p) || Files.size(p) == 0) {
            return new ArrayList<>();
        }
        List<Utente> letti = mapper.readValue(p.toFile(), new TypeReference<List<Utente>>() {});
        return (letti != null) ? letti : new ArrayList<>();
    }

    public void salvaUtenti(List<Utente> utenti) throws IOException {
        List<Utente> daSalvare = (utenti != null) ? utenti : new ArrayList<>();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path(FILE_PATH_CREDENZIALI).toFile(), daSalvare);
    }

    public DatiGenerali leggiDatiGenerali() throws IOException {
        Path p = path(FILE_PATH_GENERALE);
        if (!Files.exists(p) || Files.size(p) == 0) {
            return null;
        }

        String riga = Files.readString(p).trim();
        if (riga.isBlank()) return null;

        String[] parti = riga.split(",");
        if (parti.length < 2) return null;

        String ambito = parti[0].trim();
        String maxStr = parti[1].trim();

        try {
            int max = Integer.parseInt(maxStr);
            return new DatiGenerali(ambito, max);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void salvaDatiGenerali(DatiGenerali dati) throws IOException {
        if (dati == null) return;
        Files.writeString(
                path(FILE_PATH_GENERALE),
                dati.ambitoTerritoriale() + "," + dati.maxPersoneIscrivibiliDaFruitore()
        );
    }

    public List<Luogo> leggiLuoghi() throws IOException {
        Path p = path(FILE_PATH_LUOGHI);
        if (!Files.exists(p) || Files.size(p) == 0) {
            return new ArrayList<>();
        }
        List<Luogo> letti = mapper.readValue(p.toFile(), new TypeReference<List<Luogo>>() {});
        return (letti != null) ? letti : new ArrayList<>();
    }

    public void salvaLuoghi(List<Luogo> luoghi) throws IOException {
        List<Luogo> daSalvare = (luoghi != null) ? luoghi : new ArrayList<>();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path(FILE_PATH_LUOGHI).toFile(), daSalvare);
    }

    public List<Visita> leggiVisite() throws IOException {
        Path p = path(FILE_PATH_VISITE);
        if (!Files.exists(p) || Files.size(p) == 0) {
            return new ArrayList<>();
        }
        List<Visita> lette = mapper.readValue(p.toFile(), new TypeReference<List<Visita>>() {});
        return (lette != null) ? lette : new ArrayList<>();
    }

    public void salvaVisite(List<Visita> visite) throws IOException {
        List<Visita> daSalvare = (visite != null) ? visite : new ArrayList<>();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path(FILE_PATH_VISITE).toFile(), daSalvare);
    }

    public Set<LocalDate> leggiDatePrecluse() throws IOException {
        Set<LocalDate> date = new HashSet<>();
        Path p = path(FILE_PATH_DATE_PRECLUSE);
        if (!Files.exists(p)) return date;

        for (String r : Files.readAllLines(p)) {
            if (r != null && !r.isBlank()) {
                date.add(LocalDate.parse(r));
            }
        }
        return date;
    }

    public void appendDataPreclusa(LocalDate d) throws IOException {
        if (d == null) return;
        Files.writeString(
                path(FILE_PATH_DATE_PRECLUSE),
                d.toString() + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    public void salvaDatePrecluse(Set<LocalDate> date) throws IOException {
        List<String> righe = date.stream()
                .sorted()
                .map(LocalDate::toString)
                .toList();

        Files.write(
                path(FILE_PATH_DATE_PRECLUSE),
                righe,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public Map<String, Set<LocalDate>> leggiDisponibilitaVolontari() throws IOException {
        Map<String, Set<LocalDate>> disponibilitaPerVolontario = new HashMap<>();
        Path percorsoFile = path(FILE_PATH_DISPONIBILITA);

        if (!Files.exists(percorsoFile) || Files.size(percorsoFile) == 0) {
            return disponibilitaPerVolontario;
        }

        for (String riga : Files.readAllLines(percorsoFile)) {
            if (riga.isBlank()) continue;

            String[] partiRiga = riga.split(":");
            if (partiRiga.length != 2) continue;

            String nickname = partiRiga[0].trim();
            if (nickname.isBlank()) continue;

            Set<LocalDate> dateDisponibili = new HashSet<>();
            String parteDate = partiRiga[1].trim();

            if (!parteDate.isBlank()) {
                String[] singoleDate = parteDate.split(",");
                for (String dataTesto : singoleDate) {
                    String dataPulita = dataTesto.trim();
                    if (!dataPulita.isBlank()) {
                        dateDisponibili.add(LocalDate.parse(dataPulita));
                    }
                }
            }

            disponibilitaPerVolontario.put(nickname, dateDisponibili);
        }

        return disponibilitaPerVolontario;
    }

    public void salvaDisponibilitaVolontari(Map<String, Set<LocalDate>> mappa) throws IOException {
        Map<String, Set<LocalDate>> ordinata = new TreeMap<>(mappa);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<LocalDate>> e : ordinata.entrySet()) {
            String nick = e.getKey();

            String lista = e.getValue().stream()
                    .sorted()
                    .map(LocalDate::toString)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            sb.append(nick).append(":").append(lista).append("\n");
        }

        Files.writeString(
                path(FILE_PATH_DISPONIBILITA),
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public void salvaStatoSistema(StatoSistema stato) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path(FILE_PATH_STATO_SISTEMA).toFile(), stato);
    }

    public StatoSistema caricaStatoSistema() throws IOException {
        Path p = path(FILE_PATH_STATO_SISTEMA);
        if (!Files.exists(p) || Files.size(p) == 0) {
            return null;
        }

        try {
            return mapper.readValue(p.toFile(), StatoSistema.class);
        } catch (Exception e) {
            Files.writeString(p, "", StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            return null;
        }
    }

    public List<VisitaIstanza> leggiVisiteIstanze() throws IOException {
        Path p = path(FILE_PATH_VISITE_ISTANZE);
        if (!Files.exists(p) || Files.size(p) == 0) {
            return new ArrayList<>();
        }
        return mapper.readValue(p.toFile(), new TypeReference<List<VisitaIstanza>>() {});
    }

    public void salvaVisiteIstanze(List<VisitaIstanza> istanze) throws IOException {
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(path(FILE_PATH_VISITE_ISTANZE).toFile(), istanze);
    }

    public void dimenticaDatiFinoAlMeseIncluso(YearMonth mese) throws IOException {
        Map<String, Set<LocalDate>> disp = leggiDisponibilitaVolontari();
        boolean rimosseDate = false;
        boolean rimosseEntry = false;

        if (disp != null && !disp.isEmpty()) {

            for (Set<LocalDate> set : disp.values()) {
                if (set == null) continue;
                boolean changed = set.removeIf(d -> d != null && !YearMonth.from(d).isAfter(mese));
                rimosseDate |= changed;
            }

            int sizeBefore = disp.size();
            disp.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
            rimosseEntry = (disp.size() != sizeBefore);

            if (rimosseDate || rimosseEntry) {
                salvaDisponibilitaVolontari(disp);
            }
        }

        Set<LocalDate> precluse = leggiDatePrecluse();

        if (precluse != null && !precluse.isEmpty()) {
            boolean cambiatoPrecluse = precluse.removeIf(d -> d != null && !YearMonth.from(d).isAfter(mese));
            if (cambiatoPrecluse) {
                salvaDatePrecluse(precluse);
            }
        }
    }

    public ArchivioStorico leggiArchivioStorico() throws IOException {
        Path p = path(FILE_PATH_ARCHIVIO_STORICO);
        if (!Files.exists(p) || Files.size(p) == 0) {
            return new ArchivioStorico();
        }
        return mapper.readValue(p.toFile(), ArchivioStorico.class);
    }

    public void salvaArchivioStorico(ArchivioStorico archivio) throws IOException {
        if (archivio == null) archivio = new ArchivioStorico();
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(path(FILE_PATH_ARCHIVIO_STORICO).toFile(), archivio);
    }
}