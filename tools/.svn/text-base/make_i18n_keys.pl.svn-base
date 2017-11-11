#!/usr/bin/perl

# This script needs to be run in an environment with the same encoding as the
# Java source code!
# However, since the Java Properties Files are stored in ISO-8859-1 encoding
# with escaped Unicode letters, the Encode-Escape module is required to read
# and write Java Properties Files.
# You can download this module from CPAN at:
# http://search.cpan.org/perldoc?Encode::Escape::Unicode
use Encode::Escape;
Encode::Escape::demode 'unicode-escape', 'python';
Encode::Escape::enmode 'unicode-escape', 'python';

my $properties = shift;
my $options = "";
if ($properties =~ /^-/) {
  $options = $properties;
  $properties = shift;
}

if (length($properties) == 0) {
  printf("usage: make_i18n_keys.pl [-options] <properties>\n");
  printf("\nThis script recursively searches all .java files in the current\n" .
         "directory and any subdirectories for internationalized strings and\n" .
         "creates a new property file (based on the original one) on stdout.\n");
  printf("\nOptions:\n");
  printf("       u   update existing properties file <properties>\n");
  printf("       s   sort all keys by source file\n");
  printf("       f   print file name as a comment after each key\n");
  printf("       i   write output as ISO-8859-1\n");
  printf("       d   print DEBUG messages on stderr\n");
  printf("       r   remove all old strings\n");
  exit(1);
}

# some global variables
my $_filename;
my $_linenr;
my $_line;

# values for $keys{$key}{new}
# 0 = old (and still exisiting in current source code)
# 1 = new
# 2 = removed (old, but not existing in current source code any more)

# read existing properties
readProps($properties);

# traverse directories and search for .java files
searchdir(".");

# print new properties file
if ($options !~ /u/) {
  $properties = "";
}
writeProps($properties);

exit(0);



