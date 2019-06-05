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

package com.samczsun.skype4j.internal.participants;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.samczsun.skype4j.internal.Endpoints;
import com.samczsun.skype4j.internal.SkypeImpl;
import com.samczsun.skype4j.internal.chat.ChatImpl;
import com.samczsun.skype4j.internal.participants.info.ContactImpl;
import com.samczsun.skype4j.participants.info.Contact;
import com.samczsun.skype4j.participants.User;


public class UserImpl extends ParticipantImpl implements User {
    private Contact contactRep;

    public UserImpl(SkypeImpl skype, ChatImpl chat, String id) throws Exception {
        super(skype, chat, id);
        contactRep = new ContactImpl(skype, "8:"+getId().substring(2), updateDisplayName());
    }

    @Override
    public String getUsername() {
        return getId().substring(2);
    }

    @Override
    public String getDisplayName() {
        if (contactRep == null) return null;
        return contactRep.getDisplayName();
    }
    
    public JsonObject updateDisplayName() throws Exception {
        JsonArray usernames = new JsonArray();
        usernames.add(this.getUsername());

        JsonArray info = Endpoints.PROFILE_INFO_CHAT_GROUP
                .open(getClient())
                .expect(200, "While getting contact info")
                .as(JsonArray.class)
                .post(new JsonObject()
                        .add("usernames", usernames)
                );
        JsonObject displayNameObj = info.get(0).asObject();
        return displayNameObj;
    }

    @Override
    public Contact getContact() {
        return this.contactRep;
    }

    public void setInfo(Contact contact) {
        this.contactRep = contact;
    }
}
