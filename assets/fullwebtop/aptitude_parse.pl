#!/usr/bin/perl

use strict;
use warnings;

# This script is meant to parse the output of aptitude and determine whether a
# particular solution is what's offered. It has certain packages that it knows
# are okay to manipulate, and will abort if it encounters unexpected packages.

my $packages_ok =
  {
    'remove'    => [
        'libgtk2.0-0-dbg',
        'logrotate',
        'lxde',
        'ubuntu-minimal',
      ],
    'install'   => [
        'binutils',
        'gcc-4.3-base',
        'gnupg',
        'klibc-utils',
        'libdb4.6',
        'libdb4.7',
        'libgmp3c2',
        'libklibc',
        'libsasl2-modules',
        'passwd',
      ],
    'upgrade'   => [
        'libsasl2-2',
        '(jaunty-security,', # XXX Known bogus package. Limitation of aptitude.
      ],
    'downgrade' => [
        'libgtk2.0-0',
      ],
  };

my @input;
while (my $line = <stdin>) {
  chomp($line);
  push(@input, $line);
}

# Cut out all the lines at the beginning until we hit the list of packages.
while (scalar(@input) and
       $input[0] !~ m%^((Remove)|(Install)|(Upgrade)|(Downgrade)|(Score))%) {
  shift(@input);
}

# Packages to remove.
while (scalar(@input) and
       $input[0] !~ m%^((Install)|(Upgrade)|(Downgrade)|(Score))%) {
  if (($input[0] ne "") and
      ($input[0] !~ m%^Remove%)) {
    $input[0] =~ m%^(\S+)%;
    if (! grep($_ eq $1, @{$packages_ok->{'remove'}})) {
      die "Unexpected package to remove: ${1}\n";
    }
  }

  shift(@input);
}

# Packages to install.
while (scalar(@input) and
       $input[0] !~ m%^((Upgrade)|(Downgrade)|(Score))%) {
  if (($input[0] ne "") and
      ($input[0] !~ m%^Install%)) {
    $input[0] =~ m%^(\S+)%;
    if (! grep($_ eq $1, @{$packages_ok->{'install'}})) {
      die "Unexpected package to install: ${1}\n";
    }
  }

  shift(@input);
}

# Packages to upgrade.
while (scalar(@input) and
       $input[0] !~ m%^((Downgrade)|(Score))%) {
  if (($input[0] ne "") and
      ($input[0] !~ m%^Upgrade%)) {
    $input[0] =~ m%^(\S+)%;
    if (! grep($_ eq $1, @{$packages_ok->{'upgrade'}})) {
      die "Unexpected package to upgrade: ${1}\n";
    }
  }

  shift(@input);
}

# Packages to downgrade.
while (scalar(@input) and
       $input[0] !~ m%^Score%) {
  if (($input[0] ne "") and
      ($input[0] !~ m%^Downgrade%)) {
    $input[0] =~ m%^(\S+)%;
    if (! grep($_ eq $1, @{$packages_ok->{'downgrade'}})) {
      die "Unexpected package to downgrade: ${1}\n";
    }
  }

  shift(@input);
}

print "PASS\n";
