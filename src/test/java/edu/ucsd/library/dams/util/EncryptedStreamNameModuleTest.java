package edu.ucsd.library.dams.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests methods in EncryptedStreamNameModule class
 * @author lsitu
 *
 */
public class EncryptedStreamNameModuleTest  {

	private static EncryptedStreamNameModule module = null;

	@BeforeClass
	public static void init() throws IOException {
		File keyFile = new File("streaming.key");
		FileWriter writer = null;
		try {
			writer = new FileWriter(keyFile);
			writer.write("xxxxxxxxxxxxxxxx");
		} finally {
			if (writer != null)
				writer.close();
		}
		module = new EncryptedStreamNameModule();
		module.setKeyFile(keyFile.getAbsolutePath());
	}

	@Test
	public void testDecrypt() throws Exception {
		String encryptedtext = "kf8ih30qyrw39rpb,wSTL7GcDUZd1ic1qLwKA3ubn9Amh4rbr5NQymDdDvwT-GZW-KkzsLJfz4dd8KzwFHG5xdrqslOyaY7_m2Y60wfkJNDsVw2RbpSCxOCsX6FA=";
		String[] parts = encryptedtext.split(",");
		String nonce = parts[0];
		String ciphertext = parts[1];
		String plaintext = module.decrypt( nonce, ciphertext );
		String expected = "3j333268h d504rk75p 34cddd4138eeb3686bdb095b9a26ac077919b0f0 mp3 127.0.0.1";
		Assert.assertEquals("Descripted text need to be matched", expected, plaintext);
	}
}
