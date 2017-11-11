#!/usr/bin/perl

# traverse directories and search for .java files
my @status;
searchdir(".");
foreach $file (sort keys %status) {
  printf("%-68s - %s\n",$file,($status{$file}==1 ? "completed" : "open"));
}
exit(0);

sub parsefile {
  my $file = shift;
  my $complete = 0;
  open(JAVA,$file) || die "cannot open source file: $file\n";
  while(<JAVA>) {
    if (/\@i18n complete/) {
      $complete = 1;
      last;
    }
  }
  close(JAVA);
  $status{$file} = $complete;
}

sub searchdir {
  my $dir = shift;
  opendir(DIR,$dir) || die "cannot open directory: $dir\n";
  my @files = readdir(DIR);
  my $file;
  foreach $file (sort @files) {
    if ("$file" eq "." || "$file" eq "..") { next; }
    if (-d "$dir/$file") {
      searchdir("$dir/$file");
    } else {
      if (lc($file) =~ /.java$/) {
        parsefile("$dir/$file");
      }
    }
  }
}