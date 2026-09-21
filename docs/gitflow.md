# Ściąga: git-flow (AVH edition) w CLI

Ten dokument to command-line odpowiednik operacji wykonywanych przez ten plugin.
Flagi zweryfikowane bezpośrednio ze źródeł [petervanderdoes/gitflow](https://github.com/petervanderdoes/gitflow) (AVH edition).

## Wymagania wstępne

```bash
git flow version
```

Sprawdza wersję i implementację. Musi pokazać coś z „AVH” (np. `AVH Edition - v1.12.3`) — wersja `0.4.1` (nvie, oryginalna) **nie obsługuje** bugfixów ani squasha.

---

## 1. Inicjalizacja repozytorium

```bash
git flow init
```

Interaktywnie konfiguruje nazwy gałęzi (`main`/`develop`) i prefiksy (`feature/`, `release/`, `bugfix/`, `hotfix/`).

```bash
git flow init -d
```

Jak wyżej, ale bez pytań — od razu bierze wartości domyślne (`develop`, `feature/`, `release/`, `hotfix/`, `bugfix/`, brak prefiksu tagów).

```bash
git flow init -f
```

Wymusza ponowną konfigurację, nawet jeśli repo jest już zainicjalizowane pod git-flow.

---

## 2. Feature (`feature/*` → merge do `develop`)

Nazwa brancha = numer taska, np. `FW-17422`.

**Start** — tworzy branch feature z `develop`:

```bash
git flow feature start FW-17422
git flow feature start FW-17422 -F        # -F = najpierw fetch z origin
git flow feature start FW-17422 release/2026-09-21   # start z innej bazy niż develop
```

**Finish** — merguje feature do `develop` i usuwa branch:

```bash
git flow feature finish FW-17422
```

| Flaga | Znaczenie |
|---|---|
| `-F, --fetch` | fetch z origin przed finishem |
| `-r, --rebase` | rebase przed mergem zamiast zwykłego mergu |
| `--push` | push do origin po zakończeniu |
| `-k, --keep` | nie usuwaj brancha po finishu |
| `--keeplocal` | zachowaj tylko lokalny branch |
| `--keepremote` | zachowaj tylko zdalny branch |
| `-D, --force_delete` | wymuś usunięcie brancha nawet jeśli nie w pełni zmergowany |
| `--no-ff` | zawsze twórz commit mergujący, bez fast-forward |
| `-S, --squash` | **squash** — cała historia feature trafia do develop jako jeden commit |

Przykłady:

```bash
git flow feature finish FW-17422 -S              # squash merge
git flow feature finish FW-17422 -S --push       # squash + push develop na origin
git flow feature finish FW-17422 --no-ff -k      # zwykły merge commit, zachowaj branch
```

**Publish / Track / Pull** (współdzielenie brancha z zespołem):

```bash
git flow feature publish FW-17422        # wypchnij branch feature na origin
git flow feature track FW-17399          # ściągnij i zacznij śledzić task kolegi z zespołu
git flow feature pull origin FW-17399    # pobierz zmiany do śledzonego feature
```

---

## 3. Bugfix (`bugfix/*` → merge do `develop`)

Identyczna logika jak feature, tylko `bugfix` zamiast `feature` — nazwa brancha to też numer taska, np. `FW-17588`:

```bash
git flow bugfix start FW-17588
git flow bugfix finish FW-17588 -S           # squash merge
git flow bugfix finish FW-17588 --no-ff --push
git flow bugfix publish FW-17588
git flow bugfix track FW-17588
```

Flagi finish: `-F`, `-r/--rebase`, `--push`, `-k/--keep`, `--keeplocal`, `--keepremote`, `-D/--force_delete`, `--no-ff`, `-S/--squash` (dokładnie te same co feature).

---

## 4. Release (`release/*` → merge do `main` **i** `develop` + tag)

Nazwa brancha to data w formacie `RRRR-MM-DD`, np. `2026-09-21`. Jeśli tego samego dnia wychodzi kolejne wydanie, dopisujemy numer porządkowy: `2026-09-21-2`, `2026-09-21-3` itd.

**Start:**

```bash
git flow release start 2026-09-21
git flow release start 2026-09-21-2 -F   # drugie wydanie tego samego dnia
```

**Finish** — merguje do `main` i `develop`, taguje:

```bash
git flow release finish 2026-09-21
```

| Flaga | Znaczenie |
|---|---|
| `-F, --fetch` | fetch przed finishem |
| `-m, --message <msg>` | wiadomość tagu |
| `-f, --messagefile <plik>` | wiadomość tagu z pliku |
| `-s, --sign` | podpisz tag GPG |
| `-u, --signingkey <klucz>` | klucz do podpisu (implikuje `-s`) |
| `-p, --push` | push `main`, `develop` i tag na origin |
| `-k, --keep` / `--keeplocal` / `--keepremote` | zachowaj branch |
| `-n, --tag` (właściwie „don't tag”) | **nie** twórz tagu |
| `-b, --nobackmerge` | nie merguj z powrotem `main`/tagu do `develop` |
| `-S, --squash` | squash merge |
| `-T, --tagname <nazwa>` | własna nazwa tagu (zamiast numeru wersji) |

Przykłady:

```bash
git flow release finish 2026-09-21 -m "Release 2026-09-21" -p     # tag z wiadomością + push
git flow release finish 2026-09-21 -n                               # bez tagowania
```

**Publish / Track:**

```bash
git flow release publish 2026-09-21
git flow release track 2026-09-21
```

---

## 5. Hotfix (`hotfix/*` → merge do `main` **i** `develop` + tag)

Hotfix robimy do konkretnego taska, więc nazwa brancha to numer taska, np. `FW-17650`.

**Start** (domyślnie z `main`):

```bash
git flow hotfix start FW-17650
git flow hotfix start FW-17650 main    # jawnie wskazana baza
```

**Finish:**

```bash
git flow hotfix finish FW-17650 -m "Hotfix FW-17650"
```

Flagi identyczne jak w release: `-F`, `-m/-f` (wiadomość tagu), `-s/-u` (podpis), `-p/--push`, `-k/--keep(remote|local)`, `-n` (bez tagu), `-b/--nobackmerge`, `-S/--squash`, `-T/--tagname`.

```bash
git flow hotfix finish FW-17650 -n -p     # bez tagu, z pushem
git flow hotfix publish FW-17650
```

---

## Uwagi praktyczne

- **Nazewnictwo branchy**: feature/bugfix/hotfix nazywamy numerem taska, np. `FW-17422` — bez dodatkowego opisu w nazwie brancha. Release nazywamy datą `RRRR-MM-DD` (np. `2026-09-21`), a przy kilku wydaniach tego samego dnia dopisujemy numer porządkowy (`2026-09-21-2`).
- **`develop`, `main`** muszą już istnieć lokalnie zanim zrobisz `finish` (git-flow sam nie tworzy `main`, jeśli nigdy go nie było).
- `-S/--squash` **nie usuwa** potrzeby ręcznego pilnowania konfliktów — squash i tak może wymagać rozwiązania konfliktów przy mergu, tylko finalny commit na `develop` będzie jeden zamiast całej historii brancha.
- Jeśli robisz `--squash` i chcesz zachować info o źródłowym branchu w wiadomości commita, dodaj `--squash-info` (feature/release/hotfix).
- Wszystkie te komendy to dokładnie to, co plugin wykonuje pod spodem przez `git flow ...` — więc przechodząc na CLI, zachowanie będzie identyczne (plus dostęp do flag, których plugin w ogóle nie wystawiał, np. `-r/--rebase`, `--nobackmerge`, `-T/--tagname`, `--force_delete`).
