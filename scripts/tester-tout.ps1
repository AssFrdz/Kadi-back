<#
.SYNOPSIS
    Lance tous les niveaux de tests de Kadi (back + front + bout en bout), du plus rapide au plus complet.

.DESCRIPTION
    Niveaux exécutés, conformément à la section 06 du dossier (démarche et Definition of Done) :
      1. Back  - tests unitaires et de propriétés (domaine, algorithmes, services avec faux adaptateurs, ArchUnit)
      2. Back  - tests d'intégration sur PostgreSQL Testcontainers (persistance, migrations, API REST, sécurité, concurrence)
      3. Back  - build officiel `gradlew build` (compilation, tous les tests, couverture JaCoCo)
      4. Front - tests unitaires des composants et services Angular (Vitest)
      5. Front - build de production Angular
      6. E2E   - parcours utilisateur réels avec Playwright (Python), sur ordinateur puis téléphone

    Le niveau 6 démarre lui-même PostgreSQL (Docker Compose), le back sur une base dédiée
    `kadi_e2e` (recréée à chaque exécution, jamais la base de travail `kadi`) avec le catalogue de démonstration,
    puis le front s'il ne tourne pas déjà. Il arrête ensuite ce qu'il a démarré.

.PARAMETER SansE2E
    N'exécute pas le niveau 6 (utile sans Docker Compose ou sans navigateur).

.PARAMETER SeulementE2E
    N'exécute que le niveau 6 (parcours de bout en bout).

.PARAMETER SansBuildComplet
    N'exécute pas le niveau 3 (les niveaux 1 et 2 ont déjà lancé tous les tests du back).

.PARAMETER ArretAuPremierEchec
    S'arrête dès qu'un niveau échoue, au lieu de lancer les suivants.

.PARAMETER Front
    Chemin du dépôt front (par défaut : le dossier `front` à côté du dossier `back`).

.EXAMPLE
    .\scripts\tester-tout.ps1
.EXAMPLE
    .\scripts\tester-tout.ps1 -SansE2E -SansBuildComplet
#>
param(
    [switch]$SansE2E,
    [switch]$SeulementE2E,
    [switch]$SansBuildComplet,
    [switch]$ArretAuPremierEchec,
    [string]$Front
)

$ErrorActionPreference = 'Stop'
$Back = Split-Path -Parent $PSScriptRoot
if (-not $Front) { $Front = Join-Path (Split-Path -Parent $Back) 'front' }
$Front = (Resolve-Path $Front).Path
$Journaux = Join-Path $Back 'build\tester-tout'
New-Item -ItemType Directory -Force $Journaux | Out-Null
$Resultats = New-Object System.Collections.Generic.List[object]
$ProcessusDemarres = New-Object System.Collections.Generic.List[object]

function Write-Banniere {
    param([string]$Numero, [string]$Titre, [string]$Explication)
    Write-Host ''
    Write-Host ('=' * 100) -ForegroundColor DarkCyan
    Write-Host (" NIVEAU {0} | {1}" -f $Numero, $Titre) -ForegroundColor Cyan
    Write-Host (" {0}" -f $Explication) -ForegroundColor Gray
    Write-Host ('=' * 100) -ForegroundColor DarkCyan
}

function Invoke-Commande {
    # Lance une commande native et échoue si son code de sortie n'est pas 0.
    param([string]$Dossier, [string]$Executable, [string[]]$Arguments)
    Push-Location $Dossier
    try {
        & $Executable @Arguments
        if ($LASTEXITCODE -ne 0) { throw "La commande « $Executable $($Arguments -join ' ') » a échoué (code $LASTEXITCODE)." }
    } finally {
        Pop-Location
    }
}

function Invoke-Niveau {
    param([string]$Numero, [string]$Titre, [string]$Explication, [scriptblock]$Action)
    Write-Banniere $Numero $Titre $Explication
    $chrono = [Diagnostics.Stopwatch]::StartNew()
    $statut = 'OK'
    $detail = ''
    try {
        & $Action
    } catch {
        $statut = 'ECHEC'
        $detail = $_.Exception.Message
        Write-Host ("[ECHEC] {0}" -f $detail) -ForegroundColor Red
    }
    $chrono.Stop()
    $Resultats.Add([pscustomobject]@{ Niveau = $Numero; Titre = $Titre; Statut = $statut; Duree = ('{0:mm\:ss}' -f $chrono.Elapsed); Detail = $detail })
    if ($statut -eq 'OK') {
        Write-Host ("[OK] Niveau {0} terminé en {1:mm\:ss}." -f $Numero, $chrono.Elapsed) -ForegroundColor Green
    } elseif ($ArretAuPremierEchec) {
        throw "Arrêt demandé au premier échec (niveau $Numero)."
    }
}

