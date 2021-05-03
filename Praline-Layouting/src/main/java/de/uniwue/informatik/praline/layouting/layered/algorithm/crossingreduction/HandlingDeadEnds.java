package de.uniwue.informatik.praline.layouting.layered.algorithm.crossingreduction;

public enum HandlingDeadEnds {

    PSEUDO_BARYCENTERS {
        @Override
        public String toString() {
            return "pseudoBCs";
        }
    },
    PREV_RELATIVE_POSITIONS {
        @Override
        public String toString() {
            return "relPos";
        }
    },
    BY_OTHER_SIDE {
        @Override
        public String toString() {
            return "otherSide";
        }
    };

    public static HandlingDeadEnds string2Enum(String methodName) {
        for (HandlingDeadEnds method : HandlingDeadEnds.values()) {
            if (methodName.contains(method.toString())) {
                return method;
            }
        }
        return null;
    }

}
