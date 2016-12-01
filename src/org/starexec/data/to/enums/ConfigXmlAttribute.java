package org.starexec.data.to.enums;

import org.yaml.snakeyaml.events.Event;

/**
 * Created by agieg on 11/17/2016.
 */
public enum ConfigXmlAttribute {
    NAME("config-name"),
    ID("config-id");

    public final String attribute;
    ConfigXmlAttribute(String configAttribute) {
        this.attribute = configAttribute;
    }
}
