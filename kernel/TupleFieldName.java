//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.util.LexComparableString;

/**
 * This class extends <a href="../../util/LexComparableString.html"><tt>LexComparableString</tt></a>s
 * to represent named tuple field names.
 */

public class TupleFieldName extends LexComparableString
{
  private int _index;

  public TupleFieldName (String name, int index)
    {
      super(name);
      _index = index;
    }

  public final String name ()
    {
      return string();
    }

  public final int index ()
    {
      return _index;
    }
}

