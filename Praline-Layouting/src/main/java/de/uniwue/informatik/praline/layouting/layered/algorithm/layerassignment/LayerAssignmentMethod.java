package de.uniwue.informatik.praline.layouting.layered.algorithm.layerassignment;

public enum LayerAssignmentMethod {

    NETWORK_SIMPLEX {
        @Override
        public String toString() {
            return "ns";
        }
    },
    OLD_NETWORK_SIMPLEX {
        @Override
        public String toString() {
            return "ons";
        }
    },
    FD_POSITION {
        @Override
        public String toString() {
            return "fdp";
        }
    };

    public static LayerAssignmentMethod string2Enum(String methodName) {
        for (LayerAssignmentMethod method : LayerAssignmentMethod.values()) {
            if (methodName.contains(method.toString())) {
                return method;
            }
        }
        return null;
    }

}
