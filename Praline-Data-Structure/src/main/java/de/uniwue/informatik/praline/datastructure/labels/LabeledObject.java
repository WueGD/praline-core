package de.uniwue.informatik.praline.datastructure.labels;

/**
 * Something that is allowed to have {@link Label}s.
 * The (arbitrarily many) {@link Label}s of this object and potentially its main label are managed by a
 * {@link LabelManager}.
 *
 * The name or ID or things like these maybe attached to an object via a {@link Label}.
 *
 * {@link LabeledObject}s are:
 * <ul>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.Vertex}</li>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.VertexGroup}</li>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.Port}</li>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.Edge}</li>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.EdgeBundle}</li>
 *     <li>{@link LeaderedLabel}</li>
 * </ul>
 */
public interface LabeledObject {

    LabelManager getLabelManager();

    boolean equalLabeling(LabeledObject otherLabeledObject);
}
