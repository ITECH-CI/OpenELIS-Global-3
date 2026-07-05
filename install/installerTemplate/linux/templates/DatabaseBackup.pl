#!/usr/bin/perl -w
use File::Copy;
use File::stat;
use File::Basename;
use Cwd;

sub getTimeStamp {
	( $sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst ) =  localtime time ;
	my $fullYear  = $year + 1900;
	my $fullMonth = $mon + 1;

	return  "_" . $mday . "_" . $fullMonth . "_" . $fullYear . "_" . $hour . "_" . $min;
}

sub deleteOverAgedBackups {
	my $maxTimeSpan    = shift;
	my $cumulativeDir = shift;

	chdir $cumulativeDir;

	@$files = <*>;
	foreach my $file (@$files) {
		if ( time - $maxTimeSpan > stat($file)->mtime ) {
			unlink($file);
		}
	}
}

sub sendOffsite{
	my $queueDir = shift;
	my $upLoadtargetURL    = shift;
	my $upLoadUserName = shift;
	my $upLoadPassword = shift;
	
	my $maxRetryCount = 3;   # ré-essaie vraiment (lien instable en site isolé)
	my $curlExe = 'curl';

	chdir "$queueDir" or return;

	my @files = <$queueDir/*.backup.gz>; 

	foreach $file (@files) {
	    my $command = $curlExe . ' -T ' . $file . ' --user ' .$upLoadUserName . ':' . $upLoadPassword . ' ' . $upLoadtargetURL . basename($file);
        #print basename($file) . "\n";
  		my $retryCount = 0;
   		my $sendSuccess = 0; #false
	    
           
   		while ($retryCount < $maxRetryCount) {
   			my $curlReturn = `$command`;
        	my $returnStatus = $?;
			
        	if (($returnStatus != 0) ) {
        		print "Curl had an error. Curl said \n$curlReturn\n"
        				. "Return status $returnStatus\n";
        		$retryCount = $retryCount + 1;
        	} else {
        		$sendSuccess = 1; #true
        		last;
        	}
        	
        	sleep 7;
		}
           
		if ($sendSuccess) {
    		#remove file from system
    		unlink( $file );
    	}
	}           
}
my $db_install_type  = '[% db_install_type %]';
my $postgres_pwd_filepath = '[% secrets_dir %]datasource.password';
open my $fh, '<', $postgres_pwd_filepath or die "Can't open file $!";
read $fh, my $postgres_pwd, -s $fh;
my $keepFileDays  = 30;
my $siteId = '[% siteId %]';

# --- Configuration optionnelle externe (backup.conf) --------------------------
# L'opérateur peut créer un fichier backup.conf À CÔTÉ de ce script pour
# configurer, SANS éditer ce Perl :
#   EXTERNAL_BACKUP_DIR=/media/oeserver/USB0/Backup   # copie sur disque externe/USB
#   FTP_ENABLED=true                                  # activer l'envoi hors-site
#   FTP_TARGET_URL=ftp://serveur/dossier/             # doit finir par '/'
#   FTP_USERNAME=ftpuser
#   FTP_PASSWORD=motdepasse
# Lignes vides et commentaires (#) ignorés. Valeurs par défaut ci-dessous.
my %conf = (
    EXTERNAL_BACKUP_DIR => '/media/USB0/Backup',   # défaut historique (compat)
    FTP_ENABLED         => 'false',
    FTP_TARGET_URL      => '',
    FTP_USERNAME        => '',
    FTP_PASSWORD        => '',
);
{
    # backup.conf est cherché dans le dossier des backups (chemin absolu injecté
    # à l'install), indépendamment du répertoire courant.
    my $conf_path = '[% db_backups_dir %]backup.conf';
    if (open my $cfh, '<', $conf_path) {
        while (my $line = <$cfh>) {
            $line =~ s/^\s+|\s+$//g;
            next if $line eq '' || $line =~ /^#/ || $line !~ /=/;
            my ($k, $v) = split /=/, $line, 2;
            $k =~ s/^\s+|\s+$//g; $v =~ s/^\s+|\s+$//g;
            $conf{$k} = $v;
        }
        close $cfh;
    }
}


my $snapShotFileBase     = 'lastSnapshot_' . $siteId; 
my $snapShotFileName     = $snapShotFileBase . '.backup'; 
my $snapShotFileNameZipped     = $snapShotFileName . '.gz'; 
my $databaseDockerBackupDir	 			 = '[% docker_backups_dir %]';
my $databaseDockerImageName				 = 'openelisglobal-database'; #don't change

# HAPI-FHIR crée ses tables (hfj_/trm_/mpi_/npm_/bt2_) dans le schéma clinlims
# (currentSchema=clinlims + hbm2ddl.auto=update). Elles sont volumineuses ET
# reconstructibles (projection du métier) : les inclure alourdit le dump et
# provoque des erreurs à la restauration. On les EXCLUT du backup ; HAPI les
# recrée au démarrage (hbm2ddl=update). Les motifs sont quotés (simples) pour que
# le pattern '*' soit interprété par pg_dump, pas par le shell.
my $fhirExcludes = " --exclude-table-data='clinlims.hfj_*'"
                 . " --exclude-table-data='clinlims.trm_*'"
                 . " --exclude-table-data='clinlims.mpi_*'"
                 . " --exclude-table-data='clinlims.npm_*'"
                 . " --exclude-table-data='clinlims.bt2_*'";

#for backup task in docker database command
my $docker_cmd = 'docker exec ' . $databaseDockerImageName . ' /usr/bin/pg_dump -U clinlims -f "' . $databaseDockerBackupDir . '/' . $snapShotFileName . '" -n \"clinlims\"' . $fhirExcludes . ' clinlims';
#for backup task using postgres running on the host
my $host_cmd = 'pg_dump -h localhost  -U clinlims -f "' . $snapShotFileName . '" -n \"clinlims\"' . $fhirExcludes . ' clinlims';
my $zipCmd = 'gzip -f ' .  $snapShotFileName;
#my $backBaseDir          = cwd();
my $backBaseDir          = '[% db_backups_dir %]';
my $baseFileName         = '[% installName %]';
my $mountedBackup        = $conf{EXTERNAL_BACKUP_DIR};   # configurable via backup.conf
my $dailyDir             = "$backBaseDir/daily";
my $cumulativeDir        = "$backBaseDir/cumulative";
my $queueDir             = "$backBaseDir/transmissionQueue";
my $timeStamp            = getTimeStamp();
my $todaysCummlativeFile = "$siteId$baseFileName$timeStamp.backup.gz";
my $maxTimeSpan = 60 * 60 * 24 * $keepFileDays;


$ENV{'PGPASSWORD'} = $postgres_pwd;

# Journal horodaté + sentinelle de succès, lisibles par l'opérateur.
my $backupLog = "$backBaseDir/backup.log";
sub logmsg {
    my ($m) = @_;
    my $ts = getTimeStamp();
    if (open my $lh, '>>', $backupLog) { print $lh "$ts $m\n"; close $lh; }
    print "$m\n";
}
# abort() : échec FATAL -> on N'écrit PAS de fichier daté (évite de re-dater un
# vieux dump comme s'il était frais) et on sort en erreur pour que l'échec soit
# visible (code retour non nul du cron).
sub abort {
    my ($m) = @_;
    logmsg("[ECHEC] $m");
    die "$m\n";
}

chdir "$dailyDir" or abort("Impossible d'accéder au dossier daily: $dailyDir");

# 1) Dump — FAIL-FAST : si pg_dump échoue, on s'arrête (pas de fichier périmé daté).
if ( $db_install_type eq "docker" ) {
	system("$docker_cmd") == 0 or abort("pg_dump (docker) a échoué");
	copy( "$backBaseDir/$snapShotFileName", "$dailyDir" ) or abort("Copie du dump échouée: $!");
} elsif ( $db_install_type eq "host" ) {
	system("$host_cmd") == 0 or abort("pg_dump (host) a échoué");
} else {
	abort("Sauvegarde d'une base distante non supportée");
}

# 2) Vérifier que le dump n'est PAS vide avant de compresser.
abort("Dump vide ou absent: $snapShotFileName") unless (-s "$dailyDir/$snapShotFileName");

# 3) Compression — fail-fast.
system("$zipCmd") == 0 or abort("Compression gzip échouée");

# 4) Vérifier l'intégrité de l'archive gzip AVANT de la dater/diffuser.
system("gzip -t \"$dailyDir/$snapShotFileNameZipped\"") == 0
    or abort("Archive gzip corrompue: $snapShotFileNameZipped");

# 5) Seulement maintenant : dater dans cumulative + file d'attente (+ USB).
copy( $snapShotFileNameZipped, "$cumulativeDir/$todaysCummlativeFile" ) or abort("Copie cumulative échouée: $!");
copy( $snapShotFileNameZipped, "$queueDir/$todaysCummlativeFile" ) or abort("Copie file d'attente échouée: $!");
if (-d $mountedBackup) {
    copy( $snapShotFileNameZipped, "$mountedBackup/$todaysCummlativeFile" )
        or logmsg("[ATTENTION] Copie sur support externe échouée: $!");
}

# 6) Sentinelle de succès (timestamp) : permet de détecter un backup manquant.
if (open my $sf, '>', "$backBaseDir/LAST_BACKUP_OK") { print $sf getTimeStamp() . "\n"; close $sf; }
logmsg("[OK] Sauvegarde réussie: $todaysCummlativeFile");

deleteOverAgedBackups ($maxTimeSpan, $cumulativeDir);

# Envoi hors-site (FTP) — activé seulement si FTP_ENABLED=true dans backup.conf
# ET que l'URL/identifiants sont renseignés. sendOffsite retire de la file les
# fichiers envoyés avec succès (les autres restent pour un prochain essai).
if ( lc($conf{FTP_ENABLED}) eq 'true'
     && $conf{FTP_TARGET_URL} ne '' && $conf{FTP_USERNAME} ne '' ) {
    my $url = $conf{FTP_TARGET_URL};
    $url .= '/' unless $url =~ m{/$};   # garantir le '/' final (append du nom de fichier)
    sendOffsite($queueDir, $url, $conf{FTP_USERNAME}, $conf{FTP_PASSWORD});
}
