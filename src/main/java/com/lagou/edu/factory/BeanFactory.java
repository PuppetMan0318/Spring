package com.lagou.edu.factory;

import com.alibaba.druid.util.StringUtils;
import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Component;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 应癫
 * <p>
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

	/**
	 * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
	 * 任务二：对外提供获取实例对象的接口（根据id获取）
	 */

	private static Map<String, Object> map = new HashMap<>();

	//包名
	private final static String path = "com.lagou.edu";


	static {
		//把包路径转换成资源路径
		String paths = path.replace(".", "/");
		//获取项目的绝对路径
		URL url = BeanFactory.class.getClassLoader().getResource(paths);
		//判断路径是否存在
		if (url != null) {
			//创建文件对象
			File file = new File(url.getPath());
			try {
				getFile(path, file);
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			}

			for (String s : map.keySet()) {

				Object obj = map.get(s);

				Field[] declaredFields = obj.getClass().getDeclaredFields();
				for (Field field : declaredFields) {
					field.setAccessible(true);
					if (field.isAnnotationPresent(Autowired.class)) {
						String value = field.getAnnotation(Autowired.class).value();

						Method[] methods = obj.getClass().getMethods();

						for (Method method : methods) {
							if (method.getName().equalsIgnoreCase("set" + value)) {
								try {
									method.invoke(obj, map.get(value));
								} catch (IllegalAccessException | InvocationTargetException e) {
									e.printStackTrace();
								}
							}
						}
						map.put(s, obj);

					}
				}
			}

			for (String s : map.keySet()) {
				Object obj = map.get(s);

				if (obj.getClass().isAnnotationPresent(Transactional.class)) {
					ProxyFactory proxyFactory = (ProxyFactory) getBean("proxyFactory");
					map.put(s, proxyFactory.getJdkProxy(obj));
				}
			}

		}
	}

	private static Object getType(Class<?> type) {
		return null;
	}

	private static void getFile(String path, File file) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		//判断是否是目录
		if (file.isDirectory()) {
			//获取目录下的所有子文件
			File[] listFiles = file.listFiles();
			for (File f : listFiles) {
				getFile(path, f);
			}
		} else {
			getClass(path, file);
		}
	}

	private static void getClass(String path, File f) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		//获取文件名
		String fName = f.getName();

		String filePath = f.getPath().replace("\\", ".");
		//把文件名转成包名通过反射机制创建实例
		String pName = filePath.substring(filePath.indexOf(path), filePath.lastIndexOf(".class"));
		//创建字节码对像
		Class<?> cls = Class.forName(pName);

		if (!cls.isInterface() && !cls.isEnum() && !cls.isAnnotation()) {

			Object obj;
			String id;

			//判断是否有类注解
			if (cls.isAnnotationPresent(Component.class)) {
				//获取实例id
				id = cls.getAnnotation(Component.class).value();
				if (StringUtils.isEmpty(id)) {
					//如果没有设置id就默认用首字母小写类名
					id = toFirstLowerCase(cls.getSimpleName());
				}
				obj = cls.newInstance();
				//置入容器
				map.put(id, obj);

			}

			if (cls.isAnnotationPresent(Service.class)) {
				id = cls.getAnnotation(Service.class).value();
				if (StringUtils.isEmpty(id)) {
					//如果没有设置id就默认用首字母小写类名
					id = toFirstLowerCase(cls.getSimpleName());
				}
				obj = cls.newInstance();
				//置入容器
				map.put(id, obj);
			}
		}
	}


	private static String toFirstLowerCase(String s) {
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	/**
	 * 任务二：对外提供获取实例对象的接口（根据id获取）
	 *
	 * @param id
	 */
	public static Object getBean(String id) {
		return map.get(id);
	}


	public static void main(String[] args) {
		BeanFactory beanFactory = new BeanFactory();
	}
}
