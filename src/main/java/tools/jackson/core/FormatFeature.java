package tools.jackson.core;

import tools.jackson.core.util.JacksonFeature;

/**
 * Marker interface that is to be implemented by data format - specific features.
 * Interface used since Java Enums can not extend classes or other Enums, but
 * they can implement interfaces; and as such we may be able to use limited
 * amount of generic functionality.
 *<p>
 * At this point this type is more of an extra marker feature, as its core API is now
 * defined in more general {@link JacksonFeature}.
 */
public interface FormatFeature
    extends JacksonFeature
{
    // Same API as JacksonFeature, no additions
}
