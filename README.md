## encrypted-stream-name

Wowza plugin to decrypt an encrypted stream name, do some basic validation,
and rename the stream internally to the decrypted name.

### encryption process

1. The DAMS PAS function [dams_objects_helper#encrypt_stream_name](https://github.com/ucsdlib/damspas/blob/develop/app/helpers/dams_objects_helper.rb#L516) builds a string consisting of the video object ARK, filename, and client IP address separated by spaces.  It then generates a 16-character random nonce, encrypts the string using the secret key, and builds the request token consisting of the nonce, a comma, and then the encrypted string:

    ```
    "bd0786115s 0-2.mp4 192.168.1.100" -> "odx0klxzpuc785iy,mrKmOjRTYGH6yuG4G2oy9jwB2TrVqhS9UVf1r8Znnm36CfJ6CorePqajirhNbO-M"
    ```

2. The [EncryptedStreamNameModule.decryptStreamName](https://lib-stash.ucsd.edu/projects/ND/repos/encrypted-stream-name/browse/src/java/edu/ucsd/library/dams/util/EncryptedStreamNameModule.java#150) method splits the nonce from the encrypted string, and uses the secret key to decrypt the string.  It then checks that the client IP address matches the specified IP address, generates the path to the video file, and renames the stream internally to Wowza so it will serve the video file.


### configuration

configure the module and properties in `/usr/local/WowzaMediaServer/conf/[host]/Application.xml`:

module:

``` xml
<Module>
  <Name>encrypted-stream-name</Name>
  <Description>EncryptedStreamName</Description>
  <Class>edu.ucsd.library.dams.util.EncryptedStreamNameModule</Class>
</Module>
```

props:

``` xml
<Property>
  <Name>keyFile</Name>
  <Value>/path/to/streaming.key</Value>
  <Type>String</Type>
</Property>
<Property>
  <Name>streamBase</Name>
  <Value>mp4:localStore/</Value>
  <Type>String</Type>
</Property>
```

install:

``` sh
cp encrypted-stream-name.jar /usr/local/WowzaMediaServer/lib/
cp lib/commons-codec-1.6.jar /usr/local/WowzaMediaServer/lib/
sudo /sbin/service WowzaMediaServer restart
```
