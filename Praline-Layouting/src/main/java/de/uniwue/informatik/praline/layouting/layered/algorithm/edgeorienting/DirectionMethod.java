package de.uniwue.informatik.praline.layouting.layered.algorithm.edgeorienting;

public enum DirectionMethod {

    RANDOM {
        @Override
        public String toString() {
            return "ran";
        }
    },
    FORCE {
        @Override
        public String toString() {
            return "fd";
        }
    },
    BFS {
        @Override
        public String toString() {
            return "bfs";
        }
    };

    public static DirectionMethod string2Enum(String methodName) {
        for (DirectionMethod method : DirectionMethod.values()) {
            if (methodName.contains(method.toString())) {
                return method;
            }
        }
        return null;
    }

}
