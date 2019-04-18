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

package com.samczsun.skype4j.formatting.lang;

import com.samczsun.skype4j.formatting.IEmoticon;
import com.samczsun.skype4j.formatting.lang.en.Emoticon;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum DefaultEmoticon implements IEmoticon {

    ;

    private final String id;
    private final String etag;
    private String desc;
    private List<String> shortcuts;

    DefaultEmoticon(String id, String etag, String desc, String... shortcuts) {
        this.id = id;
        this.etag = etag;
        this.desc = desc;
        this.shortcuts = Arrays.asList(shortcuts);
    }

    public String getId() {
        return this.id;
    }

    public List<String> getShortcuts() {
        return Collections.unmodifiableList(this.shortcuts);
    }

    public String getEtag() {
        return this.etag;
    }

    public String getDescription() {
        return this.desc;
    }
    
    private static Map<String, Emoticon> dictionary;
    private static int longestEmoji = -1;

    public static Map<String, Emoticon> getDictionary() {
        if (dictionary == null) { // This is for performance, trust me
            initDictionary();
}
        return dictionary;
    }

    public static int getLongestEmoji() {
        if (longestEmoji == -1) {
            for (String s : getDictionary().keySet()) {
                if (s.charAt(0) != '(') {
                    if (s.length() > longestEmoji) {
                        longestEmoji = s.length();
                    }
                }
            }
        }
        return longestEmoji;
    }

    private static void initDictionary() {
        if (dictionary == null) {
            dictionary = new HashMap<>();
            for (Emoticon emoticon : Emoticon.values()) {
                emoticon.getShortcuts().forEach(str -> {
                    dictionary.put(str, emoticon);
                    dictionary.put(str.replace("<", "&lt;").replace(">", "&gt;"), emoticon);
                });
            }
        }
    }
}
