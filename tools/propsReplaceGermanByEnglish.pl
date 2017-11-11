#!/usr/bin/perl

use Encode::Escape;
Encode::Escape::demode 'unicode-escape', 'python';
Encode::Escape::enmode 'unicode-escape', 'python';

my $inde = shift;
my $inen = shift;
my $inXX = shift;
open(INDE,$inde) || die "cannot open $inde!\n";
open(INEN,$inen) || die "cannot open $inen!\n";
open(INXX,$inXX) || die "cannot open $inXX!\n";

while(<INDE>) {
  s/\r//g;
  my $line = encode 'utf8', decode 'unicode-escape', $_;
  if ($line =~ /([^=]+)=(.+)$/) {
    $de{$1} = $2;
  }
}

while(<INEN>) {
  s/\r//g;
  my $line = encode 'utf8', decode 'unicode-escape', $_;
  if ($line =~ /([^=]+)=(.+)$/) {
    $en{$1} = $2;
  }
}

while(<INXX>) {
  s/\r//g;
  my $line = encode 'utf8', decode 'unicode-escape', $_;
  if ($line =~ /([^=]+)=(.+)$/) {
    my $k = $1;
    my $txtxx = $2;
    my $txtde = $de{$k};
    my $txten = $en{$k};
    if (length($txtde) > 0 && $txtde eq $txtxx) {
      # same translation in language de and xx --> replace by English
      $line = "$k=$txten";
    } else {
      # already translated
      $line = "$k=$txtxx";
    }
  }
  print encode 'unicode-escape', decode 'utf8', $line;
}
