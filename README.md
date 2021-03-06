## encrypted-stream-name

Wowza plugin to decrypt an encrypted stream name, do some basic validation,
and rename the stream internally to the decrypted name.

## DAMS4 vs DAMS5

- DAMS4 changes should be made on the `dams4` branch.
- DAMS5 changes should be made on the `master` branch.

Please make your feature branches off the appropriate branch and setup your PRs accordingly.

### encryption process

1. The DAMS PAS function [dams_objects_helper#encrypt_stream_name](https://github.com/ucsdlib/damspas/blob/develop/app/helpers/dams_objects_helper.rb#L516) builds a string consisting of the video object ARK, filename, and client IP address separated by spaces.  It then generates a 16-character random nonce, encrypts the string using the secret key, and builds the request token consisting of the nonce, a comma, and then the encrypted string:

    ```
    plaintext: "bd0786115s 0-2.mp4 192.168.1.100"
    encrypted: "odx0klxzpuc785iy,mrKmOjRTYGH6yuG4G2oy9jwB2TrVqhS9UVf1r8Znnm36CfJ6CorePqajirhNbO-M"
    ```

    This stream URL is then built by adding the format and encrypted token to the Wowza base URL, using either the RTMP protocol, or the HTTP protocol with a M3U playlist suffix, e.g.:

    ```
    rtmp://lib-streaming-test.ucsd.edu:1935/dams4/_definst_/mp4:odx0klxzpuc785iy,mrKmOjRTYGH6yuG4G2oy9jwB2TrVqhS9UVf1r8Znnm36CfJ6CorePqajirhNbO-M
    http://lib-streaming-test.ucsd.edu:1935/dams4/_definst_/mp4:odx0klxzpuc785iy,mrKmOjRTYGH6yuG4G2oy9jwB2TrVqhS9UVf1r8Znnm36CfJ6CorePqajirhNbO-M/playlist.m3u8
    ```

2. The [EncryptedStreamNameModule.decryptStreamName](https://lib-stash.ucsd.edu/projects/ND/repos/encrypted-stream-name/browse/src/java/edu/ucsd/library/dams/util/EncryptedStreamNameModule.java#150) first removes the format prefix, and then splits the nonce from the encrypted string.  The encrypted string is decoded using the nonce and the secret key.  It then parses the decrypted stream info and checks that the client IP address matches the specified IP address, generates the path to the video file, and renames the stream in Wowza so it will serve the video file.

    ```
    encrypted: mp4:odx0klxzpuc785iy,mrKmOjRTYGH6yuG4G2oy9jwB2TrVqhS9UVf1r8Znnm36CfJ6CorePqajirhNbO-M
	decrypted: type=mp4, objid=bd0786115s, fileid=0-2.mp4, ip=192.168.1.100
    renamed to: mp4:localStore/bd/07/86/11/5s/20775-bd0786115s-0-2.mp4
    ```

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
  <Value>localStore/</Value>
  <Type>String</Type>
</Property>
```

install:

``` sh
cp encrypted-stream-name.jar /usr/local/WowzaMediaServer/lib/
cp lib/commons-codec-1.6.jar /usr/local/WowzaMediaServer/lib/
sudo /sbin/service WowzaMediaServer restart
```
