package org.starexec.data.to.tuples;

/**
 * Created by agieg on 11/17/2016.
 */

import org.starexec.data.to.enums.ConfigXmlAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by agieg on 11/17/2016.
 */
public class ConfigAttrMapPair {
    public final ConfigXmlAttribute attribute;
    public final Map<String, Integer> configNameToId;

    public ConfigAttrMapPair(ConfigXmlAttribute attribute) {
        this.attribute = attribute;
        this.configNameToId = new HashMap<>();
    }
}
