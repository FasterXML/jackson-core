/**
 * References:
 * <dl>
 *     <dt>This class has been derived from "FastDoubleParser".</dt>
 *     <dd>Copyright (c) Werner Randelshofer. Apache 2.0 License.
 *         <a href="https://github.com/wrandelshofer/FastDoubleParser">github.com</a>.</dd>
 * </dl>
 */

package tools.jackson.core.io.doubleparser;

public class Strings {
    private Strings(){}
    public static String repeat(String s,int count){
        StringBuilder buf = new StringBuilder(s.length() * count);
        for (int i=0;i<count;i++){
            buf.append(s);
        }
        return buf.toString();
    }

}
