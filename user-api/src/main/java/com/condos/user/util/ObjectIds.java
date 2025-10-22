package com.condos.user.util;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ObjectIds {
    private ObjectIds() {}
    public static ObjectId parse(String value) {
        try { return new ObjectId(value); }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid orgId");
        }
    }
}