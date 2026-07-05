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
	
	my $maxRetryCount = 1;
	my $curlExe = 'curl';

	chdir "$queueDir";

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

chdir "$dailyDir";
if ( $db_install_type eq "docker" ) {
	my $response = system("$docker_cmd")  and warn "Error while running: $! \n";
	copy( "$backBaseDir/$snapShotFileName", "$dailyDir" ) or die "File cannot be copied.";
} elsif ( $db_install_type eq "host" ) {
	my $response = system("$host_cmd")  and warn "Error while running: $! \n";
} else {
	die "Cannot backup remote databases";
}
system("$zipCmd")  and warn "Error while running: $! \n";

copy( $snapShotFileNameZipped, "$cumulativeDir/$todaysCummlativeFile" ) or die "File cannot be copied.";
copy( $snapShotFileNameZipped, "$queueDir/$todaysCummlativeFile" ) or die "File cannot be copied.";
if (-d $mountedBackup) {
    copy( $snapShotFileNameZipped, "$mountedBackup/$todaysCummlativeFile" ) or die "File cannot be copied.";
}

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
