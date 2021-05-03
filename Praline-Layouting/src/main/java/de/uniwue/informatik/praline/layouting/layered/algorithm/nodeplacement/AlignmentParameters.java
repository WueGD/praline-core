package de.uniwue.informatik.praline.layouting.layered.algorithm.nodeplacement;

public class AlignmentParameters {
    public enum Method {
        FIRST_COMES {
            @Override
            public String toString() {
                return "firstComes";
            }
        },
        MINIMUM_INDEPENDENT_SET {
            @Override
            public String toString() {
                return "mis";
            }
        };

        public static Method string2Enum(String methodName) {
            for (Method method : Method.values()) {
                if (methodName.contains(method.toString())) {
                    return method;
                }
            }
            return null;
        }
    }

    public enum Preference {
        NOTHING  {
            @Override
            public String toString() {
                return "noPref";
            }
        },
        LONG_EDGE {
            @Override
            public String toString() {
                return "prefLongE";
            }
        };

        public static Preference string2Enum(String preferenceName) {
            for (Preference method : Preference.values()) {
                if (preferenceName.contains(method.toString())) {
                    return method;
                }
            }
            return null;
        }
    }
}
