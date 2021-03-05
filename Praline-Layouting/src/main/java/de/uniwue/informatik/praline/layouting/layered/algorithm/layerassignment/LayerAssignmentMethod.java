package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

public enum LayerAssignmentMethod {

    NETWORK_SIMPLEX {
        @Override
        public String toString() {
            return "ns";
        }
    },
    FD_POSITION {
        @Override
        public String toString() {
            return "fdp";
        }
    }

}
