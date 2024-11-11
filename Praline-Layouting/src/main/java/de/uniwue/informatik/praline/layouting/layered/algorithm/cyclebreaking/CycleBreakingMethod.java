package de.uniwue.informatik.praline.layouting.layered.algorithm.cyclebreaking;

public enum CycleBreakingMethod {

    GREEDY {
        @Override
        public String toString() {
            return "greedy";
        }
    },
    SORT {
        @Override
        public String toString() {
            return "sort";
        }
    },
    PAGERANK {
        @Override
        public String toString() {
            return "pr";
        }
    },
    IGNORE {
        @Override
        public String toString() {
            return "ignore";
        }
    };

    public static CycleBreakingMethod string2Enum(String methodName) {
        for (CycleBreakingMethod method : CycleBreakingMethod.values()) {
            if (methodName.contains(method.toString())) {
                return method;
            }
        }
        return null;
    }

}
