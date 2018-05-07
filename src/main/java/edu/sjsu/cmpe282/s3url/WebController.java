package edu.sjsu.cmpe282.s3url;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@CrossOrigin(origins = "*")
public class WebController {

    @Autowired
    UrlMapRepository repository;

    @Autowired
    JwksConfig jwksconfig;

    @RequestMapping("/deleteall")
    public String delete() {
        repository.deleteAll();
        return "Done";
    }

    @RequestMapping(method = RequestMethod.DELETE, value="/api/v1/url/{shortURL}")
    public ResponseEntity<UrlMap> deleteURL(@RequestHeader(value="Authorization") String auth, @PathVariable String shortURL) {
        if (auth.equals(null) || auth.isEmpty() == true) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String user = JwksClient.getInstance(jwksconfig.getUrls()).getID(auth);
        if (user == null || user.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            UrlMap map = repository.findBysURL(shortURL).get(0);

            if (!map.getUser().equals(user)) {
                // User is different
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            String id = repository.findBysURL(map.getsURL()).get(0).getId();
            repository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            e.getMessage();

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/api/v1/url/{shorturl}")
    public ResponseEntity<String> expandUrl(@PathVariable String shorturl){
        if(repository.findBysURL(shorturl).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        HttpHeaders header = new HttpHeaders();
        header.add("Location", repository.findBysURL(shorturl).get(0).getoURL());
        return new ResponseEntity<>(header, HttpStatus.PERMANENT_REDIRECT);
    }


    @RequestMapping(method = RequestMethod.PUT, value="/api/v1/url/shorten/{shortURL}")
    public ResponseEntity<UrlMap> putURL (@RequestHeader(value="Authorization") String auth, @RequestBody UrlMap input, @PathVariable String shortURL){
        if (auth.equals(null) || auth.isEmpty() == true) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String user = JwksClient.getInstance(jwksconfig.getUrls()).getID(auth);
        if (user == null || user.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            String oURL = input.getoURL();
            String sURL = shortURL;

            if (input.getoURL() == null) {
                return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
            }

            // Find object by short URL
            List<UrlMap> repositoryList = repository.findBysURL(sURL);


            for(UrlMap urlmap_object: repositoryList) {
                // Found the object
                if (urlmap_object.getsURL().equals(sURL)) {
                    // User is the same as the user trying to update
                    if (urlmap_object.getUser().equals(user)) {
                        repository.deleteById(urlmap_object.getId());

                        UUID uid = UUID.randomUUID();
                        String id = uid.toString();
                        // Do update
                        UrlMap putUrlMap = new UrlMap(id, oURL, sURL, user);
                        repository.save(putUrlMap);

                        return new ResponseEntity<>(putUrlMap, HttpStatus.ACCEPTED); //202
                    } else {
                        // User is different
                        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                    }
                }
            }

            // Object is not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch(Exception e) {
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/api/v1/url/shorten")
    public ResponseEntity<UrlMap> shortenURL(@RequestHeader(value="Authorization") String auth, @RequestBody UrlMap input) {
        if (input.getoURL() == null || input.getoURL().equals("")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (auth.equals(null) || auth.isEmpty() == true) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String user = JwksClient.getInstance(jwksconfig.getUrls()).getID(auth);
        if (user == null || user.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            String oURL = input.getoURL();
            if (!oURL.startsWith("https") && !oURL.startsWith("http")) {
                oURL = "http://" + oURL;
            }
            UUID uuid = UUID.randomUUID();
            String id = uuid.toString();
            String sURL = getHash(oURL);
            if (!repository.findBysURL(sURL).isEmpty()) {
                sURL = getHash(oURL + id);
            }

            UrlMap entry = new UrlMap(id, oURL, sURL, user);
            repository.save(entry);
            return new ResponseEntity<>(entry, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
    }

    @RequestMapping("/api/v1/urls")
    public ResponseEntity<List<UrlMap>> getUrls(@RequestHeader(value="Authorization") String auth) {
        if (auth.equals(null) || auth.isEmpty() == true) {
            return new ResponseEntity<>( HttpStatus.UNAUTHORIZED);
        }

        String user = JwksClient.getInstance(jwksconfig.getUrls()).getID(auth);
        if (user == null || user.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<UrlMap> response = repository.findByuser(user);
        if (response.isEmpty() == true || response.size() == 0) {
            System.out.println("User is not FOUND");
            return new ResponseEntity<>(Collections.EMPTY_LIST, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    public static String getHash(String longurl) {
        // Moving to SHA-512 as it is the secure Hash algorithm:
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] sourceurl = md.digest(longurl.getBytes());
            BigInteger number = new BigInteger(1, sourceurl);
            String hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext.substring(0, 7);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
