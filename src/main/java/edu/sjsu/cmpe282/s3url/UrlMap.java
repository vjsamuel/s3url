package edu.sjsu.cmpe282.s3url;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.io.Serializable;

@DynamoDBTable(tableName = "UrlMap")
public class UrlMap implements Serializable {

    private String id;
    private String oURL;
    private String sURL;
    private String user;

    public UrlMap() {
    }

    public UrlMap(String id, String oURL, String sURL, String user) {
        this.id = id;
        this.oURL = oURL;
        this.sURL = sURL;
        this.user = user;
    }


    @DynamoDBHashKey(attributeName = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDBAttribute(attributeName = "oURL")
    public String getoURL() {
        return oURL;
    }

    public void setoURL(String oURL) {
        this.oURL = oURL;
    }

    @DynamoDBAttribute(attributeName = "sURL")
    public String getsURL() {
        return sURL;
    }

    public void setsURL(String sURL) {
        this.sURL = sURL;
    }

    @DynamoDBAttribute(attributeName = "user")
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    @Override
    public String toString() {
        return String.format("URL[id=%s, sURL='%s', oURL='%s' user='%s']", id, sURL, oURL, user);
    }

}
