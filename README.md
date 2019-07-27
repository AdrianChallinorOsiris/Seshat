# Seshat
Incremental backup to S3 

Seshat is a self contained application that backs up incremental folders to Amazon S3. 
By 'self contained', it means it does not rely or use any database systems to store its status and reference information. 

Seshat is a command line program and expects to run in a console environment, unless provided with a script. 

It is intended to be used for single users or small businesses, rather than enterprises. 


## S3 
It does rely on Amazon S3 and expects to have credentials stored in ${HOME}/.aws/credentials. 

It also expects to be told which bucket to use. The default is "osiris-seshat", but this can be changed via the S3 BUCKET command 
and stored in the ${HOME}/.seshatrc file. 

The use of Amazon credentials, S3 and Glacier policies will be addressed in a future 
version of this document. In the meantime, the reader is referred to the Amazon Web 
Services documentation. 

## Running Seshat 
Seshat is a Java application and expects to have a JVE for Java 8 (V1.8) or above. To run it: 

`java -jar Seshat.jar [commands]' 

If no commands are entered, it will start in interactive mode. If commands are entered, it will execute those commands, and 
unless the last command is EXIT, it will then go in to interactive mode. 

```
java -jar Seshat.jar 
Seshat>  
```

## Getting Started 

Seshat can be run interactively, where it will prompt for the actions to perform. Watch the prompt, as it will change depending 
on what sort of command or data it is expecting. At any stage enter 'help' or the '?' to get a list of options. 

The Seshat environment needs to be initialised and the database read from Amazon S3. 
This is done with the *init" command. 
However, this is a lengthy process and is not always required. When running a *BACKUP* 
or "AUTOMATIC* command the initialisation is performed for you. 

In order to backup the /home directory (assuming you have access to /home!) the commands 
will be: 

```
14:03:11.503 INFO  - SESHAT started. Version 0.7.5a 
Seshat> backup /home 
14:03:28.745 INFO  - Initializing SESHAT and reading DB from S3
14:03:39.396 INFO  - Database loaded from: S3
14:03:39.396 INFO  - Staging area is: /tmp
14:03:39.396 INFO  - Initialisation is compete
14:03:39.590 INFO  - DB Lock acquired
14:03:47.463 INFO  - Database loaded from: S3
14:03:47.467 INFO  - Created top level folder /home
14:03:47.468 INFO  - Starting File walk.../home
14:03:55.578 WARN  - Ignoring Dropbox
14:03:57.623 WARN  - Ignoring Virtual Box VMs
14:04:03.988 WARN  - Ignoring iso VBoxGuestAdditions_5.2.18.iso
14:07:50.896 INFO  - Completed file walk. Scaned Dir=65377 File=474292. Found: 472463 to backup.  elapsed: 00:04:03.425
14:07:50.897 INFO  - Creating backup timestamp: 2019-07-26T14:07:50.897
14:07:50.906 INFO  - Opening container file: /tmp/SEF_bastet_home_20190726140750_000
14:09:27.406 INFO  - Progress  47246 - 10%
14:10:37.197 WARN  - File not backed up: /home/adrian/.spamassassin/bayes_toks (Permission denied)
14:10:55.644 INFO  - Progress  94492 - 20%
14:14:09.282 INFO  - Container is full SEF_bastet_home_20190726140750_000 size 8574920320 files 141139
14:14:09.282 INFO  - Closed container: SEF_bastet_home_20190726140750_000 
14:14:09.420 INFO  - Uploading multipart part file osiris-seshat/SEF_bastet_home_20190726140750_000 size 7 GB 
14:14:09.474 INFO  - Transfer started. Size 7 GB
```


## Backup file structure
Backups are performed to staging container files. Each backup may contain multiple 
container files, and each container file will contain multiple files. Containers are 
generally split at around 8GB to improve performance of the transfer to S3. 

Container files are of the form: SEF_<node>_<directory>_<datestamp>_<seq>

The seq is a sequence number, starting at 000 and incremented for each container file. 
 
## Security 
Every file is individually encrypted with AES-256 in CBC mode, and has a separate IV 
(initialisation vector). 

This makes the container file virtually impossible to crack. 
In order to access a file in the container file, you would need to know its offset 
in the container and its length. This gets you to the encrypted file contents.  You would then 
further need to know the key and IV in order to decrypt the file. 

The encryption data itself is stored in the database file. You should take steps on 
S3 to ensure that this file is not accessible except by Seshat users on your computers. The 
database is itself encrypted using AES-256-CBC. 

_In version 0.7 the key is held within the Java Code and constructed. Whilst this offers 
protection against browsing the code to find the key and IV, it is hardly the most 
appropriate or secure method of handling the key. A later version will make used of 
Amazon Key Services (AKS) and will store the DB master key there._

## Incremental backups with SESHAT 

Once a directory has been backed up, this fact is remembered in the database. All files 
backed up have their metadata recorded in the database, along with a reference to the 
container they are located in. When a backup is performed, only files that have changed 
will be backed up. This provides the infinite incremental backup that is a feature 
of Seshat. 

What this means in practice is that the first time a directory is backed up, it can 
take a long time. Thereafter, only changed files will be backed up. Hence, a large, 
but largely static directory, say of photo images, will take a long time the first 
time it is run, but afterwards will be performed very quickly. 



## Commands
```
AUTOMATIC
BACKUP   <directory>
BYE | EXIT | QUITe
CD 
  <Integer>
  <Word>
