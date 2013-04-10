# GlacierUtil

Provides methods for uploading data to Amazon Glacier. Can be used for uploading ZFS snapshots to Glacier!

As of yet there is not a method for retrieving data uploaded by this class but there will be shortly. Additionally, it would be smart to encrypt the stream that you're piping into this class if you're backing up sensitive data.

## Required libraries:
aws-java-sdk-1.4.1.jar  
commons-logging-1.1.1.jar  
httpclient-4.1.1.jar  
httpcore-4.1.jar  
jackson-core-asl-1.8.7.jar  
jackson-mapper-asl-1.8.7.jar  

## Example usage:

### Upload a ZFS stream to a zault

For space saving considerations I suggest that you do a full 'zfs send' to a portable and off-site location then use Glacier to hold incremental diffs.

#### Full ZFS send to Glacier

	zfs send zfs_usb/backups@20130409 | java -Xmx512m -cp lib/*:. GlacierUtil 1 InitialBackupVault 256

#### Send a diff to Glacier

	zfs send -i zfs_usb/backups@20130409 zfs_usb/backups@20130415 | java -Xmx512m -cp lib/*:. GlacierUtil 1 20130415_Vault 256

### List Vaults

	java -Xmx512m -cp lib/*:. GlacierUtil 2

### Delete a Vault

	java -Xmx512m -cp lib/*:. GlacierUtil 3 20130415_Vault

### List contents of a Vault
This job takes some time to run so it is best to start the job then check it later. Take note of the JobID that is printed.

	java -Xmx512m -cp lib/*:. GlacierUtil 4 InitialBackupVault

### Get the status of a job (such as listing the contents of a Vault)

	java -Xmx512m -cp lib/*:. GlacierUtil 5 InitialBackupVault <JobID>

### List submitted jobs

	java -Xmx512m -cp lib/*:. GlacierUtil 6 InitialBackupVault

### Get job output
This can return a ton of data so use sparingly and ensure that you're not getting the job output from a retrieval request. Gets the list of archives submitted earlier.

	java -Xmx512m -cp lib/*:. GlacierUtil 7 InitialBackupVault <JobID>

### Delete an archive.
This needs to be done to all archives in a vault prior to deleting the vault.

	java -Xmx512m -cp lib/*:. GlacierUtil 8 InitialBackupVault <ArchiveID>

### Create a Vault

	java -Xmx512m -cp lib/*:. GlacierUtil 9 BrandNewVault
