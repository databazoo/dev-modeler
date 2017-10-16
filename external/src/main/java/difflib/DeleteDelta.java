/*
   Copyright 2010 Dmitry Naumenko (dm.naumenko@gmail.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package difflib;

import java.util.List;

/**
 * Describes the delete-delta between original and revised texts.
 *
 * @author <a href="dm.naumenko@gmail.com">Dmitry Naumenko</a>
 */
public class DeleteDelta extends Delta {

    /**
     * {@inheritDoc}
     *
     * @param original
     * @param revised
     */
    public DeleteDelta(Chunk original, Chunk revised) {
        super(original, revised);
    }

    @Override
    public TYPE getType() {
        return Delta.TYPE.DELETE;
    }

    @Override
    public void verify(List<?> target) throws PatchFailedException {
        getOriginal().verify(target);
    }

    @Override
    public String toString() {
        return "[DeleteDelta, position: " + getOriginal().getPosition() + ", lines: "
                + getOriginal().getLines() + "]";
    }
}
