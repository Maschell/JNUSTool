# JNUSTool
JNUSTool is a Java application for download and decrypting NUS data from the WiiU and 3DS eShop. This tool allows users to browse and select individual files associated with a NUS titlekey before downloading, supports on-the-fly decryption (no cleaning up temporary files!), and allows for high-speed multithreaded downloads.

## Features:
- Download encrypted data from the eShop
- Seamlessly decrypt encrypted data from the eShop with on-the-fly decryption
- Decrypt pre-downloaded NUS data
- Support for downloading specific versions of a title
- Download/decrypt of the meta folder only
- On the fly download and decryption of single files (Updates, DLC, etc.)
- Bulk title downloads using filelist.txt
- Bulk downloads using [regular expressions](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions/Cheatsheet) (e.g all models or sounds)
- Automatically search for and notify of available software updates during download. 
 
## Environment Requirements: 
- [Java 8](https://www.java.com/en/download/)

## Note: eShop EOL Announcement
[Nintendo has announced that the WiiU and 3DS eShop will be permanently discontinued on or before March 2023.](https://en-americas-support.nintendo.com/app/answers/detail/a_id/57847) This tool will lose full download functionality at that time. 

# Usage
There are two ways to run this application.
1. Automated:
Run the JNUSTool.jar file. Make sure you have a `config` file (with correct values) and `updatetitles.csv` in the same directory. A windows will popup and let you choose from the game updates. Feel free to add more titles.

2. Command line (when you want to provide a key or use a titleid that is not in the list)

`java -jar JNUSTool.jar <TITLEID>`

To download a specific version, specify the version after the title ID.

e.g. `java -jar JNUSTool.jar <titlekey> v16`

To disable on-the-fly decryption, append the string `-dlEncrypted`

Specific files, or a batch of specific files can be downloaded in isolation by appending `-file <file path or regex>` to the command 

e.g. 
- Download only app.xml with: `java -jar JNUSTool.jar <titlekey>  -file /code/app.xml`
- Download only code folder with: `java -jar JNUSTool.jar <titlekey>  -file /code/.*`
- Download all .szs files with: `java -jar JNUSTool.jar <titlekey>  -file .*.szs`

## Sample config file
```
http://maschell.de/ccs/download
EE10040260BBB4ABD2C42DB25570B656
updatetitles.csv
TAGAYA 1 (latest_version)
TAGAYA 2 (versionlist)
```

# Data sources
The software will not work without the required titlekey, commonkey, and Nintendo URL - which can not be included here for legal purposes. GBATemp, Reddit, and other internet sources would be a good place to look. :stuck_out_tongue_winking_eye:

# Additional Information
- [GBATemp: JNUSTool Release Announcement](https://gbatemp.net/threads/jnustool-nusgrabber-and-cdecrypt-combined.413179/)
- [hacks.guide homebrew guides](https://hacks.guide/)
