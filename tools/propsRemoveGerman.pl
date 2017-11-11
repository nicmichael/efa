#!/usr/bin/perl

use Encode::Escape;
Encode::Escape::demode 'unicode-escape', 'python';
Encode::Escape::enmode 'unicode-escape', 'python';

my $inde = shift;
my $inXX = shift;
open(INDE,$inde) || die "cannot open $inde!\n";
open(INXX,$inXX) || die "cannot open $inXX!\n";

while(<INDE>) {
  my $line = encode 'utf8', decode 'unicode-escape', $_;
  if ($line =~ /([^=]+)=(.+)/) {
    $de{$1} = $2;
  }
}

while(<INXX>) {
  my $line = encode 'utf8', decode 'unicode-escape', $_;
  if ($line =~ /([^=]+)=(.+)/) {
    $xx{$1} = $2;
  }
}

foreach $k (sort keys %xx) {
  my $txtde = $de{$k};
  my $txtxx = $xx{$k};
  if (length($txtde) > 0 && $txtde eq $txtxx) {
    # same translation in language de and xx --> skip
  } else {
    my $line = "$k=$txtxx";
    print encode 'unicode-escape', decode 'utf8', $line;
  }
}
