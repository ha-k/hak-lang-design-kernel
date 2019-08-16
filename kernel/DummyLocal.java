//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 12:05:53 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;

/**
 * This is a <a href="Local.html"><tt>Local</tt></a> that does not actually belong
 * within its scope and need never be sort-sanitized nor compiled. It is needed and
 * used only for processing slicing expressions in a <a href="Homomorphism.html">
 * <tt>Homomorphism</tt></a>.
 */
public class DummyLocal extends Local
{
  public DummyLocal (Parameter parameter)
    {
      super(parameter);
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      parameter().setCheckedType();
    }

  /**
   * No-op...
   */
  public final Expression sanitizeNames (ParameterStack parameters)
    {
      return this;
    }

  /**
   * No-op...
   */
  public final void sanitizeSorts (Enclosure enclosure)
    {
    }

  /**
   * No-op...
   */
  public final Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                        int intDepth, int realDepth, int objectDepth)
    {
      return this;
    }

  /**
   * A DummyLocal never gets to invoke this.
   */
  public final void compile (Compiler compiler)
    {   
      throw new UnsupportedOperationException("method compile may not be called on a DummyLocal! ("
                                              +this+")");
    }

}
