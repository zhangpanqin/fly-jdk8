package java.net;

import java.io.UnsupportedEncodingException;

/**
 * Utility class for HTML form decoding. This class contains static methods
 * for decoding a String from the <CODE>application/x-www-form-urlencoded</CODE>
 * MIME format.
 * <p>
 * The conversion process is the reverse of that used by the URLEncoder class. It is assumed
 * that all characters in the encoded string are one of the following:
 * &quot;{@code a}&quot; through &quot;{@code z}&quot;,
 * &quot;{@code A}&quot; through &quot;{@code Z}&quot;,
 * &quot;{@code 0}&quot; through &quot;{@code 9}&quot;, and
 * &quot;{@code -}&quot;, &quot;{@code _}&quot;,
 * &quot;{@code .}&quot;, and &quot;{@code *}&quot;. The
 * character &quot;{@code %}&quot; is allowed but is interpreted
 * as the start of a special escaped sequence.
 * <p>
 * The following rules are applied in the conversion:
 *
 * <ul>
 * <li>The alphanumeric characters &quot;{@code a}&quot; through
 *     &quot;{@code z}&quot;, &quot;{@code A}&quot; through
 *     &quot;{@code Z}&quot; and &quot;{@code 0}&quot;
 *     through &quot;{@code 9}&quot; remain the same.
 * <li>The special characters &quot;{@code .}&quot;,
 *     &quot;{@code -}&quot;, &quot;{@code *}&quot;, and
 *     &quot;{@code _}&quot; remain the same.
 * <li>The plus sign &quot;{@code +}&quot; is converted into a
 *     space character &quot; &nbsp; &quot; .
 * <li>A sequence of the form "<i>{@code %xy}</i>" will be
 *     treated as representing a byte where <i>xy</i> is the two-digit
 *     hexadecimal representation of the 8 bits. Then, all substrings
 *     that contain one or more of these byte sequences consecutively
 *     will be replaced by the character(s) whose encoding would result
 *     in those consecutive bytes.
 *     The encoding scheme used to decode these characters may be specified,
 *     or if unspecified, the default encoding of the platform will be used.
 * </ul>
 * <p>
 * There are two possible ways in which this decoder could deal with
 * illegal strings.  It could either leave illegal characters alone or
 * it could throw an {@link IllegalArgumentException}.
 * Which approach the decoder takes is left to the
 * implementation.
 *
 * @author Mark Chamness
 * @author Michael McCloskey
 * @since 1.2
 */

public class URLDecoder {

    // The platform default encoding
    static String dfltEncName = URLEncoder.dfltEncName;


    /**
     * 对 s 使用 enc 解码
     */
    public static String decode(String s, String enc) throws UnsupportedEncodingException {

        boolean needToChange = false;
        int numChars = s.length();
        StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        if (enc.length() == 0) {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    i++;
                    needToChange = true;
                    break;
                case '%':
                    try {
                        if (bytes == null) {
                            bytes = new byte[(numChars - i) / 3];
                        }
                        int pos = 0;

                        while (((i + 2) < numChars) &&
                                (c == '%')) {
                            int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            if (v < 0) {
                                throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - negative value");
                            }
                            bytes[pos++] = (byte) v;
                            i += 3;
                            if (i < numChars) {
                                c = s.charAt(i);
                            }
                        }

                        if ((i < numChars) && (c == '%')) {
                            throw new IllegalArgumentException(
                                    "URLDecoder: Incomplete trailing escape (%) pattern");
                        }

                        sb.append(new String(bytes, 0, pos, enc));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "URLDecoder: Illegal hex characters in escape (%) pattern - "
                                        + e.getMessage());
                    }
                    needToChange = true;
                    break;
                default:
                    sb.append(c);
                    i++;
                    break;
            }
        }

        return (needToChange ? sb.toString() : s);
    }
}
