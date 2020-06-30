package de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction;

public enum CrossingMinimizationMethod {

    VERTICES {
        @Override
        public String toString() {
            return "nodes";
        }
    },
    MIXED {
        @Override
        public String toString() {
            return "mixed";
        }
    },
    PORTS {
        @Override
        public String toString() {
            return "ports";
        }
    };

    public static CrossingMinimizationMethod string2Enum(String methodName) {
        for (CrossingMinimizationMethod method : CrossingMinimizationMethod.values()) {
            if (methodName.startsWith(method.toString())) {
                return method;
            }
        }
        return null;
    }

}
