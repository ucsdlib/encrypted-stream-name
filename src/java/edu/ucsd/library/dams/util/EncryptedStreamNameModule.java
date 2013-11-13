package edu.ucsd.library.dams.util;

import java.io.BufferedReader;
import java.io.FileReader;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.IClient;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.application.*;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider2;
import com.wowza.wms.mediacaster.IMediaCaster;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;

/**
 * Wowza module to decrypt an encrypted stream name and check validity before
 * renaming to correct stream name.
 * @author escowles@ucsd.edu
**/
public class EncryptedStreamNameModule extends ModuleBase
	implements IMediaStreamNameAliasProvider2
{
	private static String streamBase = null;
	private static String keyFile = null;
	private static String unknownIP = "";

	/**
	 * Initialization.
	**/
	public void onAppStart(IApplicationInstance app)
	{
		// load config
		streamBase = app.getProperties().getPropertyStr("streamBase");
		keyFile    = app.getProperties().getPropertyStr("keyFile");

		// register to handle alias resolution for all stream types
		app.setStreamNameAliasProvider(this);
	}

	/**
	 * Handle Flash client requests.
	**/
	public String resolvePlayAlias( IApplicationInstance appInstance,
		String name, IClient client )
	{
		getLogger().info("Resolve Play Flash: " + name);
		try
		{
			return decryptStreamName( name, client.getIp() );
		}
		catch ( Exception ex )
		{
			getLogger().warn( "Error decrypting stream", ex );
			sendClientOnStatusError(
				client, "NetStream.Play.Failed",
				ex.getMessage().replaceAll(": .*","")
			);
			return null;
		}
	}

	/**
	 * Handle HTTP client requests.
	**/
	public String resolvePlayAlias( IApplicationInstance appInstance,
		String name, IHTTPStreamerSession httpSession )
	{
		getLogger().info("Resolve Play HTTPSession: " + name);
		try
		{
			return decryptStreamName( name, httpSession.getIpAddress() );
		}
		catch ( Exception ex )
		{
			getLogger().warn( "Error decrypting stream", ex );
			return null;
		}
	}

	/**
	 * Handle RTP client requests.
	**/
	public String resolvePlayAlias( IApplicationInstance appInstance,
		String name, RTPSession rtpSession )
	{
		getLogger().info("Resolve Play RTPSession: " + name);
		try
		{
			return decryptStreamName( name, rtpSession.getIp() );
		}
		catch ( Exception ex )
		{
			getLogger().warn( "Error decrypting stream", ex );
			return null;
		}
	}

	/**
	 * Handle livestream client requests.
	**/
	public String resolvePlayAlias( IApplicationInstance appInstance,
		String name, ILiveStreamPacketizer liveStreamPacketizer )
	{
		getLogger().info("Resolve Play LiveStreamPacketizer: " + name);
		try
		{
			return decryptStreamName( name, unknownIP );
		}
		catch ( Exception ex )
		{
			getLogger().warn( "Error decrypting stream", ex );
			return null;
		}
	}

	public String resolveStreamAlias( IApplicationInstance appInstance,
		String name, IMediaCaster mediaCaster )
	{
		getLogger().info("Resolve Stream Mediacaster: " + name);
		try
		{
			return decryptStreamName( name, unknownIP );
		}
		catch ( Exception ex )
		{
			getLogger().warn( "Error decrypting stream", ex );
			return null;
		}
	}

	public String resolvePlayAlias( IApplicationInstance appInstance,
		String name)
	{
		getLogger().info("Resolve Play: " + name);
		try
		{
			return decryptStreamName( name, unknownIP );
		}
		catch ( Exception ex )
		{
			getLogger().warn( "Error decrypting stream", ex );
			return null;
		}
	}

	public String resolveStreamAlias( IApplicationInstance appInstance,
		String name )
	{
		getLogger().info("Resolve Stream: " + name);
		try
		{
			return decryptStreamName( name, unknownIP );
		}
		catch ( Exception ex )
		{
			getLogger().warn( "Error decrypting stream", ex );
			return null;
		}
	}

	/**
	 * Decrypt base64-encoded, AES/CBC-encrypted ciphertext.
	 * @param nonce Public, one-time-use encryption salt
	**/
	private String decrypt( String nonce, String ciphertext ) throws Exception
	{
		// read key from file
		BufferedReader buf = new BufferedReader( new FileReader(keyFile) );
		String keystr = buf.readLine();
		buf.close();

		// create secret key
		String keyType = "PBEWithMD5AndDES";
		char[] keyChars = keystr.toCharArray();
		byte[] nonceBytes = nonce.getBytes();
    	SecretKeyFactory factory = SecretKeyFactory.getInstance( keyType );
    	PBEKeySpec spec = new PBEKeySpec( keyChars, nonceBytes, 1024, 128 );
    	SecretKey raw = factory.generateSecret( spec );
    	SecretKey key = new SecretKeySpec( raw.getEncoded(), "AES" );

		// decode and decrypt
		byte[] decoded = Base64.decodeBase64( ciphertext );
		Cipher cipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
		IvParameterSpec params = new IvParameterSpec( nonceBytes );
		cipher.init( Cipher.DECRYPT_MODE, key, params );
		return new String( cipher.doFinal(decoded) );
	}

	/**
	 * Parse encrypted stream info and check request IP address.
	 * @param streamName Encrypted stream name in the format:
     *     [type:] [nonce] "," [encrypted stream info]
	 *   The encrypted stream info should be in the format:
	 *     [object id] " " [file id] " " [request IP]
	 * @param requestIP The IP address of the Wowza request, which will be
	 *   checked against the IP address in the stream info package.
	**/
	private String decryptStreamName( String streamName, String requestIP )
		throws Exception
	{
		// fallback on unencrypted stream for now
		if ( streamName.indexOf(",") == -1 )
		{
			getLogger().warn( "plain: " + streamName );
			throw new Exception(
				"Invalid stream name: Trying to decrypt plain stream name: "
				+ streamName
			);
		}

		// parse and decrypt stream info
		String[] parts = streamName.split(","); // nonce,ciphertext
		String argStr = decrypt(parts[0],parts[1]);
		String[] argArr = argStr.split(" "); // ark,file,ip
		if ( argArr == null || argArr.length != 3 )
		{
			throw new Exception("Error decrypting stream name: " + argStr);
		}

		String objid = argArr[0];
		String fileid = argArr[1];
		String ip = argArr[2];

		// check format of objid and fileid
		if ( objid == null || objid.trim().equals("") || objid.length() != 10
			|| fileid == null || fileid.trim().equals("") )
		{
			throw new Exception( "Invalid object: " + objid + "/" + fileid );
		}

		// make sure the request IP matches the verified IP
    // NOTE: temporarily commenting out since this doesn't work with Reverse Proxy configuration
    /*
		if ( requestIP == null || requestIP.trim().equals("")
			|| !requestIP.equals(ip) )
		{
			throw new Exception( "Invalid IP address: " + requestIP);
		}
    */

		//
		// Set Wowza stream name prefix, defaults to "mp4:"
		//

		String extension = "";
		String wowzaStreamNamePrefix = "";

		int dot = fileid.lastIndexOf('.');

		if (dot == -1)
		{
			throw new Exception( "Missing file extension: " + fileid );
		}
		else
		{
			extension = fileid.substring(dot+1).toLowerCase();
		}

		if (extension == "mp3")
			{wowzaStreamNamePrefix = "mp3:";}
		else if (extension == "flv")
			{wowzaStreamNamePrefix = "flv:";}
		else
			{wowzaStreamNamePrefix = "mp4:";}

		//
		// Rename the stream
		//

		String newName = wowzaStreamNamePrefix + streamBase;

		try
		{
			// pairpath based on objid
			for( int i = 0; i < (objid.length() - 1); i += 2 )
			{
				newName += objid.substring(i,i+2);
				newName += "/";
			}
			newName += "20775-" + objid + "-" + fileid.replaceAll("/","-");
			getLogger().warn( "decrypted: " + streamName  + " -> " + newName );
			return newName;
		}
		catch ( Exception ex )
		{
			throw new Exception(
				"Error building stream name: " + newName + ", " + objid + ", "
				+ fileid
			);
		}
	}
	public static void main( String[] args ) throws Exception
	{
		String[] parts = args[0].split(",");
		String nonce = parts[0];
		String ciphertext = parts[1];
		EncryptedStreamNameModule module = new EncryptedStreamNameModule();
		module.keyFile = "streaming.key";
		String plaintext = module.decrypt( nonce, ciphertext );
		System.out.println("decrypted: " + plaintext);
	}
}
