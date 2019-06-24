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

package com.samczsun.skype4j.internal.chat;

import com.samczsun.skype4j.chat.IndividualChat;
import com.samczsun.skype4j.exceptions.ChatNotFoundException;
import com.samczsun.skype4j.exceptions.ConnectionException;
import com.samczsun.skype4j.internal.Factory;
import com.samczsun.skype4j.internal.SkypeImpl;
import com.samczsun.skype4j.internal.participants.BotImpl;
import com.samczsun.skype4j.internal.participants.UserImpl;
import com.samczsun.skype4j.participants.User;

import java.util.HashMap;
import java.util.Map;

public class ChatIndividual extends ChatImpl implements IndividualChat {
    private User partner;

    public ChatIndividual(SkypeImpl skype, String identity) throws ConnectionException, ChatNotFoundException {
        super(skype, identity);
    }

    @Override
    public void load() throws ConnectionException, Exception {
        UserImpl partner = (UserImpl) Factory.createParticipant(getClient(), this, getIdentity());
        this.users.put(partner.getId().toLowerCase(), partner);

        UserImpl me = (UserImpl) Factory.createParticipant(getClient(), this, getClient().getId());
        this.users.put(me.getId().toLowerCase(), me);

        this.partner = partner;
    }

    @Override
    public void addUser(String username) {
        throw new IllegalArgumentException("Cannot add user to individual chat");
    }

    @Override
    public void removeUser(String username) {
        throw new IllegalArgumentException("Cannot remove user from individual chat");
    }

    @Override
    public User getPartner() {
        return partner;
    }
}
