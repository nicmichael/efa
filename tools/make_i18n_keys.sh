#!/bin/sh

LANG=de_DE.UTF-8
export LANG
`dirname $0`/make_i18n_keys.pl $*
