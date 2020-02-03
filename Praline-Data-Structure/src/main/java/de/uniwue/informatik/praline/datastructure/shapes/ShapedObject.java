package de.uniwue.informatik.praline.datastructure.shapes;


/**
 * Something having a {@link Shape} which is usually specified by the drawing algorithm.
 *
 * {@link ShapedObject}s are:
 * <ul>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.Vertex}</li>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.VertexGroup}</li>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.graphs.Port}</li>
 *     <li>{@link de.uniwue.informatik.praline.datastructure.labels.Label}</li>
 * </ul>
 */
public interface ShapedObject {
    Shape getShape();
    void setShape(Shape shape);
}
