#!/bin/sh

# default:
PREFIX=/usr/local
PROGS="java javac jar make sed"

valid=true
while [ "$*" != "" ]; do
	key=`echo "$1" | cut -c1-9`
	val=`echo "$1" | cut -c10-`
	case "$key" in
	--prefix=)
		PREFIX="$val"
	;;
	*)
		echo "Unsupported parameter: '$1'" >&2
		valid=false
	;;
	esac
	shift
done

[ $valid = false ] && exit 1

MESS="A required program cannot be found:"
for prog in $PROGS; do
	out="`whereis -b "$prog" 2>/dev/null`"
	if [ "$out" = "$prog:" ]; then
		echo "$MESS $prog" >&2
		valid=false
	fi
done

[ $valid = false ] && exit 2

echo "MAIN = be/nikiroo/fanfix/Main" > Makefile
echo "NAME = fanfix" >> Makefile
echo "PREFIX = $PREFIX" >> Makefile
echo "JAR_FLAGS += -C bin/ org -C bin/ be -C ./ src" >> Makefile

cat Makefile.base >> Makefile

