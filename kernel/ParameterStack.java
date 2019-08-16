//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.util.Stack;

public class ParameterStack extends Stack
{
  public final Parameter getLocalParameter (String name)
    {
      for (int i = size(); i-->0;)
        {
          Parameter parameter = (Parameter)get(i);
          if (parameter.name() == name)
            return parameter;
        }

      return null;
    }

  public final String toString ()
    {
      StringBuilder buf = new StringBuilder("[");

      for (int i = size()-1; i>=0; i--)
        buf.append(get(i)+(i==0?"":", "));

      buf.append("]");

      return buf.toString();
    }
}
