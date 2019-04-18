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

package com.samczsun.skype4j.internal.participants.info;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.chat.Chat;
import com.samczsun.skype4j.exceptions.ChatNotFoundException;
import com.samczsun.skype4j.exceptions.ConnectionException;
import com.samczsun.skype4j.exceptions.NoSuchContactException;
import com.samczsun.skype4j.internal.Endpoints;
import com.samczsun.skype4j.internal.SkypeImpl;
import com.samczsun.skype4j.internal.Utils;
import com.samczsun.skype4j.internal.client.FullClient;
import com.samczsun.skype4j.internal.utils.Encoder;
import com.samczsun.skype4j.participants.info.Contact;
import org.jsoup.helper.Validate;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.UUID;
import java.util.regex.Pattern;

public class ContactImpl implements Contact {
    private static final Pattern PHONE_NUMBER = Pattern.compile("\\+[0-9]+");

    public static Contact createContact(SkypeImpl skype, String username) throws ConnectionException {
        Validate.notEmpty(username, "Username must not be empty");
        return new ContactImpl(skype, username, getObject(skype, username));
    }

    public static Contact createContact(SkypeImpl skype, String username, JsonObject unaddedData) throws ConnectionException {
        Validate.notEmpty(username, "Username must not be empty");
        return new ContactImpl(skype, username, unaddedData);
    }

    private static JsonObject getObject(SkypeImpl skype, String username) throws ConnectionException {
        JsonObject obj = Endpoints.PROFILE_INFO
                .open(skype)
                .expect(200, "While getting profile info")
                .as(JsonObject.class)
                .get();
        String userPhones = "";
        String displayName = Utils.getString(obj, "firstname") + " " + Utils.getString(obj, "lastname");
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
        return obj;
    }

    private SkypeImpl skype;
    private String username;
    private String displayName;
    private String firstName;
    private String phones ; 
    private String status;
    private String lastName;
    private String birthday;
    private String gender;
    private String language;
    private String avatarURL;
    private BufferedImage avatar;
    private String mood;
    private String richMood;
    private String country;
    private String city;
    private boolean isPhone;

    private boolean isAuthorized;
    private boolean isBlocked;

    // What is this?
    private String authCertificate;
    private UUID personId;
    private String type;

    public ContactImpl(SkypeImpl skype, String username, JsonObject unaddedData) throws ConnectionException {
        this.skype = skype;
        this.username = username;
        if (!PHONE_NUMBER.matcher(username).matches()) {
            updateProfile(unaddedData);
//            updateContactInfo();
        } else {
            this.isPhone = true;
        }
    }

    public ContactImpl(SkypeImpl skype, JsonObject contact, String contactStatus) throws ConnectionException {
        this.skype = skype;
        getObject(skype, username);
        update(contact, contactStatus);
    }
    
    public ContactImpl(SkypeImpl skype) throws ConnectionException {
        this.skype = skype;
        getObject(skype, username);
    }