DB  LOAD            - Load the DB from S3
    LOCK            - Manually lock the DB
    RESET           - Reset the DB - DO NOT DO THIS AS YOU WILL FORGET ALL HISTORY! 
    SAVE            - Save the DB
    SIZE            - Report the DB Size 
    UNLOCK          - Manually unlock the DB. Useful in the case of a crash

EXCLUDE   <regex>   - Don't backup files that match this regex string
          RESET     - Reset the exclude list 

GRAMMAR   CHECK     - Internal command to check the command parse tables 
          PRINT     - Print the list of commands 
          PRUNE     - Internal command to prune the command parse tables 

HELP                - Displays what is expect at that point. Also can use ?
   
INCLUDE   <regex>   - Force the inclusion of files. This overides all other esclusions 
          RESET     - Reset the the inclusion list 

INITIALIZE          - Initialize Seshat 

LIST [<n>]          - List parts of the database. The <n> can be used to limit the number 
                      of rows printed
    AUTO                    - Directories that will be backed up using AUTOMATIC
    BACKUPS                 - What backups have been performed 
    DIRECTORIES | DIRS      - The Directories backed up 
    FILES                   - Files backed up. 
    FOLDERS                 - Synoym for DIRS     
    FOUND                   - List of files found after a scan 
    HOSTS                   - Hosts in the DB
    SELECTED                - Files selected for restoring
    TREE                    - The tree of files for restoring 
    VERSIONS                - The versions of files 
  
MEMSTATS            - Display virtual memory statistics 

RESET  DB           - Reset the database. DONT DO THIS. 
       RESTORE      - Reset the restore setup 

RESTORE ADD FILE [<number> | ALL | RESET]
            FOLDER
            TREE
        BAREMETAL
        GO
        TARGET [<name> | ORIGINAL ]
        TIMESTAMP <dd-MMM-yyyy HH:MM> 

S3 BUCKET <name>    - Set the S3 Bucket name 
   REGION <name>    - Set the S3 Region name 

SEARCH CONtAINS <word>
       REGEX <pattern>

SELECT BACKUP <Date DD-MMM-YYYY> | <number>
       FOLDER <name> | <number>  
       HOST <name> | number>
       RESET                    - Reset the selections 

SET DELAY  [YES | NO]
    DETAIL [YES | NO]
    DRYRUN [YES | NO]
    HIDDEN [YES | NO]
    MAX [MAX] <size> [GB | MB]
    SYMLINKS  [YES | NO]
    WAIT  [YES | NO]

SHOW CONFIG

  