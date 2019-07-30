/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.samczsun.skype4j.internal.client;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.samczsun.skype4j.chat.GroupChat;
import com.samczsun.skype4j.events.contact.ContactRequestEvent;
import com.samczsun.skype4j.exceptions.AccountNotFoundException;
import com.samczsun.skype4j.exceptions.ChatNotFoundException;
import com.samczsun.skype4j.exceptions.ConnectionException;
import com.samczsun.skype4j.exceptions.InvalidCredentialsException;
import com.samczsun.skype4j.exceptions.SuspiciousLoginException;
import com.samczsun.skype4j.exceptions.WrongPasswordException;
import com.samczsun.skype4j.exceptions.handler.ErrorHandler;
import com.samczsun.skype4j.exceptions.handler.ErrorSource;
import com.samczsun.skype4j.internal.*;
import com.samczsun.skype4j.internal.participants.info.ContactImpl;
import com.samczsun.skype4j.internal.participants.info.ContactRequestImpl;
import com.samczsun.skype4j.internal.utils.Encoder;
import com.samczsun.skype4j.internal.utils.UncheckedRunnable;
import com.samczsun.skype4j.participants.info.Contact;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class FullClient extends SkypeImpl {
    private static final Pattern URL_PATTERN = Pattern.compile("threads/(.*)", Pattern.CASE_INSENSITIVE);
    private static final String SERVER_HOSTNAME = "https://login.live.com";
    private final String password;

    public FullClient(String username, String password, Set<String> resources, Logger customLogger, List<ErrorHandler> errorHandlers) throws ConnectionException {
        super(username, resources, customLogger, errorHandlers);
        this.password = password;
    }

    @Override
    public void login() throws InvalidCredentialsException, ConnectionException, WrongPasswordException, AccountNotFoundException, SuspiciousLoginException, IOException, UnknownHostException, Exception {
        logger.finer("Refreshing tokens");
    
        Response authorize = null;
        try {
            authorize = Jsoup.connect(SERVER_HOSTNAME + "/oauth20_authorize.srf?client_id=00000000480BC46C&scope=service%3A%3Alw.skype.com%3A%3AMBI_SSL&response_type=token&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf&state=999&locale=en").maxBodySize(100 * 1024 * 1024).timeout(30000).method(Connection.Method.GET).ignoreContentType(true).ignoreHttpErrors(true).execute();
        } catch (Exception ex) {
            this.handleError(ErrorSource.SESSION_ACTIVE, ex, false);
            Logger.getLogger(FullClient.class.getName()).log(Level.SEVERE, null, ex);
            UnknownHostException e = new UnknownHostException();
            throw e;
        }

        String MSPOK = authorize.cookie("MSPOK");
        if (MSPOK == null) {
          IOException e = new IOException("Error while connecting to Live: MSPOK not set.");
          logger.severe(e.toString());
          throw e;
        }

        Elements PPFTs = null;
        for (int i = 0; i < authorize.body().length(); i++) {
          i = authorize.body().indexOf("<input", i);
          if (i == -1) {
            break;
          }
          int j = authorize.body().indexOf(">", i);
          if (j == -1) {
            break;
          }
          PPFTs = Jsoup.parseBodyFragment(authorize.body().substring(i, j + ">".length())).select("input[name=PPFT][value]");
          if (!PPFTs.isEmpty()) {
            break;
          }
        }
        if (PPFTs == null || PPFTs.isEmpty()) {
          IOException e = new IOException("Error while connecting to Live: PPFT not found.");
          logger.severe(e.toString());
          throw e;
        }
        String PPFT = PPFTs.first().attr("value");

        String postUrl = SERVER_HOSTNAME + "/ppsecure/post.srf?client_id=00000000480BC46C&scope=service%3A%3Alw.skype.com%3A%3AMBI_SSL&response_type=token&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf&state=999&locale=en";
        int urlStart = authorize.body().indexOf("urlPost:'");
        if (urlStart != -1) {
          int urlEnd = authorize.body().indexOf('\'', urlStart + "urlPost:'".length());
          if (urlEnd != 1) {
            postUrl = authorize.body().substring(urlStart + "urlPost:'".length(), urlEnd);
          }
        }

        Connection.Response post = null;
        try {
            post = Jsoup.connect(postUrl).data("PPFT", PPFT, "login", getUsername().toLowerCase(), "passwd", password).cookie("MSPOK", MSPOK).maxBodySize(100 * 1024 * 1024).timeout(30000).method(Connection.Method.POST).followRedirects(false).ignoreContentType(true).ignoreHttpErrors(true).execute();
        } catch (IOException ex) {
            Logger.getLogger(FullClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (post.statusCode() != 302) {
            int index = post.body().indexOf("sErrTxt:'");
            int end;
            end = post.body().indexOf('\'', index + "sErrTxt:'".length());
            
            if (!(index == -1 && end == -1)) {
                while (post.body().charAt(end - 1) == '\\') {
                  end = post.body().indexOf('\'', end + 1);
                }
                if (index == -1 || end == -1) {
                  IOException e = new IOException("Error while connecting to Live: not redirected, no reason given.");
                  logger.severe(e.toString());
                  throw e;
                }

                String errMessage = post.body().substring(index + "sErrTxt:'".length(), end);
                if ((errMessage.contains("account") || errMessage.contains("password")) && errMessage.contains("incorrect")) {
                  WrongPasswordException e = new WrongPasswordException("Error while connecting to Live: " + errMessage);
                  logger.severe(e.toString());
                  throw e;
                } else if (errMessage.toLowerCase().contains("microsoft account doesn\\'t exist")) {
                  AccountNotFoundException e = new AccountNotFoundException("Error while connecting to Live: " + errMessage);
                  logger.severe(e.toString());
                  throw e;
                } else {
                  IOException e = new IOException("Error while connecting to Live: " + errMessage);
                  logger.severe(e.toString());
                  throw e;
                }
            } else {
                SuspiciousLoginException e = new SuspiciousLoginException("Error while connecting to Live, suspicious login");
                logger.severe(e.toString());
                throw e;
            }
        }
        String url = post.header("Location");

        int accessTokenStart = url.indexOf("access_token=");
        if (accessTokenStart == -1) {
            SuspiciousLoginException e = new SuspiciousLoginException("Error while connecting to Live: access token not found.");
            logger.severe(e.toString());
            throw e;
        }
        int accessTokenEnd = url.indexOf('&', accessTokenStart + "access_token=".length());
        if (accessTokenEnd == -1) {
          accessTokenEnd = url.length();
        }
        String accessToken = url.substring(accessTokenStart + "access_token=".length(), accessTokenEnd);
        try {
            accessToken = URLDecoder.decode(accessToken, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FullClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Map<String, String> data = new HashMap<>();
        data.put("scopes", "client");
        data.put("clientVersion", "1431/8.30.0.50");
        data.put("username", getUsername().toLowerCase());
        data.put("passwordHash", hash());
        data.put("access_token", accessToken);
        data.put("site_name", "lw");

        logger.finer("Sending post request to: " + Endpoints.LOGIN_URL.url());
        JsonObject loginData = Endpoints.LOGIN_URL.open(this)
                .as(JsonObject.class)
                .expect(200, "While logging in")
                .post(Encoder.encode(data));

        this.setSkypeToken(loginData.get("skypetoken").asString());
        this.setLiveUsername(loginData.get("skypeid").asString());

        List<UncheckedRunnable> tasks = new ArrayList<>();
        tasks.add(() -> {
            HttpURLConnection asmResponse = getAsmToken();
            String[] setCookie = asmResponse.getHeaderField("Set-Cookie").split(";")[0].split("=");
            this.cookies.put(setCookie[0], setCookie[1]);
        });
        tasks.add(this::loadAllContacts);
        tasks.add(() -> this.getContactRequests(false));
        tasks.add(() -> {
            try {
                this.registerWebSocket();
            } catch (Exception e) {
                handleError(ErrorSource.REGISTERING_WEBSOCKET, e, false);
            }
        });
        tasks.add(this::registerEndpoint);

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            tasks.forEach(executorService::submit);
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        super.login();
    }

    @Override
    public void logout() throws ConnectionException {
        logger.finer("Sending get request to: " + Endpoints.LOGOUT_URL.url());
        if (!this.getEndpointId().isEmpty()) {
            Endpoints.LOGOUT_URL
                .open(this, this.getEndpointId())
                .noRedirects()
                .expect(200, "While logging out")
                .cookies(cookies)
                .delete();
        }
        shutdown();
    }

    @Override
    public void loadAllContacts() throws Exception {
        int i = 0;
        
        JsonObject object = null;
        try {
            setProfileInfo(this);
            
            logger.finer("Sending get request to: " + Endpoints.GET_ALL_CONTACTS.url());
            object = Endpoints.GET_ALL_CONTACTS
                .open(this)
                .as(JsonObject.class)
                .expect(200, "While loading contacts")
                .get();

            for (JsonValue value : object.get("contacts").asArray()) {

                JsonObject obj = value.asObject();
                if (obj.get("suggested") == null || !obj.get("suggested").asBoolean()) {
                    if (!allContacts.containsKey(obj.get("person_id").asString())) {
                        logger.finer("Implementing contact: " + obj.get("person_id").asString());
                        this.allContacts.put(obj.get("person_id").asString(), new ContactImpl(this, obj));
                    }
                }
                i++;
            }
            for(JsonValue value : object.get("blocklist").asArray()) {
                JsonObject obj = value.asObject();
                if (!this.allContacts.containsKey(obj.get("mri").asString())) {
                    this.allContacts.put(obj.get("mri").asString(), new ContactImpl(this, obj.get("mri").asString()));
                }
            }

        } catch (Exception e) {
            logger.severe("Error while send get request to: " + Endpoints.GET_ALL_CONTACTS.url() + e.getMessage());
        }  
    }

    @Override
    public void getContactRequests(boolean fromWebsocket) throws Exception {
        JsonArray array =  Endpoints.AUTH_REQUESTS_URL
                .open(this)
                .as(JsonArray.class)
                .expect(200, "While loading authorization requests")
                .get();
        for (JsonValue contactRequest : array) {
            JsonObject contactRequestObj = contactRequest.asObject();
            try {
                ContactRequestImpl request = new ContactRequestImpl(contactRequestObj.get("event_time").asString(),
                        contactRequestObj.get("sender").asString(),
                        contactRequestObj.get("greeting").asString(), this);
                if (this.allContactRequests.add(request)) {
                    if (fromWebsocket) {
                        ContactRequestEvent event = new ContactRequestEvent(request);
                        getEventDispatcher().callEvent(event);
                    }
                }
            } catch (java.text.ParseException e) {
                getLogger().log(Level.WARNING, "Could not parse date for contact request", e);
            }
        }
        if (fromWebsocket) this.updateContactList();
    }

    @Override
    public void updateContactList() throws Exception {
        int i = 0;
        JsonObject object = null;

        try {
            setProfileInfo(this);
            
            logger.finer("Sending get request to: " + Endpoints.GET_ALL_CONTACTS.url());
            object = Endpoints.GET_ALL_CONTACTS
                .open(this, getUsername(), "notification")
                .as(JsonObject.class)
                .expect(200, "While loading contacts")
                .get();

            for (JsonValue value : object.get("contacts").asArray()) {
                if (value.asObject().get("suggested") == null || !value.asObject().get("suggested").asBoolean()) {
                    String id = value.asObject().get("person_id").asString();
                    logger.finer("Updating contact: " + id);
                    ContactImpl impl = (ContactImpl) allContacts.get(id);
                    if (impl == null) impl = (ContactImpl) loadContact(id);
                    impl.update(value.asObject());
                }
                i++;
            }
            for(JsonValue value : object.get("blocklist").asArray()) {
                JsonObject obj = value.asObject();
                if (!this.allContacts.containsKey(obj.get("mri").asString())) {
                    this.allContacts.put(obj.get("mri").asString(), new ContactImpl(this, obj.get("mri").asString()));
                }
            }
        } catch (Exception e) {
            logger.severe("Error while send get request to: " + Endpoints.GET_ALL_CONTACTS.url() + e.getMessage());
        }
    }

    @Override
    public GroupChat createGroupChat(Contact... contacts) throws ConnectionException {
        JsonObject obj = new JsonObject();
        JsonArray allContacts = new JsonArray();
        allContacts.add(new JsonObject().add("id", "8:" + this.getUsername()).add("role", "Admin"));
        for (Contact contact : contacts) {
            allContacts.add(new JsonObject().add("id", "8:" + contact.getUsername()).add("role", "User"));
        }
        obj.add("members", allContacts);
        HttpURLConnection con = Endpoints.THREAD_URL
                .open(this)
                .as(HttpURLConnection.class)
                .expect(201, "While creating group chat")
                .post(obj);
        String url = con.getHeaderField("Location");
        Matcher chatMatcher = URL_PATTERN.matcher(url);
        if (chatMatcher.find()) {
            String id = chatMatcher.group(1);
            try {
                return (GroupChat) this.getOrLoadChat(id);
            } catch (ChatNotFoundException e) {
                throw new RuntimeException(e);
            } catch (Exception ex) {
                Logger.getLogger(FullClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            throw ExceptionHandler.generateException("No chat location", con);
        }
        return null;
    }
    
    private static void setProfileInfo(SkypeImpl skype) throws ConnectionException {
        JsonObject obj = Endpoints.PROFILE_INFO
                .open(skype)
                .expect(200, "While getting profile info")
                .as(JsonObject.class)
                .get();
        String userPhones = "";
        String displayName = "";
        if (Utils.getString(obj, "lastname") != null){
            displayName = Utils.getString(obj, "firstname") + " " + Utils.getString(obj, "lastname");
        } else {
            displayName = Utils.getString(obj, "firstname");
        }
        if (Utils.getString(obj, "phoneHome") != null){
            userPhones = Utils.getString(obj, "phoneHome");
        }
        if (Utils.getString(obj, "phoneMobile") != null){
            if (userPhones == ""){
                userPhones = Utils.getString(obj, "phoneMobile");
            } else {
                userPhones += ", " + Utils.getString(obj, "phoneMobile");
            }
        }
        if (Utils.getString(obj, "phoneOffice") != null){
            if (userPhones == ""){
                userPhones = Utils.getString(obj, "phoneOffice");
            } else {
                userPhones += ", " + Utils.getString(obj, "phoneOffice");
            }
        }
        skype.setUserPhones(userPhones);
        skype.setDisplayName(displayName);
    }

    private String hash() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] encodedMD = messageDigest.digest(String.format("%s\nskyper\n%s", getUsername().toLowerCase(), password).getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printBase64Binary(encodedMD);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
