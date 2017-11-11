#!/usr/bin/perl

use Encode::Escape;
Encode::Escape::demode 'unicode-escape', 'python';
Encode::Escape::enmode 'unicode-escape', 'python';

my $infile = shift;
my $outfile = $infile . ".properties";
open(INFILE,$infile) || die "cannot open $infile!\n";
open(OUTFILE,">$outfile") || die "cannot create $outfile!\n";
printf("Creating $outfile ...\n");
while(<INFILE>) {
  my $line = "";
  if (/^(#.+)/) {
    $line = $1;
  } else {
    if (/([^=]+)=(.*)/) {
      $line = sprintf("%s=%s",$1,$2);
    }
  }
  chomp $line;
  print OUTFILE encode 'unicode-escape', decode 'iso-8859-1', $line;
}
close(INFILE);
close(OUTFILE);
printf("Done.\n");
