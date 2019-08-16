//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

import hlt.language.util.Queue;
import hlt.language.util.ArrayList;

import java.util.HashSet;

/**
 * This is the class defining a compiler object. Such an object serves as
 * the common compilation context shared by an <a
 * href="Expression.html"><tt>Expression</tt></a> and the subexpressions
 * comprising it. Each type of expression representing a syntactic construct
 * of the kernel language defines a <tt>compile(Compiler)</tt> method that
 * specifies the way the construct is to be compiled in the context of a
 * given compiler.  Such a compiler object consists of attributes and
 * methods for generating straight-line code which consists of the sequence
 * of <a href="../base/Instruction.html"><tt>Instruction</tt></a>s corresponding
 * to a top-level expression and its subexpressions. Upon completion of the compilation
 * of a top-level expression, a resulting code array is extracted from the
 * sequence of instructions, which may then be executed in the context of a
 * <a href="../backend/Runtime.html"><tt>Runtime</tt></a> object, or, in the case of
 * a <a href="Definition.html"><tt>Definition</tt></a>, be saved in the
 * code array in the <tt>Definition</tt>'s <tt>_codeEntry</tt> field - a <a
 * href="../types/DefinedEntry.html"><tt>DefinedEntry</tt></a> object encapsulating
 * its code entry point (which may then be used to access the defined
 * symbol's code for execution).
 */
public class Compiler
{
  /**
   * Creates and returns a new Compiler.
   */
  public Compiler ()
    {
    }

  /**
   * This queue records pairs consisting of the <a
   * href="../base/PushScope.html"><tt>PushScope</tt></a> instruction generated
   * by compiling an <a href="Scope.html"><tt>Scope</tt></a>, the body
   * expressions of the compiled scope, and the address of the
   * <tt>PushScope</tt> instruction.  It is used for backpatching the addresses
   * of <tt>PushScope</tt> instructions corresponding to each nested scope.
   * These addresses are only known when the body expression is dequeued from this
   * queue to be compiled in its turn by the <tt>_backpatch</tt> method. 
   *
   * <p>
   *
   * <b>NB:</b> One could as well use a stack rather than a queue for this
   * structure.  I (hlt) prefer a queue. It is better in that it enables
   * generating instructions in a FIFO order. This locates each
   * <tt>PushScope</tt> instruction closer to the address it
   * references.
   */
  private Queue _pushScopeQueue = new Queue();

  /**
   * This structure records the <a href="../base/Instruction.html"><tt>Instruction</tt></a>
   * sequence being generated as a compilation is taking place.
   */
  private ArrayList _codeList = new ArrayList();

  /**
   * This structure records the the contents of the <tt>_codeList</tt>
   * sequence at the end of the compilation of an an outermost
   * expression. It is extracted from <tt>_codeList</tt> sequence used to set
   * the reference code field of <tt>PushScope</tt> instructions whose
   * address refer to this sequence and, if the case applies, the code entry
   * field of a definition.
   */
  private Instruction[] _code;

  /**
   * This field contains the next available index in <tt>_codeList</tt>
   * to use as the address of the next instruction that may be
   * generated. It is generally, but not necessarily, equal to
   * <tt>_codeList.size()</tt>: it may be less when code optimization
   * deletes the latest instructions if they were unnecessarily generated.
   */
  private int _codeEnd = 0;

  /**
   * When compiling a definition, this is set to the
   * <a href="../types/DefinedEntry.html"><tt>DefinedEntry</tt></a> of the
   * symbol being defined.
   */
  private DefinedEntry _codeEntry;

  /**
   * This is a flag used for pragmas requesting to show this compiler's
   * code at the end of the compilation of an outermost expression.
   */
  private boolean _showCode = false;

  /**
   * Contains the addresses of instructions that are the targets of branching
   * instructions.
   */
  private HashSet _targets = new HashSet();

  /**
   * When this flag is set to <tt>true</tt>, last call optimization is made effective.
   */
  public static boolean LCO_IS_EFFECTIVE = false;
    
  /**
   * Resets this compiler's attributes to enable a new compilation.
   */
  private final void _reset ()
    {
      _codeList.clear();
      _targets.clear();
      _code = null;   
      _codeEnd = 0;   
    }

  /**
   * Returns the extracted code array of this compiler's latest complete
   * compilation.
   */
  public final Instruction[] code ()
    {
      return _code;
    }

  /**
   * Returns the next available index in <tt>_codeList</tt>
   * to use as the address of the next instruction.
   */
  public final int nextCodeAddress ()
    {
      return _codeEnd;
    }

  /**
   * Returns the next available index in <tt>_codeList</tt>
   * marking as a target address.
   */
  public final int targetAddress ()
    {
      _targets.add(new Integer(_codeEnd));
      return _codeEnd;
    }

  /**
   * Sets the <tt>_codeEntry</tt> of this compiler to the specified
   * <a href="../types/DefinedEntry.html"><tt>DefinedEntry</tt></a>. This is
   * called by the <tt>compile</tt> method of a
   * <a href="Definition.html"><tt>Definition</tt></a>.
   */
  public final void setCodeEntry (DefinedEntry entry)
    {
      _codeEntry = entry;
    }

