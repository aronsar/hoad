#! /bin/bash
##
# YARCC hanabi agregation script
#
# take YARCC cluster out and error log, seperate them by job and combine standard outs to form results.csv
##
shopt -s extglob

INDIR=mx_yarcc
OUTDIR=grouped

# take the log directory (INDIR), extract job name and task id from the files and put them into jobid folders in OUTDIR
# new files will be of the format $OUTDIR/jobid/{o,e}/taskid.out
for FILENAME in $(ls -1 $INDIR)
do
	NEW_FILENAME=$(echo $FILENAME | awk 'BEGIN { FS = "."; } { printf "%s/%s/%s.out\n", substr($2,2),substr($2,0,1),$3}')
	mkdir -p $OUTDIR/$(dirname $NEW_FILENAME)
	mv $INDIR/$FILENAME $OUTDIR/$NEW_FILENAME
done

# Aggregate the outfiles into a csv (outfiles are already valid CSVs which end in newline)
for DIRNAME in $(ls -1 $OUTDIR)
do
	cat $OUTDIR/$DIRNAME/o/*.out > $OUTDIR/$DIRNAME/results.csv
done

