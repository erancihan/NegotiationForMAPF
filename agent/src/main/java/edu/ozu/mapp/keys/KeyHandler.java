package edu.ozu.mapp.keys;

import edu.ozu.mapp.agent.Agent;
import org.springframework.util.Assert;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class KeyHandler
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KeyHandler.class);
    private static KeyHandler instance;

    private final ConcurrentHashMap<String, String> vault;

    private KeyHandler()
    {
        vault = new ConcurrentHashMap<>();
    }

    public static KeyHandler getInstance()
    {
        if (instance == null)
        {
            synchronized (KeyHandler.class)
            {
                if (instance == null)
                {
                    instance = new KeyHandler();
                }
            }
        }

        return instance;
    }

    private static KeyPair generateKeyPair()
    {
        try
        {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);

            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            logger.error("error while generating key pairs");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public AgentKeys create(Agent agent)
    {
        Assert.isTrue(agent.AGENT_ID != null && !agent.AGENT_ID.isEmpty(), "Agent is empty!!!");

        try {
            AgentKeys keys = new AgentKeys(generateKeyPair(), agent);

            vault.put(agent.AGENT_ID,Base64.getEncoder().encodeToString(keys.GetPublicKey().getEncoded()));

            return keys;
        } catch (Exception e) {
            logger.error("an error happened while creating Key Pairs");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static String encrypt(String text, Agent agent)
    {
        return ""; //encrypt(text, agent.GetPubKey());
    }

    public static String encrypt(String text, PublicKey key)
    {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes()));
        } catch (Exception e) {
            logger.error("error when encrypting!");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public static String decrypt(String text, PrivateKey key)
    {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(text)));
        } catch (Exception e) {
            logger.error("error when decrypting!");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public PublicKey getPubKey(String agentID)
    {
        String key_str = vault.get(agentID);

        PublicKey key = null;
        try {
            key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key_str)));
        } catch (Exception e) {
            logger.error("failed to fetch public key!");
            e.printStackTrace();
            System.exit(1);
        }

        return key;
    }
}
