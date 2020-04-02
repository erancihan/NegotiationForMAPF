package edu.ozu.mapp.keys;

import edu.ozu.mapp.agent.Agent;
import edu.ozu.mapp.agent.client.handlers.JedisConnection;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Base64;

public class KeyHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KeyHandler.class);
    private static redis.clients.jedis.Jedis jedis = JedisConnection.getInstance();

    private static final String KEY_VAULT = "PubKeyVault";

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

    public static AgentKeys create(Agent agent)
    {
        try {
            if (jedis.exists(KEY_VAULT)) {
                if (jedis.hexists(KEY_VAULT, agent.AGENT_ID)) {
                    return null;
                }
            }

            AgentKeys keys = new AgentKeys(generateKeyPair(), agent);

            jedis.hset(KEY_VAULT, agent.AGENT_ID, keys.get_public().toString());

            return keys;
        } catch (Exception e) {
            logger.error("an error happened while creating Key Pairs");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public static String encrypt(String text, PrivateKey key)
    {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes()));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            logger.error("error when encrypting!");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public static String decrypt(String text, PublicKey key)
    {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(text)));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            logger.error("error when decrypting!");
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public static String getPubKey(String agentID)
    {
        return jedis.hget(KEY_VAULT, agentID);
    }
}
