//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Tue Mar 26 06:15:33 2019 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;

/**
 * This class factors out implementation that's common to all expressions.
 * @see Expression
 */
public abstract class ProtoExpression extends Expression
{
  protected Type _type = new TypeParameter();
  protected Type _checkedType;

  public final Type type ()
    {
      return _type.value();
    }

  public final void setType (Type type)
    {
      if (type != null) _type = type;
    }

  public final Type typeRef ()
    {
      return _type;
    }

  public final Type checkedType ()
    {
      return _checkedType;
    }

  /**
   * <b>NB:</b> this is not <tt>final</tt> because it is overridden in
   * <a href="Abstraction.html"><tt>Abstraction</tt></a> and
   * <a href="Global.html"> <tt>Global</tt></a>.
   */
  public void setCheckedType (Type type)
    {
      _checkedType = type;
    }

}
