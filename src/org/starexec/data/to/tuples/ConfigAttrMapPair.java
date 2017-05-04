package org.starexec.data.to.tuples;
import org.starexec.data.to.enums.ConfigXmlAttribute;

import java.util.HashMap;
import java.util.Map;

// Tuple that contains a ConfigXmlAttribute and map from config names to ids.
public class ConfigAttrMapPair {
    public final ConfigXmlAttribute attribute;
    public final Map<String, Integer> configNameToId;

    public ConfigAttrMapPair(ConfigXmlAttribute attribute) {
        this.attribute = attribute;
        this.configNameToId = new HashMap<>();
    }
}
