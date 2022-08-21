//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package software.amazonaws.sample.resolver.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonConverter {

    Gson gson = new GsonBuilder().create();

    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public String toJson(Object src) {
        return gson.toJson(src);
    }

}
