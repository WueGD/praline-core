package de.uniwue.informatik.praline.datastructure;

/**
 * If you use this library for visualizing diagrams with data from outside, your diagram may contain objects having
 * some reference or ID or something similar.
 *
 * To keep track of how they are drawn here, they are {@link ReferenceObject}s, i.e., you can assign them some string
 * as a reference you may query later.
 * The methods {@link ReferenceObject#getReference()} and {@link ReferenceObject#setReference(String)} may be used by
 * the user to query and set this reference without the algorithm changing it -- so it is purely for convenience
 * reasons for the user.
 *
 * There is no need to set anything as reference, so it may just be null.
 */
public interface ReferenceObject {

    /**
     * Query the reference you may have set via {@link ReferenceObject#setReference(String)} -- see there for more.
     * This value may be zero if it was not set before.
     */
    String getReference();

    /**
     * Set a reference (e.g. an ID) to an object to identify it later.
     * This value is not changed by the algorithm, it may be set to zero.
     * It is only for the convenience of the user to save additional internal information for some object.
     *
     * @param reference
     */
    void setReference(String reference);
}
