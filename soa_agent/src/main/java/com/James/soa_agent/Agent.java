package com.James.soa_agent;

import com.James.basic.UtilsTools.JsonConvert;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by James on 16/5/25.
 * VirtualMachine 加载 jar中的agentmain 实现字节码织入,动态加载class
 *
 */
public class Agent {

    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    private static final Agent_Cache agent_cache = new Agent_Cache();
    private transient static String add_transformer_lock = "";

    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
        // Caused by: java.lang.ClassCastException: com.infogen.aop.agent.InfoGen_Agent_Advice_Class cannot be cast to com.infogen.aop.agent.InfoGen_Agent_Advice_Class
        Agent_Advice_Class agent_advice_class = null;
        synchronized (add_transformer_lock) {
            Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
            Boolean has = false;
            for (Class<?> loadedClasse : allLoadedClasses) {
                if (loadedClasse.getName().equals(agent_cache.getClass().getName())) {
                    has = true;
                    try {
                        Field field = loadedClasse.getField("class_advice_map");
                        @SuppressWarnings("unchecked")
                        Map<String, String> class_advice_map = (Map<String, String>) field.get(loadedClasse);
                        for (String infogen_advice : class_advice_map.values()) {
                            agent_advice_class = JsonConvert.toObject(infogen_advice, Agent_Advice_Class.class);
                            for (Class<?> clazz : allLoadedClasses) {
                                String class_name = agent_advice_class.getClass_name();
                                if (clazz.getName().equals(class_name)) {
                                    try {
                                        Agent_Transformer transformer = new Agent_Transformer(agent_advice_class, clazz);
                                        inst.addTransformer(transformer, true);
                                        System.out.println("重新加载class文件 -> " + class_name);
                                        inst.retransformClasses(clazz);
                                        inst.removeTransformer(transformer);
                                    } catch (Exception e) {
                                        System.out.println("重新加载class文件失败 :");
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | IOException e) {
                        System.out.println("重新加载class文件失败 :");
                        e.printStackTrace();
                        help();
                    }
                }
            }
            if (!has) {
                System.out.println("没有找到" + agent_cache.getClass().getName() + "对象");
            }
        }

    }

    public static void help() {
        System.out.println("eg -> ");
        System.out.println("AOP.getInstance().add_advice_method(Authc.class, new InfoGen_AOP_Handle_Authc());");
        System.out.println("AOP.getInstance().add_autowired_field(\"com.James.service.Signup\", \"user_dao\", \"com.James.service.Signup.dao.User_DAO_Impl();\");");
        System.out.println("AOP.getInstance().advice();");
    }
}
