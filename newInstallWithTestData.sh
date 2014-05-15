#! /bin/bash
# A scipt to be run in the top level directory of a stardev instance.
# Runs NewInstall.sql and uploadTestData.sh

echo "Running /home/starexec/scripts/starsql < NewInstall.sql"
cd deployed-sql/
/home/starexec/scripts/starsql < NewInstall.sql
cd ../
echo "Running uploadTestData.sh"
cd deployed-upload-test/
./uploadTestData.sh
cd ../
echo "Done!"