    private void updateContactInfo() throws ConnectionException {
        if (this.skype instanceof FullClient) {
            JsonObject obj = Endpoints.GET_CONTACT_BY_ID
                    .open(skype, skype.getUsername(), username)
                    .as(JsonObject.class)
                    .expect(200, "While getting contact info")
                    .get();
            if (obj.get("contacts").asArray().size() > 0) {
                JsonObject contact = obj.get("contacts").asArray().get(0).asObject();
//                update(contact, contactStatus);
            } else {
                this.isAuthorized = false;
                this.isBlocked = false;
            }
        }
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String getFirstName() {
        return this.firstName;
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }
    
    public String getPhoneNumbers() {
        return this.phones;
    }
    
    public String getStatus() {
        return this.status;
    }

    @Override
    public BufferedImage getAvatarPicture() throws ConnectionException {
        if (this.avatarURL != null) {
            if (this.avatar == null) {
                //Casting might not be safe.
                this.avatar = Endpoints
                        .custom(this.avatarURL, skype)
                        .expect(200, "While fetching avatar")
                        .as(BufferedImage.class)
                        .get();

            }
            BufferedImage clone = new BufferedImage(avatar.getWidth(), avatar.getHeight(), avatar.getType());
            Graphics2D g2d = clone.createGraphics();
            g2d.drawImage(avatar, 0, 0, null);
            g2d.dispose();
            return clone;
        }
        return null;
    }

    @Override
    public String getAvatarURL() {
        return this.avatarURL;
    }

    @Override
    public String getMood() {
        return this.mood;
    }

    @Override
    public String getRichMood() {
        return this.richMood;
    }

    @Override
    public String getCountry() {
        return this.country;
    }

    @Override
    public String getCity() {
        return this.city;
    }

    @Override
    public boolean isAuthorized() {
        return this.isAuthorized;
    }

    @Override
    public void authorize() throws ConnectionException {
        Endpoints.AUTHORIZE_CONTACT.open(skype, this.username).expect(200, "While authorizing contact").put();
        updateContactInfo();
    }

    @Override
    public void unauthorize() throws ConnectionException {
        if (isAuthorized) {
            Endpoints.UNAUTHORIZE_CONTACT_SELF
                    .open(skype, this.username)
                    .expect(200, "While unauthorizing contact")
                    .put();
        } else {
            Endpoints.DECLINE_CONTACT_REQUEST
                    .open(skype, this.username)
                    .expect(201, "While unauthorizing contact")
                    .put();
        }
        updateContactInfo();
    }

    @Override
    public void sendRequest(String message) throws ConnectionException, NoSuchContactException {
        Endpoints.AUTHORIZATION_REQUEST
                .open(skype, this.username)
                .on(404, (connection) -> {
                    throw new NoSuchContactException();
                })
                .expect(201, "While sending request")
                .expect(200, "While sending request")
                .put("greeting=" + Encoder.encode(message));
        updateContactInfo();
    }

    @Override
    public boolean isBlocked() {
        return this.isBlocked;
    }

    @Override
    public void block(boolean reportAbuse) throws ConnectionException {
        Endpoints.BLOCK_CONTACT
                .open(skype, this.username)
                .expect(201, "While unblocking contact")
                .put("reporterIp=127.0.0.1&uiVersion=" + Skype.VERSION + (reportAbuse ? "&reportAbuse=1" : ""));
        updateContactInfo();
    }

    @Override
    public void unblock() throws ConnectionException {
        Endpoints.UNBLOCK_CONTACT.open(skype, this.username).expect(201, "While unblocking contact").put();
        updateContactInfo();
    }

    @Override
    public boolean isPhone() {
        return this.isPhone;
    }

    @Override
    public Chat getPrivateConversation() throws ConnectionException, ChatNotFoundException {
        return skype.getOrLoadChat("8:" + this.username);
    }

    public void update(JsonObject contact, String contactStatus) throws ConnectionException {
        this.username = contact.get("person_id").asString();
        this.isAuthorized = contact.get("authorized").asBoolean();
        this.isBlocked = contact.get("blocked").asBoolean();
        this.displayName = Utils.getString(contact, "display_name");
        this.avatarURL = Utils.getString(contact, "avatar_url");
        this.mood = Utils.getString(contact, "mood");
        this.type = Utils.getString(contact, "type");
        this.authCertificate = Utils.getString(contact, "auth_certificate");
        this.status = contactStatus;
        if (contact.get("locations") != null) {
            JsonObject locations = contact.get("locations").asArray().get(0).asObject();
            this.country = locations.get("country") == null ? null : locations.get("country").asString();
            this.city = locations.get("city") == null ? null : locations.get("city").asString();
        }
        JsonObject profile = (JsonObject) contact.get("profile");
        updateProfile(profile);
    }

    public void updateProfile(JsonObject profile) {
        JsonObject nameDetails = (JsonObject) profile.get("name");
        JsonArray phonesArray = (JsonArray) profile.get("phones");
        String phonesStr = "";
        if (phonesArray != null){
            for (int i = 0; i < phonesArray.size(); i++){
                JsonObject eachPhone = (JsonObject) phonesArray.get(i);
                if (i > 0) {
                    phonesStr += ", ";
                }
                phonesStr += eachPhone.get("number").asString();
            }
        }
        if (nameDetails != null){
            this.firstName = Utils.getString(nameDetails, "first");
            this.lastName = Utils.getString(nameDetails, "surname");    
        } else {
            this.firstName = Utils.getString(profile, "firstname");
            this.lastName = Utils.getString(profile, "lastname");
        }
        this.birthday = Utils.getString(profile, "birthday");
        this.phones = phonesStr;

        if (profile.get("gender") != null) {
            if (profile.get("gender").isNumber()) {
                this.gender = String.valueOf(profile.get("gender").asInt());
            } else if (profile.get("gender").isString()) {
                this.gender = profile.get("gender").asString();
            } else if (profile.get("gender").isBoolean()) {
                this.gender = profile.get("gender").asBoolean() ? "1" : "0";
            } else {
                this.gender = profile.get("gender").toString();
            }
        }

        this.language = Utils.getString(profile, "language");
        this.avatarURL = Utils.getString(profile, "avatar_url");

        if (this.displayName == null)
            this.displayName = Utils.getString(profile, "displayname");
        if (this.mood == null)
            this.mood = Utils.getString(profile, "mood");
        if (this.richMood == null)
            this.richMood = Utils.getString(profile, "richMood");
        if (this.country == null)
            this.country = Utils.getString(profile, "country");
        if (this.city == null)
            this.city = Utils.getString(profile, "city");

        if (this.displayName == null) {
            if (this.firstName != null) {
                this.displayName = this.firstName;
                if (this.lastName != null) {
                    this.displayName = this.displayName + " " + this.lastName;
                }
            } else if (this.lastName != null) {
                this.displayName = this.lastName;
            } else {
                this.displayName = this.username;
            }
        }
    }
}