sub readProps {
  my $properties = shift;
  open(PROPS,$properties) || die "cannot open property file: $properties\n";
  my $file = "global";
  while(<PROPS>) {
    my $line = encode 'utf8', decode 'unicode-escape', $_;
    if ($line =~ /^# file: (.+)/) {
      $file = $1;
    }
    next if $line =~ /^#/;
    if ($line =~ /([^=]+)=(.+)/) {
      my $key = $1;
      my $txt = $2;
      $keys{$key}{txt} = $txt;
      $keys{$key}{file} = $file;
      $keys{$key}{new} = 2; # set to 0 if found while parsing source code
    }
  }
  close(PROPS);
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

sub parsefile {
  my $file = shift;
  printf STDERR ("#DEBUG: File %s ...\n",$file) unless $options !~ /d/;;
  open(JAVA,$file) || die "cannot open source file: $file\n";
  $_filename = $file;
  $_linenr = 0;
  while(<JAVA>) {
    $_line = $_;
    $_linenr++;
    parseLine($_line);
  }
  close(JAVA);
}

sub parseLine {
  my $line = shift;
  my $remaining = $line;
  my $isMessage = 0;

  while (length($remaining) > 0) {
    $line = $remaining;
    $remaining = "";
    my $txt = "";
    my $discr = "";

    # getString(...) or getStringWithMnemonic(...)
    if ($line =~ /International.getString[^\(]*\s*\((.*)/) {
      $isMessage = 0;
      $remaining = getStrings($1,2);
      if ($#strings <= 1) {
        $txt = $strings[0];
      }
      if ($#strings == 1) {
        $discr = $strings[1];
      }
    }

    # getMessage(...)
    if ($line =~ /International.getMessage[^\(]*\s*\((.*)/) {
      $isMessage = 1;
      $remaining = getStrings($1,1);
      if ($#strings >= 0) {
        $txt = $strings[0];
      }
    }

    # key found?
    if (length($txt) > 0) {

      # create key
      my $key = $txt;
      if (length($discr) > 0) {
        $key .= "___" . $discr;
      }
      $key =~ s/ /_/g;
      $key =~ s/=/_/g;
      $key =~ s/:/_/g;
      $key =~ s/#/_/g;
      $key =~ s/\\n/_/g;
      $key =~ s/\\([^\\])/\1/g; # replace something as \' or \" by ' or "
      $key =~ s/\\/_/g;
      $key =~ s/'/_/g;

      # create message text for compound messages
      if ($isMessage) {
        my $i = 1;
        while ($txt =~ /{([^\}]+)}/) {
          my $keystr = $1;
          if ($keystr =~ /, *choice *,/) {
            $keystr =~ s/[^,]+,/,/;
          } else {
            $keystr = "";
          }
          $txt =~ s/{[^\}]+}/%_1_%${i}${keystr}%_2_%/;
          $i++;
        }
        $txt =~ s/%_1_%/{/g;
        $txt =~ s/%_2_%/}/g;
        $txt =~ s/'{([^}]+)}'/''{\1}''/g;  # replace '{1}' by ''{1}''
      }

      # handle special characters in translated text
      $txt =~ s/&/&&/g;
      $txt =~ s/\\([^\\n])/\1/g; # replace something as \' or \" by ' or ", but do not replace \\ or \n!!

      # handle special English strings from International.java (keys must remain English, but translation should be German)
      if ($txt =~ /^Default$/) { $txt = "Standard"; }
      if ($txt =~ /^Select Language$/) { $txt = "Sprache wählen"; }
      if ($txt =~ /^Please select your language$/) { $txt = "Bitte wähle Deine Sprache"; }
      if ($txt =~ /^efa can't start$/) { $txt = "efa kann nicht starten"; }
      if ($txt =~ /^Basic Configuration File ''\{1\}'' could not be created.$/) { $txt = "Basis-Konfigurationsdatei ''{1}'' konnte nicht erstellt werden."; }
      if ($txt =~ /^Basic Configuration File ''\{1\}'' could not be opened.$/) { $txt = "Basis-Konfigurationsdatei ''{1}'' konnte nicht geöffnet werden."; }

      # print key and text
      if (exists $keys{$key}) {
        printf(stderr "#DEBUG: Duplicate Key $key=$txt\n") unless $options !~ /d/;
        if ($keys{$key}{new} == 2) { # if this is a key read from the original properties file ...
          $keys{$key}{new} = 0;      # set it's status to "0" (meaning "old and found in current source")
        }
      } else {
        if ($key =~ /^\+\+\+/) {
          $txt = ""; # keys starting with "+++" are internal keys which should not have a "default translation"
        }
        printf(stderr "#DEBUG: New Key $key=$txt\n") unless $options !~ /d/;
        $keys{$key}{txt} = $txt;
        $keys{$key}{file} = $file;
        $keys{$key}{new} = 1;
      }
    }
  }
}

sub getStrings {
  my $line = shift;
  my $nrOfStrings = shift; # number of strings to look for
  printf(stderr "#DEBUG: getStrings(%s)\n",$line) unless $options !~ /d/;
  my $str = "";
  my $i = 0;
  my $remaining = "";

  # 0 = before string
  # 1 = in string
  # 2 = after string, search for concatenated strings
  # 98 = search for next string
  # 99 = strings complete
  my $inString = 0;

  # 0 = no comment
  # 1 = in comment
  my $inComment = 0;

  @strings = ();
  while($inString != 100) {

    if ($inString == 98 || $inString == 99) {
      $strings[$#strings+1] = $str;
      printf(stderr "#DEBUG: String complete: >>%s<<\n",$str) unless $options !~ /d/;
      if ($inString == 98) {
        my $foundStrings = $#strings + 1;
        if ($foundStrings == $nrOfStrings) {
          printf(stderr "#DEBUG: $nrOfStrings Strings found, we're done!\n",$str) unless $options !~ /d/;
          $inString = 100;
        } else {
          printf(stderr "#DEBUG: $foundStrings Strings found, searching for furhter strings ...\n",$str) unless $options !~ /d/;
          $inString = 0;
        }
      } else {
        $inString = 100;
      }
      $str = "";
      next;
    }

    $remaining = substr($line,$i++);
    printf(stderr "#DEBUG: remaining string is >>%s<<\n",$remaining) unless $options !~ /d/;
    if (length($remaining) == 0) {
      $line = <JAVA>;
      chomp($line);
      $_line = $line;
      $_linenr++;
      $i = 0;
      next;
    }

    if ($inString == 0 || $inString == 2) { # we're searching for the beginning of a string

      if (!$inComment) { # we're not inside a comment

        # comment until end of line?
        if ($remaining =~ /^\/\//) {
          printf(stderr "#DEBUG: Comment until EOL found: %s\n",$remaining) unless $options !~ /d/;
          $line = <JAVA>;
          chomp($line);
          $_line = $line;
          $_linenr++;
          $i = 0;
          next;
        }

        # start of a comment?
        if ($remaining =~ /^\/\*/) {
          printf(stderr "#DEBUG: Beginning of a Comment found: %s\n",$remaining) unless $options !~ /d/;
          $inComment = 1;
          $i++;
          next;
        }

        # start of a string?
        if ($remaining =~ /^"/) {
          printf(stderr "#DEBUG: Beginning of a String found: %s\n",$remaining) unless $options !~ /d/;
          $inString = 1;
          next;
        }

        # concatenated string?
        if ($remaining =~ /^\+/) {
          printf(stderr "#DEBUG: Concatenation found: %s\n",$remaining) unless $options !~ /d/;
          $inString = 2;
          next;
        }

        # next parameter?
        if ($remaining =~ /^,/) {
          printf(stderr "#DEBUG: Next Method Parameter found: %s\n",$remaining) unless $options !~ /d/;
          $inString = 98;
          next;
        }

        # method finished?
        if ($remaining =~ /^\)/) {
          printf(stderr "#DEBUG: End of Method found: %s\n",$remaining) unless $options !~ /d/;
          $inString = 99;
          next;
        }
        if ($remaining !~ /^[ \t]/) { # instead of a string constant, a variable has been found
          if ($#strings < 0) { # only print a warning, if this is the first string!
            printf(stderr "#WARNING: %s:%d: unexpected international string will be ignored (dynamic key?): >>%s<<\n %s\n",
                           $_filename,$_linenr,$remaining,$_line);
            $inString = 100;
          } else { # if this is the second parameter to the function, as in International.getString("Fahrt beginnen", bundle), ignore this
            $inString = 99;
          }
        }

      } else { # we're inside a comment

        # end of a comment?
        if ($remaining =~ /^\*\//) {
          printf(stderr "#DEBUG: End of a Comment found: %s\n",$remaining) unless $options !~ /d/;
          $inComment = 0;
          $i++;
          next;
        }

      }

    }

    if ($inString == 1) { # we're inside a string, adding characters to the current string
      printf(stderr "#DEBUG: in string: >>%s<<\n",$remaining) unless $options !~ /d/;

      if ($remaining =~ /^"/) {
        printf(stderr "#DEBUG: end of part of string found, string is now: >>%s<<\n",$str) unless $options !~ /d/;
        $inString = 2;
        next;
      }

      if ($remaining =~ /^\\./) {
        $str .= substr($remaining,0,2);
        $i++;
      } else {
        $str .= substr($remaining,0,1);
      }

    }

  }

  printf(stderr "#DEBUG: done getting strings, remaining line: >>%s<<\n",$remaining) unless $options !~ /d/;
  return $remaining;
}

sub writeProps {
  my $properties = shift;

  if (length($properties) > 0) {
    open(OUTPROPS,">$properties") || die "cannot create $properties!\n";
  } else {
    open(OUTPROPS,">&STDOUT") || die "cannot open stdout!\n";
  }

  printf OUTPROPS ("# Property File created by make_i18n_keys.pl:\n# ------------------------------------------\n");
  my %data;
  foreach $key (sort %keys) {
    if (exists $keys{$key}{txt}) {
      my $txt = $keys{$key}{txt};
      my $file = $keys{$key}{file};
      my $new = $keys{$key}{new};

      my $sortkey = $key;
      if ($options !~ /s/) {
        $file = "global";
        $sortkey = lc($sortkey);
        while ($data{$file}{$new}{$sortkey}{key}) {
          $sortkey .= "X";
        }
      }

      $data{$file}{$new}{$sortkey}{key} = $key;
      $data{$file}{$new}{$sortkey}{value} = $txt;
    }
  }

  foreach $file (sort keys %data) {
    printf OUTPROPS ("# file: $file\n");
    my $header = -1;
    foreach $new (sort keys %{$data{$file}}) {
      if ($header == -1) {
        if ($new == 1) {
          printf OUTPROPS ("# new keys in $file:\n");
        }
        if ($new == 2) {
          if ($options =~ /r/) {
            next;
          }
          printf OUTPROPS ("# removed keys in $file (these keys do not exist in the source code any more):\n");
        }
        $header = -1;
      }
      foreach $key (sort keys %{$data{$file}{$new}}) {
        my $line = sprintf("%s=%s%s",
                   $data{$file}{$new}{$key}{key},
                   $data{$file}{$new}{$key}{value},
                   ($options =~ /f/ ? "\t\t### " . $keys{$key}{file} : "")
                   );
        if ($options =~ /i/) {
          $line = encode 'iso-8859-1', decode 'utf8', $line;
        } else {
          $line = encode 'unicode-escape', decode 'utf8', $line;
        }
        $line =~ s/\\\\n/\\n/g;
        print OUTPROPS $line;
      }
    }
  }

  close(OUTPROPS);
}