package org.openelisglobal.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class IdValueNameKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String value;
    private String nameKey;

    public IdValueNameKey(String id, String value, String nameKey) {
        this.id = id;
        this.value = value;
        this.nameKey = nameKey;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    @JsonProperty("displayKey")
    public String getNameKey() {
        return nameKey;
    }
}
