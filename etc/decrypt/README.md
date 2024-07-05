
# experimental(!) script to decrypt all backups

    DISCLAIMER: without any warranty!
    first create a backup of the backups!!!!!
    you have been warned... 😊

I used this for testing ... so only a few tests were made.

The original script is from Nils:
<https://github.com/Tiefkuehlpizze/OABXDecrypt/blob/main/misc/decrypt.py>

The script scans the given directory for backups and creates decrypted
backups with the postfix `-DECRYPTED`.

Those will be shown as `<date time> - DECRYPTED` in the backup list.

The decrypted backups have the same date, and NB traditionally used the backup date as ID to distiguish the backups. So, it's possible, that NB reacts weird in
some areas. If you want to stay safe, move the DECRYPTED backups somewhere else.

You need to have pipenv installed to use the provided shell script, which uses pipenv to manage a virtual environment.

Otherwise you are on your own and need to do something similar.

Usage:

copy this folder to your workstation

then either:

```sh
cd <path-to-backup-root>
<path-to-script-directory>/decrypt-all <the-password>
```

or:

```shell
<path-to-script-directory>/decrypt-all <the-password> <path-to-backup-root>
```
