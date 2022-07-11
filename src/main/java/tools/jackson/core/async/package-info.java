/**
 * Package that contains abstractions needed to support optional
 * non-blocking decoding (parsing) functionality.
 * Although parsers are constructed normally via
 * {@link tools.jackson.core.json.JsonFactory}
 * (and are, in fact, sub-types of {@link tools.jackson.core.JsonParser}),
 * the way input is provided differs.
 *
 * @since 2.9
 */

package tools.jackson.core.async;
