package edu.sjsu.cmpe282.s3url;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.VerificationJwkSelector;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JwksClient {
    private List<String> jwks;
    private static JwksClient client;

    private JwksClient(List<String> jwksUrls) {
        this.jwks = new ArrayList<>();
        for (String url : jwksUrls) {
            try {
                this.jwks.add(this.getJwksJson(url));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private String getJwksJson(String url) throws Exception{
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();
        return response.toString();
    }

    private Map<String, String> getJwsMap(String payload) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> result = gson.fromJson(payload, type);
        return result;
    }

    public String getID(String token) {
        for (String jwks : this.jwks) {
            try {
                String payload = this.getJwtPayload(jwks, token);
                if (payload != null && payload.isEmpty() == false) {
                    Map<String, String> map = this.getJwsMap(payload);
                    String expStr =  map.get("exp");
                    long exp = Long.parseLong(expStr);
                    // Check for token expiry
                    if (exp < System.currentTimeMillis()/1000) {
                        System.out.println("Token expired");
                        return null;
                    }

                    return map.get("sub");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        return null;
    }

    private String getJwtPayload(String jwks, String token) throws Exception {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.BLACKLIST,   AlgorithmIdentifiers.NONE));
        jws.setCompactSerialization(token);
        JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jwks);

        VerificationJwkSelector jwkSelector = new VerificationJwkSelector();
        JsonWebKey jwk = jwkSelector.select(jws, jsonWebKeySet.getJsonWebKeys());
        jws.setKey(jwk.getKey());

        boolean signatureVerified = jws.verifySignature();
        if (signatureVerified == false) {
            throw new Exception("Signature not verified");
        }
        String payload = jws.getPayload();
        return payload;
    }

    public static JwksClient getInstance(List<String> urls) {
        if (client == null) {
            client = new JwksClient(urls);
        }

        return client;
    }

    public static JwksClient getInstance() {
        return client;
    }
}