  /**
   * When compiling a definition, this returns the
   * <a href="../types/DefinedEntry.html"><tt>DefinedEntry</tt></a> of the
   * symbol being defined; <tt>null</tt> otherwise.
   */
  public final DefinedEntry codeEntry ()
    {
      return _codeEntry;
    }

  /**
   * Return <tt>true</tt> iff this is compiling a definition.
   */
  public final boolean isCompilingDefinition ()
    {
      return (_codeEntry != null);
    }

  /**
   * This sets the code array of the current <a href="../types/DefinedEntry.html">
   * <tt>DefinedEntry</tt></a> and initiates a propagation in its unsafe entry
   * dependency graph to release those code entries that were unsafe due to a
   * reference to the newly defined entry.
   */
  private final void _releaseCodeEntry ()
    {
      _codeEntry.setCode(code());
      if (_codeEntry.isField())
        _codeEntry.setInitCode();
      _codeEntry.releaseUnsafeEntries();
    }

  /**
   * Compiles the specified expression in the context of this compiler. This
   * method is (must be, in fact) the topmost entry point when compiling a
   * top-level (<i>e.g.</i>, non-nested) expression as it unnests all inner scopes
   * into straightline code (which is what the <tt>_backpatch</tt> method does)
   * and manages code dependencies for incremental compiling of forward
   * definitions (which is what the <tt>releaseCodeEntry</tt> method does).
   */
  public final void compile (Expression exp)
    {
      _reset();

      exp.compile(this);
      _backpatch();

      if (isCompilingDefinition())
        _releaseCodeEntry();

      if (_showCode) showCode();

      _codeEntry = null;
    }

  /**
   * This method ends a code sequence and adds to it the code of all the nested
   * scopes encountered thus far in this compilation. The expressions
   * corresponding to the bodies of these inner scopes have been recorded in the
   * queue <tt>_pushScopeQueue</tt>. Each of these expression's corresponding
   * <tt>PushScope</tt> instruction must be made to refer to the entry code
   * address of their to-be-compiled code.  This method also enables Last Call
   * Optimization for scope applications ending a scope's code, otherwise it
   * generates a return instruction of the appropriate runtime sort depending on
   * that of the scope's body. Finally, it extracts the code sequence
   * corresponding to the complete (<i>i.e.</i>, outermost) expression into an
   * array of instructions setting the reference code of those
   * <tt>PushScope</tt>s that need it to the extracted code array.
   */
  private final Compiler _backpatch ()
    {
      generate(Instruction.END);

      while (!_pushScopeQueue.isEmpty())
        {
          ScopeBody cb = (ScopeBody)_pushScopeQueue.pop();
          cb.pushScope.setAddress(nextCodeAddress());
          cb.body.compile(this);

          Instruction previous = lastInstruction();

          if (LCO_IS_EFFECTIVE && !_isTarget(_codeEnd)
              && !cb.pushScope.isExitable()
              && previous instanceof Enter)
            {
              _codeEnd--;
              generate(((Enter)previous).setLCO());
              generate(Instruction.END);
            }
          else
            switch (cb.body.boxSort())
              {
              case Type.VOID_SORT:
                generate(Instruction.RETURN_VOID);
                break;
              case Type.INT_SORT:
                generate(Instruction.RETURN_I);
                break;
              case Type.REAL_SORT:
                generate(Instruction.RETURN_R);
                break;
              default:
                generate(Instruction.RETURN_O);
                break;
              }
        }

      _extractCode();
      return this;
    }

  /**
   * Extracts the current compiled code from the <tt>ArrayList _codeList</tt> into an
   * array of <a href="../base/Instruction.html"><tt>Instruction</tt></a> and sets this
   * compiler's <tt>_code</tt> to the resulting array. Along the way, <tt>PushScope</tt>
   * instructions missing a reference code are given one (the extracted code array).
   */
  private final void _extractCode ()
    {
      _code = new Instruction[_codeEnd];

      for (int i=0; i<_code.length; i++)
        {
          _code[i] = (Instruction)_codeList.get(i);
          if (_code[i] instanceof PushScope)
            _code[i] = ((PushScope)_code[i]).setReferenceCode(_code);
        }
    }

  public final Instruction lastInstruction ()
    {
      return (Instruction)_codeList.get(_codeEnd-1);
    }

  public final void toggleShowCode ()
    {
      _showCode = !_showCode;
    }

  public final boolean isShowingCode ()
    {
      return _showCode;
    }

  public final void showCode ()
    {
      CodeEntry.showCode(_codeEntry,_code);
    }

  public final Instruction generate (Instruction inst)
    {
      if (inst == Instruction.NO_OP)
        return inst;

      inst = _checkSetInstruction(inst);
      if (_codeEnd < _codeList.size())
        _codeList.set(_codeEnd,inst);
      else
        _codeList.add(inst);
      _codeEnd++;

      return inst;
    }

