package net.miginfocom;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Arrays;

/*
 * License (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 1.0
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 2006-sep-08
 */

/**
 * A constraint that holds the column <b>or</b> row constraints for the grid. It also holds the gaps between the rows and columns.
 * <p>
 * This class is a holder and builder for a number of {@link DimConstraint}s.
 * <p>
 * For a more thorough explanation of what these constraints do, and how to build the constraints, see the White Paper or Cheat Sheet at www.migcomponents.com.
 * <p>
 * Note that there are two way to build this constraint. Through String (e.g. <code>"[100]3[200,fill]"</code> or through API (E.g.
 * <code>new AxisConstraint().size("100").gap("3").size("200").fill()</code>.
 */
final class AC implements Externalizable {
    private final ArrayList<DimConstraint> cList = new ArrayList<>(8);

    private transient int curIx = 0;

    /**
     * Constructor. Creates an instance that can be configured manually. Will be initialized with a default
     * {@link DimConstraint}.
     */
    public AC() {
        cList.add(new DimConstraint());
    }

    /**
     * Property. The different {@link DimConstraint}s that this object consists of.
     * These <code><DimConstraints/code> contains all information in this class.
     * <p>
     * Yes, we are embarrassingly aware that the method is misspelled.
     *
     * @return The different {@link DimConstraint}s that this object consists of. A new list and
     * never <code>null</code>.
     */
    DimConstraint[] getConstaints() {
        return cList.toArray(new DimConstraint[cList.size()]);
    }

    /**
     * Sets the different {@link DimConstraint}s that this object should consists of.
     * <p>
     * Yes, we are embarrassingly aware that the method is misspelled.
     *
     * @param constr The different {@link DimConstraint}s that this object consists of. The list
     *               will be copied for storage. <code>null</code> or and emty array will reset the constraints to one <code>DimConstraint</code>
     *               with default values.
     */
    void setConstaints(DimConstraint[] constr) {
        if (constr == null || constr.length < 1) {
            constr = new DimConstraint[]{new DimConstraint()};
        }

        cList.clear();
        cList.ensureCapacity(constr.length);
        cList.addAll(Arrays.asList(constr));
    }

    /**
     * Returns the number of rows/columns that this constraints currently have.
     *
     * @return The number of rows/columns that this constraints currently have. At least 1.
     */
    public int getCount() {
        return cList.size();
    }

    // ************************************************
    // Persistence Delegate and Serializable combined.
    // ************************************************

    private Object readResolve() throws ObjectStreamException {
        return LayoutUtil.getSerializedObject(this);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        LayoutUtil.setSerializedObject(this, LayoutUtil.readAsXML(in));
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        if (getClass() == AC.class) {
            LayoutUtil.writeAsXML(out, this);
        }
    }
}