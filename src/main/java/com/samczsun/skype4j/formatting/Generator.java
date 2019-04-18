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

package com.samczsun.skype4j.formatting;

import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.SkypeBuilder;
import com.samczsun.skype4j.events.EventHandler;
import com.samczsun.skype4j.events.Listener;
import com.samczsun.skype4j.events.chat.message.MessageReceivedEvent;
import com.samczsun.skype4j.events.chat.message.MessageSentEvent;
import com.samczsun.skype4j.participants.Participant;

import java.util.Collection;

public class Generator {
    private static final String LANG = "en"; //Change to whatever lang you want
    private static final String VERSION = "908_1.20.0.98"; //Magic number

//    public static void main(String[] args) throws Exception {
//       
//        System.out.println("1");
////        Skype skype = new SkypeBuilder("haroflow", "14wolforah91").withAllResources().build();
////        Skype skype = new SkypeBuilder("skype2@setinet.com.br", "Sk2Hy9px").withAllResources().build();
////        Skype skype = new SkypeBuilder("47988551018", "13256seti").withAllResources().build();
//        Skype skype = new SkypeBuilder("setinet_conta_teste1@outlook.com", "13256seti").withAllResources().build();
//        System.out.println("2");
//        skype.login();
//         System.out.println("3");
//        skype.getEventDispatcher().registerListener(new Listener() {
//          @EventHandler
//          public void onMessageSent(MessageSentEvent e) {
//            System.out.println("Sent message: " + e.getMessage().getContent());
//            System.out.println(e.getChat().getIdentity());
//            if (e.getChat().getIdentity().startsWith("8")){
//                System.out.println("individual");
//            } else {
//                System.out.println("grupo");
//            }
//            //+ e.getMessage().getClientId() + e.getChat().getParticipant(e.getMessage().getSender().getId()).getClient().getUsername());
//            System.out.println("5");
//          }
//          @EventHandler
//          public void onMessageReceived(MessageReceivedEvent e) {
//            System.out.println("Got message: " + e.getMessage().getContent());
//            Collection<Participant> users;
//            users = e.getChat().getAllParticipants();
//            for (Participant each : users) {
//                String teste = each.getDisplayName();
//                String teste2 = each.getId();
//                System.out.println(teste);
//                System.out.println(teste2);
//            }
////            System.out.println(e.getChat().getParticipant("8:live:skype2_1132").getDisplayName());
//            System.out.println(e.getChat().getIdentity());
//            if (e.getChat().getIdentity().startsWith("8")){
//                System.out.println("individual");
//            } else {
//                System.out.println("grupo");
//            }
//            //+ e.getMessage().getClientId() + e.getMessage().getId() + e.getChat().getClient().getLiveUsername()
//            //+ e.getChat().getParticipant(e.getChat().getIdentity()).getClient().getUsername()
//            System.out.println("4");
//          }
//        });
//        skype.subscribe();
////        skype.setVisibility(Visibility.DO_NOT_DISTURB);
//        // Do stuff
//        //skype.logout();
//    }
}
