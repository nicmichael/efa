#!/usr/bin/perl

use Encode::Escape;
Encode::Escape::demode 'unicode-escape', 'python';
Encode::Escape::enmode 'unicode-escape', 'python';

my $infile = shift;
my $outcsv = $infile . ".csv";
my $outtxt = $infile . ".txt";
open(INFILE,$infile) || die "cannot open $infile!\n";
open(OUTCSV,">$outcsv") || die "cannot create $outcsv!\n";
open(OUTTXT,">$outtxt") || die "cannot create $outtxt!\n";
printf("Creating $outcsv ...\n");
printf("Creating $outtxt ...\n");
while(<INFILE>) {
  my $linecsv = "";
  my $linetxt = "";
  if (/^(#.+)/) {
    $linetxt = $1;
  } else {
    if (/([^=]+)=(.*)/) {
      $linecsv = sprintf("%s=%s",$1,$2);
      $linetxt = sprintf("%s=%s",$1,$2);
    }
  }
  print OUTCSV encode 'utf8', decode 'unicode-escape', $linecsv;
  print OUTTXT encode 'iso-8859-1', decode 'unicode-escape', $linetxt;
}
close(INFILE);
close(OUTCSV);
close(OUTTXT);
printf("Done.\n");
