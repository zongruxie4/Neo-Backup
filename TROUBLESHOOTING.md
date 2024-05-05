just a beginning,
only collecting what comes along when supporting users...


### tar: chdir '/data/user/0/a.package.name': no such file or directory

=> set Magisk option "Mount Namespace Mode" to global

- may be a thing on Android 13 (or 12+ or even 11 ?)
- on Android 10, it seems that "inherited" works, at least it does for me
- with "inherited" it may be that some apps work and others don't

### backups are not visible (all or only some of them)

=> NB "seeing" a backup means, it finds a valid xxx.properties file for the backup.

So for backups that are not seen, first check the properties file.

It's in json format. It should start with a { and end with }

One obvious reason could be lack of storage,
e.g. the properties files are not there or they are zero length.

Basically the files could also be inaccessible,
e.g. read protected, or owned by the wrong user.

### Firefox: "couldn't update permission for data"

- restore Firefox apk and data from OAndBackupX. Ignore the error.
- force-close Firefox
- delete the lock file (/data/data/[package.name]/files/mozilla/[profile.id].default/lock).
- start Firefox

### "install failed verification"

=> Disable verify apps over USB   [x]

### *.properties files are zero size, but corresponding archives are created

=> it seems ANExplorer replaced DocumentsUI and doesn't work

this is not really proofed and it doesn't feel logical,
but using DocumentsUI again worked, which also means reselecting the backup directory,
it is possible that the permission got lost.

However, if access is not working, no file would have been created.

Though, ANExplorer seems to be the provider for the storage and this means it is at least
between a request and the execution by the system.

Perhaps text files are handled differently and don't work.

### I cannot select a backup directory

=> DocumentsUI a.k.a. Files app is used/necessary to grant access to the folder via SAF

* there seem to be ROMs that remove DocumentsUI or maybe you did it manually
  ("debloating" apps, you didn't know => not everything you don't know is "bloat")
* at some point there also was a Magisk module for MiXplorer that removed DocumentsUI.
* ANExplorer also seems to mess with it.
