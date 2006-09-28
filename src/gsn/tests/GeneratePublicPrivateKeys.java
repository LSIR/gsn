package gsn.tests;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class GeneratePublicPrivateKeys {

    public static void main(String[] args) throws NoSuchAlgorithmException,
	    NoSuchProviderException, FileNotFoundException, IOException {
	if (args.length != 2) {
	    System.out.println("Error, the right usage is :");
	    System.out
		    .println("GeneratePublicPrivateKeys privateKeyFileName  publicKeyFileName");
	    System.exit(1);
	}
	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
	SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
	keyGen.initialize(512, random);
	KeyPair pair = keyGen.generateKeyPair();
	PrivateKey priv = pair.getPrivate();
	PublicKey pub = pair.getPublic();

	OutputStream output = new FileOutputStream(args[0]);
	output.write(priv.getEncoded());
	output.close();
	System.out.println("The private key stored at : " + args[0]);
	output = new FileOutputStream(args[1]);
	output.write(pub.getEncoded());
	output.close();
	System.out.println("The public key stored at : " + args[1]);
    }
}