  private final Instruction _checkSetInstruction (Instruction inst)
    {
      if (_codeEnd == 0) return inst;

      Instruction last = lastInstruction();

      if (!(inst instanceof SetElementInstruction && last instanceof BoxingUnboxingInstruction))
        return inst;

      SetElementInstruction i = (SetElementInstruction)inst;

      switch (i.id())
        {
        case SetElementInstruction.SET_ADD_O_ID:
        case SetElementInstruction.SET_RMV_O_ID:
        case SetElementInstruction.BELONGS_O_ID:
          if (last == Instruction.I_TO_O)
            {
              _codeEnd--;
              switch (i.id())
                {
                case SetElementInstruction.SET_ADD_O_ID:
                  return Instruction.SET_ADD_I;
                case SetElementInstruction.SET_RMV_O_ID:
                  return Instruction.SET_RMV_I;
                case SetElementInstruction.BELONGS_O_ID:
                  return Instruction.BELONGS_I;
                }
            }
          else
            if (last == Instruction.R_TO_O)
              {
                _codeEnd--;
                switch (i.id())
                  {
                  case SetElementInstruction.SET_ADD_O_ID:
                    return Instruction.SET_ADD_R;
                  case SetElementInstruction.SET_RMV_O_ID:
                    return Instruction.SET_RMV_R;
                  case SetElementInstruction.BELONGS_O_ID:
                    return Instruction.BELONGS_R;
                  }
              }
        default:
          if (last == Instruction.O_TO_I || last == Instruction.O_TO_R)
            {
              _codeEnd--;
              switch (i.id())
                {
                case SetElementInstruction.SET_ADD_I_ID:
                case SetElementInstruction.SET_ADD_R_ID:
                  return Instruction.SET_ADD_O;
                case SetElementInstruction.SET_RMV_I_ID:
                case SetElementInstruction.SET_RMV_R_ID:
                  return Instruction.SET_RMV_O;
                case SetElementInstruction.BELONGS_I_ID:
                case SetElementInstruction.BELONGS_R_ID:
                  return Instruction.BELONGS_O;
                }
            }
        }
      
      return inst;
    }        

  public final Instruction generate (PushScope pushScope, Expression body)
    {
      _pushScopeQueue.push(new ScopeBody(pushScope,body,_codeEnd));
      return generate(pushScope); // NB: its reference code array will be set in _backpatch
    }

  public final void inline (Instruction[] code)
    {
      for (int i=0; i<code.length; i++)
        {
          if (code[i] == Instruction.END)
            return;

          if (code[i] instanceof Relocatable)
            {
              Relocatable relocatable = (Relocatable)code[i];
              generate(relocatable.relocate(_codeEnd - i + relocatable.address()));
            }
          else
            generate(code[i]);
        }
    }  

  private final boolean _isTarget (int address)
    {
      return _targets.contains(new Integer(address));
    }

  private final void _skipPush (Instruction pop)
    {
      if (!_isTarget(_codeEnd))
        _codeEnd--;
      else
        {
          generate(pop);
          _codeList.set(_codeEnd-2,new Jump(targetAddress()));
        }
    }

  public final void generateStackPop (byte sort)
    {
      Instruction previous = lastInstruction();

      switch (sort)
        {
        case Type.VOID_SORT:
          return;
        case Type.INT_SORT:
          if (previous instanceof PushInt)
            _skipPush(Instruction.POP_I);
          else
            generate(Instruction.POP_I);
          return;
        case Type.REAL_SORT:
          if (previous instanceof PushReal)
            _skipPush(Instruction.POP_R);
          else
            generate(Instruction.POP_R);
          return;
        default:
          if (previous instanceof PushObject)
            _skipPush(Instruction.POP_O);
          else
            generate(Instruction.POP_O);
          return;
        }
    }

  public final void generateWrapper (byte sort)
    {
      Instruction previous = lastInstruction();

      switch (sort)
        {
        case Type.INT_SORT:
          if (previous == Instruction.O_TO_I)
            _codeEnd--;
          else
            generate(Instruction.I_TO_O);
          return;
        case Type.REAL_SORT:
          if (previous == Instruction.O_TO_R)
            _codeEnd--;
          else
            generate(Instruction.R_TO_O);
          return;
        }
    }

  public final void generateUnwrapper (byte sort)
    {
      Instruction previous = lastInstruction();

      switch (sort)
        {
        case Type.INT_SORT:
          if (previous == Instruction.I_TO_O)
            _codeEnd--;
          else
            generate(Instruction.O_TO_I);
          return;
        case Type.REAL_SORT:
          if (previous == Instruction.R_TO_O)
            _codeEnd--;
          else
            generate(Instruction.O_TO_R);
          return;
        }
    }

  private static class ScopeBody
    {
      PushScope pushScope;
      Expression body;

      ScopeBody (PushScope pushScope, Expression body, int address)
        {
          this.pushScope = pushScope;
          this.body = body;
        }
    }
}