function Test-PortOccupe {
    param([int]$Port)
    try {
        $connexion = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop
        return [bool]$connexion
    } catch {
        return $false
    }
}

function Wait-Url {
    param([string]$Url, [int]$Secondes, [string]$Nom)
    $fin = (Get-Date).AddSeconds($Secondes)
    while ((Get-Date) -lt $fin) {
        try {
            $reponse = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($reponse.StatusCode -lt 500) { Write-Host ("  {0} répond sur {1}." -f $Nom, $Url); return }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "$Nom ne répond pas sur $Url après $Secondes secondes."
}

function Stop-ProcessusDemarres {
    foreach ($processus in $ProcessusDemarres) {
        if (-not $processus.HasExited) {
            Write-Host ("  Arrêt de {0} (PID {1})." -f $processus.ProcessName, $processus.Id)
            & taskkill.exe /PID $processus.Id /T /F | Out-Null
        }
    }
    $ProcessusDemarres.Clear()
}

Write-Host "Kadi - tests de tous les niveaux" -ForegroundColor White
Write-Host ("  Back  : {0}`n  Front : {1}`n  Journaux : {2}" -f $Back, $Front, $Journaux)

try {
    if (-not $SeulementE2E) {
        Invoke-Niveau '1' 'Back - tests unitaires et de propriétés' `
        'Domaine (Quantite, ListeCourses, NatureBesoin), similarité de Dice face à une référence exhaustive (jqwik), services avec faux adaptateurs, règles ArchUnit. Sans Spring ni base.' {
        Invoke-Commande $Back '.\gradlew.bat' @('test', '--tests', 'fr.miage.kadi.*Test', '--rerun', '--console=plain')
    }

    Invoke-Niveau '2' "Back - tests d'intégration PostgreSQL et API" `
        'PostgreSQL 17 réel (Testcontainers) : migrations Flyway, persistance, API REST (codes, problem+json), session, CSRF, isolation des comptes, concurrence, scénario de démonstration. Docker doit tourner.' {
        Invoke-Commande $Back '.\gradlew.bat' @('test', '--tests', 'fr.miage.kadi.*IT', '--rerun', '--console=plain')
    }

    if (-not $SansBuildComplet) {
        Invoke-Niveau '3' 'Back - build officiel (gradlew build)' `
            "Critère de la Definition of Done : compilation, tous les tests en une seule passe et rapport de couverture JaCoCo (build\reports\jacoco\test\html\index.html)." {
            Invoke-Commande $Back '.\gradlew.bat' @('build', '--console=plain')
        }
    }

    Invoke-Niveau '4' 'Front - tests des composants et services (Vitest)' `
        'Composants Angular avec API simulées : « Ma liste », champ de recherche partagé, appels HTTP (session, CSRF), états chargement / vide / erreur / 401 / 403.' {
        Invoke-Commande $Front 'npx.cmd' @('ng', 'test', '--watch=false')
    }

    Invoke-Niveau '5' 'Front - build de production' `
        'Compilation Angular optimisée, telle que déployée : vérifie les types, les gabarits et les budgets de taille.' {
        Invoke-Commande $Front 'npx.cmd' @('ng', 'build')
    }
    }

    if (-not $SansE2E) {
        Invoke-Niveau '6' 'E2E - parcours utilisateur Playwright (ordinateur puis téléphone)' `
            "Vrai navigateur, vrai back, vrai PostgreSQL (base kadi_e2e recréée), utilisateur simulé choisi dans la liste déroulante : scénario « lait / pâtes barilla / coca 6 », modification, suppression, listes séparées par profil." {
            if (Test-PortOccupe 8080) {
                throw "Le port 8080 est déjà utilisé. Arrête ton back avant de lancer les tests de bout en bout (ils démarrent leur propre back sur la base kadi_e2e)."
            }

            Write-Host '  Démarrage de PostgreSQL (Docker Compose)...'
            Invoke-Commande $Back 'docker' @('compose', 'up', '-d', 'db')
            $fin = (Get-Date).AddSeconds(60)
            do {
                Push-Location $Back
                & docker compose exec -T db pg_isready -U kadi | Out-Null
                $pret = ($LASTEXITCODE -eq 0)
                Pop-Location
                if (-not $pret) { Start-Sleep -Seconds 2 }
            } until ($pret -or (Get-Date) -gt $fin)
            if (-not $pret) { throw 'PostgreSQL ne répond pas.' }

            Write-Host '  Recréation de la base de test kadi_e2e (la base de travail kadi n''est pas touchée)...'
            Invoke-Commande $Back 'docker' @('compose', 'exec', '-T', 'db', 'psql', '-U', 'kadi', '-d', 'postgres', '-v', 'ON_ERROR_STOP=1',
                '-c', 'DROP DATABASE IF EXISTS kadi_e2e WITH (FORCE)', '-c', 'CREATE DATABASE kadi_e2e')

            Write-Host '  Démarrage du back (profil demo ; base kadi_e2e)...'
            $anciensProfils = $env:SPRING_PROFILES_ACTIVE
            $ancienneUrl = $env:SPRING_DATASOURCE_URL
            $env:SPRING_PROFILES_ACTIVE = 'demo'
            $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/kadi_e2e'
            try {
                $back = Start-Process -FilePath (Join-Path $Back 'gradlew.bat') -ArgumentList 'bootRun', '--console=plain' `
                    -WorkingDirectory $Back -PassThru -WindowStyle Hidden `
                    -RedirectStandardOutput (Join-Path $Journaux 'back-e2e.log') -RedirectStandardError (Join-Path $Journaux 'back-e2e-erreurs.log')
                $ProcessusDemarres.Add($back)
            } finally {
                $env:SPRING_PROFILES_ACTIVE = $anciensProfils
                $env:SPRING_DATASOURCE_URL = $ancienneUrl
            }
            Wait-Url 'http://127.0.0.1:8080/actuator/health' 240 'Le back'

            if (Test-PortOccupe 4200) {
                Write-Host '  Le front tourne déjà sur le port 4200 : il est réutilisé (il doit servir le code à tester).' -ForegroundColor Yellow
            } else {
                Write-Host '  Démarrage du front (npm start)...'
                $frontProcessus = Start-Process -FilePath 'npm.cmd' -ArgumentList 'start' -WorkingDirectory $Front -PassThru -WindowStyle Hidden `
                    -RedirectStandardOutput (Join-Path $Journaux 'front-e2e.log') -RedirectStandardError (Join-Path $Journaux 'front-e2e-erreurs.log')
                $ProcessusDemarres.Add($frontProcessus)
            }
            Wait-Url 'http://127.0.0.1:4200/' 180 'Le front'

            $e2e = Join-Path $Front 'e2e'
            $python = Join-Path $e2e '.venv\Scripts\python.exe'
            if (-not (Test-Path $python)) {
                Write-Host '  Première exécution : création de l''environnement Python des tests (e2e\.venv)...'
                Invoke-Commande $e2e 'python' @('-m', 'venv', '.venv')
                Invoke-Commande $e2e $python @('-m', 'pip', 'install', '--quiet', '-r', 'requirements.txt')
                Invoke-Commande $e2e $python @('-m', 'playwright', 'install', 'chromium')
            }
            Invoke-Commande $e2e $python @('-m', 'pytest', '-v', '--junitxml', (Join-Path $Journaux 'e2e-junit.xml'))
        }
    }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
} finally {
    Stop-ProcessusDemarres
}

Write-Host ''
Write-Host ('=' * 100) -ForegroundColor DarkCyan
Write-Host ' RÉCAPITULATIF' -ForegroundColor Cyan
Write-Host ('=' * 100) -ForegroundColor DarkCyan
$Resultats | Format-Table Niveau, Titre, Statut, Duree -AutoSize | Out-String | Write-Host
$echecs = @($Resultats | Where-Object { $_.Statut -ne 'OK' })
if ($echecs.Count -gt 0) {
    foreach ($echec in $echecs) { Write-Host ("Niveau {0} : {1}" -f $echec.Niveau, $echec.Detail) -ForegroundColor Red }
    Write-Host 'Des niveaux ont échoué : voir les messages ci-dessus et les journaux dans build\tester-tout.' -ForegroundColor Red
    exit 1
}
Write-Host 'Tous les niveaux exécutés sont au vert.' -ForegroundColor Green
exit 0
