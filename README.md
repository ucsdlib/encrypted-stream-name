## encrypted-stream-name

Wowza plugin to decrypt an encrypted stream name, do some basic validation,
and rename the stream internally to the decrypted name.


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
