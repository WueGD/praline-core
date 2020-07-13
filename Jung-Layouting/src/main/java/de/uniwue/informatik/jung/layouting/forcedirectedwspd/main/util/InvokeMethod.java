package de.uniwue.informatik.jung.layouting.forcedirectedwspd.main.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class InvokeMethod {
	public static Object invokeMethod(String methodName, Class[] signature,
			Object[] parameters, Class targetClass, Object target) throws Exception {
		Method m = targetClass.getDeclaredMethod(methodName, signature);
		
		if (Modifier.isPrivate(m.getModifiers())) {
			m.setAccessible(true);
		}
		return m.invoke(target, parameters);
	}
}
